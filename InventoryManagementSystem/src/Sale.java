import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.Iterator;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.ZoneOffset;

// 假设 Item.java 和 Inventory.java 在同一个包或已正确导入
// 如果您的 Item 和 Inventory 类在特定包下，例如 com.example.inventory:
// import com.example.inventory.Item;
// import com.example.inventory.Inventory;


public class Sale {

    // SaleItem Inner Class
    public static class SaleItem {
        private String sku;
        private String itemName;
        private int quantitySold;
        private double priceAtSale;
        private double subtotal; // Calculated: quantitySold * priceAtSale

        // Constructor
        public SaleItem(String sku, String itemName, int quantitySold, double priceAtSale) {
            if (quantitySold <= 0) {
                throw new IllegalArgumentException("Quantity sold must be positive for SKU: " + sku);
            }
            if (priceAtSale < 0) {
                throw new IllegalArgumentException("Price at sale cannot be negative for SKU: " + sku);
            }
            this.sku = sku;
            this.itemName = itemName;
            this.quantitySold = quantitySold;
            this.priceAtSale = priceAtSale;
            this.recalculateSubtotal(); // Use new method to initialize subtotal
        }

        // Getters
        public String getSku() { return sku; }
        public String getItemName() { return itemName; }
        public int getQuantitySold() { return quantitySold; }
        public double getPriceAtSale() { return priceAtSale; }
        public double getSubtotal() { return subtotal; }

        // --- MODIFIED/NEW: Setters and Recalculate Subtotal ---
        public void setQuantitySold(int quantitySold) {
            if (quantitySold <= 0) {
                System.err.println("Error: Quantity sold must be positive. Value not changed for SKU: " + this.sku);
                // Optionally throw an IllegalArgumentException here if preferred
                // throw new IllegalArgumentException("Quantity sold must be positive.");
                return;
            }
            this.quantitySold = quantitySold;
            this.recalculateSubtotal();
        }

        public void setPriceAtSale(double priceAtSale) {
            if (priceAtSale < 0) {
                System.err.println("Error: Price at sale cannot be negative. Value not changed for SKU: " + this.sku);
                // Optionally throw an IllegalArgumentException here
                // throw new IllegalArgumentException("Price at sale cannot be negative.");
                return;
            }
            this.priceAtSale = priceAtSale;
            this.recalculateSubtotal();
        }

        private void recalculateSubtotal() {
            this.subtotal = this.quantitySold * this.priceAtSale;
        }
        // --- END MODIFIED/NEW ---

        @Override
        public String toString() {
            DecimalFormat df = new DecimalFormat("#0.00", new DecimalFormatSymbols(Locale.US));
            return "  - SKU: " + sku + ", Name: " + itemName +
                    ", Qty: " + quantitySold + ", Price: $" + df.format(priceAtSale) +
                    ", Subtotal: $" + df.format(subtotal);
        }

