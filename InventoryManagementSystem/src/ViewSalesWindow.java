// ViewSalesWindow.java
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat; // For table date display
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date; // For Sale.getSaleDate()
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.swing.RowSorter; // Added for SortKey
import javax.swing.SortOrder; // Added for SortOrder


public class ViewSalesWindow extends JDialog {
    private SalesManager salesManager;
    private Inventory inventory;
    private SalesReturnManager salesReturnManager;
    private MainInventoryWindow ownerWindow;

    private JTextField saleIdSearchField, dateFromField, dateToField;
    private JButton searchButton, clearSearchButton, createReturnButton, viewSaleDetailsButton, closeButton;
    private JTable salesTable;
    private DefaultTableModel salesTableModel;
    private JTable saleItemsTable;
    private DefaultTableModel saleItemsTableModel;

    private static final SimpleDateFormat TABLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter INPUT_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("$#,##0.00", new DecimalFormatSymbols(Locale.US));

    private Sale selectedSale = null;

    public ViewSalesWindow(MainInventoryWindow owner, SalesManager salesManager, SalesReturnManager salesReturnManager, Inventory inventory) {
        super(owner, "View / Search Sales", true);
        this.ownerWindow = owner;
        this.salesManager = salesManager;
        this.salesReturnManager = salesReturnManager;
        this.inventory = inventory;

        initComponents();
        layoutComponents();
        attachEventHandlers();
        loadInitialSalesData();

        setSize(950, 750);
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        saleIdSearchField = new JTextField(15);
        dateFromField = new JTextField(10);
        dateToField = new JTextField(10);

        searchButton = new JButton("Search Sales");
        clearSearchButton = new JButton("Clear Search");
        createReturnButton = new JButton("Create Return for Selected Sale");
        createReturnButton.setEnabled(false);
        viewSaleDetailsButton = new JButton("View Sale Details");
        viewSaleDetailsButton.setEnabled(false);
        closeButton = new JButton("Close");

        String[] salesColumns = {"Sale ID", "Sale Date", "Total Amount", "Status"};
        salesTableModel = new DefaultTableModel(salesColumns, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        salesTable = new JTable(salesTableModel);
        salesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        salesTable.setAutoCreateRowSorter(true); // Enable user sorting

        String[] itemColumns = {"SKU", "Name", "Qty Sold", "Unit Price", "Subtotal"};
        saleItemsTableModel = new DefaultTableModel(itemColumns, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        saleItemsTable = new JTable(saleItemsTableModel);
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));
        JPanel searchPanelOuter = new JPanel(new BorderLayout());
        searchPanelOuter.setBorder(BorderFactory.createTitledBorder("Search Sales"));
        JPanel searchFieldsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0; gbc.gridy = 0; searchFieldsPanel.add(new JLabel("Sale ID:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.fill = GridBagConstraints.HORIZONTAL; searchFieldsPanel.add(saleIdSearchField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; searchFieldsPanel.add(new JLabel("Date From (YYYY-MM-DD):"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.fill = GridBagConstraints.HORIZONTAL; searchFieldsPanel.add(dateFromField, gbc);
        gbc.gridx = 2; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; searchFieldsPanel.add(new JLabel("Date To (YYYY-MM-DD):"), gbc);
        gbc.gridx = 3; gbc.gridy = 1; gbc.fill = GridBagConstraints.HORIZONTAL; searchFieldsPanel.add(dateToField, gbc);
        searchPanelOuter.add(searchFieldsPanel, BorderLayout.CENTER);
        JPanel searchButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchButtonPanel.add(searchButton);
        searchButtonPanel.add(clearSearchButton);
        searchPanelOuter.add(searchButtonPanel, BorderLayout.SOUTH);
        add(searchPanelOuter, BorderLayout.NORTH);

        JScrollPane salesScrollPane = new JScrollPane(salesTable);
        salesScrollPane.setBorder(BorderFactory.createTitledBorder("Sales List"));
        JScrollPane itemsScrollPane = new JScrollPane(saleItemsTable);
        itemsScrollPane.setBorder(BorderFactory.createTitledBorder("Sale Item Details"));
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, salesScrollPane, itemsScrollPane);
        splitPane.setResizeWeight(0.6);
        add(splitPane, BorderLayout.CENTER);

        JPanel bottomButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomButtonPanel.add(viewSaleDetailsButton);
        bottomButtonPanel.add(createReturnButton);
        bottomButtonPanel.add(closeButton);
        add(bottomButtonPanel, BorderLayout.SOUTH);
    }

    private void attachEventHandlers() {
        closeButton.addActionListener(e -> dispose());
        searchButton.addActionListener(e -> performSearch());
        clearSearchButton.addActionListener(e -> clearSearchFieldsAndReload());

        salesTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && salesTable.getSelectedRow() != -1) {
                int modelRow = salesTable.convertRowIndexToModel(salesTable.getSelectedRow());
                String saleId = (String) salesTableModel.getValueAt(modelRow, 0);
                Optional<Sale> saleOpt = salesManager.getSaleById(saleId);
                if (saleOpt.isPresent()) {
                    selectedSale = saleOpt.get();
                    loadSaleItemDetails(selectedSale);
                    updateActionButtonsForSelectedSale();
                } else {
                    selectedSale = null;
                    saleItemsTableModel.setRowCount(0);
                    updateActionButtonsForSelectedSale();
                }
            } else if (salesTable.getSelectedRow() == -1) {
                selectedSale = null;
                saleItemsTableModel.setRowCount(0);
                updateActionButtonsForSelectedSale();
            }
        });

        salesTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                if (me.getClickCount() == 2 && selectedSale != null) {
                    viewSaleDetailsButton.doClick();
                }
            }
        });

        viewSaleDetailsButton.addActionListener(e -> {
            if (selectedSale != null) {
                loadSaleItemDetails(selectedSale); // Ensure details are fresh
                JOptionPane.showMessageDialog(this, selectedSale.getSaleDetails(), "Sale Details: " + selectedSale.getSaleID(), JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Please select a sale to view its details.", "No Sale Selected", JOptionPane.WARNING_MESSAGE);
            }
        });

        createReturnButton.addActionListener(e -> {
            if (selectedSale != null) {
                if (!Sale.STATUS_COMPLETED.equals(selectedSale.getStatus())) {
                    JOptionPane.showMessageDialog(this,
                            "Returns can only be processed for 'Completed' sales.\nSelected sale status: " + selectedSale.getStatus(),
                            "Return Not Allowed", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                CreateSalesReturnWindow returnWindow = new CreateSalesReturnWindow(ownerWindow, salesReturnManager, salesManager, inventory, selectedSale.getSaleID());
                returnWindow.setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, "Please select a sale to create a return for.", "No Sale Selected", JOptionPane.WARNING_MESSAGE);
            }
        });
    }

    private void updateActionButtonsForSelectedSale() {
        if (selectedSale != null) {
            viewSaleDetailsButton.setEnabled(true);
            createReturnButton.setEnabled(Sale.STATUS_COMPLETED.equals(selectedSale.getStatus()));
        } else {
            viewSaleDetailsButton.setEnabled(false);
            createReturnButton.setEnabled(false);
        }
    }

    private void loadInitialSalesData() {
        performSearch();
    }

    private void clearSearchFieldsAndReload() {
        saleIdSearchField.setText("");
        dateFromField.setText("");
        dateToField.setText("");
        performSearch();
    }

    private void performSearch() {
        String saleIdQuery = saleIdSearchField.getText().trim();
        String dateFromString = dateFromField.getText().trim();
        String dateToString = dateToField.getText().trim();
        List<Sale> salesResult;

        if (!saleIdQuery.isEmpty()) {
            Optional<Sale> saleOpt = salesManager.getSaleById(saleIdQuery);
            salesResult = new ArrayList<>();
            saleOpt.ifPresent(salesResult::add);
        } else if (!dateFromString.isEmpty() && !dateToString.isEmpty()) {
            try {
                LocalDate startDate = LocalDate.parse(dateFromString, INPUT_DATE_FORMATTER);
                LocalDate endDate = LocalDate.parse(dateToString, INPUT_DATE_FORMATTER);
                if (endDate.isBefore(startDate)) {
                    JOptionPane.showMessageDialog(this, "End date cannot be before start date.", "Date Range Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                salesResult = salesManager.getCompletedSalesByDateRange(startDate, endDate);
            } catch (DateTimeParseException e) {
                JOptionPane.showMessageDialog(this, "Invalid date format. Please use YYYY-MM-DD.", "Date Format Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } else if (!dateFromString.isEmpty() && dateToString.isEmpty()){
            try {
                LocalDate startDate = LocalDate.parse(dateFromString, INPUT_DATE_FORMATTER);
                salesResult = salesManager.getCompletedSalesByDateRange(startDate, LocalDate.now().plusDays(1)); // To include today fully
            } catch (DateTimeParseException e) {
                JOptionPane.showMessageDialog(this, "Invalid date format for 'Date From'. Please use YYYY-MM-DD.", "Date Format Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } else {
            salesResult = salesManager.getAllSales(); // Default: load all sales
            // Consider filtering to only "Completed" by default for performance if list is large
            // salesResult = salesManager.getAllSales().stream().filter(s -> Sale.STATUS_COMPLETED.equals(s.getStatus())).collect(Collectors.toList());
        }
        populateSalesTable(salesResult);
    }

    private void populateSalesTable(List<Sale> sales) {
        salesTableModel.setRowCount(0);
        saleItemsTableModel.setRowCount(0);
        selectedSale = null;
        // updateActionButtonsForSelectedSale(); // Will be called by selection listener or if table is empty

        if (sales != null) {
            for (Sale sale : sales) {
                salesTableModel.addRow(new Object[]{
                        sale.getSaleID(),
                        TABLE_DATE_FORMAT.format(sale.getSaleDate()), // Display formatted date string
                        CURRENCY_FORMAT.format(sale.getTotalAmount()),
                        sale.getStatus()
                });
            }
        }

        // --- Apply default sort by Sale Date (column 1) DESCENDING ---
        TableRowSorter<?> sorter = (TableRowSorter<?>) salesTable.getRowSorter();
        // Ensure sorter exists
        if (sorter == null && salesTableModel.getRowCount() > 0) {
            sorter = new TableRowSorter<>(salesTableModel);
            salesTable.setRowSorter(sorter);
        }

        if (sorter != null) {
            List<RowSorter.SortKey> sortKeys = new ArrayList<>();
            // Assuming "Sale Date" is the second column (index 1 in the model)
            int saleDateColumnModelIndex = 1;
            // Find column index by name to be more robust
            for(int i=0; i < salesTableModel.getColumnCount(); i++){
                if("Sale Date".equalsIgnoreCase(salesTableModel.getColumnName(i))){
                    saleDateColumnModelIndex = i;
                    break;
                }
            }
            sortKeys.add(new RowSorter.SortKey(saleDateColumnModelIndex, SortOrder.DESCENDING));
            sorter.setSortKeys(sortKeys);
            // sorter.sort(); // Usually not needed as setSortKeys triggers it.
        }
        // --- Default sort applied ---

        if (salesTable.getRowCount() > 0) {
            salesTable.setRowSelectionInterval(0, 0); // Select the first row (which is now the newest after sort)
        } else {
            updateActionButtonsForSelectedSale(); // Ensure buttons are disabled if table is empty
        }
    }

    private void loadSaleItemDetails(Sale sale) {
        saleItemsTableModel.setRowCount(0);
        if (sale == null) return;
        for (Sale.SaleItem si : sale.getItemsSold()) {
            saleItemsTableModel.addRow(new Object[]{
                    si.getSku(),
                    si.getItemName(),
                    si.getQuantitySold(),
                    CURRENCY_FORMAT.format(si.getPriceAtSale()),
                    CURRENCY_FORMAT.format(si.getSubtotal())
            });
        }
    }
}