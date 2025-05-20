// MainInventoryWindow.java
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale; // Locale is not strictly needed for storing Double, but good for formatting if done manually elsewhere
import java.util.Optional;
import javax.swing.RowSorter;
import javax.swing.SortOrder;

public class MainInventoryWindow extends JFrame {

    private JTable inventoryTable;
    private DefaultTableModel tableModel;

    // UI Components
    private JButton addButton, editButton, deleteButton, reportButton, searchButton;
    private JButton makeSaleButton;
    private JButton logoutButton;
    private JButton createPOButton;
    private JButton viewPOsButton;
    private JButton viewSearchSalesButton; // Changed from createSalesReturnButton

    private JTextField searchField;
    private JLabel statusBarLabel;

    // Static manager instances
    private static User currentUser;
    private static UserManager userManager;
    private static Inventory appInventory;
    private static SalesManager salesManagerInstance;
    private static SupplierManager supplierManagerInstance;
    private static OrderManager orderManagerInstance;
    private static SalesReturnManager salesReturnManagerInstance;

    public MainInventoryWindow(Inventory inventoryInstanceNouse) {
        setTitle("Inventory and Sales Management System");
        setSize(1050, 800);
        setLocationRelativeTo(null);

        initComponents();
        layoutComponents();
        attachEventHandlers();
        setupCloseAction();
    }

