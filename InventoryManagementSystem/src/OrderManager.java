import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class OrderManager {
    private List<Order> ordersList;
    private Inventory inventory;
    private SupplierManager supplierManager;

    // 统一数据目录路径
    public static final String DATA_DIRECTORY = "data/";
    public static final String DEFAULT_ORDERS_FILE_PATH = DATA_DIRECTORY + "orders.csv";
    public static final String DEFAULT_ORDER_ITEMS_FILE_PATH = DATA_DIRECTORY + "order_items.csv";

    private static final String ORDERS_CSV_HEADER = "orderID,supplierID,orderDate,status,totalCost";
    private static final String ORDER_ITEMS_CSV_HEADER = "orderID,itemSKU,itemName,orderedQuantity,receivedQuantity,purchasePrice";

    public OrderManager(Inventory inventory, SupplierManager supplierManager) {
        this.inventory = inventory;
        this.supplierManager = supplierManager;
        this.ordersList = new ArrayList<>();
        loadOrdersFromFile();
    }

    public Order createNewOrder(Supplier supplier) {
        if (supplier == null) {
            System.err.println("Cannot create order: Supplier is null.");
            return null;
        }
        Order newOrder = new Order(supplier);
        this.ordersList.add(newOrder);
        System.out.println("New Purchase Order created: " + newOrder.getOrderID() + " for Supplier: " + supplier.getName());
        return newOrder;
    }

    public Optional<Order> getOrderById(String orderId) {
        return ordersList.stream().filter(o -> o.getOrderID().equals(orderId)).findFirst();
    }

    public List<Order> getAllOrders() {
        return new ArrayList<>(ordersList);
    }

    public List<Order> getOrdersByStatus(String status) {
        return ordersList.stream()
                .filter(o -> o.getStatus().equalsIgnoreCase(status))
                .collect(Collectors.toList());
    }

    public void loadOrdersFromFile() {
        Map<String, Order> loadedOrdersMap = new HashMap<>();
        File ordersFile = new File(DEFAULT_ORDERS_FILE_PATH);
        File orderItemsFile = new File(DEFAULT_ORDER_ITEMS_FILE_PATH);

        // Ensure data directory exists for potential save operations later
        File dataDir = new File(DATA_DIRECTORY);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        if (ordersFile.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(ordersFile))) {
                String line = br.readLine();
                if (line == null || !line.trim().equalsIgnoreCase(ORDERS_CSV_HEADER)) {
                    System.err.println("Warning: orders.csv header mismatch or file empty. Expected: " + ORDERS_CSV_HEADER);
                } else {
                    while ((line = br.readLine()) != null) {
                        if (line.trim().isEmpty() || line.startsWith("#")) continue;
                        String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                        if (parts.length < 5) {
                            System.err.println("Skipping invalid line in orders.csv (not enough parts): " + line);
                            continue;
                        }
                        try {
                            String orderID = Order.unescapeCsv(parts[0]);
                            String supplierID = Order.unescapeCsv(parts[1]);
                            Date orderDate = Order.parseIsoDateString(Order.unescapeCsv(parts[2]));
                            String status = Order.unescapeCsv(parts[3]);
                            double totalCost = Double.parseDouble(Order.unescapeCsv(parts[4]));

                            if (orderDate == null) {
                                System.err.println("Skipping order due to invalid date in orders.csv: " + line);
                                continue;
                            }

                            Order order = new Order(orderID, supplierID, orderDate, status, totalCost);
                            supplierManager.findSupplierById(supplierID).ifPresent(order::setSupplier);
                            loadedOrdersMap.put(orderID, order);
                        } catch (NumberFormatException e) {
                            System.err.println("Error parsing numeric value from orders.csv line: " + line + " - " + e.getMessage());
                        } catch (Exception e) {
                            System.err.println("Error processing line from orders.csv: " + line + " - " + e.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Error loading from " + DEFAULT_ORDERS_FILE_PATH + ": " + e.getMessage());
            }
        } else {
            System.out.println(DEFAULT_ORDERS_FILE_PATH + " not found. No purchase orders loaded.");
        }

        if (orderItemsFile.exists() && !loadedOrdersMap.isEmpty()) {
            try (BufferedReader br = new BufferedReader(new FileReader(orderItemsFile))) {
                String line = br.readLine();
                if (line == null || !line.trim().equalsIgnoreCase(ORDER_ITEMS_CSV_HEADER)) {
                    System.err.println("Warning: order_items.csv header mismatch or file empty. Expected: " + ORDER_ITEMS_CSV_HEADER);
                } else {
                    while ((line = br.readLine()) != null) {
                        if (line.trim().isEmpty() || line.startsWith("#")) continue;
                        String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                        if (parts.length < 6) {
                            System.err.println("Skipping invalid line in order_items.csv (not enough parts): " + line);
                            continue;
                        }
                        try {
                            String orderID = Order.unescapeCsv(parts[0]);
                            Order targetOrder = loadedOrdersMap.get(orderID);
                            if (targetOrder != null) {
                                String[] itemParts = new String[parts.length - 1];
                                System.arraycopy(parts, 1, itemParts, 0, parts.length - 1);
                                OrderItem orderItem = OrderItem.fromCsvParts(itemParts);
                                if (orderItem != null) {
                                    targetOrder.addLoadedOrderItem(orderItem);
                                }
                            } else {
                                System.err.println("Warning: OrderItem found for non-existent OrderID " + orderID + " in order_items.csv: " + line);
                            }
                        } catch (Exception e) {
                            System.err.println("Error processing line from order_items.csv: " + line + " - " + e.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Error loading from " + DEFAULT_ORDER_ITEMS_FILE_PATH + ": " + e.getMessage());
            }
        } else if (!loadedOrdersMap.isEmpty()) {
            System.out.println(DEFAULT_ORDER_ITEMS_FILE_PATH + " not found, but orders were loaded. Order items might be missing.");
        }

        this.ordersList.clear();
        for (Order order : loadedOrdersMap.values()) {
            order.calculateTotalCost();
            this.ordersList.add(order);
        }
        System.out.println(this.ordersList.size() + " purchase orders processed and loaded.");
    }

    public void saveOrdersToFile() {
        File ordersFile = new File(DEFAULT_ORDERS_FILE_PATH); // e.g., "data/orders.csv"
        File parentDir = ordersFile.getParentFile(); // This will be "data"
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                System.err.println("Could not create directory: " + parentDir.getPath());
                return; // Stop if directory creation fails
            }
        }
        // The parentDir for orderItemsFile will be the same, so no need to check/create again.

        try (PrintWriter ordersOut = new PrintWriter(new BufferedWriter(new FileWriter(ordersFile)))) {
            ordersOut.println(ORDERS_CSV_HEADER);
            for (Order order : this.ordersList) {
                ordersOut.println(order.toOrderCsvString());
            }
            System.out.println(this.ordersList.size() + " purchase orders saved to " + DEFAULT_ORDERS_FILE_PATH);
        } catch (IOException e) {
            System.err.println("Error saving to " + DEFAULT_ORDERS_FILE_PATH + ": " + e.getMessage());
        }

        File orderItemsFile = new File(DEFAULT_ORDER_ITEMS_FILE_PATH);
        try (PrintWriter itemsOut = new PrintWriter(new BufferedWriter(new FileWriter(orderItemsFile)))) {
            itemsOut.println(ORDER_ITEMS_CSV_HEADER);
            for (Order order : this.ordersList) {
                for (OrderItem oi : order.getItems()) {
                    itemsOut.println(Order.escapeCsv(order.getOrderID()) + "," + oi.toCsvString());
                }
            }
            System.out.println("Purchase order items saved to " + DEFAULT_ORDER_ITEMS_FILE_PATH);
        } catch (IOException e) {
            System.err.println("Error saving to " + DEFAULT_ORDER_ITEMS_FILE_PATH + ": " + e.getMessage());
        }
    }

    public boolean receiveOrderItem(Order order, OrderItem itemToReceive, int quantityReceived) {
        if (order == null || itemToReceive == null || quantityReceived <= 0) {
            System.err.println("Invalid parameters for receiving order item.");
            return false;
        }
        if (!order.getStatus().equals(Order.STATUS_PLACED) && !order.getStatus().equals(Order.STATUS_PARTIALLY_RECEIVED)) {
            System.err.println("Cannot receive items for order " + order.getOrderID() + " with status: " + order.getStatus());
            return false;
        }

        int actualQtyRecordedAsReceived = itemToReceive.receiveItems(quantityReceived);

        if (actualQtyRecordedAsReceived > 0) {
            Item inventoryItem = inventory.getItem(itemToReceive.getItemSKU());
            if (inventoryItem != null) {
                inventoryItem.updateQuantity(actualQtyRecordedAsReceived);
                System.out.println("Inventory updated for SKU " + itemToReceive.getItemSKU() + ": +" + actualQtyRecordedAsReceived + " units. New stock: " + inventoryItem.getQuantity());
            } else {
                System.err.println("Critical Error: Item SKU " + itemToReceive.getItemSKU() + " from PO not found in inventory during receiving!");
            }
        }

        order.updateOrderStatusBasedOnReceipts();
        return actualQtyRecordedAsReceived > 0;
    }

    public boolean updateOrderStatus(String orderId, String newStatus) {
        Optional<Order> orderOpt = getOrderById(orderId);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            order.setStatus(newStatus);
            System.out.println("Order " + orderId + " status updated to " + newStatus);
            return true;
        }
        System.err.println("Failed to update status: Order " + orderId + " not found.");
        return false;
    }

    public static void main(String[] args) {
        System.out.println("Testing OrderManager (data in " + DATA_DIRECTORY + ")...");
        Inventory inv = new Inventory();
        if(inv.getItem("LAP001") == null) inv.addItem(new Item("LAP001", "Laptop X", "Electronics", 5, 800.00, "SUP001", "Active"));
        if(inv.getItem("MOU001") == null) inv.addItem(new Item("MOU001", "Wireless Mouse", "Accessory", 20, 15.00, "SUP001", "Active"));
        inv.saveItemsToFile(Inventory.DEFAULT_ITEMS_FILE_PATH); // Save inventory if modified

        SupplierManager sm = new SupplierManager();
        if (sm.getAllSuppliers().isEmpty()) {
            sm.addSupplier(new Supplier("SUP001", "Tech Distributor", "sales@techdist.com"));
            sm.saveSuppliersToFile(SupplierManager.DEFAULT_SUPPLIERS_FILE_PATH);
        }
        Supplier testSupplier = sm.findSupplierById("SUP001").orElse(null);

        OrderManager om = new OrderManager(inv, sm);

        if (om.getAllOrders().isEmpty() && testSupplier != null) {
            System.out.println("No orders loaded. Creating a test PO.");
            Order testOrder = om.createNewOrder(testSupplier);
            testOrder.addItem(new OrderItem("LAP001", "Laptop X", 2, 750.00));
            testOrder.addItem(new OrderItem("MOU001", "Wireless Mouse", 10, 10.00));
            testOrder.setStatus(Order.STATUS_PLACED);
            om.saveOrdersToFile();
        } else {
            System.out.println("Loaded " + om.getAllOrders().size() + " purchase orders.");
        }

        Optional<Order> orderToReceiveOpt = om.getAllOrders().stream()
                .filter(o -> o.getStatus().equals(Order.STATUS_PLACED) && !o.getItems().isEmpty())
                .findFirst();
        if (orderToReceiveOpt.isPresent()) {
            Order order = orderToReceiveOpt.get();
            OrderItem firstItem = order.getItems().get(0);
            System.out.println("\nAttempting to receive items for Order: " + order.getOrderID() + ", Item: " + firstItem.getItemSKU());
            om.receiveOrderItem(order, firstItem, 1);
            System.out.println("Order status after partial receive: " + order.getStatus());
            Item updatedInvItem = inv.getItem(firstItem.getItemSKU());
            if(updatedInvItem != null) {
                System.out.println("Inventory for " + firstItem.getItemSKU() + ": " + updatedInvItem.getQuantity());
            }
            om.saveOrdersToFile();
            inv.saveItemsToFile(Inventory.DEFAULT_ITEMS_FILE_PATH);
        }
        System.out.println("\nOrderManager test finished.");
    }
}
