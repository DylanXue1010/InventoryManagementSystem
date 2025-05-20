// MakeSaleWindow.java
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
// import java.awt.event.ActionEvent; // Not strictly needed with lambdas
// import java.awt.event.ActionListener; // Not strictly needed with lambdas
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat; // For JFormattedTextField
import java.util.List;
import java.util.Locale;
import java.util.Optional; // For Java 8 Optional, if ReflectionAccess returns it

public class MakeSaleWindow extends JDialog {

    private SalesManager salesManager;
    private Inventory inventory;
    private Sale currentSale;
    private MainInventoryWindow ownerWindow;

    private JTextField skuField;
    private JFormattedTextField quantityField;
    private JFormattedTextField sellingPriceField; // Using JFormattedTextField for price too
    private JLabel itemNameLabel, itemStockLabel, itemCurrentPriceLabel, totalAmountLabel;
    private JButton findItemButton, addItemToSaleButton, removeItemButton, finalizeSaleButton, cancelSaleButton;
    private JTable saleItemsTable;
    private DefaultTableModel saleItemsTableModel;

    private Item foundItem = null;

    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("$#,##0.00", new DecimalFormatSymbols(Locale.US));

    public MakeSaleWindow(MainInventoryWindow owner, SalesManager salesManager, Inventory inventory) {
        super(owner, "Create New Sale", true);
        this.ownerWindow = owner;
        this.salesManager = salesManager;
        this.inventory = inventory;
        this.currentSale = salesManager.createNewSale();

        initComponents();
        layoutComponents();
        attachEventHandlers();

        setTitle("Create New Sale - ID: " + currentSale.getSaleID());
        setSize(800, 650);
        setLocationRelativeTo(owner);
        updateTotalAmountDisplay();
    }

    private void initComponents() {
        skuField = new JTextField(15);
        findItemButton = new JButton("Find Item");
        itemNameLabel = new JLabel("Item Name: -");
        itemStockLabel = new JLabel("Available Stock: -");
        itemCurrentPriceLabel = new JLabel("Current Price: -");

        NumberFormat integerFormat = NumberFormat.getIntegerInstance();
        integerFormat.setGroupingUsed(false);
        quantityField = new JFormattedTextField(integerFormat);
        quantityField.setColumns(5);
        quantityField.setValue(1); // Default quantity

        // Use NumberFormat for price field too for better input control
        NumberFormat currencyInstance = NumberFormat.getNumberInstance(Locale.US); // Or NumberFormat.getCurrencyInstance(Locale.US);
        currencyInstance.setMinimumFractionDigits(2);
        currencyInstance.setMaximumFractionDigits(2);
        currencyInstance.setGroupingUsed(false); // Avoid commas during input
        sellingPriceField = new JFormattedTextField(currencyInstance);
        sellingPriceField.setColumns(8);
        sellingPriceField.setValue(0.00);


        addItemToSaleButton = new JButton("Add to Sale");
        addItemToSaleButton.setEnabled(false); // Initially disabled until an active item is found

        String[] saleTableColumns = {"SKU", "Name", "Qty Sold", "Unit Price", "Subtotal"};
        saleItemsTableModel = new DefaultTableModel(saleTableColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Double-click to edit logic is separate
            }
        };
        saleItemsTable = new JTable(saleItemsTableModel);
        saleItemsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        totalAmountLabel = new JLabel("Total Amount: " + CURRENCY_FORMAT.format(0.00));
        totalAmountLabel.setFont(new Font("SansSerif", Font.BOLD, 18));

