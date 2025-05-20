import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Supplier {
    // 属性 (Attributes)
    private String supplierID;      // supplierID (String): Unique identifier for the supplier
    private String name;            // name (String): Supplier name
    private String contactInfo;     // contactinfo (String): Contact information
    private List<Item> productsSupplied; // productsSupplied (List of Item): Products supplied by this supplier

    // 构造函数 (Constructor)
    public Supplier(String supplierID, String name, String contactInfo) {
        this.supplierID = supplierID;
        this.name = name;
        this.contactInfo = contactInfo;
        this.productsSupplied = new ArrayList<>(); // Initialize with an empty list
    }

    // Getter 和 Setter 方法
    public String getSupplierID() {
        return supplierID;
    }

    public void setSupplierID(String supplierID) {
        this.supplierID = supplierID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }

    public List<Item> getProductsSupplied() {
        // Return a copy to prevent external modification of the internal list
        return new ArrayList<>(productsSupplied);
    }

    // 核心业务方法 (Core Business Methods)

    /**
     * Adds a product to the list of products supplied by this supplier.
     * @param item The Item object to add.
     */
    public void addProduct(Item item) {
        if (item != null) {
            // Optional: Check if the product (by SKU) is already in the list to avoid duplicates
            if (this.productsSupplied.stream().noneMatch(p -> p.getSku().equals(item.getSku()))) {
                this.productsSupplied.add(item);
                System.out.println("Product " + item.getName() + " (SKU: " + item.getSku() + ") added to supplier " + this.name);
            } else {
                System.out.println("Product " + item.getName() + " (SKU: " + item.getSku() + ") is already listed for supplier " + this.name);
            }
        } else {
            System.out.println("Cannot add a null product to the supplier.");
        }
    }

    /**
     * Removes a product from the list of products supplied by this supplier.
     * This method was not explicitly in the proposal but is a common requirement.
     * @param sku The SKU of the item to remove.
     * @return true if the item was removed, false otherwise.
     */
    public boolean removeProduct(String sku) {
        if (sku == null || sku.isEmpty()) {
            System.out.println("SKU cannot be null or empty for removal.");
            return false;
        }
        boolean removed = this.productsSupplied.removeIf(item -> item.getSku().equals(sku));
        if (removed) {
            System.out.println("Product (SKU: " + sku + ") removed from supplier " + this.name);
        } else {
            System.out.println("Product (SKU: " + sku + ") not found for supplier " + this.name);
        }
        return removed;
    }


    /**
     * Returns supplier information.
     * This can be customized to include more or less detail.
     * @return A string containing the supplier's details.
     */
    public String getDetails() {
        StringBuilder details = new StringBuilder();
        details.append("Supplier ID: ").append(supplierID).append("\n");
        details.append("Name: ").append(name).append("\n");
        details.append("Contact Info: ").append(contactInfo).append("\n");
        details.append("Products Supplied (Count): ").append(productsSupplied.size()).append("\n");

        if (!productsSupplied.isEmpty()) {
            details.append("Products List:\n");
            for (Item item : productsSupplied) {
                details.append("  - ").append(item.getName()).append(" (SKU: ").append(item.getSku()).append(")\n");
            }
        }
        return details.toString();
    }

    // main 方法用于测试 Supplier 类的功能
    public static void main(String[] args) {
        // 需要 Item.java 已编译或在同一包内
        Item apple = new Item("SKU001", "Apple", "Fruit", 100, 0.5, "SupplierA_ID_Placeholder", "Active");
        Item pear = new Item("SKU006", "Pear", "Fruit", 70, 0.6, "SupplierA_ID_Placeholder", "Active");
        Item inkCartridge = new Item("SKU007", "Ink Cartridge", "Office Supplies", 200, 15.0, "SupplierB_ID_Placeholder", "Active");

        // 1. 创建 Supplier 对象
        System.out.println("--- Creating Suppliers ---");
        Supplier supplierA = new Supplier("SUP001", "FreshProduce Inc.", "contact@freshproduce.com");
        Supplier supplierB = new Supplier("SUP002", "OfficeNeeds Ltd.", "sales@officeneeds.com");
        System.out.println("Supplier A created: " + supplierA.getName());
        System.out.println("Supplier B created: " + supplierB.getName());
        System.out.println();

        // 2. 给供应商添加产品
        System.out.println("--- Adding Products to Suppliers ---");
        supplierA.addProduct(apple); // Apple's supplier attribute in Item object might differ, this demonstrates association at Supplier level
        supplierA.addProduct(pear);
        supplierA.addProduct(apple); // Try adding duplicate
        supplierB.addProduct(inkCartridge);
        supplierB.addProduct(null); // Try adding null
        System.out.println();

        // 3. 获取供应商详情
        System.out.println("--- Supplier A Details ---");
        System.out.println(supplierA.getDetails());
        System.out.println();

        System.out.println("--- Supplier B Details ---");
        System.out.println(supplierB.getDetails());
        System.out.println();

        // 4. (新增) 从供应商移除产品
        System.out.println("--- Removing Product from Supplier A ---");
        supplierA.removeProduct("SKU006"); // Remove Pear
        supplierA.removeProduct("SKU999"); // Try removing non-existent product
        System.out.println("\n--- Supplier A Details After Removal ---");
        System.out.println(supplierA.getDetails());
        System.out.println();

        // 5. 更新供应商信息 (使用 setters)
        System.out.println("--- Updating Supplier B Contact Info ---");
        supplierB.setContactInfo("support@officeneeds.com");
        System.out.println("New contact for Supplier B: " + supplierB.getContactInfo());
        System.out.println("\n--- Supplier B Details After Update ---");
        System.out.println(supplierB.getDetails());
    }
}
