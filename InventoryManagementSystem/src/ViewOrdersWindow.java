// ViewOrdersWindow.java
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;


public class ViewOrdersWindow extends JDialog {
    private OrderManager orderManager;
    private Inventory inventory; // Needed for receiving items
    private MainInventoryWindow ownerWindow;

    private JTable ordersTable;
    private DefaultTableModel ordersTableModel;
    private JTable orderItemsTable; // For details of a selected order
    private DefaultTableModel orderItemsTableModel;
    private JButton closeButton, viewDetailsButton, receiveItemsButton, cancelOrderButton; // Added cancelOrderButton
    private JComboBox<String> statusFilterComboBox;

    private Order selectedOrder = null; // To keep track of the currently detailed order
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("$#,##0.00", new DecimalFormatSymbols(Locale.US));


    public ViewOrdersWindow(MainInventoryWindow owner, OrderManager orderManager, Inventory inventory) {
        super(owner, "View Purchase Orders", true);
        this.ownerWindow = owner;
        this.orderManager = orderManager;
        this.inventory = inventory;

        initComponents();
        layoutComponents();
        attachEventHandlers();
        loadOrdersData(); // Initial load

        setSize(900, 700);
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        // Orders Table (Master)
        String[] ordersColumns = {"Order ID", "Supplier ID", "Supplier Name", "Order Date", "Status", "Total Cost"};
        ordersTableModel = new DefaultTableModel(ordersColumns, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        ordersTable = new JTable(ordersTableModel);
        ordersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ordersTable.setAutoCreateRowSorter(true); // Enable sorting

        // Order Items Table (Detail)
        String[] itemColumns = {"SKU", "Name", "Ordered", "Received", "Unit Price", "Subtotal"};
        orderItemsTableModel = new DefaultTableModel(itemColumns, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        orderItemsTable = new JTable(orderItemsTableModel);

        closeButton = new JButton("Close");
        viewDetailsButton = new JButton("View/Refresh Details");
        viewDetailsButton.setEnabled(false); // Enabled when an order is selected
        receiveItemsButton = new JButton("Receive Items for Selected Order");
        receiveItemsButton.setEnabled(false); // Enabled for Placed/Partially Received orders
        cancelOrderButton = new JButton("Cancel Selected Order");
        cancelOrderButton.setEnabled(false); // Enabled for Pending/Placed orders

        String[] statuses = {"All", Order.STATUS_PENDING, Order.STATUS_PLACED, Order.STATUS_PARTIALLY_RECEIVED, Order.STATUS_RECEIVED, Order.STATUS_CANCELLED};
        statusFilterComboBox = new JComboBox<>(statuses);
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));

        // Top: Filter and Refresh
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Filter by Status:"));
        topPanel.add(statusFilterComboBox);
        // JButton refreshButton = new JButton("Refresh List");
        // refreshButton.addActionListener(e -> loadOrdersData());
        // topPanel.add(refreshButton);
        add(topPanel, BorderLayout.NORTH);


        // Center: Split pane for Orders and Order Items
        JScrollPane ordersScrollPane = new JScrollPane(ordersTable);
        ordersScrollPane.setBorder(BorderFactory.createTitledBorder("Purchase Orders"));

        JScrollPane itemsScrollPane = new JScrollPane(orderItemsTable);
        itemsScrollPane.setBorder(BorderFactory.createTitledBorder("Order Items (Details)"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, ordersScrollPane, itemsScrollPane);
        splitPane.setResizeWeight(0.5); // Give half space to each initially
        add(splitPane, BorderLayout.CENTER);


        // Bottom: Action Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(viewDetailsButton);
        buttonPanel.add(receiveItemsButton);
        buttonPanel.add(cancelOrderButton);
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void attachEventHandlers() {
        closeButton.addActionListener(e -> dispose());

        statusFilterComboBox.addActionListener(e -> loadOrdersData());

        ordersTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && ordersTable.getSelectedRow() != -1) {
                viewDetailsButton.setEnabled(true);
                int modelRow = ordersTable.convertRowIndexToModel(ordersTable.getSelectedRow());
                String orderId = (String) ordersTableModel.getValueAt(modelRow, 0);
                Optional<Order> orderOpt = orderManager.getOrderById(orderId);
                if (orderOpt.isPresent()) {
                    selectedOrder = orderOpt.get();
                    loadOrderItems(selectedOrder);
                    updateActionButtonsForSelectedOrder();
                } else {
                    selectedOrder = null;
                    orderItemsTableModel.setRowCount(0); // Clear details if order not found
                    updateActionButtonsForSelectedOrder();
                }
            } else if (ordersTable.getSelectedRow() == -1){
                viewDetailsButton.setEnabled(false);
                selectedOrder = null;
                orderItemsTableModel.setRowCount(0);
                updateActionButtonsForSelectedOrder();
            }
        });