        removeItemButton = new JButton("Remove Selected Item");
        finalizeSaleButton = new JButton("Finalize Sale");
        cancelSaleButton = new JButton("Cancel Sale");
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));

        JPanel itemInputPanel = new JPanel(new GridBagLayout());
        itemInputPanel.setBorder(BorderFactory.createTitledBorder("Add Item to Sale"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; itemInputPanel.add(new JLabel("Item SKU:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; itemInputPanel.add(skuField, gbc);
        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; itemInputPanel.add(findItemButton, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL;
        itemNameLabel.setPreferredSize(new Dimension(350, itemNameLabel.getPreferredSize().height));
        itemInputPanel.add(itemNameLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE;
        itemInputPanel.add(itemStockLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        itemInputPanel.add(itemCurrentPriceLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE;
        itemInputPanel.add(new JLabel("Quantity:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; itemInputPanel.add(quantityField, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.fill = GridBagConstraints.NONE;
        itemInputPanel.add(new JLabel("Selling Price: $"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; itemInputPanel.add(sellingPriceField, gbc);

        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 3; gbc.anchor = GridBagConstraints.CENTER; gbc.fill = GridBagConstraints.NONE;
        itemInputPanel.add(addItemToSaleButton, gbc);

        add(itemInputPanel, BorderLayout.NORTH);

        JScrollPane tableScrollPane = new JScrollPane(saleItemsTable);
        tableScrollPane.setBorder(BorderFactory.createTitledBorder("Current Sale Items (Double-click row to edit)"));
        add(tableScrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(10,10));
        totalAmountLabel.setHorizontalAlignment(SwingConstants.CENTER);
        bottomPanel.add(totalAmountLabel, BorderLayout.NORTH);

        JPanel actionButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionButtonPanel.add(removeItemButton);
        actionButtonPanel.add(cancelSaleButton);
        actionButtonPanel.add(finalizeSaleButton);
        bottomPanel.add(actionButtonPanel, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void attachEventHandlers() {
        findItemButton.addActionListener(e -> findItem());
        addItemToSaleButton.addActionListener(e -> addItemToSale());
        removeItemButton.addActionListener(e -> removeItemFromSale());
        finalizeSaleButton.addActionListener(e -> finalizeSale());
        cancelSaleButton.addActionListener(e -> cancelSale());
        skuField.addActionListener(e -> findItem()); // Allow Enter in SKU field to find item

        saleItemsTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) { // Double-click to edit
                    int selectedRow = saleItemsTable.getSelectedRow();
                    if (selectedRow >= 0) {
                        // Logic to get SaleItem from currentSale and show edit dialog
                        // This requires Sale.getItemsSold() to return a mutable list or provide modification methods
                        // For simplicity, using ReflectionAccess or assuming Sale class allows item modification.
                        String skuOfItemToEdit = (String) saleItemsTableModel.getValueAt(selectedRow, 0);
                        List<Sale.SaleItem> actualSaleItems = ReflectionAccess.getActualItemsSoldList(currentSale);
                        Sale.SaleItem itemToEdit = null;
                        if (actualSaleItems != null) {
                            for (Sale.SaleItem si : actualSaleItems) {
                                if (si.getSku().equals(skuOfItemToEdit)) {
                                    // Assuming first match is the one if SKUs can repeat (though unlikely for this structure)
                                    itemToEdit = si;
                                    break;
                                }
                            }
                        }
                        if (itemToEdit != null) {
                            showEditSaleItemDialog(itemToEdit);
                        } else {
                            System.err.println("MakeSaleWindow: Could not find SKU " + skuOfItemToEdit + " in current sale for editing.");
                        }
                    }
                }
            }
        });
    }

    private void findItem() {
        String sku = skuField.getText().trim();
        addItemToSaleButton.setEnabled(false); // Disable by default until an active item is confirmed
        if (sku.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter an item SKU.", "SKU Missing", JOptionPane.WARNING_MESSAGE);
            return;
        }
        foundItem = inventory.getItem(sku);
        if (foundItem != null) {
            String statusInfo = "";
            if (Item.STATUS_INACTIVE.equals(foundItem.getStatus())) {
                statusInfo = " (Status: Inactive - Cannot be sold)";
                itemNameLabel.setForeground(Color.RED); // Visual cue for inactive
            } else if (Item.STATUS_ACTIVE.equals(foundItem.getStatus())) {
                statusInfo = " (Status: Active)";
                itemNameLabel.setForeground(Color.BLACK); // Default color
                addItemToSaleButton.setEnabled(true); // Enable button for active items
            } else {
                statusInfo = " (Status: " + foundItem.getStatus() + ")"; // Other statuses
                itemNameLabel.setForeground(Color.ORANGE); // Cue for other statuses
            }

            itemNameLabel.setText("Item Name: " + foundItem.getName() + statusInfo);
            itemStockLabel.setText("Available Stock: " + foundItem.getQuantity());
            itemCurrentPriceLabel.setText("Current Price: " + CURRENCY_FORMAT.format(foundItem.getPrice()));
            sellingPriceField.setValue(foundItem.getPrice()); // Pre-fill selling price
            quantityField.setValue(1); // Reset quantity to 1
            quantityField.requestFocus(); // Focus quantity field
        } else {
            itemNameLabel.setText("Item Name: - Not Found -");
            itemNameLabel.setForeground(Color.BLACK);
            itemStockLabel.setText("Available Stock: -");
            itemCurrentPriceLabel.setText("Current Price: -");
            sellingPriceField.setValue(0.00);
            foundItem = null;
            JOptionPane.showMessageDialog(this, "Item with SKU '" + sku + "' not found in inventory.", "Item Not Found", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addItemToSale() {
        if (foundItem == null) {
            JOptionPane.showMessageDialog(this, "Please find a valid item first using its SKU.", "No Item Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // --- CRITICAL CHECK: Item Status ---
        if (!Item.STATUS_ACTIVE.equals(foundItem.getStatus())) { // Only allow ACTIVE items
            JOptionPane.showMessageDialog(this,
                    "Item '" + foundItem.getName() + "' (SKU: " + foundItem.getSku() + ") is not Active (Status: " + foundItem.getStatus() + ") and cannot be sold.",
                    "Item Not Available for Sale",
                    JOptionPane.ERROR_MESSAGE);
            // Do not clear fields here, user might want to see why it failed. FindItem already updated display.
            return; // Prevent adding to sale
        }
        // --- END CRITICAL CHECK ---

        int quantity;
        double sellingPrice;
        try {
            // For JFormattedTextField, it's better to get the value directly
            quantity = ((Number) quantityField.getValue()).intValue();
            if (quantity <= 0) {
                JOptionPane.showMessageDialog(this, "Quantity must be a positive number.", "Invalid Quantity", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (Exception ex) { // Catch broader exceptions as getValue() might return non-Number if input is invalid
            JOptionPane.showMessageDialog(this, "Invalid quantity format. Please enter a whole number.", "Invalid Quantity", JOptionPane.ERROR_MESSAGE);
            quantityField.setValue(1); // Reset to default
            return;
        }

        try {
            sellingPrice = ((Number) sellingPriceField.getValue()).doubleValue();
            if (sellingPrice < 0) { // Allow 0 for promotional items, but not negative
                JOptionPane.showMessageDialog(this, "Selling price cannot be negative.", "Invalid Price", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid selling price format. Please enter a valid number.", "Invalid Price", JOptionPane.ERROR_MESSAGE);
            sellingPriceField.setValue(foundItem != null ? foundItem.getPrice() : 0.00); // Reset
            return;
        }

        // Check if item already in sale, offer to update
        List<Sale.SaleItem> actualItems = ReflectionAccess.getActualItemsSoldList(currentSale); // Helper needed
        Sale.SaleItem existingSaleItem = null;
        if (actualItems != null) {
            for (Sale.SaleItem si : actualItems) {
                if (si.getSku().equals(foundItem.getSku())) {
                    existingSaleItem = si;
                    break;
                }
            }
        }

        if (existingSaleItem != null) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Item " + foundItem.getName() + " (SKU: " + foundItem.getSku() + ") is already in the sale.\n" +
                            "Current Qty: " + existingSaleItem.getQuantitySold() + ". Add " + quantity + " more (Total: " + (existingSaleItem.getQuantitySold() + quantity) + ")?\n" +
                            "(Choosing 'No' will not add this quantity).",
                    "Item Already in Sale", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                // Check stock for the *additional* quantity
                if (foundItem.getQuantity() < quantity) { // Check if current stock can cover the *newly added* quantity
                    int confirmStock = JOptionPane.showConfirmDialog(this,
                            "Warning: Adding " + quantity + " units of " + foundItem.getName() +
                                    " exceeds available stock (" + foundItem.getQuantity() + " remaining for this addition).\n" +
                                    "Add anyway (sale might fail or be partially filled on finalize)?",
                            "Stock Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (confirmStock == JOptionPane.NO_OPTION) return;
                }
                existingSaleItem.setQuantitySold(existingSaleItem.getQuantitySold() + quantity);
                // Optionally update price or keep existing price for that line; current logic updates quantity only
                currentSale.calculateTotalAmount();
            } else {
                clearItemInputFields(); // Clear inputs if user cancels update
                return; // User chose not to update
            }
        } else {
            // Item not yet in sale, check stock for the full requested quantity
            if (foundItem.getQuantity() < quantity) {
                int choice = JOptionPane.showConfirmDialog(this,
                        "Warning: Requested quantity (" + quantity + ") for " + foundItem.getName() +
                                " exceeds available stock (" + foundItem.getQuantity() + ").\n" +
                                "Add to sale anyway (sale might fail or be partially filled on finalize)?",
                        "Stock Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (choice == JOptionPane.NO_OPTION) {
                    return;
                }
            }
            // Add as a new item to the sale
            if (!currentSale.addItemToSale(foundItem, quantity, sellingPrice)) {
                JOptionPane.showMessageDialog(this, "Could not add item to sale. Check console for details.", "Error Adding Item", JOptionPane.ERROR_MESSAGE);
                return; // Stop if addItemToSale failed
            }
        }

        refreshSaleItemsTable();
        updateTotalAmountDisplay();
        clearItemInputFields();
    }

    private void showEditSaleItemDialog(Sale.SaleItem saleItemToEdit) {
        if (!currentSale.getStatus().equals(Sale.STATUS_PENDING)) {
            JOptionPane.showMessageDialog(this,
                    "Cannot edit items. Sale status is: " + currentSale.getStatus(),
                    "Edit Not Allowed", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JPanel editPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 3, 3, 3);
        gbc.anchor = GridBagConstraints.WEST;

        NumberFormat integerFormat = NumberFormat.getIntegerInstance();
        integerFormat.setGroupingUsed(false);
        JFormattedTextField editQuantityField = new JFormattedTextField(integerFormat);
        editQuantityField.setValue(saleItemToEdit.getQuantitySold());
        editQuantityField.setColumns(7);

        NumberFormat decimalFormat = NumberFormat.getNumberInstance(Locale.US);
        decimalFormat.setMinimumFractionDigits(2);
        decimalFormat.setMaximumFractionDigits(2);
        decimalFormat.setGroupingUsed(false);
        JFormattedTextField editPriceField = new JFormattedTextField(decimalFormat);
        editPriceField.setValue(saleItemToEdit.getPriceAtSale());
        editPriceField.setColumns(7);

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth=2; editPanel.add(new JLabel("Editing: " + saleItemToEdit.getItemName()), gbc);
        gbc.gridy++; gbc.gridwidth=1;
        gbc.gridx = 0; editPanel.add(new JLabel("New Quantity:"), gbc);
        gbc.gridx = 1; editPanel.add(editQuantityField, gbc);
        gbc.gridy++;
        gbc.gridx = 0; editPanel.add(new JLabel("New Selling Price: $"), gbc);
        gbc.gridx = 1; editPanel.add(editPriceField, gbc);

        int result = JOptionPane.showConfirmDialog(this, editPanel,
                "Edit Sale Item (SKU: " + saleItemToEdit.getSku() + ")",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                int newQuantity = ((Number) editQuantityField.getValue()).intValue();
                double newPrice = ((Number) editPriceField.getValue()).doubleValue();

                if (newQuantity <= 0) {
                    JOptionPane.showMessageDialog(this, "Quantity must be positive.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (newPrice < 0) {
                    JOptionPane.showMessageDialog(this, "Price cannot be negative.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                Item inventoryItem = inventory.getItem(saleItemToEdit.getSku());
                int quantityChange = newQuantity - saleItemToEdit.getQuantitySold();

                if (inventoryItem != null && quantityChange > 0 && inventoryItem.getQuantity() < quantityChange) {
                    int choice = JOptionPane.showConfirmDialog(this,
                            "Warning: Increasing quantity for " + saleItemToEdit.getItemName() +
                                    " by " + quantityChange + " requires " + quantityChange + " more units, but only " + inventoryItem.getQuantity() + " are available in stock.\n" +
                                    "Update anyway (sale might fail or be partially filled on finalize)?",
                            "Stock Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (choice == JOptionPane.NO_OPTION) return;
                }

                saleItemToEdit.setQuantitySold(newQuantity);
                saleItemToEdit.setPriceAtSale(newPrice); // This will internally call recalculateSubtotal
                currentSale.calculateTotalAmount(); // Recalculate grand total for the Sale

                refreshSaleItemsTable();
                updateTotalAmountDisplay();
                System.out.println("SaleItem " + saleItemToEdit.getSku() + " updated. New Qty: " + newQuantity + ", New Price: " + newPrice);

            } catch (Exception ex) { // Catch ClassCastException or others if JFormattedTextField value is not Number
                JOptionPane.showMessageDialog(this, "Invalid number format for quantity or price.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }


    private void clearItemInputFields() {
        skuField.setText("");
        itemNameLabel.setText("Item Name: -");
        itemNameLabel.setForeground(Color.BLACK); // Reset color
        itemStockLabel.setText("Available Stock: -");
        itemCurrentPriceLabel.setText("Current Price: -");
        sellingPriceField.setValue(0.00);
        quantityField.setValue(1);
        foundItem = null;
        addItemToSaleButton.setEnabled(false); // Disable until an active item is found
        skuField.requestFocus();
    }

    private void removeItemFromSale() {
        int selectedRow = saleItemsTable.getSelectedRow();
        if (selectedRow >= 0) {
            String skuToRemove = (String) saleItemsTableModel.getValueAt(selectedRow, 0);
            // To remove from Sale's actual list, we need a robust way.
            // Assuming Sale.removeItemFromSale(sku) exists and works or using ReflectionAccess.
            List<Sale.SaleItem> actualItems = ReflectionAccess.getActualItemsSoldList(currentSale);
            Sale.SaleItem itemToRemoveInstance = null;
            if (actualItems != null) {
                // If SKUs could be duplicated in the sale list (e.g. same item added twice)
                // this would remove the first instance. If table represents the list directly and
                // is not sorted/filtered by user, selectedRow could map to index.
                // For now, assume SKU is unique identifier for item *within the current displayed sale lines*.
                // A more robust way would be to pass the exact SaleItem object or its unique index if possible.
                if (selectedRow < actualItems.size()) { // Basic check
                    Sale.SaleItem tempItem = actualItems.get(selectedRow);
                    if (tempItem.getSku().equals(skuToRemove)) { // Confirm it's the right one
                        itemToRemoveInstance = tempItem;
                    }
                }
                // Fallback if direct index didn't match or wasn't reliable
                if (itemToRemoveInstance == null) {
                    for(Sale.SaleItem si : actualItems) {
                        if(si.getSku().equals(skuToRemove)){
                            itemToRemoveInstance = si;
                            break;
                        }
                    }
                }
            }

            if (itemToRemoveInstance != null && actualItems != null) {
                if (actualItems.remove(itemToRemoveInstance)) {
                    currentSale.calculateTotalAmount(); // Recalculate total in Sale object
                    refreshSaleItemsTable();
                    updateTotalAmountDisplay();
                    System.out.println("Removed item SKU " + skuToRemove + " from current sale.");
                } else {
                    System.err.println("Failed to remove item instance " + skuToRemove + " from actual list, though found.");
                }
            } else {
                JOptionPane.showMessageDialog(this, "Could not find the exact item with SKU " + skuToRemove + " in the current sale's internal list for removal.", "Item Not Found in Sale", JOptionPane.ERROR_MESSAGE);
            }

        } else {
            JOptionPane.showMessageDialog(this, "Please select an item from the sale list to remove.", "No Item Selected", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void finalizeSale() {
        if (currentSale.getItemsSold().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cannot finalize an empty sale.", "Empty Sale", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Finalize this sale for " + CURRENCY_FORMAT.format(currentSale.getTotalAmount()) + "?\n" +
                        "Inventory stock levels will be updated.",
                "Confirm Finalize Sale", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            boolean success = salesManager.finalizeSale(currentSale);
            if (success) {
                JOptionPane.showMessageDialog(this, "Sale " + currentSale.getSaleID() + " finalized successfully!", "Sale Completed", JOptionPane.INFORMATION_MESSAGE);
                if (ownerWindow != null) {
                    ownerWindow.loadInventoryData(); // Refresh main inventory view
                }
                dispose(); // Close the MakeSaleWindow
            } else {
                // Error message from finalizeSale (in Sale or SalesManager) should provide details.
                JOptionPane.showMessageDialog(this, "Failed to finalize sale. Check stock levels or console for errors.\nSale status is currently: " + currentSale.getStatus(), "Finalization Failed", JOptionPane.ERROR_MESSAGE);
                // Refresh main inventory view even on failure, as some stock might have been pre-checked or partially affected if logic allows
                if (ownerWindow != null) {
                    ownerWindow.loadInventoryData();
                }
            }
        }
    }

    private void cancelSale() {
        // If items were added, confirm cancellation.
        // If no items, just close.
        if (currentSale.getItemsSold().isEmpty()){
            System.out.println("Sale " + currentSale.getSaleID() + " cancelled (was empty). Not saved.");
            // The pending sale object in SalesManager will be ignored on save if it remains PENDING
            dispose();
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to cancel this sale?\nAny items added will be discarded from this transaction.\nThe sale will be marked as Cancelled and saved for record.",
                "Confirm Cancel Sale", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            currentSale.setStatus(Sale.STATUS_CANCELLED); // Set status to Cancelled
            System.out.println("Sale " + currentSale.getSaleID() + " marked as Cancelled.");
            // SalesManager will save this sale with "Cancelled" status at application shutdown or next save point.
            dispose();
        }
    }

    private void refreshSaleItemsTable() {
        saleItemsTableModel.setRowCount(0);
        // It's better to get the items directly from the currentSale object for display
        List<Sale.SaleItem> itemsInSale = ReflectionAccess.getActualItemsSoldList(currentSale);
        if (itemsInSale == null) { // Fallback if reflection fails
            itemsInSale = currentSale.getItemsSold(); // This gets a copy, so edits won't reflect if reflection failed
            System.err.println("MakeSaleWindow Warning: Refreshing sale table from a copy of items. Edits might not reflect if reflection failed.");
        }

        for (Sale.SaleItem si : itemsInSale) {
            saleItemsTableModel.addRow(new Object[]{
                    si.getSku(),
                    si.getItemName(),
                    si.getQuantitySold(),
                    CURRENCY_FORMAT.format(si.getPriceAtSale()),
                    CURRENCY_FORMAT.format(si.getSubtotal())
            });
        }
    }

    private void updateTotalAmountDisplay() {
        currentSale.calculateTotalAmount(); // Ensure total is calculated in Sale object
        totalAmountLabel.setText("Total Amount: " + CURRENCY_FORMAT.format(currentSale.getTotalAmount()));
    }
}

// Helper class for reflection to access the actual itemsSold list in Sale object
// Ideally, Sale class should provide methods to modify its internal list if needed by UI.
// Using reflection is a workaround if Sale class is not easily modifiable for this.
class ReflectionAccess {
    public static List<Sale.SaleItem> getActualItemsSoldList(Sale saleInstance) {
        try {
            java.lang.reflect.Field field = Sale.class.getDeclaredField("itemsSold");
            field.setAccessible(true);
            // Type safety: Ensure the cast is correct.
            @SuppressWarnings("unchecked") // Suppress warning if confident about the type
            List<Sale.SaleItem> list = (List<Sale.SaleItem>) field.get(saleInstance);
            return list;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            System.err.println("ReflectionAccess Error: Could not access 'itemsSold' field in Sale object: " + e.getMessage());
            // e.printStackTrace(); // Uncomment for detailed stack trace during debugging
            return null; // Return null or an empty list to indicate failure
        }
    }
}
