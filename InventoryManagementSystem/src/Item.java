// Item.java
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class Item {
    // Attributes
    private String sku;         // SKU (String): Unique identifier
    private String name;        // name (String): Product name
    private String category;    // category (String): Product category
    private int quantity;       // quantity (Integer): Stock quantity
    private double price;       // price (Double): Product price (selling price)
    private String supplier;    // supplier (String): SupplierID
    private String status;      // status (String): e.g., "Active", "Inactive"

    // Status Constants
    public static final String STATUS_ACTIVE = "Active";
    public static final String STATUS_INACTIVE = "Inactive";
    // Add other statuses here if needed, e.g., "Damaged", "Discontinued"

    // Constructor
    public Item(String sku, String name, String category, int quantity, double price, String supplier, String status) {
        this.sku = sku; // Assuming SKU is now just the number part based on previous CSV modifications
        this.name = name;
        this.category = category;
        this.quantity = quantity;
        this.price = price;
        this.supplier = supplier; // This should be SupplierID
        this.status = status;
    }

    // Getter and Setter methods
    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        if (quantity >= 0) {
            this.quantity = quantity;
        } else {
            System.out.println("Error: Quantity cannot be negative. Setting to 0 for SKU: " + this.sku);
            this.quantity = 0;
        }
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        if (price >= 0) {
            this.price = price;
        } else {
            System.out.println("Error: Price cannot be negative. Setting to 0.0 for SKU: " + this.sku);
            this.price = 0.0;
        }
    }

    public String getSupplier() {
        return supplier; // Should be SupplierID
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        // Validate against known statuses if necessary
        if (STATUS_ACTIVE.equals(status) || STATUS_INACTIVE.equals(status) /* || other valid statuses */) {
            this.status = status;
        } else {
            System.err.println("Warning: Attempting to set an unrecognized status '" + status + "' for SKU: " + this.sku + ". Assigning as Inactive.");
            this.status = STATUS_INACTIVE; // Default to a safe status or throw an error
        }
    }

    // Core Business Methods
    public String getDetails() {
        DecimalFormat df = new DecimalFormat("#0.00", new DecimalFormatSymbols(Locale.US));
        return "SKU: " + sku +
                "\nName: " + name +
                "\nCategory: " + category +
                "\nQuantity: " + quantity +
                "\nPrice: $" + df.format(price) +
                "\nSupplier: " + supplier + // SupplierID
                "\nStatus: " + status;
    }

    public void updateQuantity(int amount) {
        int newQuantity = this.quantity + amount;
        if (newQuantity >= 0) {
            this.quantity = newQuantity;
        } else {
            System.out.println("Error: Not enough stock for SKU " + this.sku + " to decrease by " + Math.abs(amount) +
                    ". Current quantity is " + this.quantity + ". Quantity not changed.");
        }
    }

    public String toCsvString() {
        DecimalFormat df = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US));
        return String.join(",",
                escapeCsv(sku),
                escapeCsv(name),
                escapeCsv(category),
                String.valueOf(quantity),
                df.format(price),
                escapeCsv(supplier), // SupplierID
                escapeCsv(status));
    }

    public static Item fromCsvString(String csvLine) {
        if (csvLine == null || csvLine.trim().isEmpty()) {
            return null;
        }
        String[] parts = csvLine.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

        if (parts.length < 7) {
            System.err.println("Invalid CSV line for Item (not enough parts): " + csvLine + ". Expected 7, got " + parts.length);
            return null;
        }
        try {
            String sku = unescapeCsv(parts[0]);
            String name = unescapeCsv(parts[1]);
            String category = unescapeCsv(parts[2]);
            int quantity = Integer.parseInt(unescapeCsv(parts[3]));
            double price = Double.parseDouble(unescapeCsv(parts[4]));
            String supplierId = unescapeCsv(parts[5]);
            String status = unescapeCsv(parts[6]);
            // Basic status validation during load
            if (!STATUS_ACTIVE.equals(status) && !STATUS_INACTIVE.equals(status)) {
                System.err.println("Warning: Item SKU " + sku + " loaded with unknown status '" + status + "'. Defaulting to Inactive.");
                status = STATUS_INACTIVE;
            }
            return new Item(sku, name, category, quantity, price, supplierId, status);
        } catch (NumberFormatException e) {
            System.err.println("Error parsing numeric value from CSV line for Item: '" + csvLine + "' - " + e.getMessage());
            return null;
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println("Error processing CSV line for Item due to unexpected format: '" + csvLine + "' - " + e.getMessage());
            return null;
        }
    }

    private static String escapeCsv(String data) {
        if (data == null) return "";
        if (data.contains(",") || data.contains("\"") || data.contains("\n")) {
            return "\"" + data.replace("\"", "\"\"") + "\"";
        }
        return data;
    }

    private static String unescapeCsv(String data) {
        if (data == null) return "";
        if (data.startsWith("\"") && data.endsWith("\"")) {
            data = data.substring(1, data.length() - 1);
            return data.replace("\"\"", "\"");
        }
        return data;
    }

    public static void main(String[] args) {
        Item apple = new Item("001", "Red Delicious Apple, Large", "Fruit", 100, 0.59, "SUP001", STATUS_ACTIVE);
        System.out.println("--- Initial Item Details ---");
        System.out.println(apple.getDetails());

        apple.setStatus(STATUS_INACTIVE);
        System.out.println("\n--- Item Details After Setting Inactive ---");
        System.out.println(apple.getDetails());

        // Test CSV
        String csv = apple.toCsvString();
        System.out.println("\n--- CSV String ---");
        System.out.println(csv);
        Item appleFromCsv = Item.fromCsvString(csv);
        if (appleFromCsv != null) {
            System.out.println("\n--- Item from CSV String ---");
            System.out.println(appleFromCsv.getDetails());
        }

        Item invalidStatusItem = Item.fromCsvString("002,Banana,Fruit,50,0.30,SUP002,Discontinued");
        if(invalidStatusItem != null) {
            System.out.println("\n--- Item with Invalid Status from CSV (should default to Inactive) ---");
            System.out.println(invalidStatusItem.getDetails());
        }
    }
}
