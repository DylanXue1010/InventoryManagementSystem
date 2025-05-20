// CreateOrderWindow.java
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
// import java.awt.event.ActionEvent; // Not strictly needed if using lambdas
// import java.awt.event.ActionListener; // Not strictly needed if using lambdas
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class CreateOrderWindow extends JDialog {
    private OrderManager orderManager;
    private Inventory inventory;
    private SupplierManager supplierManager;
    private MainInventoryWindow ownerWindow;

    private JComboBox<SupplierWrapper> supplierComboBox;
    private JTextField itemSkuField, quantityField, purchasePriceField;
    private JButton findItemButton, addItemButton, createOrderButton, cancelButton;
    private JTable orderItemsTable;
    private DefaultTableModel orderItemsTableModel;
    private JLabel currentItemNameLabel, totalOrderCostLabel;

    private Item currentFoundItem = null;
    private List<OrderItem> currentOrderItemsList = new ArrayList<>();
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("$#,##0.00", new DecimalFormatSymbols(Locale.US));

    private static class SupplierWrapper {
        private Supplier supplier;
        public SupplierWrapper(Supplier supplier) { this.supplier = supplier; }
        public Supplier getSupplier() { return supplier; }
        @Override public String toString() {
            if (supplier == null || "N/A".equals(supplier.getSupplierID())) {
                return "No Suppliers Available";
            }
            return supplier.getName() + " (ID: " + supplier.getSupplierID() + ")";
        }
    }

    public CreateOrderWindow(MainInventoryWindow owner, OrderManager orderManager, Inventory inventory, SupplierManager supplierManager) {
        super(owner, "Create New Purchase Order", true);
        this.ownerWindow = owner;
        this.orderManager = orderManager;
        this.inventory = inventory;
        this.supplierManager = supplierManager;

        initComponents();
        layoutComponents();
        attachEventHandlers();
        populateSupplierComboBox();
        updateTotalCostDisplay();

        setSize(750, 600);
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        supplierComboBox = new JComboBox<>();

        itemSkuField = new JTextField(15);
        findItemButton = new JButton("Find Item");
        currentItemNameLabel = new JLabel("Item: -"); // English

        quantityField = new JTextField(5);
        purchasePriceField = new JTextField(8);

        addItemButton = new JButton("Add Item to Order");
        createOrderButton = new JButton("Create Purchase Order");
        cancelButton = new JButton("Cancel");

        String[] tableColumns = {"SKU", "Name", "Ordered Qty", "Purchase Price", "Subtotal"};
        orderItemsTableModel = new DefaultTableModel(tableColumns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        orderItemsTable = new JTable(orderItemsTableModel);

        totalOrderCostLabel = new JLabel("Total Cost: " + CURRENCY_FORMAT.format(0.00));
        totalOrderCostLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
    }

    private void populateSupplierComboBox() {
        supplierComboBox.removeAllItems();
        if (supplierManager == null) {
            System.err.println("CreateOrderWindow Error: SupplierManager is null. Cannot populate suppliers.");
            supplierComboBox.addItem(new SupplierWrapper(new Supplier("N/A", "Supplier System Error", "")));
            supplierComboBox.setEnabled(false);
            createOrderButton.setEnabled(false);
            return;
        }

        List<Supplier> suppliers = supplierManager.getAllSuppliers();
        if (suppliers.isEmpty()) {
            supplierComboBox.addItem(new SupplierWrapper(new Supplier("N/A", "No Suppliers Available", "")));
            supplierComboBox.setEnabled(false);
            createOrderButton.setEnabled(false);
        } else {
            for (Supplier s : suppliers) {
                supplierComboBox.addItem(new SupplierWrapper(s));
            }
            supplierComboBox.setEnabled(true);
            createOrderButton.setEnabled(true);
        }
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));

        JPanel topInputPanel = new JPanel(new GridBagLayout());
        topInputPanel.setBorder(BorderFactory.createTitledBorder("Order Details"));
        GridBagConstraints gbcTop = new GridBagConstraints();
        gbcTop.insets = new Insets(5, 5, 5, 5);
        gbcTop.anchor = GridBagConstraints.WEST;

        gbcTop.gridx = 0; gbcTop.gridy = 0; topInputPanel.add(new JLabel("Item SKU:"), gbcTop); // English
        gbcTop.gridx = 1; gbcTop.gridy = 0; gbcTop.fill = GridBagConstraints.HORIZONTAL; topInputPanel.add(itemSkuField, gbcTop);
        gbcTop.gridx = 2; gbcTop.gridy = 0; gbcTop.fill = GridBagConstraints.NONE; topInputPanel.add(findItemButton, gbcTop);

        gbcTop.gridx = 0; gbcTop.gridy = 1; topInputPanel.add(new JLabel("Select Supplier:"), gbcTop); // English
        gbcTop.gridx = 1; gbcTop.gridy = 1; gbcTop.gridwidth = 2; gbcTop.fill = GridBagConstraints.HORIZONTAL;
        supplierComboBox.setPrototypeDisplayValue(new SupplierWrapper(new Supplier("TEMP_ID_PROTOTYPE", "Longest Expected Supplier Name Here Inc.", "")));
        topInputPanel.add(supplierComboBox, gbcTop);
        gbcTop.gridwidth = 1;

        add(topInputPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(5,5));
        JPanel itemEntryPanel = new JPanel(new GridBagLayout());
        itemEntryPanel.setBorder(BorderFactory.createTitledBorder("Item to Add")); // English
        GridBagConstraints gbcItem = new GridBagConstraints();
        gbcItem.insets = new Insets(3, 3, 3, 3);
        gbcItem.anchor = GridBagConstraints.WEST;

        gbcItem.gridx = 0; gbcItem.gridy = 0; gbcItem.gridwidth = 3; gbcItem.fill = GridBagConstraints.HORIZONTAL;
        currentItemNameLabel.setPreferredSize(new Dimension(350, currentItemNameLabel.getPreferredSize().height));
        itemEntryPanel.add(currentItemNameLabel, gbcItem);
        gbcItem.gridwidth = 1;

        gbcItem.gridx = 0; gbcItem.gridy = 1; itemEntryPanel.add(new JLabel("Order Qty:"), gbcItem); // English
        gbcItem.gridx = 1; gbcItem.gridy = 1; gbcItem.fill = GridBagConstraints.HORIZONTAL; itemEntryPanel.add(quantityField, gbcItem);

        gbcItem.gridx = 0; gbcItem.gridy = 2; itemEntryPanel.add(new JLabel("Purchase Price ($/unit):"), gbcItem); // English
        gbcItem.gridx = 1; gbcItem.gridy = 2; gbcItem.fill = GridBagConstraints.HORIZONTAL; itemEntryPanel.add(purchasePriceField, gbcItem);

        gbcItem.gridx = 0; gbcItem.gridy = 3; gbcItem.gridwidth = 2; gbcItem.anchor = GridBagConstraints.CENTER; gbcItem.fill = GridBagConstraints.NONE;
        itemEntryPanel.add(addItemButton, gbcItem);

        centerPanel.add(itemEntryPanel, BorderLayout.NORTH);
        centerPanel.add(new JScrollPane(orderItemsTable), BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(10,10));
        JPanel totalPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        totalPanel.add(totalOrderCostLabel);
        bottomPanel.add(totalPanel, BorderLayout.NORTH);

        JPanel actionButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionButtonPanel.add(cancelButton);
        actionButtonPanel.add(createOrderButton);
        bottomPanel.add(actionButtonPanel, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void attachEventHandlers() {
        findItemButton.addActionListener(e -> findItem());
        itemSkuField.addActionListener(e -> findItem());
        addItemButton.addActionListener(e -> addItemToCurrentOrder());
        createOrderButton.addActionListener(e -> createPurchaseOrder());
        cancelButton.addActionListener(e -> dispose());
    }

    private void findItem() {
        String sku = itemSkuField.getText().trim();
        if (sku.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter an item SKU.", "SKU Missing", JOptionPane.WARNING_MESSAGE); // English
            return;
        }
        currentFoundItem = inventory.getItem(sku);

        if (currentFoundItem != null) {
            currentItemNameLabel.setText("Item: " + currentFoundItem.getName() + " (Current Stock: " + currentFoundItem.getQuantity() + ")"); // English
            purchasePriceField.setText(String.format(Locale.US, "%.2f", currentFoundItem.getPrice()));
            quantityField.setText("1");
            quantityField.requestFocus();

            String itemSupplierId = currentFoundItem.getSupplier();
            if (itemSupplierId != null && !itemSupplierId.isEmpty()) {
                boolean supplierFoundAndSet = false;
                for (int i = 0; i < supplierComboBox.getItemCount(); i++) {
                    SupplierWrapper wrapper = supplierComboBox.getItemAt(i);
                    if (wrapper != null && wrapper.getSupplier() != null &&
                            itemSupplierId.equals(wrapper.getSupplier().getSupplierID())) {
                        supplierComboBox.setSelectedIndex(i);
                        supplierFoundAndSet = true;
                        System.out.println("Supplier " + wrapper.getSupplier().getName() + " auto-selected for item " + currentFoundItem.getName());
                        break;
                    }
                }
                if (!supplierFoundAndSet) {
                    JOptionPane.showMessageDialog(this,
                            "Item's designated supplier (ID: " + itemSupplierId + ") is not in the current supplier list or is not set for this item.\nPlease select a supplier manually.",
                            "Supplier Information", JOptionPane.INFORMATION_MESSAGE); // English
                }
            } else {
                JOptionPane.showMessageDialog(this,
                        "This item does not have a designated supplier. Please select a supplier manually.",
                        "Supplier Information", JOptionPane.INFORMATION_MESSAGE); // English
            }

        } else {
            currentItemNameLabel.setText("Item: - Not Found -"); // English
            purchasePriceField.setText("");
            currentFoundItem = null;
            JOptionPane.showMessageDialog(this, "Item with SKU '" + sku + "' not found in inventory.", "Item Not Found", JOptionPane.INFORMATION_MESSAGE); // English
        }
    }

    private void addItemToCurrentOrder() {
        if (currentFoundItem == null) {
            JOptionPane.showMessageDialog(this, "Please find a valid item first.", "No Item Selected", JOptionPane.WARNING_MESSAGE); // English
            return;
        }

        SupplierWrapper selectedSupplierWrapper = (SupplierWrapper) supplierComboBox.getSelectedItem();
        if (selectedSupplierWrapper == null || selectedSupplierWrapper.getSupplier() == null || "N/A".equals(selectedSupplierWrapper.getSupplier().getSupplierID())) {
            JOptionPane.showMessageDialog(this, "Please select a valid supplier.", "Supplier Not Selected", JOptionPane.ERROR_MESSAGE); // English
            return;
        }

        int quantity;
        double price;
        try {
            quantity = Integer.parseInt(quantityField.getText().trim());
            if (quantity <= 0) {
                JOptionPane.showMessageDialog(this, "Ordered quantity must be a positive number.", "Invalid Quantity", JOptionPane.ERROR_MESSAGE); // English
                return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid quantity format.", "Input Error", JOptionPane.ERROR_MESSAGE); // English
            return;
        }
        try {
            price = Double.parseDouble(purchasePriceField.getText().trim().replace(',', '.'));
            if (price < 0) {
                JOptionPane.showMessageDialog(this, "Purchase price cannot be negative.", "Invalid Price", JOptionPane.ERROR_MESSAGE); // English
                return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid purchase price format.", "Input Error", JOptionPane.ERROR_MESSAGE); // English
            return;
        }

        Optional<OrderItem> existingOrderItemOpt = currentOrderItemsList.stream()
                .filter(oi -> oi.getItemSKU().equals(currentFoundItem.getSku()))
                .findFirst();

        if (existingOrderItemOpt.isPresent()) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Item " + currentFoundItem.getName() + " is already in the order list.\n" +
                            "Do you want to update its quantity and price?",
                    "Item Exists", JOptionPane.YES_NO_OPTION); // English
            if (choice == JOptionPane.YES_OPTION) {
                OrderItem existingOrderItem = existingOrderItemOpt.get();
                existingOrderItem.setOrderedQuantity(quantity);
                existingOrderItem.setPurchasePrice(price);
            } else {
                return;
            }
        } else {
            OrderItem newOrderItem = new OrderItem(currentFoundItem.getSku(), currentFoundItem.getName(), quantity, price);
            currentOrderItemsList.add(newOrderItem);
        }

        refreshOrderItemsTable();
        updateTotalCostDisplay();
        clearItemInputFieldsAfterAdd();
    }

    private void refreshOrderItemsTable() {
        orderItemsTableModel.setRowCount(0);
        for (OrderItem oi : currentOrderItemsList) {
            orderItemsTableModel.addRow(new Object[]{
                    oi.getItemSKU(),
                    oi.getItemName(),
                    oi.getOrderedQuantity(),
                    CURRENCY_FORMAT.format(oi.getPurchasePrice()),
                    CURRENCY_FORMAT.format(oi.getSubtotal())
            });
        }
    }

    private void updateTotalCostDisplay() {
        double total = 0;
        for (OrderItem oi : currentOrderItemsList) {
            total += oi.getSubtotal();
        }
        totalOrderCostLabel.setText("Total Cost: " + CURRENCY_FORMAT.format(total)); // English
    }

    private void clearItemInputFieldsAfterAdd() {
        itemSkuField.setText("");
        currentItemNameLabel.setText("Item: -"); // English
        quantityField.setText("");
        purchasePriceField.setText("");
        currentFoundItem = null;
        itemSkuField.requestFocus();
    }

    private void createPurchaseOrder() {
        SupplierWrapper selectedSupplierWrapper = (SupplierWrapper) supplierComboBox.getSelectedItem();
        if (selectedSupplierWrapper == null || selectedSupplierWrapper.getSupplier() == null || "N/A".equals(selectedSupplierWrapper.getSupplier().getSupplierID())) {
            JOptionPane.showMessageDialog(this, "Please select a valid supplier.", "Supplier Not Selected", JOptionPane.ERROR_MESSAGE); // English
            return;
        }
        Supplier selectedSupplier = selectedSupplierWrapper.getSupplier();

        if (currentOrderItemsList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cannot create an empty purchase order. Please add items.", "Empty Order", JOptionPane.WARNING_MESSAGE); // English
            return;
        }

        Order newOrder = orderManager.createNewOrder(selectedSupplier);
        if (newOrder == null) {
            JOptionPane.showMessageDialog(this, "Failed to initialize new order in OrderManager.", "Order Creation Error", JOptionPane.ERROR_MESSAGE); // English
            return;
        }

        for (OrderItem oi : currentOrderItemsList) {
            newOrder.addItem(oi);
        }
        newOrder.setStatus(Order.STATUS_PLACED);

        JOptionPane.showMessageDialog(this, "Purchase Order " + newOrder.getOrderID() + " created successfully for " + selectedSupplier.getName() + "!",
                "Order Created", JOptionPane.INFORMATION_MESSAGE); // English

        dispose();
    }
}
