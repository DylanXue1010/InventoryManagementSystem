import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
// import java.util.stream.Collectors; // Not strictly needed for current methods

public class SupplierManager {
    private List<Supplier> suppliers;
    // 统一数据目录路径
    public static final String DATA_DIRECTORY = "data/";
    public static final String DEFAULT_SUPPLIERS_FILE_PATH = DATA_DIRECTORY + "suppliers.csv";
    private static final String CSV_HEADER = "supplierID,name,contactInfo";

    public SupplierManager() {
        this.suppliers = new ArrayList<>();
        loadSuppliersFromFile(DEFAULT_SUPPLIERS_FILE_PATH);
    }

    public void loadSuppliersFromFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("Suppliers file not found: " + filePath + ". Starting with an empty supplier list.");
            // Ensure data directory exists for potential save operations later
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line = br.readLine();
            if (line == null || !line.trim().equalsIgnoreCase(CSV_HEADER)) {
                System.err.println("Warning: Suppliers CSV file header mismatch or file is empty. Expected: '" + CSV_HEADER + "'. Got: '" + (line != null ? line.trim() : "null") + "'");
                return;
            }

            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#")) continue;
                // More robust split needed if names/contact info can contain commas
                // For now, assuming simple CSV. If complex, use a CSV parsing library or improve split logic.
                String[] parts = line.split(",", 3); // Split into at most 3 parts
                if (parts.length >= 3) {
                    // Trim parts to remove leading/trailing whitespace
                    String id = parts[0].trim();
                    String name = parts[1].trim();
                    String contact = parts[2].trim();

                    // Basic unescaping if fields were quoted
                    id = unescapeCsvSimple(id);
                    name = unescapeCsvSimple(name);
                    contact = unescapeCsvSimple(contact);

                    this.suppliers.add(new Supplier(id, name, contact));
                } else {
                    System.err.println("Skipping malformed supplier line (not enough parts): " + line);
                }
            }
            System.out.println(this.suppliers.size() + " suppliers loaded successfully from " + filePath);
        } catch (IOException e) {
            System.err.println("Error loading suppliers from file " + filePath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void saveSuppliersToFile(String filePath) {
        File file = new File(filePath);
        File parentDir = file.getParentFile(); // Should be "data" directory
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                System.err.println("Could not create directory for suppliers file: " + parentDir.getPath());
                return;
            }
        }

        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file)))) {
            out.println(CSV_HEADER);
            for (Supplier supplier : this.suppliers) {
                out.println(String.join(",",
                        escapeCsv(supplier.getSupplierID()),
                        escapeCsv(supplier.getName()),
                        escapeCsv(supplier.getContactInfo())
                ));
            }
            System.out.println(this.suppliers.size() + " suppliers saved successfully to " + filePath);
        } catch (IOException e) {
            System.err.println("Error saving suppliers to file " + filePath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Simple CSV field escaping
    private String escapeCsv(String data) {
        if (data == null) return "";
        // If data contains comma, quote, or newline, then enclose in double quotes
        // and escape existing double quotes by doubling them.
        if (data.contains(",") || data.contains("\"") || data.contains("\n") || data.contains("\r")) {
            return "\"" + data.replace("\"", "\"\"") + "\"";
        }
        return data;
    }

    // Simple CSV field unescaping (assumes field was quoted if necessary)
    private String unescapeCsvSimple(String data) {
        if (data == null) return "";
        if (data.startsWith("\"") && data.endsWith("\"")) {
            // Remove wrapping quotes
            String unquoted = data.substring(1, data.length() - 1);
            // Replace doubled double quotes with a single double quote
            return unquoted.replace("\"\"", "\"");
        }
        return data;
    }


    public void addSupplier(Supplier supplier) {
        if (supplier == null || supplier.getSupplierID() == null || supplier.getSupplierID().isEmpty()) {
            System.out.println("Error: Supplier or Supplier ID cannot be null or empty.");
            return;
        }
        if (this.suppliers.stream().anyMatch(s -> s.getSupplierID().equals(supplier.getSupplierID()))) {
            System.out.println("Error: Supplier with ID " + supplier.getSupplierID() + " already exists.");
        } else {
            this.suppliers.add(supplier);
            System.out.println("Supplier " + supplier.getName() + " (ID: " + supplier.getSupplierID() + ") added.");
        }
    }

    public boolean removeSupplier(String supplierID) {
        if (supplierID == null || supplierID.isEmpty()) {
            System.out.println("Error: Supplier ID cannot be null or empty for removal.");
            return false;
        }
        return this.suppliers.removeIf(s -> s.getSupplierID().equals(supplierID));
    }

    public Optional<Supplier> findSupplierById(String supplierId) {
        return suppliers.stream()
                .filter(s -> s.getSupplierID().equals(supplierId))
                .findFirst();
    }

    public List<Supplier> getAllSuppliers() {
        return new ArrayList<>(this.suppliers);
    }

    public static void main(String[] args) {
        System.out.println("Testing SupplierManager (data in " + DATA_DIRECTORY + ")...");
        SupplierManager sm = new SupplierManager();
        if (sm.getAllSuppliers().isEmpty()) {
            System.out.println("No suppliers loaded. Adding sample suppliers.");
            sm.addSupplier(new Supplier("SUPPLIER_A", "Fresh Produce Co.", "orders@freshproduce.co"));
            sm.addSupplier(new Supplier("SUPPLIER_B", "Office Solutions Ltd.", "contact@officesolutions.com"));
            sm.addSupplier(new Supplier("SUPPLIER_C", "Gadget Galaxy, \"Best Gadgets\"", "support@gadgetgalaxy.net, sales@gadgetgalaxy.net"));
            sm.saveSuppliersToFile(DEFAULT_SUPPLIERS_FILE_PATH);
        }

        System.out.println("\n--- Current Suppliers (" + sm.getAllSuppliers().size() + ") ---");
        for (Supplier s : sm.getAllSuppliers()) {
            System.out.println(s.getDetails());
        }

        System.out.println("\n--- Loading suppliers again to test parsing of escaped CSV ---");
        SupplierManager sm2 = new SupplierManager();
        System.out.println("\n--- Current Suppliers after reload (" + sm2.getAllSuppliers().size() + ") ---");
        for (Supplier s : sm2.getAllSuppliers()) {
            System.out.println("ID: " + s.getSupplierID() + ", Name: " + s.getName() + ", Contact: " + s.getContactInfo());
        }

        System.out.println("\nSupplierManager test finished.");
    }
}