// Order.java
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.ZoneOffset;


public class Order {
    private String orderID;
    private String supplierID; // Store Supplier ID for simplicity in CSV
    private transient Supplier supplier; // Transient: not directly part of CSV, but can be loaded/linked
    private Date orderDate;
    private List<OrderItem> items;
    private String status; // e.g., "Pending", "Placed", "Partially Received", "Received", "Cancelled"
    private double totalCost; // Calculated based on ordered items

    public static final String STATUS_PENDING = "Pending";
    public static final String STATUS_PLACED = "Placed"; // Order sent to supplier
    public static final String STATUS_PARTIALLY_RECEIVED = "Partially Received";
    public static final String STATUS_RECEIVED = "Received"; // All items received
    public static final String STATUS_CANCELLED = "Cancelled";

    private static final DateTimeFormatter CSV_DATE_FORMATTER = DateTimeFormatter.ISO_INSTANT; // UTC for persistence
    private static final SimpleDateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    // Constructor for creating a new order
    public Order(Supplier supplier) {
        if (supplier == null) {
            throw new IllegalArgumentException("Supplier cannot be null when creating an order.");
        }
        this.orderID = generateOrderID();
        this.supplier = supplier;
        this.supplierID = supplier.getSupplierID();
        this.orderDate = new Date();
        this.items = new ArrayList<>();
        this.status = STATUS_PENDING;
        this.totalCost = 0.0;
    }

    // Constructor for loading from CSV
    public Order(String orderID, String supplierID, Date orderDate, String status, double totalCost) {
        this.orderID = orderID;
        this.supplierID = supplierID;
        this.orderDate = orderDate;
        this.items = new ArrayList<>(); // Items will be loaded separately
        this.status = status;
        this.totalCost = totalCost; // This is the stored total, might be recalculated after loading items
    }

