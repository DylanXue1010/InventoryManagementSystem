// ReportWindow.java
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Date;

public class ReportWindow extends JDialog {

    private JComboBox<String> reportTypeComboBox;
    private JButton generateButton;
    private JLabel noDataLabel;
    private JTable reportTable;
    private DefaultTableModel reportTableModel;
    private JScrollPane scrollPane;

    private JPanel dateInputPanelContainer;
    private JTextField reportDateFromField;
    private JTextField reportDateToField;
    private JLabel dateFromLabel;
    private JLabel dateToLabel;

    private Inventory inventory;
    private SupplierManager supplierManager;
    private SalesManager salesManager;

    private final String SELECT_REPORT_PROMPT = "-- Select a Report Type --";
    private final String ENHANCED_LOW_STOCK_REPORT = "Low Stock Report (with Supplier)";
    private final String TOTAL_INVENTORY_VALUE_REPORT = "Total Inventory Value";
    private final String SALES_BY_PRODUCT_SUMMARY = "Sales by Product (Summary)";
    private final String SALES_BY_CATEGORY_REPORT = "Sales by Category";

    private static final DateTimeFormatter INPUT_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("$#,##0.00", new DecimalFormatSymbols(Locale.US));
    // This format takes a number (e.g., 25.0) and appends a '%', outputting "25.00%"
    private static final DecimalFormat PERCENTAGE_FORMAT = new DecimalFormat("0.00'%'", new DecimalFormatSymbols(Locale.US));