        public String toCsvString() {
            DecimalFormat df = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US));
            return String.join(",",
                    Sale.escapeCsv(sku),
                    Sale.escapeCsv(itemName),
                    String.valueOf(quantitySold),
                    df.format(priceAtSale));
        }

        public static SaleItem fromCsvString(String[] parts) {
            if (parts.length < 4) {
                System.err.println("Invalid CSV parts for SaleItem: not enough parts.");
                return null;
            }
            try {
                String itemSku = Sale.unescapeCsv(parts[0]);
                String itemName = Sale.unescapeCsv(parts[1]);
                int quantitySold = Integer.parseInt(Sale.unescapeCsv(parts[2]));
                double priceAtSale = Double.parseDouble(Sale.unescapeCsv(parts[3]));
                return new SaleItem(itemSku, itemName, quantitySold, priceAtSale);
            } catch (NumberFormatException e) {
                System.err.println("Error parsing numeric value for SaleItem from CSV: " + e.getMessage());
                return null;
            } catch (ArrayIndexOutOfBoundsException e) {
                System.err.println("Error processing CSV parts for SaleItem: " + e.getMessage());
                return null;
            }
        }
    }
    // End of SaleItem inner class

    // Sale Class Attributes
    private String saleID;
    private Date saleDate;
    private List<SaleItem> itemsSold;
    private double totalAmount;
    private String status;

    public static final String STATUS_PENDING = "Pending";
    public static final String STATUS_COMPLETED = "Completed";
    public static final String STATUS_CANCELLED = "Cancelled";

    private static final DateTimeFormatter CSV_DATE_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    // Constructor
    public Sale() {
        this.saleID = generateSaleID();
        this.saleDate = new Date();
        this.itemsSold = new ArrayList<>();
        this.totalAmount = 0.0;
        this.status = STATUS_PENDING;
    }

    // Constructor for loading from CSV
    public Sale(String saleID, Date saleDate, double totalAmount, String status) {
        this.saleID = saleID;
        this.saleDate = saleDate;
        this.itemsSold = new ArrayList<>();
        this.totalAmount = totalAmount; // This might be recalculated after loading items
        this.status = status;
    }


    private String generateSaleID() {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyyMMddHHmmss");
        return "SALE-" + sdfDate.format(new Date()) + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    // Getters
    public String getSaleID() { return saleID; }
    public Date getSaleDate() { return saleDate; }
    public List<SaleItem> getItemsSold() { return new ArrayList<>(itemsSold); } // Return copy
    public double getTotalAmount() { return totalAmount; }
    public String getStatus() { return status; }

    // Public method to add a pre-constructed SaleItem (useful when loading)
    public void addLoadedSaleItem(SaleItem item) {
        if (item != null) {
            this.itemsSold.add(item);
        }
    }

    public void setTotalAmount(double totalAmount) { // Useful when loading directly from sales.csv
        this.totalAmount = totalAmount;
    }

    public void setStatus(String status) {
        if (STATUS_PENDING.equals(status) || STATUS_COMPLETED.equals(status) || STATUS_CANCELLED.equals(status)) {
            this.status = status;
        } else {
            System.err.println("Invalid sale status: " + status + ". Status not changed for Sale ID: " + this.saleID);
        }
    }

    public boolean addItemToSale(Item itemFromInventory, int quantityToSell, double sellingPrice) {
        if (!this.status.equals(STATUS_PENDING)) {
            System.err.println("Cannot add items to sale " + saleID + "; status is: " + this.status);
            return false;
        }
        if (itemFromInventory == null) {
            System.err.println("Item to add to sale " + saleID + " cannot be null.");
            return false;
        }
        // Validations for quantityToSell and sellingPrice are in SaleItem constructor now
        try {
            SaleItem saleItem = new SaleItem(itemFromInventory.getSku(), itemFromInventory.getName(), quantityToSell, sellingPrice);
            this.itemsSold.add(saleItem);
            System.out.println("Added to sale " + saleID + ": " + itemFromInventory.getName() + ", Qty: " + quantityToSell + ", Price: " + sellingPrice);
            calculateTotalAmount();
            return true;
        } catch (IllegalArgumentException e) {
            System.err.println("Error adding item to sale " + saleID + ": " + e.getMessage());
            return false;
        }
    }

    public boolean removeItemFromSale(String sku) {
        if (!this.status.equals(STATUS_PENDING)) {
            System.err.println("Cannot remove items from sale " + saleID + "; status is: " + this.status);
            return false;
        }
        boolean removed = this.itemsSold.removeIf(si -> si.getSku().equals(sku));
        if (removed) {
            System.out.println("Removed SKU " + sku + " from sale " + saleID);
            calculateTotalAmount();
        } else {
            System.out.println("SKU " + sku + " not found in sale " + saleID + " for removal.");
        }
        return removed;
    }

    public void calculateTotalAmount() {
        this.totalAmount = 0.0;
        for (SaleItem item : this.itemsSold) {
            this.totalAmount += item.getSubtotal();
        }
    }

    public void refreshTotalAmountFromItems() { // Call after loading all items for a sale
        calculateTotalAmount();
    }

    public boolean finalizeSale(Inventory inventory) {
        if (!this.status.equals(STATUS_PENDING)) {
            System.err.println("Sale " + saleID + " cannot be finalized. Current status: " + this.status);
            return false;
        }
        if (this.itemsSold.isEmpty()) {
            System.err.println("Sale " + saleID + " has no items. Cannot finalize.");
            return false;
        }
        System.out.println("Finalizing sale: " + this.saleID);

        for (SaleItem si : this.itemsSold) {
            Item itemInStock = inventory.getItem(si.getSku());
            if (itemInStock == null) {
                System.err.println("Finalize Error (Sale " + saleID + "): Item " + si.getSku() + " not found in inventory.");
                return false;
            }
            if (itemInStock.getQuantity() < si.getQuantitySold()) {
                System.err.println("Finalize Error (Sale " + saleID + "): Insufficient stock for " + itemInStock.getName() +
                        " (SKU: " + si.getSku() + "). Required: " + si.getQuantitySold() +
                        ", Available: " + itemInStock.getQuantity());
                return false;
            }
        }

        for (SaleItem si : this.itemsSold) {
            Item itemInStock = inventory.getItem(si.getSku());
            itemInStock.updateQuantity(-si.getQuantitySold());
            System.out.println("Inventory updated for SKU " + si.getSku() + " (Sale " + saleID +"): new quantity " + itemInStock.getQuantity());
        }
        this.status = STATUS_COMPLETED;
        this.saleDate = new Date();
        // Recalculate total amount just to be absolutely sure, though it should be correct
        calculateTotalAmount();
        System.out.println("Sale " + saleID + " successfully finalized. Total: $" + String.format(Locale.US, "%.2f", this.totalAmount));
        return true;
    }

    public String getSaleDetails() {
        DecimalFormat df = new DecimalFormat("#0.00", new DecimalFormatSymbols(Locale.US));
        SimpleDateFormat displaySdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        StringBuilder details = new StringBuilder();
        details.append("-------------------- SALE RECEIPT --------------------\n");
        details.append("Sale ID: ").append(saleID).append("\n");
        details.append("Date: ").append(displaySdf.format(saleDate)).append("\n");
        details.append("Status: ").append(status).append("\n");
        details.append("Items Sold (").append(itemsSold.size()).append("):\n");
        if (itemsSold.isEmpty()) {
            details.append("  (No items in this sale)\n");
        } else {
            for (SaleItem item : itemsSold) {
                details.append(item.toString()).append("\n");
            }
        }
        details.append("-----------------------------------------------------\n");
        details.append("Total Amount: $").append(df.format(totalAmount)).append("\n");
        details.append("-----------------------------------------------------\n");
        return details.toString();
    }

    public String toSaleCsvString() {
        DecimalFormat df = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US));
        String isoSaleDate = CSV_DATE_FORMATTER.format(this.saleDate.toInstant().atZone(ZoneOffset.UTC));
        return String.join(",",
                escapeCsv(saleID),
                escapeCsv(isoSaleDate),
                df.format(totalAmount),
                escapeCsv(status));
    }

    public static String escapeCsv(String data) {
        if (data == null) return "";
        if (data.contains(",") || data.contains("\"") || data.contains("\n")) {
            return "\"" + data.replace("\"", "\"\"") + "\"";
        }
        return data;
    }

    public static String unescapeCsv(String data) {
        if (data == null) return "";
        if (data.startsWith("\"") && data.endsWith("\"")) {
            data = data.substring(1, data.length() - 1);
            return data.replace("\"\"", "\"");
        }
        return data;
    }

    public static Date parseIsoDateString(String dateString) {
        try {
            return Date.from(Instant.parse(dateString));
        } catch (Exception e) {
            System.err.println("Error parsing ISO date string: " + dateString + " - " + e.getMessage() + ". Falling back.");
            try {
                SimpleDateFormat legacySdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
                return legacySdf.parse(dateString); // Default Date.toString() format
            } catch (ParseException pe) {
                System.err.println("Error parsing legacy date string after ISO fail: " + dateString + " - " + pe.getMessage());
                return null;
            }
        }
    }

    public static void main(String[] args) {
        Inventory testInventory = new Inventory();
        if (testInventory.getItem("SKU001") == null) {
            testInventory.addItem(new Item("SKU001", "Laptop", "Electronics", 10, 1200.00, "SUP001", "Active"));
        }
        if (testInventory.getItem("SKU002") == null) {
            testInventory.addItem(new Item("SKU002", "Mouse", "Accessory", 50, 25.00, "SUP001", "Active"));
        }

        Sale sale1 = new Sale();
        Item laptop = testInventory.getItem("SKU001");
        if (laptop != null) sale1.addItemToSale(laptop, 1, 1199.00);

        System.out.println(sale1.getSaleDetails());

        // Simulate editing an item in the sale
        if (!sale1.itemsSold.isEmpty()) {
            SaleItem itemToEdit = sale1.itemsSold.get(0); // Get the first item (laptop)
            System.out.println("\n--- Editing first sale item ---");
            System.out.println("Old Qty: " + itemToEdit.getQuantitySold() + ", Old Price: " + itemToEdit.getPriceAtSale());
            itemToEdit.setQuantitySold(2); // Change quantity
            itemToEdit.setPriceAtSale(1150.00); // Change price
            sale1.calculateTotalAmount(); // Sale needs to recalculate its grand total
            System.out.println("New Qty: " + itemToEdit.getQuantitySold() + ", New Price: " + itemToEdit.getPriceAtSale());
            System.out.println("\nSale details after edit:");
            System.out.println(sale1.getSaleDetails());
        }

        sale1.finalizeSale(testInventory);
        System.out.println("\n--- Final Sale details after finalization ---");
        System.out.println(sale1.getSaleDetails());
        if (laptop != null) {
            System.out.println("Laptop stock after sale: " + testInventory.getItem("SKU001").getQuantity());
        }
    }
}
