import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Inventory {
    private Map<String, Item> items;
    // 统一数据目录路径 (可以考虑从一个中心配置类获取)
    public static final String DATA_DIRECTORY = "data/";
    public static final String DEFAULT_ITEMS_FILE_PATH = DATA_DIRECTORY + "items.csv";
    private static final String CSV_HEADER = "SKU,Name,Category,Quantity,Price,SupplierID,Status";

    public Inventory() {
        this.items = new HashMap<>();
        loadItemsFromFile(DEFAULT_ITEMS_FILE_PATH);
    }

    public void loadItemsFromFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("Items file not found: " + filePath + ". Starting with an empty inventory for items.");
            // Ensure data directory exists for potential save operations later if needed by other logic
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            String actualHeader = br.readLine();

            if (actualHeader == null) {
                System.err.println("Items file is empty: " + filePath);
                return;
            }
            if (!actualHeader.trim().equalsIgnoreCase(CSV_HEADER)) {
                System.err.println("Warning: Items CSV file header mismatch.");
                System.err.println("Expected: '" + CSV_HEADER + "'");
                System.err.println("Got:      '" + actualHeader.trim() + "'");
                System.err.println("Attempting to parse anyway, but data integrity may be compromised.");
            }

            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                Item item = Item.fromCsvString(line);
                if (item != null) {
                    this.items.put(item.getSku(), item);
                }
            }
            System.out.println(this.items.size() + " items loaded successfully from " + filePath);
        } catch (IOException e) {
            System.err.println("Error loading items from file " + filePath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void saveItemsToFile(String filePath) {
        File file = new File(filePath);
        File parentDir = file.getParentFile(); // Should be "data" directory
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                System.err.println("Could not create directory: " + parentDir.getPath());
                return; // Stop if directory creation fails
            }
        }

        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file)))) {
            out.println(CSV_HEADER);
            for (Item item : this.items.values()) {
                out.println(item.toCsvString());
            }
            System.out.println(this.items.size() + " items saved successfully to " + filePath);
        } catch (IOException e) {
            System.err.println("Error saving items to file " + filePath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- 现有方法 ---
    public void addItem(Item item) {
        if (item == null || item.getSku() == null || item.getSku().isEmpty()) {
            System.out.println("Error: Item or SKU cannot be null or empty. Item not added.");
            return;
        }
        if (this.items.containsKey(item.getSku())) {
            System.out.println("Error: Item with SKU " + item.getSku() + " already exists. Use updateItem() to modify.");
        } else {
            this.items.put(item.getSku(), item);
            System.out.println("Item " + item.getName() + " (SKU: " + item.getSku() + ") added to inventory.");
        }
    }

    public boolean removeItem(String sku) {
        if (sku == null || sku.isEmpty()) {
            System.out.println("Error: SKU cannot be null or empty.");
            return false;
        }
        if (this.items.containsKey(sku)) {
            Item removedItem = this.items.remove(sku);
            System.out.println("Item " + removedItem.getName() + " (SKU: " + sku + ") removed from inventory.");
            return true;
        } else {
            System.out.println("Error: Item with SKU " + sku + " not found. Nothing removed.");
            return false;
        }
    }

    public boolean updateItem(String sku, Item newItem) {
        if (sku == null || sku.isEmpty() || newItem == null) {
            System.out.println("Error: SKU or newItem cannot be null or empty.");
            return false;
        }
        if (!sku.equals(newItem.getSku())) {
            System.out.println("Error: SKU parameter (" + sku + ") does not match newItem's SKU (" + newItem.getSku() + "). Update failed.");
            return false;
        }
        if (this.items.containsKey(sku)) {
            this.items.put(sku, newItem);
            System.out.println("Item (SKU: " + sku + ") updated.");
            return true;
        } else {
            System.out.println("Error: Item with SKU " + sku + " not found. Cannot update.");
            return false;
        }
    }

    public Item getItem(String sku) {
        if (sku == null || sku.isEmpty()) {
            return null;
        }
        return this.items.get(sku);
    }

    public List<Item> getAllItems() {
        return new ArrayList<>(this.items.values());
    }

    public List<Item> searchItems(String criteria) {
        if (criteria == null || criteria.trim().isEmpty()) {
            return getAllItems();
        }
        String lowerCaseCriteria = criteria.toLowerCase();
        return this.items.values().stream()
                .filter(item -> item.getSku().toLowerCase().contains(lowerCaseCriteria) ||
                        item.getName().toLowerCase().contains(lowerCaseCriteria) ||
                        item.getCategory().toLowerCase().contains(lowerCaseCriteria))
                .collect(Collectors.toList());
    }

    public List<Item> generateLowStockReport(int threshold) {
        if (threshold < 0) {
            System.out.println("Warning: Low stock threshold cannot be negative. Using 0.");
            threshold = 0;
        }
        final int finalThreshold = threshold;
        return this.items.values().stream()
                .filter(item -> item.getQuantity() <= finalThreshold)
                .collect(Collectors.toList());
    }

    public double calculateTotalValue() {
        double totalValue = 0.0;
        for (Item item : this.items.values()) {
            totalValue += item.getPrice() * item.getQuantity();
        }
        return totalValue;
    }

    public static void main(String[] args) {
        System.out.println("--- Testing Inventory CSV Persistence (in " + DATA_DIRECTORY + ") ---");
        Inventory inventory = new Inventory();

        if (inventory.getAllItems().isEmpty()) {
            System.out.println("Inventory is empty after initial load. Adding sample items...");
            inventory.addItem(new Item("TSKU001", "Test Apple", "Fruit", 100, 0.55, "TSUP001", "Active"));
            inventory.addItem(new Item("TSKU002", "Test Banana", "Fruit", 150, 0.33, "TSUP001", "Active"));
            inventory.addItem(new Item("TSKU003", "Test Milk, Whole", "Dairy", 50, 1.25, "TSUP002", "Active"));
            inventory.saveItemsToFile(DEFAULT_ITEMS_FILE_PATH);
        } else {
            System.out.println("Loaded " + inventory.getAllItems().size() + " items from " + DEFAULT_ITEMS_FILE_PATH);
        }

        System.out.println("\n--- Current Items in Inventory (" + inventory.getAllItems().size() + ") ---");
        for (Item item : inventory.getAllItems()) {
            System.out.println(item.getDetails() + "\n");
        }

        System.out.println("\n--- Adding a new item ---");
        Item newItem = new Item("TSKU004", "Test Bread, \"Whole Wheat\"", "Bakery", 30, 2.75, "TSUP003", "Active");
        inventory.addItem(newItem);

        System.out.println("\n--- Saving all items to file ---");
        inventory.saveItemsToFile(DEFAULT_ITEMS_FILE_PATH);

        System.out.println("\n--- Creating a new Inventory instance to test loading ---");
        Inventory inventory2 = new Inventory();

        System.out.println("\n--- Items in new Inventory instance (" + inventory2.getAllItems().size() + ") ---");
        boolean foundNewItem = false;
        for (Item item : inventory2.getAllItems()) {
            System.out.println(item.getName() + " (SKU: " + item.getSku() + ")");
            if (item.getSku().equals("TSKU004")) {
                foundNewItem = true;
            }
        }
        if (foundNewItem) {
            System.out.println("Test Bread (TSKU004) was successfully loaded into the new inventory instance.");
        } else {
            System.out.println("Test Bread (TSKU004) was NOT found in the new inventory instance after saving and reloading.");
        }
    }
}