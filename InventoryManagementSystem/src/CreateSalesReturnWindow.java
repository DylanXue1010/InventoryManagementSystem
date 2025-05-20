// CreateSalesReturnWindow.java
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class CreateSalesReturnWindow extends JDialog {
    private SalesReturnManager salesReturnManager;
    private SalesManager salesManager;
    private Inventory inventory;
    private MainInventoryWindow ownerWindow; // Main application window

    private JTextField originalSaleIdField;
    private JButton findSaleButton;
    private JTable originalSaleItemsTable;
    private DefaultTableModel originalSaleItemsTableModel;
    private JTable returnItemsTable;
    private DefaultTableModel returnItemsTableModel;

    private JButton addItemToReturnButton, removeItemFromReturnButton, processReturnButton, cancelButton;
    private JLabel totalRefundLabel;
    private JTextArea notesField;

    private Sale originalSale = null;
    private List<SalesReturnItem> currentReturnItemsList = new ArrayList<>();
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("$#,##0.00", new DecimalFormatSymbols(Locale.US));

    // Existing constructor (can be kept for direct opening if needed, or marked deprecated)
    public CreateSalesReturnWindow(MainInventoryWindow owner, SalesReturnManager salesReturnManager, SalesManager salesManager, Inventory inventory) {
        this(owner, salesReturnManager, salesManager, inventory, null); // Call the new constructor with null saleId
    }

    // New constructor that accepts an originalSaleId
    public CreateSalesReturnWindow(MainInventoryWindow owner, SalesReturnManager salesReturnManager, SalesManager salesManager, Inventory inventory, String originalSaleIdToLoad) {
        super(owner, "Create Customer Sales Return", true);
        this.ownerWindow = owner;
        this.salesReturnManager = salesReturnManager;
        this.salesManager = salesManager;
        this.inventory = inventory;

        initComponents();
        layoutComponents();
        attachEventHandlers();
        updateTotalRefundDisplay();

        if (originalSaleIdToLoad != null && !originalSaleIdToLoad.trim().isEmpty()) {
            originalSaleIdField.setText(originalSaleIdToLoad);
            originalSaleIdField.setEditable(false); // Optionally make it non-editable if pre-filled
            // Automatically trigger the find operation
            // Using SwingUtilities.invokeLater to ensure UI is ready before action
            SwingUtilities.invokeLater(this::findOriginalSale);
        }

        setSize(850, 700);
        setLocationRelativeTo(owner);
    }


    private void initComponents() {
        originalSaleIdField = new JTextField(20);
        findSaleButton = new JButton("Find Original Sale");

        String[] origSaleCols = {"SKU", "Name", "Qty Sold", "Unit Price", "Select"};
        originalSaleItemsTableModel = new DefaultTableModel(origSaleCols, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 4 ? Boolean.class : super.getColumnClass(columnIndex);
            }
            @Override
            public boolean isCellEditable(int row, int column) {
                // Allow editing only the "Select" checkbox
                return column == 4;
            }
        };
        originalSaleItemsTable = new JTable(originalSaleItemsTableModel);

        addItemToReturnButton = new JButton("Add Selected to Return >>");
        addItemToReturnButton.setEnabled(false);

        String[] returnCols = {"SKU", "Name", "Qty Returned", "Unit Price", "Condition", "Reason", "Subtotal"};
        returnItemsTableModel = new DefaultTableModel(returnCols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        returnItemsTable = new JTable(returnItemsTableModel);

        removeItemFromReturnButton = new JButton("<< Remove Selected from Return");
        removeItemFromReturnButton.setEnabled(false);

        processReturnButton = new JButton("Process This Return");
        cancelButton = new JButton("Cancel");

        totalRefundLabel = new JLabel("Total Refund: " + CURRENCY_FORMAT.format(0.00));
        totalRefundLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        notesField = new JTextArea(3, 40);
        notesField.setLineWrap(true);
        notesField.setWrapStyleWord(true);
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));

        JPanel findSalePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        findSalePanel.setBorder(BorderFactory.createTitledBorder("Find Original Sale Transaction"));
        findSalePanel.add(new JLabel("Original Sale ID:"));
        findSalePanel.add(originalSaleIdField);
        findSalePanel.add(findSaleButton);
        add(findSalePanel, BorderLayout.NORTH);

        JScrollPane origSaleScrollPane = new JScrollPane(originalSaleItemsTable);
        origSaleScrollPane.setBorder(BorderFactory.createTitledBorder("Items in Original Sale (Check items to return)"));
        origSaleScrollPane.setPreferredSize(new Dimension(400, 200));

        JPanel controlAndReturnPanel = new JPanel(new BorderLayout(5, 5));
        JPanel addRemoveButtonPanel = new JPanel();
        addRemoveButtonPanel.setLayout(new BoxLayout(addRemoveButtonPanel, BoxLayout.Y_AXIS));
        addRemoveButtonPanel.add(Box.createVerticalGlue());
        addItemToReturnButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        addRemoveButtonPanel.add(addItemToReturnButton);
        addRemoveButtonPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        removeItemFromReturnButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        addRemoveButtonPanel.add(removeItemFromReturnButton);
        addRemoveButtonPanel.add(Box.createVerticalGlue());
        addRemoveButtonPanel.setBorder(BorderFactory.createEmptyBorder(10,5,10,5));

        JScrollPane returnItemsScrollPane = new JScrollPane(returnItemsTable);
        returnItemsScrollPane.setBorder(BorderFactory.createTitledBorder("Items Being Returned by Customer"));
        returnItemsScrollPane.setPreferredSize(new Dimension(400, 200));

        controlAndReturnPanel.add(addRemoveButtonPanel, BorderLayout.WEST);
        controlAndReturnPanel.add(returnItemsScrollPane, BorderLayout.CENTER);

        JSplitPane centerSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, origSaleScrollPane, controlAndReturnPanel);
        centerSplitPane.setResizeWeight(0.55); // Adjust split ratio as needed
        add(centerSplitPane, BorderLayout.CENTER);

        JPanel bottomOuterPanel = new JPanel(new BorderLayout(5, 5));
        JPanel notesPanel = new JPanel(new BorderLayout(5, 5));
        notesPanel.setBorder(BorderFactory.createTitledBorder("Return Notes/Comments"));
        notesPanel.add(new JScrollPane(notesField), BorderLayout.CENTER);
        bottomOuterPanel.add(notesPanel, BorderLayout.CENTER);

        JPanel totalAndActionsPanel = new JPanel(new BorderLayout());
        JPanel totalPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        totalPanel.add(totalRefundLabel);
        totalAndActionsPanel.add(totalPanel, BorderLayout.NORTH);

        JPanel actionButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionButtonPanel.add(cancelButton);
        actionButtonPanel.add(processReturnButton);
        totalAndActionsPanel.add(actionButtonPanel, BorderLayout.SOUTH);
        bottomOuterPanel.add(totalAndActionsPanel, BorderLayout.SOUTH);

        add(bottomOuterPanel, BorderLayout.SOUTH);
    }

    private void attachEventHandlers() {
        findSaleButton.addActionListener(e -> findOriginalSale());
        originalSaleIdField.addActionListener(e -> findOriginalSale()); // Allow Enter key to trigger find
        addItemToReturnButton.addActionListener(e -> addSelectedItemsToReturnList());
        removeItemFromReturnButton.addActionListener(e -> removeSelectedItemFromReturnList());
        processReturnButton.addActionListener(e -> processTheReturn());
        cancelButton.addActionListener(e -> dispose());
    }

    private void findOriginalSale() {
        String saleId = originalSaleIdField.getText().trim();
        if (saleId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter an Original Sale ID.", "Input Missing", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Optional<Sale> saleOpt = salesManager.getSaleById(saleId);

        if (saleOpt.isPresent()) {
            originalSale = saleOpt.get();
            if (!Sale.STATUS_COMPLETED.equals(originalSale.getStatus())) {
                JOptionPane.showMessageDialog(this,
                        "Warning: Sale " + saleId + " is not 'Completed' (Status: "+originalSale.getStatus()+").\n" +
                                "Returns are typically processed for completed sales. Please verify.",
                        "Sale Status Warning", JOptionPane.WARNING_MESSAGE);
                // Decide if you want to prevent loading or just warn. For now, we load.
            }
            populateOriginalSaleItemsTable();
            addItemToReturnButton.setEnabled(true); // Enable adding items
            currentReturnItemsList.clear(); // Clear any previous return items if a new sale is found
            refreshReturnItemsTable();
            updateTotalRefundDisplay();
        } else {
            JOptionPane.showMessageDialog(this, "Original Sale ID '" + saleId + "' not found.", "Sale Not Found", JOptionPane.ERROR_MESSAGE);
            originalSale = null; // Clear previous sale if any
            originalSaleItemsTableModel.setRowCount(0); // Clear table
            addItemToReturnButton.setEnabled(false);
            currentReturnItemsList.clear();
            refreshReturnItemsTable();
            updateTotalRefundDisplay();
        }
    }

    private void populateOriginalSaleItemsTable() {
        originalSaleItemsTableModel.setRowCount(0); // Clear existing rows
        if (originalSale == null) return;

        for (Sale.SaleItem si : originalSale.getItemsSold()) {
            originalSaleItemsTableModel.addRow(new Object[]{
                    si.getSku(),
                    si.getItemName(),
                    si.getQuantitySold(),
                    CURRENCY_FORMAT.format(si.getPriceAtSale()),
                    Boolean.FALSE // Checkbox for selection, initially false
            });
        }
    }

    private void addSelectedItemsToReturnList() {
        if (originalSale == null) {
            JOptionPane.showMessageDialog(this, "Please find an original sale first.", "No Sale Loaded", JOptionPane.WARNING_MESSAGE);
            return;
        }
        boolean itemEffectivelyAdded = false;
        for (int i = 0; i < originalSaleItemsTableModel.getRowCount(); i++) {
            Boolean isSelected = (Boolean) originalSaleItemsTableModel.getValueAt(i, 4); // Column 4 is "Select"
            if (Boolean.TRUE.equals(isSelected)) {
                String sku = (String) originalSaleItemsTableModel.getValueAt(i, 0);
                String name = (String) originalSaleItemsTableModel.getValueAt(i, 1);
                int soldQty = (Integer) originalSaleItemsTableModel.getValueAt(i, 2);

                Sale.SaleItem originalSaleItem = originalSale.getItemsSold().stream()
                        .filter(si -> si.getSku().equals(sku) && si.getQuantitySold() == soldQty) // More precise match if SKU could repeat in a sale (unlikely here)
                        .findFirst().orElse(null);

                if (originalSaleItem == null) { // Fallback if exact quantity match fails but SKU exists
                    originalSaleItem = originalSale.getItemsSold().stream()
                            .filter(si -> si.getSku().equals(sku))
                            .findFirst().orElse(null);
                }

                if (originalSaleItem == null) {
                    System.err.println("Error: Could not find original sale item for SKU: " + sku + " in originalSale object matching table row.");
                    originalSaleItemsTableModel.setValueAt(Boolean.FALSE, i, 4); // Uncheck
                    continue;
                }
                double unitPrice = originalSaleItem.getPriceAtSale();

                // Check if item with this SKU is already in the currentReturnItemsList
                boolean alreadyInReturnList = currentReturnItemsList.stream().anyMatch(sri -> sri.getItemSKU().equals(sku));
                if (alreadyInReturnList) {
                    JOptionPane.showMessageDialog(this, "Item '" + name + "' (SKU: " + sku + ") is already in the return list.\n" +
                                    "Please remove it first if you want to change its quantity or condition.",
                            "Item Already in Return List", JOptionPane.WARNING_MESSAGE);
                    originalSaleItemsTableModel.setValueAt(Boolean.FALSE, i, 4); // Uncheck
                    continue;
                }

                // Prompt for return quantity
                String qtyStr = JOptionPane.showInputDialog(this,
                        "Enter quantity to return for:\n" + name + " (SKU: " + sku + ")\nOriginally Sold: " + soldQty,
                        "Return Quantity for " + sku, JOptionPane.PLAIN_MESSAGE);
                if (qtyStr == null) { // User cancelled input
                    originalSaleItemsTableModel.setValueAt(Boolean.FALSE, i, 4); // Uncheck
                    continue;
                }

                int returnQty;
                try {
                    returnQty = Integer.parseInt(qtyStr);
                    if (returnQty <= 0 || returnQty > soldQty) {
                        JOptionPane.showMessageDialog(this, "Invalid return quantity for " + name + ".\nMust be between 1 and " + soldQty + ".", "Input Error", JOptionPane.ERROR_MESSAGE);
                        originalSaleItemsTableModel.setValueAt(Boolean.FALSE, i, 4); // Uncheck
                        continue;
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid number format for quantity.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    originalSaleItemsTableModel.setValueAt(Boolean.FALSE, i, 4); // Uncheck
                    continue;
                }

                // Prompt for condition
                String[] conditions = {SalesReturnItem.CONDITION_RESELLABLE, SalesReturnItem.CONDITION_DAMAGED, SalesReturnItem.CONDITION_DEFECTIVE};
                String condition = (String) JOptionPane.showInputDialog(this, "Select condition for returned " + name + ":",
                        "Item Condition", JOptionPane.QUESTION_MESSAGE, null, conditions, conditions[0]);
                if (condition == null) { // User cancelled
                    originalSaleItemsTableModel.setValueAt(Boolean.FALSE, i, 4); // Uncheck
                    continue;
                }

                // Prompt for reason (optional)
                String reason = JOptionPane.showInputDialog(this, "Enter reason for returning " + name + " (optional):", "Return Reason", JOptionPane.PLAIN_MESSAGE);
                reason = (reason == null) ? "" : reason.trim(); // Handle null from cancel

                currentReturnItemsList.add(new SalesReturnItem(sku, name, returnQty, unitPrice, condition, reason));
                itemEffectivelyAdded = true;
                originalSaleItemsTableModel.setValueAt(Boolean.FALSE, i, 4); // Uncheck after adding
            }
        }

        if (itemEffectivelyAdded) {
            refreshReturnItemsTable();
            updateTotalRefundDisplay();
            removeItemFromReturnButton.setEnabled(!currentReturnItemsList.isEmpty());
        }
    }

    private void removeSelectedItemFromReturnList() {
        int selectedRow = returnItemsTable.getSelectedRow();
        if (selectedRow >= 0) {
            // Ensure index is valid for the list
            if (selectedRow < currentReturnItemsList.size()) {
                currentReturnItemsList.remove(selectedRow);
                refreshReturnItemsTable();
                updateTotalRefundDisplay();
                removeItemFromReturnButton.setEnabled(!currentReturnItemsList.isEmpty());
            } else {
                System.err.println("Error: Selected row in returnItemsTable is out of bounds for currentReturnItemsList.");
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please select an item from the 'Items Being Returned' list to remove.", "No Selection", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void refreshReturnItemsTable() {
        returnItemsTableModel.setRowCount(0);
        for (SalesReturnItem sri : currentReturnItemsList) {
            returnItemsTableModel.addRow(new Object[]{
                    sri.getItemSKU(),
                    sri.getItemName(),
                    sri.getReturnedQuantity(),
                    CURRENCY_FORMAT.format(sri.getUnitPriceAtSale()),
                    sri.getCondition(),
                    sri.getReason(),
                    CURRENCY_FORMAT.format(sri.getSubtotalRefund())
            });
        }
    }

    private void updateTotalRefundDisplay() {
        double total = 0;
        for (SalesReturnItem sri : currentReturnItemsList) {
            total += sri.getSubtotalRefund();
        }
        totalRefundLabel.setText("Total Refund: " + CURRENCY_FORMAT.format(total));
    }

    private void processTheReturn() {
        if (originalSale == null) {
            JOptionPane.showMessageDialog(this, "Please find and load an original sale first.", "No Original Sale", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (currentReturnItemsList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No items have been added to this return.", "Empty Return List", JOptionPane.WARNING_MESSAGE);
            return;
        }

        SalesReturn newReturn = salesReturnManager.createNewSalesReturn(originalSale.getSaleID());
        if (newReturn == null) {
            JOptionPane.showMessageDialog(this, "Failed to initialize sales return record.\n(Original Sale ID might be invalid or SalesManager could not find it).", "Error Initializing Return", JOptionPane.ERROR_MESSAGE);
            return;
        }

        for (SalesReturnItem sri : currentReturnItemsList) {
            newReturn.addReturnItem(sri);
        }
        newReturn.setCustomerNotes(notesField.getText().trim());
        newReturn.setStatus(SalesReturn.STATUS_APPROVED); // Set status to Approved before processing inventory

        boolean success = salesReturnManager.processReturnInventoryUpdate(newReturn);
        // processReturnInventoryUpdate should set status to Completed if successful internally

        if (success) {
            JOptionPane.showMessageDialog(this, "Sales Return " + newReturn.getReturnID() + " processed successfully!\nInventory has been updated. Status: " + newReturn.getStatus(), "Return Processed", JOptionPane.INFORMATION_MESSAGE);
            if (ownerWindow != null) {
                ownerWindow.loadInventoryData(); // Refresh main inventory display
            }
            salesReturnManager.saveSalesReturnsToFile(); // Save all returns including the new/updated one
            dispose();
        } else {
            // Even if some inventory updates failed, the return record itself might have been created.
            // SalesReturnManager should handle partial success states if necessary.
            JOptionPane.showMessageDialog(this, "There were issues processing the return fully. Some inventory updates might have failed.\nPlease check console logs and verify inventory.\nThe return record (ID: " + newReturn.getReturnID() + ") status is: " + newReturn.getStatus() +".", "Processing Issue", JOptionPane.ERROR_MESSAGE);
            salesReturnManager.saveSalesReturnsToFile(); // Save state even if errors occurred
        }
    }
}