    public ReportWindow(Frame owner, Inventory inventory, SupplierManager supplierManager, SalesManager salesManager) {
        super(owner, "Generate Reports", true);
        this.inventory = inventory;
        this.supplierManager = supplierManager;
        this.salesManager = salesManager;

        initComponents();
        layoutComponents();
        attachEventHandlers();
        updateDateInputVisibility();

        setSize(800, 600);
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        String[] reportTypes = {
                SELECT_REPORT_PROMPT,
                SALES_BY_PRODUCT_SUMMARY,
                SALES_BY_CATEGORY_REPORT,
                ENHANCED_LOW_STOCK_REPORT,
                TOTAL_INVENTORY_VALUE_REPORT
        };
        reportTypeComboBox = new JComboBox<>(reportTypes);
        reportTypeComboBox.setPreferredSize(new Dimension(280, reportTypeComboBox.getPreferredSize().height));

        generateButton = new JButton("Generate Report");

        reportTableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        reportTable = new JTable(reportTableModel);
        reportTable.setFillsViewportHeight(true);
        reportTable.setAutoCreateRowSorter(true);

        noDataLabel = new JLabel("Select a report type and click 'Generate Report'.", SwingConstants.CENTER);
        noDataLabel.setFont(new Font("SansSerif", Font.ITALIC, 14));

        scrollPane = new JScrollPane(noDataLabel);

        dateFromLabel = new JLabel("Start Date (YYYY-MM-DD):");
        reportDateFromField = new JTextField(10);
        dateToLabel = new JLabel("End Date (YYYY-MM-DD):");
        reportDateToField = new JTextField(10);
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));

        JPanel topControlPanel = new JPanel(new BorderLayout(10,5));
        JPanel reportSelectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        reportSelectionPanel.add(new JLabel("Select Report Type:"));
        reportSelectionPanel.add(reportTypeComboBox);
        reportSelectionPanel.add(generateButton);
        topControlPanel.add(reportSelectionPanel, BorderLayout.NORTH);

        dateInputPanelContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 5,0));
        dateInputPanelContainer.add(dateFromLabel);
        dateInputPanelContainer.add(reportDateFromField);
        dateInputPanelContainer.add(Box.createHorizontalStrut(10));
        dateInputPanelContainer.add(dateToLabel);
        dateInputPanelContainer.add(reportDateToField);
        dateInputPanelContainer.setVisible(false);
        topControlPanel.add(dateInputPanelContainer, BorderLayout.CENTER);

        add(topControlPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void updateDateInputVisibility() {
        String selectedReport = (String) reportTypeComboBox.getSelectedItem();
        boolean needsDateRange = SALES_BY_PRODUCT_SUMMARY.equals(selectedReport) ||
                SALES_BY_CATEGORY_REPORT.equals(selectedReport);

        dateInputPanelContainer.setVisible(needsDateRange);
        this.revalidate();
        this.repaint();
    }

    private void attachEventHandlers() {
        generateButton.addActionListener(e -> generateSelectedReport());
        reportTypeComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateDateInputVisibility();
                if (!SELECT_REPORT_PROMPT.equals(e.getItem())) {
                    showNoDataMessage("New report type selected. Click 'Generate Report'.");
                } else {
                    showNoDataMessage("Select a report type and click 'Generate Report'.");
                }
            }
        });
    }

    private void showNoDataMessage(String message) {
        reportTableModel.setRowCount(0);
        reportTableModel.setColumnCount(0);
        noDataLabel.setText(message);
        scrollPane.setViewportView(noDataLabel);
        scrollPane.revalidate();
        scrollPane.repaint();
    }

    private void prepareTableForReport(String[] columnNames) {
        reportTableModel.setColumnIdentifiers(columnNames);
        reportTableModel.setRowCount(0);
        scrollPane.setViewportView(reportTable);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(reportTableModel);
        reportTable.setRowSorter(sorter);
        scrollPane.revalidate();
        scrollPane.repaint();
    }

    private void generateSelectedReport() {
        String selectedReport = (String) reportTypeComboBox.getSelectedItem();
        if (selectedReport == null || SELECT_REPORT_PROMPT.equals(selectedReport)) {
            JOptionPane.showMessageDialog(this, "Please select a valid report type.", "Selection Missing", JOptionPane.WARNING_MESSAGE);
            return;
        }
        showNoDataMessage("Generating '" + selectedReport + "', please wait...");

        switch (selectedReport) {
            case SALES_BY_PRODUCT_SUMMARY:
                generateSalesByProductReport();
                break;
            case SALES_BY_CATEGORY_REPORT:
                generateSalesByCategoryReport();
                break;
            case ENHANCED_LOW_STOCK_REPORT:
                generateEnhancedLowStockReport();
                break;
            case TOTAL_INVENTORY_VALUE_REPORT:
                generateTotalInventoryValueReport();
                break;
            default:
                showNoDataMessage("Selected report type '" + selectedReport + "' is not yet implemented.");
                break;
        }
    }

    private static class ProductSalesReportEntry {
        String sku;
        String name;
        int totalQuantitySold;
        double totalRevenue;
        ProductSalesReportEntry(String sku, String name) {
            this.sku = sku; this.name = name; this.totalQuantitySold = 0; this.totalRevenue = 0.0;
        }
        void addSale(int quantity, double subtotal) {
            this.totalQuantitySold += quantity; this.totalRevenue += subtotal;
        }
    }

    private static class CategorySalesReportEntry {
        String categoryName;
        int totalQuantitySold;
        double totalRevenue;
        CategorySalesReportEntry(String categoryName) {
            this.categoryName = categoryName; this.totalQuantitySold = 0; this.totalRevenue = 0.0;
        }
        void addSale(int quantity, double subtotal) {
            this.totalQuantitySold += quantity; this.totalRevenue += subtotal;
        }
    }

    private void generateSalesByProductReport() {
        if (salesManager == null) {
            showNoDataMessage("Sales Manager is not available.");
            JOptionPane.showMessageDialog(this, "Sales data system is not ready.", "System Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String dateFromString = reportDateFromField.getText().trim();
        String dateToString = reportDateToField.getText().trim();
        if (dateFromString.isEmpty() || dateToString.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Both start date and end date are required for this report.", "Input Error", JOptionPane.ERROR_MESSAGE);
            showNoDataMessage("Date range not provided for 'Sales by Product' report.");
            return;
        }
        LocalDate startDate, endDate;
        try {
            startDate = LocalDate.parse(dateFromString, INPUT_DATE_FORMATTER);
            endDate = LocalDate.parse(dateToString, INPUT_DATE_FORMATTER);
            if (endDate.isBefore(startDate)) {
                JOptionPane.showMessageDialog(this, "End date cannot be before start date.", "Date Range Error", JOptionPane.ERROR_MESSAGE);
                showNoDataMessage("Invalid date range provided.");
                return;
            }
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "Invalid date format. Please use YYYY-MM-DD.", "Date Format Error", JOptionPane.ERROR_MESSAGE);
            showNoDataMessage("Invalid date format entered.");
            return;
        }

        List<Sale> completedSales = salesManager.getCompletedSalesByDateRange(startDate, endDate);
        if (completedSales.isEmpty()) {
            showNoDataMessage("No completed sales found in the selected date range: " + startDate + " to " + endDate + ".");
            return;
        }

        Map<String, ProductSalesReportEntry> productSalesData = new HashMap<>();
        double grandTotalRevenue = 0.0;
        for (Sale sale : completedSales) {
            grandTotalRevenue += sale.getTotalAmount();
            for (Sale.SaleItem item : sale.getItemsSold()) {
                productSalesData.computeIfAbsent(item.getSku(), sku -> new ProductSalesReportEntry(sku, item.getItemName()))
                        .addSale(item.getQuantitySold(), item.getSubtotal());
            }
        }

        String[] columnNames = {"SKU", "Product Name", "Total Qty Sold", "Avg. Selling Price ($)", "Total Revenue ($)", "% of Total Revenue"};
        prepareTableForReport(columnNames);
        List<ProductSalesReportEntry> sortedEntries = new ArrayList<>(productSalesData.values());
        sortedEntries.sort((e1, e2) -> Double.compare(e2.totalRevenue, e1.totalRevenue));

        for (ProductSalesReportEntry entry : sortedEntries) {
            double averageSellingPrice = (entry.totalQuantitySold > 0) ? (entry.totalRevenue / entry.totalQuantitySold) : 0.0;
            // Calculate percentage as a value like 25.0 for 25%
            double percentageOfTotalRevenue = (grandTotalRevenue > 0) ? (entry.totalRevenue / grandTotalRevenue) * 100.0 : 0.0;
            reportTableModel.addRow(new Object[]{
                    entry.sku, entry.name, entry.totalQuantitySold,
                    CURRENCY_FORMAT.format(averageSellingPrice),
                    CURRENCY_FORMAT.format(entry.totalRevenue),
                    PERCENTAGE_FORMAT.format(percentageOfTotalRevenue) // Pass the value like 25.0
            });
        }
        if (reportTableModel.getRowCount() == 0) {
            showNoDataMessage("No product sales data to display for the selected criteria after processing.");
        } else {
            reportTableModel.addRow(new Object[]{});
            reportTableModel.addRow(new Object[]{
                    "GRAND TOTAL", "", "", "",
                    CURRENCY_FORMAT.format(grandTotalRevenue),
                    PERCENTAGE_FORMAT.format(100.00) // Pass 100.0 for 100.00%
            });
        }
    }

    private void generateSalesByCategoryReport() {
        if (salesManager == null || inventory == null) {
            showNoDataMessage("Sales Manager or Inventory data is not available.");
            JOptionPane.showMessageDialog(this, "Required data system is not ready.", "System Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String dateFromString = reportDateFromField.getText().trim();
        String dateToString = reportDateToField.getText().trim();
        if (dateFromString.isEmpty() || dateToString.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Both start date and end date are required for this report.", "Input Error", JOptionPane.ERROR_MESSAGE);
            showNoDataMessage("Date range not provided for 'Sales by Category' report.");
            return;
        }
        LocalDate startDate, endDate;
        try {
            startDate = LocalDate.parse(dateFromString, INPUT_DATE_FORMATTER);
            endDate = LocalDate.parse(dateToString, INPUT_DATE_FORMATTER);
            if (endDate.isBefore(startDate)) {
                JOptionPane.showMessageDialog(this, "End date cannot be before start date.", "Date Range Error", JOptionPane.ERROR_MESSAGE);
                showNoDataMessage("Invalid date range provided.");
                return;
            }
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "Invalid date format. Please use YYYY-MM-DD.", "Date Format Error", JOptionPane.ERROR_MESSAGE);
            showNoDataMessage("Invalid date format entered.");
            return;
        }

        List<Sale> completedSales = salesManager.getCompletedSalesByDateRange(startDate, endDate);
        if (completedSales.isEmpty()) {
            showNoDataMessage("No completed sales found in the selected date range: " + startDate + " to " + endDate + ".");
            return;
        }

        Map<String, CategorySalesReportEntry> categorySalesData = new HashMap<>();
        double grandTotalRevenue = 0.0;
        int grandTotalQuantity = 0;

        for (Sale sale : completedSales) {
            grandTotalRevenue += sale.getTotalAmount();
            for (Sale.SaleItem saleItem : sale.getItemsSold()) {
                grandTotalQuantity += saleItem.getQuantitySold();
                Item itemDetails = inventory.getItem(saleItem.getSku());
                String category = "Unknown Category";
                if (itemDetails != null && itemDetails.getCategory() != null && !itemDetails.getCategory().isEmpty()) {
                    category = itemDetails.getCategory();
                }
                categorySalesData.computeIfAbsent(category, catName -> new CategorySalesReportEntry(catName))
                        .addSale(saleItem.getQuantitySold(), saleItem.getSubtotal());
            }
        }

        String[] columnNames = {"Category", "Total Quantity Sold", "Total Revenue ($)", "% of Total Revenue"};
        prepareTableForReport(columnNames);
        List<CategorySalesReportEntry> sortedEntries = new ArrayList<>(categorySalesData.values());
        sortedEntries.sort((e1, e2) -> Double.compare(e2.totalRevenue, e1.totalRevenue));

        for (CategorySalesReportEntry entry : sortedEntries) {
            // Calculate percentage as a value like 25.0 for 25%
            double percentageOfTotalRevenue = (grandTotalRevenue > 0) ? (entry.totalRevenue / grandTotalRevenue) * 100.0 : 0.0;
            reportTableModel.addRow(new Object[]{
                    entry.categoryName,
                    entry.totalQuantitySold,
                    CURRENCY_FORMAT.format(entry.totalRevenue),
                    PERCENTAGE_FORMAT.format(percentageOfTotalRevenue) // Pass the value like 25.0
            });
        }
        if (reportTableModel.getRowCount() == 0) {
            showNoDataMessage("No category sales data to display for the selected criteria after processing.");
        } else {
            reportTableModel.addRow(new Object[]{});
            reportTableModel.addRow(new Object[]{
                    "GRAND TOTAL",
                    grandTotalQuantity,
                    CURRENCY_FORMAT.format(grandTotalRevenue),
                    PERCENTAGE_FORMAT.format(100.00) // Pass 100.0 for 100.00%
            });
        }
    }

    private void generateEnhancedLowStockReport() {
        if (inventory == null) {
            showNoDataMessage("Inventory data is not available.");
            JOptionPane.showMessageDialog(this, "Inventory data system is not ready.", "System Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String thresholdStr = JOptionPane.showInputDialog(this, "Enter low stock threshold:", "Low Stock Threshold", JOptionPane.QUESTION_MESSAGE);
        if (thresholdStr == null || thresholdStr.trim().isEmpty()) {
            showNoDataMessage("Low stock report cancelled or no threshold provided.");
            return;
        }

        try {
            int threshold = Integer.parseInt(thresholdStr.trim());
            if (threshold < 0) {
                JOptionPane.showMessageDialog(this, "Threshold cannot be negative.", "Input Error", JOptionPane.ERROR_MESSAGE);
                showNoDataMessage("Invalid threshold: cannot be negative.");
                return;
            }

            List<Item> lowStockItems = inventory.generateLowStockReport(threshold);
            String[] columnNames = {"SKU", "Name", "Category", "Quantity", "Price ($)", "Supplier ID", "Supplier Name", "Status"};
            prepareTableForReport(columnNames);

            if (lowStockItems.isEmpty()) {
                showNoDataMessage("No items are currently below the threshold of " + threshold + ".");
            } else {
                for (Item item : lowStockItems) {
                    String supplierId = item.getSupplier() != null ? item.getSupplier() : "N/A";
                    String supplierName = "N/A";
                    if (supplierManager != null && item.getSupplier() != null && !item.getSupplier().isEmpty()) {
                        Optional<Supplier> supOpt = supplierManager.findSupplierById(item.getSupplier());
                        if (supOpt.isPresent()) {
                            supplierName = supOpt.get().getName();
                        } else {
                            supplierName = "Unknown (ID: "+item.getSupplier()+")";
                        }
                    } else if (supplierManager == null && item.getSupplier() != null && !item.getSupplier().isEmpty()){
                        supplierName = "Supplier Data N/A (ID: "+item.getSupplier()+")";
                    }
                    reportTableModel.addRow(new Object[]{
                            item.getSku(), item.getName(), item.getCategory(), item.getQuantity(),
                            String.format(Locale.US, "%.2f", item.getPrice()),
                            supplierId, supplierName, item.getStatus()
                    });
                }
                if (reportTableModel.getRowCount() == 0) {
                    showNoDataMessage("No low stock items to display for the threshold of " + threshold + ".");
                }
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid threshold format. Please enter a whole number.", "Input Error", JOptionPane.ERROR_MESSAGE);
            showNoDataMessage("Invalid threshold format entered.");
        }
    }

    private void generateTotalInventoryValueReport() {
        if (inventory == null) {
            showNoDataMessage("Inventory data is not available.");
            JOptionPane.showMessageDialog(this, "Inventory data system is not ready.", "System Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        double totalValue = inventory.calculateTotalValue();
        String reportDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        String[] columnNames = {"Metric", "Value", "Notes"};
        prepareTableForReport(columnNames);

        reportTableModel.addRow(new Object[]{
                "Report Generation Time",
                reportDate,
                ""
        });
        reportTableModel.addRow(new Object[]{
                "Total Inventory Potential Retail Value",
                CURRENCY_FORMAT.format(totalValue),
                "Calculated using current selling prices."
        });
        reportTableModel.addRow(new Object[]{
                "Number of Unique SKUs in Inventory",
                inventory.getAllItems().size(),
                ""
        });

        if (reportTableModel.getRowCount() == 0) {
            showNoDataMessage("Could not generate total inventory value data.");
        }
    }
}