    private String generateOrderID() {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyyMMddHHmmss");
        return "PO-" + sdfDate.format(new Date()) + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    // Getters
    public String getOrderID() { return orderID; }
    public String getSupplierID() { return supplierID; }
    public Supplier getSupplier() { return supplier; } // May be null if not linked after loading
    public Date getOrderDate() { return orderDate; }
    public List<OrderItem> getItems() { return new ArrayList<>(items); } // Return copy
    public String getStatus() { return status; }
    public double getTotalCost() {
        calculateTotalCost(); // Ensure it's up-to-date
        return totalCost;
    }
    public String getOrderDateString() { return DISPLAY_DATE_FORMAT.format(this.orderDate); }


    // Setters
    public void setSupplier(Supplier supplier) { // Used by OrderManager after loading to link supplier object
        this.supplier = supplier;
        if (supplier != null && !this.supplierID.equals(supplier.getSupplierID())) {
            System.err.println("Warning: Supplier object ID " + supplier.getSupplierID() +
                    " does not match stored supplierID " + this.supplierID + " for order " + this.orderID);
            // this.supplierID = supplier.getSupplierID(); // Optionally update if supplier object is master
        }
    }

    public void setStatus(String status) {
        // Add validation for allowed statuses if necessary
        this.status = status;
        System.out.println("Order " + orderID + " status updated to: " + status);
    }

    public void addItem(OrderItem item) {
        if (item != null) {
            // Check if item with same SKU already exists; if so, perhaps update quantity or disallow
            boolean itemExists = items.stream().anyMatch(oi -> oi.getItemSKU().equals(item.getItemSKU()));
            if (itemExists) {
                System.err.println("Item with SKU " + item.getItemSKU() + " already exists in order " + orderID + ". Consider updating existing item.");
                // For now, allow adding, but in UI, this should be handled by editing existing line
            }
            this.items.add(item);
            calculateTotalCost();
        }
    }

    public void addLoadedOrderItem(OrderItem item) { // For loading from CSV without recalculating total yet
        if (item != null) {
            this.items.add(item);
        }
    }

    public boolean removeItemBySKU(String sku) {
        boolean removed = this.items.removeIf(oi -> oi.getItemSKU().equals(sku));
        if (removed) {
            calculateTotalCost();
        }
        return removed;
    }

    public void calculateTotalCost() {
        this.totalCost = 0.0;
        for (OrderItem item : this.items) {
            this.totalCost += item.getSubtotal();
        }
    }

    /**
     * Checks if all items in the order have been fully received.
     * @return true if all items are fully received, false otherwise.
     */
    public boolean isFullyReceived() {
        if (items.isEmpty() && !status.equals(STATUS_PENDING) && !status.equals(STATUS_PLACED)) {
            // Empty order that was perhaps cancelled or an error.
            // Or, if an order can be empty and "received", this logic needs adjustment.
            // For now, an empty order isn't "received" in terms of items.
            return false;
        }
        for (OrderItem item : items) {
            if (item.getReceivedQuantity() < item.getOrderedQuantity()) {
                return false;
            }
        }
        return !items.isEmpty(); // True only if not empty and all items received
    }

    /**
     * Updates the overall order status based on the received quantities of its items.
     * Call this after processing item receipts.
     */
    public void updateOrderStatusBasedOnReceipts() {
        if (items.isEmpty()) {
            // If an order has no items, its status might not change based on receipts.
            // Or it could be considered "Received" if it was intentionally empty and placed.
            // Keep current status or set to an appropriate one.
            if (status.equals(STATUS_PENDING) || status.equals(STATUS_PLACED)) {
                // perhaps it becomes Received if no items were ever expected and it was placed
            }
            return;
        }

        if (isFullyReceived()) {
            setStatus(STATUS_RECEIVED);
        } else {
            boolean anyReceived = items.stream().anyMatch(item -> item.getReceivedQuantity() > 0);
            if (anyReceived) {
                setStatus(STATUS_PARTIALLY_RECEIVED);
            } else {
                // If nothing received, status remains as it was (e.g., Placed) unless changed manually
                // Or, if it was Pending/Placed, it stays that way.
            }
        }
    }


    // For CSV Persistence (Order Header: orders.csv)
    // orderID,supplierID,orderDate (ISO),status,totalCost
    public String toOrderCsvString() {
        DecimalFormat df = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US));
        String isoOrderDate = CSV_DATE_FORMATTER.format(this.orderDate.toInstant().atZone(ZoneOffset.UTC));
        return String.join(",",
                escapeCsv(orderID),
                escapeCsv(supplierID),
                escapeCsv(isoOrderDate),
                escapeCsv(status),
                df.format(totalCost) // Store calculated total at time of saving
        );
    }

    // For CSV Persistence (Order Items: order_items.csv - each line will also need OrderID prepended by OrderManager)
    // OrderItem itself has toCsvString() for its part.

    public static Date parseIsoDateString(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) return null;
        try {
            return Date.from(Instant.parse(dateString.trim()));
        } catch (Exception e) {
            System.err.println("Error parsing ISO date string for Order: '" + dateString + "' - " + e.getMessage() + ". Trying legacy format.");
            // Fallback for older Date.toString() format if needed, though ISO is preferred
            try {
                SimpleDateFormat legacySdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
                return legacySdf.parse(dateString.trim());
            } catch (ParseException pe) {
                System.err.println("Error parsing legacy date string for Order after ISO fail: '" + dateString + "' - " + pe.getMessage());
                return null;
            }
        }
    }

    // Helper for CSV string generation
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


    public String getOrderDetails() {
        DecimalFormat df = new DecimalFormat("#0.00", new DecimalFormatSymbols(Locale.US));
        StringBuilder details = new StringBuilder();
        details.append("---------------- PURCHASE ORDER ----------------\n");
        details.append("Order ID: ").append(orderID).append("\n");
        details.append("Order Date: ").append(getOrderDateString()).append("\n");
        details.append("Supplier ID: ").append(supplierID);
        if (supplier != null) {
            details.append(" (Name: ").append(supplier.getName()).append(")\n");
        } else {
            details.append(" (Supplier details not loaded)\n");
        }
        details.append("Status: ").append(status).append("\n");
        details.append("Total Estimated Cost: $").append(df.format(getTotalCost())).append("\n");
        details.append("Items (").append(items.size()).append(" line item(s)):\n");
        if (items.isEmpty()) {
            details.append("  (No items in this order)\n");
        } else {
            for (OrderItem item : items) {
                details.append(item.toString()).append("\n");
            }
        }
        details.append("----------------------------------------------\n");
        return details.toString();
    }


    public static void main(String[] args) {
        // Test
        Supplier testSupplier = new Supplier("SUPPLIER_TEST", "Test Supplier Co.", "test@supplier.com");
        Order order = new Order(testSupplier);
        System.out.println("Created Order: " + order.getOrderID());

        OrderItem item1 = new OrderItem("ITEM001", "Test Item A", 10, 15.99);
        OrderItem item2 = new OrderItem("ITEM002", "Test Item B", 5, 25.00);
        order.addItem(item1);
        order.addItem(item2);

        System.out.println(order.getOrderDetails());

        System.out.println("Order CSV String: " + order.toOrderCsvString());
        for(OrderItem oi : order.getItems()){
            System.out.println("OrderItem CSV String for Order " + order.getOrderID() + ": " + oi.toCsvString());
        }

        // Simulate receiving some items
        item1.receiveItems(5);
        order.updateOrderStatusBasedOnReceipts(); // Should be Partially Received
        System.out.println("\nAfter receiving 5 of ITEM001:");
        System.out.println(order.getOrderDetails());

        item1.receiveItems(5); // Receive remaining
        item2.receiveItems(5); // Receive all of item2
        order.updateOrderStatusBasedOnReceipts(); // Should be Received
        System.out.println("\nAfter receiving all items:");
        System.out.println(order.getOrderDetails());
    }
}
