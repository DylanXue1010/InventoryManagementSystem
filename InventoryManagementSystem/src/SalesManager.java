// SalesManager.java
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class SalesManager {
    private List<Sale> salesList;
    private Inventory inventory; // To update stock upon sale finalization

    // Unified data directory path
    public static final String DATA_DIRECTORY = "data/";
    public static final String DEFAULT_SALES_FILE_PATH = DATA_DIRECTORY + "sales.csv";
    public static final String DEFAULT_SALE_ITEMS_FILE_PATH = DATA_DIRECTORY + "sale_items.csv";

    private static final String SALES_CSV_HEADER = "SaleID,SaleDate,TotalAmount,Status";
    private static final String SALE_ITEMS_CSV_HEADER = "SaleID,ItemSKU,ItemName,QuantitySold,PriceAtSale";

    public SalesManager(Inventory inventory) {
        this.inventory = inventory;
        this.salesList = new ArrayList<>();
        loadSalesFromFile();
    }

    public Sale createNewSale() {
        Sale newSale = new Sale();
        this.salesList.add(newSale);
        System.out.println("New sale created with ID: " + newSale.getSaleID() + " (Status: " + newSale.getStatus() + ")");
        return newSale;
    }

    public boolean finalizeSale(Sale sale) {
        if (sale == null) {
            System.err.println("Cannot finalize a null sale.");
            return false;
        }
        // The finalizeSale method in Sale object itself updates inventory and status to COMPLETED
        boolean success = sale.finalizeSale(this.inventory);
        if (success) {
            System.out.println("Sale " + sale.getSaleID() + " successfully finalized by SalesManager. Status: " + sale.getStatus());
        } else {
            System.err.println("SalesManager: Finalization failed for sale " + sale.getSaleID() + ". Check logs or sale status (" + sale.getStatus() + ").");
        }
        return success;
    }

    public Optional<Sale> getSaleById(String saleId) {
        if (saleId == null || saleId.trim().isEmpty()) {
            return Optional.empty();
        }
        if (this.salesList == null) {
            return Optional.empty();
        }
        return this.salesList.stream()
                .filter(sale -> sale != null && sale.getSaleID() != null && sale.getSaleID().equals(saleId))
                .findFirst();
    }

    public List<Sale> getAllSales() {
        if (this.salesList == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(salesList); // Return a copy
    }

    public List<Sale> getCompletedSalesByDateRange(LocalDate startDate, LocalDate endDate) {
        if (this.salesList == null) {
            return new ArrayList<>();
        }
        if (startDate == null || endDate == null) {
            System.err.println("SalesManager Error: Start date or end date cannot be null for date range search.");
            return new ArrayList<>();
        }
        if (endDate.isBefore(startDate)) {
            System.err.println("SalesManager Error: End date cannot be before start date for date range search.");
            return new ArrayList<>();
        }

        return this.salesList.stream()
                .filter(sale -> sale != null && Sale.STATUS_COMPLETED.equals(sale.getStatus()))
                .filter(sale -> {
                    if (sale.getSaleDate() == null) return false;
                    Instant instant = sale.getSaleDate().toInstant();
                    ZoneId zoneId = ZoneId.systemDefault(); // Consider a fixed ZoneId like ZoneId.of("UTC") for consistency
                    LocalDate saleLocalDate = instant.atZone(zoneId).toLocalDate();
                    return !saleLocalDate.isBefore(startDate) && !saleLocalDate.isAfter(endDate);
                })
                .collect(Collectors.toList());
    }

    public List<Sale> getCompletedSalesByDate(LocalDate specificDate) {
        if (specificDate == null) {
            System.err.println("SalesManager Error: Specific date cannot be null for single date search.");
            return new ArrayList<>();
        }
        return getCompletedSalesByDateRange(specificDate, specificDate);
    }


    public void loadSalesFromFile() {
        Map<String, Sale> loadedSalesMap = new HashMap<>();
        File salesFile = new File(DEFAULT_SALES_FILE_PATH);
        File saleItemsFile = new File(DEFAULT_SALE_ITEMS_FILE_PATH);

        File dataDir = new File(DATA_DIRECTORY);
        if (!dataDir.exists()) {
            if (!dataDir.mkdirs()){
                System.err.println("Could not create data directory: " + DATA_DIRECTORY);
            }
        }

        if (salesFile.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(salesFile))) {
                String line = br.readLine();
                if (line == null || !line.trim().equalsIgnoreCase(SALES_CSV_HEADER)) {
                    System.err.println("Warning: sales.csv header mismatch or file empty. Expected: " + SALES_CSV_HEADER + ". Got: " + (line != null ? line.trim() : "null"));
                } else {
                    while ((line = br.readLine()) != null) {
                        if (line.trim().isEmpty() || line.startsWith("#")) continue;
                        String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                        if (parts.length < 4) {
                            System.err.println("Skipping invalid line in sales.csv (not enough parts): " + line); continue;
                        }
                        try {
                            String saleID = Sale.unescapeCsv(parts[0]);
                            Date saleDate = Sale.parseIsoDateString(Sale.unescapeCsv(parts[1]));
                            double totalAmount = Double.parseDouble(Sale.unescapeCsv(parts[2]));
                            String status = Sale.unescapeCsv(parts[3]);
                            if (saleDate == null) {
                                System.err.println("Skipping sale " + saleID + " due to invalid date in sales.csv: " + line); continue;
                            }
                            // Only load Completed or Cancelled sales from file. Pending sales should not be in the file.
                            if (Sale.STATUS_COMPLETED.equals(status) || Sale.STATUS_CANCELLED.equals(status)) {
                                Sale sale = new Sale(saleID, saleDate, totalAmount, status);
                                loadedSalesMap.put(saleID, sale);
                            } else {
                                System.out.println("SalesManager Load: Skipping sale " + saleID + " with status '" + status + "' from sales.csv. Only loading Completed or Cancelled.");
                            }
                        } catch (NumberFormatException e) {
                            System.err.println("Error parsing numeric value from sales.csv line: '" + line + "' - " + e.getMessage());
                        } catch (Exception e) {
                            System.err.println("Error processing line from sales.csv: '" + line + "' - " + e.getMessage());
                        }
                    }
                }
            } catch (IOException e) { System.err.println("Error loading sales from " + DEFAULT_SALES_FILE_PATH + ": " + e.getMessage()); }
        } else { System.out.println(DEFAULT_SALES_FILE_PATH + " not found. No sales loaded."); }

        if (saleItemsFile.exists() && !loadedSalesMap.isEmpty()) {
            try (BufferedReader br = new BufferedReader(new FileReader(saleItemsFile))) {
                String line = br.readLine();
                if (line == null || !line.trim().equalsIgnoreCase(SALE_ITEMS_CSV_HEADER)) {
                    System.err.println("Warning: sale_items.csv header mismatch or file empty. Expected: " + SALE_ITEMS_CSV_HEADER + ". Got: " + (line != null ? line.trim() : "null"));
                } else {
                    while ((line = br.readLine()) != null) {
                        if (line.trim().isEmpty() || line.startsWith("#")) continue;
                        String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                        if (parts.length < 5) {
                            System.err.println("Skipping invalid line in sale_items.csv (not enough parts): " + line); continue;
                        }
                        try {
                            String saleID = Sale.unescapeCsv(parts[0]);
                            Sale targetSale = loadedSalesMap.get(saleID); // Will only be non-null if sale was Completed/Cancelled
                            if (targetSale != null) {
                                String[] itemParts = new String[parts.length - 1];
                                System.arraycopy(parts, 1, itemParts, 0, parts.length - 1);
                                Sale.SaleItem saleItem = Sale.SaleItem.fromCsvString(itemParts);
                                if (saleItem != null) {
                                    targetSale.addLoadedSaleItem(saleItem);
                                }
                            }
                            // No "else" needed here, as if targetSale is null, it means the sale header was not loaded (e.g. was Pending)
                        } catch (Exception e) {
                            System.err.println("Error processing line from sale_items.csv: '" + line + "' - " + e.getMessage());
                        }
                    }
                }
            } catch (IOException e) { System.err.println("Error loading sale items from " + DEFAULT_SALE_ITEMS_FILE_PATH + ": " + e.getMessage()); }
        } else if (!loadedSalesMap.isEmpty()) {
            System.out.println(DEFAULT_SALE_ITEMS_FILE_PATH + " not found, but some sales headers were loaded. Sale items might be missing for them.");
        }

        this.salesList.clear();
        for (Sale sale : loadedSalesMap.values()) {
            if (!sale.getItemsSold().isEmpty()) {
                sale.refreshTotalAmountFromItems();
            }
            this.salesList.add(sale);
        }
        System.out.println(this.salesList.size() + " sales (Completed or Cancelled) processed and loaded into SalesManager.");
    }

    public void saveSalesToFile() {
        File salesFile = new File(DEFAULT_SALES_FILE_PATH);
        File salesDir = salesFile.getParentFile();
        if (salesDir != null && !salesDir.exists()) {
            if (!salesDir.mkdirs()) {
                System.err.println("Could not create directory for sales file: " + salesDir.getPath());
                return;
            }
        }
        File saleItemsFile = new File(DEFAULT_SALE_ITEMS_FILE_PATH);

        // Filter out PENDING sales before saving
        List<Sale> salesToSave = new ArrayList<>();
        if (this.salesList != null) {
            for (Sale sale : this.salesList) {
                if (sale != null &&
                        (Sale.STATUS_COMPLETED.equals(sale.getStatus()) || Sale.STATUS_CANCELLED.equals(sale.getStatus()))) {
                    salesToSave.add(sale);
                } else if (sale != null && Sale.STATUS_PENDING.equals(sale.getStatus())) {
                    System.out.println("SalesManager Save: Skipping PENDING Sale ID: " + sale.getSaleID() + ". It will not be saved to CSV.");
                }
            }
        }

        try (PrintWriter salesOut = new PrintWriter(new BufferedWriter(new FileWriter(salesFile)))) {
            salesOut.println(SALES_CSV_HEADER);
            for (Sale sale : salesToSave) { // Use the filtered list
                salesOut.println(sale.toSaleCsvString());
            }
            System.out.println(salesToSave.size() + " sales records (Completed or Cancelled) saved to " + DEFAULT_SALES_FILE_PATH);
        } catch (IOException e) {
            System.err.println("Error saving sales to " + DEFAULT_SALES_FILE_PATH + ": " + e.getMessage());
            e.printStackTrace();
        }

        try (PrintWriter itemsOut = new PrintWriter(new BufferedWriter(new FileWriter(saleItemsFile)))) {
            itemsOut.println(SALE_ITEMS_CSV_HEADER);
            for (Sale sale : salesToSave) { // Use the filtered list for items as well
                List<Sale.SaleItem> itemsSold = sale.getItemsSold();
                if (itemsSold != null) {
                    for (Sale.SaleItem si : itemsSold) {
                        itemsOut.println(Sale.escapeCsv(sale.getSaleID()) + "," + si.toCsvString());
                    }
                }
            }
            System.out.println("Sale items for (Completed or Cancelled) sales saved to " + DEFAULT_SALE_ITEMS_FILE_PATH);
        } catch (IOException e) {
            System.err.println("Error saving sale items to " + DEFAULT_SALE_ITEMS_FILE_PATH + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        System.out.println("Testing SalesManager (data in " + DATA_DIRECTORY + ")...");
        Inventory testInv = new Inventory();
        if (testInv.getItem("BOOK001") == null) {
            testInv.addItem(new Item("BOOK001", "Java Programming Guide", "Books", 20, 49.99, "SUP009", "Active"));
        }
        if (testInv.getItem("PEN001") == null) {
            testInv.addItem(new Item("PEN001", "Premium Ballpoint Pen", "Stationery", 100, 1.99, "SUP012", "Active"));
        }
        // testInv.saveItemsToFile(Inventory.DEFAULT_ITEMS_FILE_PATH); // Only save if inventory was modified

        SalesManager sm = new SalesManager(testInv);

        System.out.println("Initial sales in manager after load: " + sm.getAllSales().size());
        for(Sale s : sm.getAllSales()){
            System.out.println("Loaded Sale: ID " + s.getSaleID() + ", Status " + s.getStatus());
        }


        // Test creating a PENDING sale (should not be saved if app closes now)
        Sale pendingSale = sm.createNewSale(); // Status is PENDING
        Item book = testInv.getItem("BOOK001");
        if (book != null) pendingSale.addItemToSale(book, 1, 40.00);
        System.out.println("Created a PENDING sale: " + pendingSale.getSaleID());

        // Test creating and COMPLETING a sale
        Sale completedSale = sm.createNewSale();
        Item pen = testInv.getItem("PEN001");
        if (pen != null) completedSale.addItemToSale(pen, 3, 1.75);
        sm.finalizeSale(completedSale); // Status becomes COMPLETED
        System.out.println("Created and FINALIZED a sale: " + completedSale.getSaleID());

        // Test creating and CANCELLING a sale
        Sale cancelledSale = sm.createNewSale();
        if (book != null) cancelledSale.addItemToSale(book, 2, 42.00);
        cancelledSale.setStatus(Sale.STATUS_CANCELLED); // Manually set to Cancelled
        System.out.println("Created and CANCELLED a sale: " + cancelledSale.getSaleID());

        System.out.println("\nTotal sales in manager before explicit save: " + sm.getAllSales().size());
        for(Sale s : sm.getAllSales()){
            System.out.println("In Memory Sale: ID " + s.getSaleID() + ", Status " + s.getStatus());
        }

        System.out.println("\nSimulating end of program: Saving sales data via SalesManager...");
        sm.saveSalesToFile();
        // After this, only completedSale and cancelledSale (and any previously loaded Completed/Cancelled sales) should be in the CSV.
        // pendingSale should not be.

        System.out.println("\n--- Reloading SalesManager to verify what was saved ---");
        SalesManager sm2 = new SalesManager(testInv); // This will load from CSV
        System.out.println("Sales loaded by sm2 after save: " + sm2.getAllSales().size());
        boolean pendingFound = false;
        boolean completedFound = false;
        boolean cancelledFound = false;
        for(Sale s : sm2.getAllSales()){
            System.out.println("Reloaded Sale: ID " + s.getSaleID() + ", Status " + s.getStatus());
            if(s.getSaleID().equals(pendingSale.getSaleID())) pendingFound = true;
            if(s.getSaleID().equals(completedSale.getSaleID())) completedFound = true;
            if(s.getSaleID().equals(cancelledSale.getSaleID())) cancelledFound = true;
        }
        if(pendingFound){
            System.err.println("ERROR: PENDING sale was found in CSV after reload!");
        } else {
            System.out.println("SUCCESS: PENDING sale was NOT found in CSV after reload, as expected.");
        }
        if(completedFound){
            System.out.println("SUCCESS: COMPLETED sale was found in CSV after reload, as expected.");
        } else {
            System.err.println("ERROR: COMPLETED sale was NOT found in CSV after reload!");
        }
        if(cancelledFound){
            System.out.println("SUCCESS: CANCELLED sale was found in CSV after reload, as expected.");
        } else {
            System.err.println("ERROR: CANCELLED sale was NOT found in CSV after reload!");
        }

        System.out.println("\nSalesManager test finished.");
    }
}
