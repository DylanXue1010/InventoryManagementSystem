// SalesReturnManager.java
import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class SalesReturnManager {
    private List<SalesReturn> salesReturnList;
    private Inventory inventory; // To update stock
    private SalesManager salesManager; // To find original sales

    //统一数据目录路径
    public static final String DATA_DIRECTORY = "data/";
    public static final String DEFAULT_SALES_RETURNS_FILE_PATH = DATA_DIRECTORY + "sales_returns.csv";
    public static final String DEFAULT_SALES_RETURN_ITEMS_FILE_PATH = DATA_DIRECTORY + "sales_return_items.csv";

    private static final String RETURNS_CSV_HEADER = "returnID,originalSaleID,returnDate,totalRefundAmount,status,customerNotes";
    private static final String RETURN_ITEMS_CSV_HEADER = "returnID,itemSKU,itemName,returnedQuantity,unitPriceAtSale,condition,reason";

    public SalesReturnManager(Inventory inventory, SalesManager salesManager) {
        this.inventory = inventory;
        this.salesManager = salesManager;
        this.salesReturnList = new ArrayList<>();
        loadSalesReturnsFromFile();
    }

    public SalesReturn createNewSalesReturn(String originalSaleID) {
        Optional<Sale> originalSaleOpt = salesManager.getSaleById(originalSaleID);
        if (originalSaleOpt.isEmpty()) {
            System.err.println("Cannot create return: Original Sale ID '" + originalSaleID + "' not found.");
            return null;
        }
        // Further checks: is the sale completed?
        Sale originalSale = originalSaleOpt.get();
        if (!Sale.STATUS_COMPLETED.equals(originalSale.getStatus())) {
            System.err.println("Cannot create return: Original Sale ID '" + originalSaleID + "' is not completed. Status: " + originalSale.getStatus());
            return null;
        }


        SalesReturn newReturn = new SalesReturn(originalSaleID);
        this.salesReturnList.add(newReturn);
        System.out.println("New Sales Return created: " + newReturn.getReturnID() + " for Original Sale: " + originalSaleID);
        return newReturn;
    }

    public Optional<SalesReturn> getSalesReturnById(String returnId) {
        return salesReturnList.stream().filter(sr -> sr.getReturnID().equals(returnId)).findFirst();
    }

    public List<SalesReturn> getAllSalesReturns() {
        return new ArrayList<>(salesReturnList);
    }

    public List<SalesReturn> getSalesReturnsByStatus(String status) {
        return salesReturnList.stream()
                .filter(sr -> sr.getStatus().equalsIgnoreCase(status))
                .collect(Collectors.toList());
    }

    public boolean processReturnInventoryUpdate(SalesReturn salesReturn) {
        if (salesReturn == null || !salesReturn.getStatus().equals(SalesReturn.STATUS_APPROVED)) {
            System.err.println("Return cannot be processed or is not in an approvable state for inventory update. SR_ID: " + (salesReturn != null ? salesReturn.getReturnID() : "null"));
            return false;
        }

        System.out.println("Processing inventory update for Sales Return: " + salesReturn.getReturnID());
        boolean allItemsProcessedSuccessfully = true;

        for (SalesReturnItem sri : salesReturn.getReturnedItems()) {
            Item inventoryItem = inventory.getItem(sri.getItemSKU());
            if (inventoryItem == null) {
                System.err.println("  Error: Item SKU '" + sri.getItemSKU() + "' not found in inventory. Cannot update stock for this item.");
                allItemsProcessedSuccessfully = false;
                continue;
            }

            if (SalesReturnItem.CONDITION_RESELLABLE.equalsIgnoreCase(sri.getCondition())) {
                inventoryItem.updateQuantity(sri.getReturnedQuantity());
                System.out.println("  SKU " + sri.getItemSKU() + ": +" + sri.getReturnedQuantity() + " (Resellable). New Qty: " + inventoryItem.getQuantity());
            } else if (SalesReturnItem.CONDITION_DAMAGED.equalsIgnoreCase(sri.getCondition()) ||
                    SalesReturnItem.CONDITION_DEFECTIVE.equalsIgnoreCase(sri.getCondition())) {
                // inventoryItem.setStatus(sri.getCondition()); // This might be too simplistic if an item has multiple damages
                // For now, we just log it. A more complex system might have separate stock for damaged goods
                // or specific workflows for handling them.
                System.out.println("  SKU " + sri.getItemSKU() + ": " + sri.getReturnedQuantity() + " units returned as '" + sri.getCondition() + "'. Active stock quantity unchanged, status of this SKU in inventory might need manual review or specific handling based on business rules.");
            } else {
                System.out.println("  SKU " + sri.getItemSKU() + ": " + sri.getReturnedQuantity() + " units with condition '" + sri.getCondition() + "'. No specific inventory action defined for this condition.");
            }
        }
        salesReturn.setStatus(SalesReturn.STATUS_COMPLETED);
        System.out.println("Sales Return " + salesReturn.getReturnID() + " processed and status set to " + SalesReturn.STATUS_COMPLETED);
        return allItemsProcessedSuccessfully;
    }

    public boolean updateSalesReturnStatus(String returnId, String newStatus) {
        Optional<SalesReturn> returnOpt = getSalesReturnById(returnId);
        if (returnOpt.isPresent()) {
            SalesReturn sr = returnOpt.get();
            sr.setStatus(newStatus);
            System.out.println("Sales Return " + returnId + " status updated to " + newStatus);
            return true;
        }
        System.err.println("Failed to update status: Sales Return " + returnId + " not found.");
        return false;
    }

    public void loadSalesReturnsFromFile() {
        Map<String, SalesReturn> loadedReturnsMap = new HashMap<>();
        File returnsFile = new File(DEFAULT_SALES_RETURNS_FILE_PATH);
        File returnItemsFile = new File(DEFAULT_SALES_RETURN_ITEMS_FILE_PATH);

        File dataDir = new File(DATA_DIRECTORY);
        if (!dataDir.exists()) {
            if (!dataDir.mkdirs()){
                System.err.println("Failed to create data directory: " + DATA_DIRECTORY + " while loading sales returns.");
                // If data dir cannot be created, subsequent file operations will likely fail.
            }
        }

        if (returnsFile.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(returnsFile))) {
                String line = br.readLine();
                if (line == null || !line.trim().equalsIgnoreCase(RETURNS_CSV_HEADER)) {
                    System.err.println("Warning: sales_returns.csv header mismatch or empty. Expected: " + RETURNS_CSV_HEADER);
                } else {
                    while ((line = br.readLine()) != null) {
                        if (line.trim().isEmpty() || line.startsWith("#")) continue;
                        String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                        if (parts.length < 6) {
                            System.err.println("Skipping invalid line in sales_returns.csv: " + line); continue;
                        }
                        try {
                            String returnID = SalesReturn.unescapeCsv(parts[0]);
                            String originalSaleID = SalesReturn.unescapeCsv(parts[1]);
                            Date returnDate = SalesReturn.parseIsoDateString(SalesReturn.unescapeCsv(parts[2]));
                            double totalRefund = Double.parseDouble(SalesReturn.unescapeCsv(parts[3]));
                            String status = SalesReturn.unescapeCsv(parts[4]);
                            String notes = SalesReturn.unescapeCsv(parts[5]);
                            if (returnDate == null) { System.err.println("Skipping return " + returnID + " due to invalid date."); continue; }
                            loadedReturnsMap.put(returnID, new SalesReturn(returnID, originalSaleID, returnDate, totalRefund, status, notes));
                        } catch (Exception e) { System.err.println("Error processing line from sales_returns.csv: " + line + " - " + e.getMessage()); }
                    }
                }
            } catch (IOException e) { System.err.println("Error loading " + DEFAULT_SALES_RETURNS_FILE_PATH + ": " + e.getMessage()); }
        } else { System.out.println(DEFAULT_SALES_RETURNS_FILE_PATH + " not found. No sales returns loaded."); }

        if (returnItemsFile.exists() && !loadedReturnsMap.isEmpty()) {
            try (BufferedReader br = new BufferedReader(new FileReader(returnItemsFile))) {
                String line = br.readLine();
                if (line == null || !line.trim().equalsIgnoreCase(RETURN_ITEMS_CSV_HEADER)) {
                    System.err.println("Warning: sales_return_items.csv header mismatch or empty. Expected: " + RETURN_ITEMS_CSV_HEADER);
                } else {
                    while ((line = br.readLine()) != null) {
                        if (line.trim().isEmpty() || line.startsWith("#")) continue;
                        String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                        if (parts.length < 7) {
                            System.err.println("Skipping invalid line in sales_return_items.csv: " + line); continue;
                        }
                        try {
                            String returnID = SalesReturn.unescapeCsv(parts[0]);
                            SalesReturn targetReturn = loadedReturnsMap.get(returnID);
                            if (targetReturn != null) {
                                String[] itemParts = new String[parts.length - 1];
                                System.arraycopy(parts, 1, itemParts, 0, parts.length - 1);
                                SalesReturnItem sri = SalesReturnItem.fromCsvParts(itemParts);
                                if (sri != null) targetReturn.addLoadedReturnItem(sri);
                            } else { System.err.println("Warning: SalesReturnItem for non-existent ReturnID " + returnID + " in " + DEFAULT_SALES_RETURN_ITEMS_FILE_PATH); }
                        } catch (Exception e) { System.err.println("Error processing line from sales_return_items.csv: " + line + " - " + e.getMessage());}
                    }
                }
            } catch (IOException e) { System.err.println("Error loading " + DEFAULT_SALES_RETURN_ITEMS_FILE_PATH + ": " + e.getMessage());}
        }

        this.salesReturnList.clear();
        for (SalesReturn sr : loadedReturnsMap.values()) {
            sr.calculateTotalRefundAmount();
            this.salesReturnList.add(sr);
        }
        System.out.println(this.salesReturnList.size() + " sales returns processed and loaded.");
    }

    public void saveSalesReturnsToFile() {
        File returnsFile = new File(DEFAULT_SALES_RETURNS_FILE_PATH);
        File parentDir = returnsFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                System.err.println("Could not create directory: " + parentDir.getPath());
                return;
            }
        }

        try (PrintWriter returnsOut = new PrintWriter(new BufferedWriter(new FileWriter(returnsFile)))) {
            returnsOut.println(RETURNS_CSV_HEADER);
            for (SalesReturn sr : this.salesReturnList) {
                returnsOut.println(sr.toSalesReturnCsvString());
            }
            System.out.println(this.salesReturnList.size() + " sales returns saved to " + DEFAULT_SALES_RETURNS_FILE_PATH);
        } catch (IOException e) { System.err.println("Error saving " + DEFAULT_SALES_RETURNS_FILE_PATH + ": " + e.getMessage()); }

        File returnItemsFile = new File(DEFAULT_SALES_RETURN_ITEMS_FILE_PATH);
        try (PrintWriter itemsOut = new PrintWriter(new BufferedWriter(new FileWriter(returnItemsFile)))) {
            itemsOut.println(RETURN_ITEMS_CSV_HEADER);
            for (SalesReturn sr : this.salesReturnList) {
                for (SalesReturnItem sri : sr.getReturnedItems()) {
                    itemsOut.println(SalesReturn.escapeCsv(sr.getReturnID()) + "," + sri.toCsvString());
                }
            }
            System.out.println("Sales return items saved to " + DEFAULT_SALES_RETURN_ITEMS_FILE_PATH);
        } catch (IOException e) { System.err.println("Error saving " + DEFAULT_SALES_RETURN_ITEMS_FILE_PATH + ": " + e.getMessage()); }
    }

    // Main for testing
    public static void main(String[] args) {
        System.out.println("Testing SalesReturnManager (data in " + DATA_DIRECTORY + ")...");

        // Setup Inventory
        Inventory inv = new Inventory(); // Will load from data/items.csv or create if not exists
        if (inv.getItem("BOOK001") == null) inv.addItem(new Item("BOOK001", "Java Book", "Books", 10, 50.00, "SUP001", "Active"));
        if (inv.getItem("PEN001") == null) inv.addItem(new Item("PEN001", "Blue Pen", "Stationery", 100, 1.00, "SUP002", "Active"));
        inv.saveItemsToFile(Inventory.DEFAULT_ITEMS_FILE_PATH); // Save if items were added/modified

        // Setup SalesManager and ensure a test sale exists
        SalesManager sm = new SalesManager(inv); // Will load from data/sales.csv
        String saleIdToUseForReturn = "SALE-FOR-RETURN-PURPOSES-001"; // A predictable ID for testing

        Optional<Sale> saleToReturnOpt = sm.getSaleById(saleIdToUseForReturn);

        if (saleToReturnOpt.isEmpty()) {
            System.out.println("Test sale (ID: " + saleIdToUseForReturn + ") not found by SalesManager. Creating a new one for this test session.");

            Sale testSale = sm.createNewSale(); // This will generate a NEW ID.
            saleIdToUseForReturn = testSale.getSaleID(); // CRITICAL: Use the actual auto-generated ID for the return test
            System.out.println("Created a new sale with auto-generated ID for return testing: " + saleIdToUseForReturn);

            Item book = inv.getItem("BOOK001");
            if (book != null) {
                testSale.addItemToSale(book, 2, 45.00);
            }
            Item pen = inv.getItem("PEN001");
            if (pen != null) {
                testSale.addItemToSale(pen, 5, 0.90);
            }

            boolean finalized = sm.finalizeSale(testSale);

            if (finalized) {
                System.out.println("Newly created test sale (ID: " + saleIdToUseForReturn + ") finalized successfully.");
                sm.saveSalesToFile();
                inv.saveItemsToFile(Inventory.DEFAULT_ITEMS_FILE_PATH);
            } else {
                System.err.println("CRITICAL TEST SETUP ERROR: Failed to finalize newly created sale " + saleIdToUseForReturn);
                return;
            }
            saleToReturnOpt = sm.getSaleById(saleIdToUseForReturn); // Re-fetch to ensure it's in SalesManager
            if(saleToReturnOpt.isEmpty()){
                System.err.println("CRITICAL TEST SETUP ERROR: Newly created and finalized sale " + saleIdToUseForReturn + " could not be re-fetched from SalesManager.");
                return;
            }
        } else {
            System.out.println("Found existing test sale for return with ID: " + saleIdToUseForReturn);
            Sale existingSale = saleToReturnOpt.get();
            if (!Sale.STATUS_COMPLETED.equals(existingSale.getStatus())) {
                System.out.println("Existing test sale " + saleIdToUseForReturn + " is not completed. Attempting to finalize for test.");
                if(Sale.STATUS_PENDING.equals(existingSale.getStatus())){
                    boolean finalized = sm.finalizeSale(existingSale);
                    if(finalized){
                        System.out.println("Finalized existing pending sale " + saleIdToUseForReturn);
                        sm.saveSalesToFile();
                        inv.saveItemsToFile(Inventory.DEFAULT_ITEMS_FILE_PATH);
                    } else {
                        System.err.println("Failed to finalize existing pending sale " + saleIdToUseForReturn + ". Return test may fail.");
                    }
                } else {
                    System.out.println("Existing test sale " + saleIdToUseForReturn + " status is " + existingSale.getStatus() + ". Return test may fail if not completed.");
                }
            }
        }

        // Now proceed with SalesReturnManager testing
        SalesReturnManager srm = new SalesReturnManager(inv, sm);

        // **Declare a new final variable for use in lambda**
        final String finalSaleIdForReturn = saleIdToUseForReturn;

        if (srm.getAllSalesReturns().stream().noneMatch(sr -> sr.getOriginalSaleID().equals(finalSaleIdForReturn))) {
            System.out.println("No sales returns loaded for sale ID " + finalSaleIdForReturn + ". Creating a test return.");
            SalesReturn testReturn = srm.createNewSalesReturn(finalSaleIdForReturn);

            if (testReturn != null) {
                Item bookToReturn = inv.getItem("BOOK001");
                Item penToReturn = inv.getItem("PEN001");

                if (bookToReturn != null) {
                    testReturn.addReturnItem(new SalesReturnItem("BOOK001", bookToReturn.getName(), 1, 45.00, SalesReturnItem.CONDITION_RESELLABLE, "Customer changed mind"));
                } else {
                    System.out.println("INFO: BOOK001 not in inventory, cannot add to return item test.");
                }
                if (penToReturn != null) {
                    testReturn.addReturnItem(new SalesReturnItem("PEN001", penToReturn.getName(), 2, 0.90, SalesReturnItem.CONDITION_DAMAGED, "Broken casing"));
                } else {
                    System.out.println("INFO: PEN001 not in inventory, cannot add to return item test.");
                }

                if (!testReturn.getReturnedItems().isEmpty()) {
                    testReturn.setStatus(SalesReturn.STATUS_APPROVED);
                    srm.processReturnInventoryUpdate(testReturn);
                    srm.saveSalesReturnsToFile();
                    inv.saveItemsToFile(Inventory.DEFAULT_ITEMS_FILE_PATH);
                    System.out.println("Test return " + testReturn.getReturnID() + " created, processed, and saved.");
                } else {
                    System.out.println("No items could be added to the test return. Return not processed.");
                }
            } else {
                System.out.println("Could not create test return for sale ID '" + finalSaleIdForReturn + "'. Original sale might not be 'Completed' or other issue.");
            }
        } else {
            System.out.println("Loaded " + srm.getAllSalesReturns().size() + " sales returns. Found existing return for sale ID " + finalSaleIdForReturn);
        }

        Item bookAfterReturn = inv.getItem("BOOK001");
        if (bookAfterReturn != null) {
            System.out.println("\nFinal inventory for BOOK001: " + bookAfterReturn.getQuantity() + " (Status: " + bookAfterReturn.getStatus() + ")");
        }
        Item penAfterReturn = inv.getItem("PEN001");
        if (penAfterReturn != null) {
            System.out.println("Final inventory for PEN001: " + penAfterReturn.getQuantity() + " (Status: " + penAfterReturn.getStatus() + ")");
        }
        System.out.println("\nSalesReturnManager test finished.");
    }
}