        // Double click on orders table to view details
        ordersTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                if (me.getClickCount() == 2) {
                    if (selectedOrder != null) { // Ensure an order is selected
                        viewDetailsButton.doClick(); // Simulate click
                    }
                }
            }
        });

        viewDetailsButton.addActionListener(e -> {
            if (selectedOrder != null) {
                loadOrderItems(selectedOrder); // Refresh items for the selected order
            } else {
                JOptionPane.showMessageDialog(this, "Please select an order to view details.", "No Order Selected", JOptionPane.WARNING_MESSAGE);
            }
        });

        receiveItemsButton.addActionListener(e -> handleReceiveItems());
        cancelOrderButton.addActionListener(e -> handleCancelOrder());
    }

    private void loadOrdersData() {
        ordersTableModel.setRowCount(0); // Clear existing data
        orderItemsTableModel.setRowCount(0); // Clear details
        selectedOrder = null;
        updateActionButtonsForSelectedOrder();

        String filterStatus = (String) statusFilterComboBox.getSelectedItem();
        List<Order> ordersToShow;

        if (filterStatus == null || "All".equalsIgnoreCase(filterStatus)) {
            ordersToShow = orderManager.getAllOrders();
        } else {
            ordersToShow = orderManager.getOrdersByStatus(filterStatus);
        }

        for (Order order : ordersToShow) {
            String supplierName = "N/A";
            if (order.getSupplier() != null) { // Supplier object might have been linked by OrderManager
                supplierName = order.getSupplier().getName();
            } else { // Fallback if supplier object not linked, try finding by ID
                Optional<Supplier> supOpt = MainInventoryWindow.getSupplierManager().findSupplierById(order.getSupplierID());
                if (supOpt.isPresent()) {
                    supplierName = supOpt.get().getName();
                    order.setSupplier(supOpt.get()); // Link it now
                }
            }

            ordersTableModel.addRow(new Object[]{
                    order.getOrderID(),
                    order.getSupplierID(),
                    supplierName,
                    order.getOrderDateString(), // Use formatted date string
                    order.getStatus(),
                    CURRENCY_FORMAT.format(order.getTotalCost())
            });
        }
        if (ordersTable.getRowCount() > 0) {
            ordersTable.setRowSelectionInterval(0,0); // Select first row by default
        }
    }

    private void loadOrderItems(Order order) {
        orderItemsTableModel.setRowCount(0);
        if (order == null) return;

        for (OrderItem oi : order.getItems()) {
            orderItemsTableModel.addRow(new Object[]{
                    oi.getItemSKU(),
                    oi.getItemName(),
                    oi.getOrderedQuantity(),
                    oi.getReceivedQuantity(),
                    CURRENCY_FORMAT.format(oi.getPurchasePrice()),
                    CURRENCY_FORMAT.format(oi.getSubtotal())
            });
        }
    }

    private void updateActionButtonsForSelectedOrder() {
        if (selectedOrder == null) {
            receiveItemsButton.setEnabled(false);
            cancelOrderButton.setEnabled(false);
            viewDetailsButton.setEnabled(false);
            return;
        }
        viewDetailsButton.setEnabled(true);

        String status = selectedOrder.getStatus();
        boolean canReceive = Order.STATUS_PLACED.equals(status) || Order.STATUS_PARTIALLY_RECEIVED.equals(status);
        receiveItemsButton.setEnabled(canReceive && !selectedOrder.isFullyReceived());

        boolean canCancel = Order.STATUS_PENDING.equals(status) || Order.STATUS_PLACED.equals(status);
        cancelOrderButton.setEnabled(canCancel);
    }

    private void handleReceiveItems() {
        if (selectedOrder == null) {
            JOptionPane.showMessageDialog(this, "Please select an order first.", "No Order Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!selectedOrder.getStatus().equals(Order.STATUS_PLACED) && !selectedOrder.getStatus().equals(Order.STATUS_PARTIALLY_RECEIVED)) {
            JOptionPane.showMessageDialog(this, "Items can only be received for 'Placed' or 'Partially Received' orders.", "Invalid Status", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (selectedOrder.isFullyReceived()) {
            JOptionPane.showMessageDialog(this, "This order has already been fully received.", "Order Complete", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // For simplicity, we'll process one item at a time, or allow entering all at once.
        // Let's try a dialog that lists all non-fully-received items.
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2,2,2,2);
        gbc.anchor = GridBagConstraints.WEST;
        int gridY = 0;

        List<OrderItem> itemsToReceive = selectedOrder.getItems().stream()
                .filter(oi -> oi.getReceivedQuantity() < oi.getOrderedQuantity())
                .collect(Collectors.toList());

        if (itemsToReceive.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No items pending receipt for this order.", "No Items to Receive", JOptionPane.INFORMATION_MESSAGE);
            selectedOrder.updateOrderStatusBasedOnReceipts(); // Might mark as Received if logic allows empty and received
            loadOrdersData(); // Refresh view
            return;
        }

        List<JSpinner> spinners = new ArrayList<>();
        List<OrderItem> displayedItems = new ArrayList<>();

        for (OrderItem oi : itemsToReceive) {
            gbc.gridx = 0; gbc.gridy = gridY;
            panel.add(new JLabel(oi.getItemName() + " (SKU: " + oi.getItemSKU() + ") - Ordered: " + oi.getOrderedQuantity() + ", Received: " + oi.getReceivedQuantity() + ". Receive now:"), gbc);

            int maxToReceive = oi.getOrderedQuantity() - oi.getReceivedQuantity();
            SpinnerNumberModel spinnerModel = new SpinnerNumberModel(0, 0, maxToReceive, 1);
            JSpinner quantitySpinner = new JSpinner(spinnerModel);
            quantitySpinner.setPreferredSize(new Dimension(80, quantitySpinner.getPreferredSize().height));
            spinners.add(quantitySpinner);
            displayedItems.add(oi);

            gbc.gridx = 1; gbc.gridy = gridY;
            panel.add(quantitySpinner, gbc);
            gridY++;
        }

        int result = JOptionPane.showConfirmDialog(this, new JScrollPane(panel), "Receive Items for Order " + selectedOrder.getOrderID(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            boolean anyItemReceivedThisSession = false;
            for (int i = 0; i < displayedItems.size(); i++) {
                OrderItem oi = displayedItems.get(i);
                int qtyToReceiveNow = (Integer) spinners.get(i).getValue();
                if (qtyToReceiveNow > 0) {
                    if (orderManager.receiveOrderItem(selectedOrder, oi, qtyToReceiveNow)) {
                        anyItemReceivedThisSession = true;
                    } else {
                        // Error message already shown by receiveOrderItem or OrderItem
                    }
                }
            }
            if (anyItemReceivedThisSession) {
                JOptionPane.showMessageDialog(this, "Item receipts processed. Inventory updated.", "Receipts Processed", JOptionPane.INFORMATION_MESSAGE);
                ownerWindow.loadInventoryData(); // Crucial: Refresh main inventory table
            }
            // Order status is updated within receiveOrderItem via order.updateOrderStatusBasedOnReceipts()
            loadOrdersData(); // Refresh this window's order list (shows updated status and item details)
            if(selectedOrder != null) loadOrderItems(selectedOrder); // Refresh details pane
        }
    }

    private void handleCancelOrder() {
        if (selectedOrder == null) {
            JOptionPane.showMessageDialog(this, "Please select an order to cancel.", "No Order Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!selectedOrder.getStatus().equals(Order.STATUS_PENDING) && !selectedOrder.getStatus().equals(Order.STATUS_PLACED)) {
            JOptionPane.showMessageDialog(this, "Only 'Pending' or 'Placed' orders can be cancelled.", "Invalid Status", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to cancel Order ID: " + selectedOrder.getOrderID() + "?\nThis action cannot be undone.",
                "Confirm Cancellation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            if (orderManager.updateOrderStatus(selectedOrder.getOrderID(), Order.STATUS_CANCELLED)) {
                JOptionPane.showMessageDialog(this, "Order " + selectedOrder.getOrderID() + " has been cancelled.", "Order Cancelled", JOptionPane.INFORMATION_MESSAGE);
                loadOrdersData(); // Refresh the list
            } else {
                JOptionPane.showMessageDialog(this, "Failed to cancel order.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