    private void initComponents() {
        // Inventory Table
        String[] columnNames = {"SKU", "Name", "Category", "Quantity", "Price ($)", "Status"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            // --- MODIFICATION: Override getColumnClass for correct sorting ---
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                switch (columnIndex) {
                    case 0: // SKU
                        return String.class;
                    case 1: // Name
                        return String.class;
                    case 2: // Category
                        return String.class;
                    case 3: // Quantity
                        return Integer.class; // Treat as Integer
                    case 4: // Price ($)
                        return Double.class;  // Treat as Double
                    case 5: // Status
                        return String.class;
                    default:
                        return String.class;
                }
            }
            // --- END MODIFICATION ---
        };
        inventoryTable = new JTable(tableModel);
        inventoryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        inventoryTable.setFillsViewportHeight(true);
        inventoryTable.setRowHeight(25);
        if (inventoryTable.getTableHeader() != null) {
            inventoryTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));
        }
        inventoryTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        inventoryTable.setAutoCreateRowSorter(true);

        // Initialize buttons
        makeSaleButton = new JButton("Make New Sale");
        addButton = new JButton("Add Product");
        editButton = new JButton("Edit Product");
        deleteButton = new JButton("Delete Product");
        reportButton = new JButton("Generate Report");
        logoutButton = new JButton("Logout");
        createPOButton = new JButton("Create Purchase Order");
        viewPOsButton = new JButton("View Purchase Orders");
        viewSearchSalesButton = new JButton("View/Search Sales");

        searchField = new JTextField(25);
        searchButton = new JButton("Search");

        statusBarLabel = new JLabel("Not logged in");
        statusBarLabel.setHorizontalAlignment(SwingConstants.LEFT);
        statusBarLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));

        JPanel topSearchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        topSearchPanel.add(new JLabel("Search Inventory:"));
        topSearchPanel.add(searchField);
        topSearchPanel.add(searchButton);
        add(topSearchPanel, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(inventoryTable);
        add(scrollPane, BorderLayout.CENTER);

        JPanel southContainerPanel = new JPanel(new BorderLayout());
        JPanel buttonActionPanel = new JPanel();
        buttonActionPanel.setLayout(new BoxLayout(buttonActionPanel, BoxLayout.Y_AXIS));
        buttonActionPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel inventorySalesActions = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        inventorySalesActions.setBorder(BorderFactory.createTitledBorder("Inventory & Sales Actions"));
        inventorySalesActions.add(makeSaleButton);
        inventorySalesActions.add(addButton);
        inventorySalesActions.add(editButton);
        inventorySalesActions.add(deleteButton);
        inventorySalesActions.add(reportButton);
        inventorySalesActions.add(viewSearchSalesButton);
        buttonActionPanel.add(inventorySalesActions);

        JPanel purchaseOrderActions = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        purchaseOrderActions.setBorder(BorderFactory.createTitledBorder("Purchase Order Actions"));
        purchaseOrderActions.add(createPOButton);
        purchaseOrderActions.add(viewPOsButton);
        buttonActionPanel.add(purchaseOrderActions);

        JPanel sessionActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        sessionActions.add(logoutButton);
        buttonActionPanel.add(sessionActions);

        southContainerPanel.add(buttonActionPanel, BorderLayout.CENTER);
        southContainerPanel.add(statusBarLabel, BorderLayout.SOUTH);
        add(southContainerPanel, BorderLayout.SOUTH);
    }

    private void attachEventHandlers() {
        makeSaleButton.addActionListener(e -> {
            if (salesManagerInstance == null || appInventory == null) { showErrorDialog("Sales system not ready."); return; }
            MakeSaleWindow saleDialog = new MakeSaleWindow(this, salesManagerInstance, appInventory);
            saleDialog.setVisible(true);
            loadInventoryData();
        });
        addButton.addActionListener(e -> {
            if (appInventory == null) { showErrorDialog("Inventory system not ready."); return; }
            AddProductWindow addDialog = new AddProductWindow(this, appInventory);
            addDialog.setVisible(true);
            // AddProductWindow calls loadInventoryData on success
        });
        editButton.addActionListener(e -> {
            int selectedRowInView = inventoryTable.getSelectedRow();
            if (selectedRowInView >= 0) {
                int modelRow = inventoryTable.convertRowIndexToModel(selectedRowInView);
                String sku = (String) tableModel.getValueAt(modelRow, 0); // Assuming SKU is still String
                Item itemToEdit = appInventory.getItem(sku);
                if (itemToEdit != null) {
                    EditProductWindow editDialog = new EditProductWindow(this, appInventory, itemToEdit);
                    editDialog.setVisible(true);
                    // EditProductWindow calls loadInventoryData on success
                } else {
                    showErrorDialog("Could not retrieve product details for SKU: " + sku);
                    loadInventoryData();
                }
            } else { showWarningDialog("Please select a product to edit."); }
        });
        deleteButton.addActionListener(e -> {
            int selectedRowInView = inventoryTable.getSelectedRow();
            if (selectedRowInView >= 0) {
                int modelRow = inventoryTable.convertRowIndexToModel(selectedRowInView);
                String sku = (String) tableModel.getValueAt(modelRow, 0);
                String name = (String) tableModel.getValueAt(modelRow, 1);
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Are you sure you want to delete product:\nSKU: " + sku + "\nName: " + name + "?\n(Consider deactivating if it has transaction history)",
                        "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (confirm == JOptionPane.YES_OPTION) {
                    if (appInventory.removeItem(sku)) {
                        loadInventoryData();
                        showInfoDialog("Product " + sku + " deleted successfully.");
                    } else { showErrorDialog("Failed to delete product " + sku + "."); }
                }
            } else { showWarningDialog("Please select a product to delete."); }
        });
        reportButton.addActionListener(e -> {
            if (appInventory == null || supplierManagerInstance == null || salesManagerInstance == null) {
                showErrorDialog("Required data systems are not ready for reports.");
                return;
            }
            ReportWindow reportDialog = new ReportWindow(this, appInventory, supplierManagerInstance, salesManagerInstance);
            reportDialog.setVisible(true);
        });
        searchField.addActionListener(e -> performSearch());
        searchButton.addActionListener(e -> performSearch());
        inventoryTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2 && editButton.isEnabled()) {
                    int row = inventoryTable.rowAtPoint(evt.getPoint());
                    if (row >= 0) {
                        inventoryTable.setRowSelectionInterval(row, row);
                        editButton.doClick();
                    }
                }
            }
        });
        logoutButton.addActionListener(e -> performLogout());
        createPOButton.addActionListener(e -> {
            if (orderManagerInstance == null || appInventory == null || supplierManagerInstance == null) {
                showErrorDialog("Purchase Order system components not ready."); return;
            }
            if (supplierManagerInstance.getAllSuppliers().isEmpty()){
                int choice = JOptionPane.showConfirmDialog(this,
                        "No suppliers found. Please add suppliers first.\nContinue to PO creation anyway?",
                        "No Suppliers Found", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (choice == JOptionPane.NO_OPTION) return;
            }
            CreateOrderWindow coWin = new CreateOrderWindow(this, orderManagerInstance, appInventory, supplierManagerInstance);
            coWin.setVisible(true);
        });
        viewPOsButton.addActionListener(e -> {
            if (orderManagerInstance == null || appInventory == null) {
                showErrorDialog("Purchase Order system components not ready."); return;
            }
            ViewOrdersWindow voWin = new ViewOrdersWindow(this, orderManagerInstance, appInventory);
            voWin.setVisible(true);
            loadInventoryData();
        });
        viewSearchSalesButton.addActionListener(e -> {
            if (salesManagerInstance == null || salesReturnManagerInstance == null || appInventory == null) {
                showErrorDialog("Sales system components not properly initialized.");
                return;
            }
            ViewSalesWindow vsWindow = new ViewSalesWindow(this, salesManagerInstance, salesReturnManagerInstance, appInventory);
            vsWindow.setVisible(true);
            loadInventoryData();
        });
    }

    private void performSearch() {
        String searchTerm = searchField.getText().trim();
        if (appInventory == null) { showErrorDialog("Inventory system not ready for search."); return; }
        if (searchTerm.isEmpty()) {
            loadInventoryData(); // Load all items if search is empty
        } else {
            List<Item> searchResult = appInventory.searchItems(searchTerm);
            loadInventoryData(searchResult); // Load only search results
            if (searchResult.isEmpty()) { showInfoDialog("No products found matching: '" + searchTerm + "'."); }
        }
    }

    public void loadInventoryData(List<Item> itemsToLoad) {
        if (tableModel == null) return;
        tableModel.setRowCount(0);

        if (itemsToLoad != null) {
            for (Item item : itemsToLoad) {
                // --- MODIFICATION: Store actual numeric types ---
                Object[] rowData = {
                        item.getSku(),       // Assuming SKU is String and its format is consistent
                        item.getName(),
                        item.getCategory(),
                        item.getQuantity(),  // Store as Integer (autoboxed from int)
                        item.getPrice(),     // Store as Double (autoboxed from double)
                        item.getStatus()
                };
                // --- END MODIFICATION ---
                tableModel.addRow(rowData);
            }
        }

        // Apply default sort by SKU (column 0) ASCENDING
        // This part should now work correctly with numeric columns too, if user clicks on them
        TableRowSorter<?> sorter = (TableRowSorter<?>) inventoryTable.getRowSorter();
        if (sorter == null) { // Should not be null if autoCreateRowSorter is true
            sorter = new TableRowSorter<>(tableModel);
            inventoryTable.setRowSorter(sorter);
        }

        List<RowSorter.SortKey> sortKeys = new ArrayList<>();
        int skuColumnModelIndex = 0;
        for(int i=0; i < tableModel.getColumnCount(); i++){
            if("SKU".equalsIgnoreCase(tableModel.getColumnName(i))){
                skuColumnModelIndex = i;
                break;
            }
        }
        sortKeys.add(new RowSorter.SortKey(skuColumnModelIndex, SortOrder.ASCENDING));
        sorter.setSortKeys(sortKeys);
        // sorter.sort(); // Usually not needed as setSortKeys triggers sorting.
    }

    // Default loadInventoryData method - loads all items
    public void loadInventoryData() {
        if (appInventory != null) {
            loadInventoryData(appInventory.getAllItems());
        } else {
            if (tableModel != null) tableModel.setRowCount(0);
            System.err.println("MainInventoryWindow: appInventory is null. Cannot load inventory data.");
        }
    }

    public void onLoginSuccess(User user) {
        currentUser = user;
        setTitle("Inventory System - User: " + currentUser.getUsername() + " (" + currentUser.getRole() + ")");
        statusBarLabel.setText("Logged in as: " + currentUser.getUsername() + " (Role: " + currentUser.getRole() + ")");
        adjustUiForRole();
        loadInventoryData();
        this.setVisible(true);
    }

    private void adjustUiForRole() {
        // ... (Keep existing adjustUiForRole logic) ...
        if (currentUser == null) {
            setAllButtonsEnabled(false);
            searchButton.setEnabled(false); searchField.setEnabled(false);
            if (inventoryTable != null) inventoryTable.setEnabled(false);
            return;
        }
        setAllButtonsEnabled(true);
        searchButton.setEnabled(true); searchField.setEnabled(true);
        if (inventoryTable != null) inventoryTable.setEnabled(true);

        boolean isAdmin = "Admin".equalsIgnoreCase(currentUser.getRole());
        boolean isStaff = "Staff".equalsIgnoreCase(currentUser.getRole());

        makeSaleButton.setEnabled(isAdmin || isStaff);
        addButton.setEnabled(isAdmin || isStaff);
        editButton.setEnabled(isAdmin || isStaff);
        deleteButton.setEnabled(isAdmin);
        reportButton.setEnabled(true);
        createPOButton.setEnabled(isAdmin || isStaff);
        viewPOsButton.setEnabled(isAdmin || isStaff);
        viewSearchSalesButton.setEnabled(isAdmin || isStaff);
    }

    private void setAllButtonsEnabled(boolean enabled) {
        // ... (Keep existing setAllButtonsEnabled logic) ...
        makeSaleButton.setEnabled(enabled); addButton.setEnabled(enabled);
        editButton.setEnabled(enabled); deleteButton.setEnabled(enabled);
        reportButton.setEnabled(enabled); createPOButton.setEnabled(enabled);
        viewPOsButton.setEnabled(enabled); viewSearchSalesButton.setEnabled(enabled);
    }

    private void setupCloseAction() {
        // ... (Keep existing setupCloseAction logic) ...
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int confirm = JOptionPane.showConfirmDialog(MainInventoryWindow.this,
                        "Are you sure you want to exit the application?",
                        "Exit Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (confirm == JOptionPane.YES_OPTION) {
                    System.out.println("Saving all data before closing application...");
                    if (appInventory != null) appInventory.saveItemsToFile(Inventory.DEFAULT_ITEMS_FILE_PATH);
                    if (salesManagerInstance != null) salesManagerInstance.saveSalesToFile();
                    if (supplierManagerInstance != null) supplierManagerInstance.saveSuppliersToFile(SupplierManager.DEFAULT_SUPPLIERS_FILE_PATH);
                    if (orderManagerInstance != null) orderManagerInstance.saveOrdersToFile();
                    if (salesReturnManagerInstance != null) salesReturnManagerInstance.saveSalesReturnsToFile();
                    System.out.println("All data saved. Exiting application.");
                    System.exit(0);
                }
            }
        });
    }

    private void performLogout() {
        // ... (Keep existing performLogout logic) ...
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to logout?", "Logout Confirmation",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            currentUser = null;
            this.setVisible(false);
            launchLoginProcess();
        }
    }

    public static UserManager getUserManager() { return userManager; }
    public static Inventory getAppInventory() { return appInventory; }
    public static SalesManager getSalesManager() { return salesManagerInstance; }
    public static SupplierManager getSupplierManager() { return supplierManagerInstance; }
    public static OrderManager getOrderManager() { return orderManagerInstance; }
    public static SalesReturnManager getSalesReturnManager() { return salesReturnManagerInstance; }

    private void showInfoDialog(String message) { JOptionPane.showMessageDialog(this, message, "Information", JOptionPane.INFORMATION_MESSAGE); }
    private void showWarningDialog(String message) { JOptionPane.showMessageDialog(this, message, "Warning", JOptionPane.WARNING_MESSAGE); }
    private void showErrorDialog(String message) { JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE); }

    public static void main(String[] args) {
        // ... (Keep existing main method logic) ...
        appInventory = new Inventory();
        userManager = new UserManager();
        supplierManagerInstance = new SupplierManager();
        salesManagerInstance = new SalesManager(appInventory);
        orderManagerInstance = new OrderManager(appInventory, supplierManagerInstance);
        salesReturnManagerInstance = new SalesReturnManager(appInventory, salesManagerInstance);

        userManager.createDefaultAdminUserIfNotExists(true);
        if (supplierManagerInstance.getAllSuppliers().isEmpty()) {
            System.out.println("INFO: No suppliers found. Consider pre-populating suppliers.csv.");
        }
        if (salesManagerInstance.getAllSales().isEmpty()){
            System.out.println("INFO: No sales found. For testing, ensure some sales exist in sales.csv and are 'Completed'.");
        }
        launchLoginProcess();
    }

    private static void launchLoginProcess() {
        // ... (Keep existing launchLoginProcess logic) ...
        final MainInventoryWindow mainFrame = new MainInventoryWindow(appInventory);
        SwingUtilities.invokeLater(() -> {
            LoginWindow loginDialog = new LoginWindow(null, userManager, mainFrame);
            loginDialog.setVisible(true);
            if (loginDialog.getAuthenticatedUser() == null && !mainFrame.isVisible()) {
                System.out.println("Login cancelled or failed. Exiting application.");
                System.exit(0);
            }
        });
    }
}