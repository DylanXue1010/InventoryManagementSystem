// ViewSalesReturnsWindow.java
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Optional;


public class ViewSalesReturnsWindow extends JDialog {
    private SalesReturnManager salesReturnManager;
    private JTable returnsTable;
    private DefaultTableModel returnsTableModel;
    private JTable returnItemsTable; // For details
    private DefaultTableModel returnItemsTableModel;
    private JButton closeButton, viewDetailsButton; // Add more action buttons if needed (e.g., "Process/Approve Pending")
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("$#,##0.00", new DecimalFormatSymbols(Locale.US));


    public ViewSalesReturnsWindow(Frame owner, SalesReturnManager salesReturnManager) {
        super(owner, "View Customer Sales Returns", true);
        this.salesReturnManager = salesReturnManager;

        initComponents();
        layoutComponents();
        attachEventHandlers();
        loadReturnsData();

        setSize(800, 600);
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        String[] returnsCols = {"Return ID", "Original Sale ID", "Return Date", "Status", "Total Refund"};
        returnsTableModel = new DefaultTableModel(returnsCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        returnsTable = new JTable(returnsTableModel);
        returnsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        returnsTable.setAutoCreateRowSorter(true);

        String[] itemCols = {"SKU", "Name", "Qty Rtn'd", "Unit Price", "Condition", "Reason", "Subtotal"};
        returnItemsTableModel = new DefaultTableModel(itemCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        returnItemsTable = new JTable(returnItemsTableModel);

        closeButton = new JButton("Close");
        viewDetailsButton = new JButton("View Details");
        viewDetailsButton.setEnabled(false);
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(10,10));

        JScrollPane returnsScrollPane = new JScrollPane(returnsTable);
        returnsScrollPane.setBorder(BorderFactory.createTitledBorder("Sales Returns"));

        JScrollPane itemsScrollPane = new JScrollPane(returnItemsTable);
        itemsScrollPane.setBorder(BorderFactory.createTitledBorder("Returned Items (Details)"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, returnsScrollPane, itemsScrollPane);
        splitPane.setResizeWeight(0.5);
        add(splitPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(viewDetailsButton);
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void attachEventHandlers() {
        closeButton.addActionListener(e -> dispose());

        returnsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && returnsTable.getSelectedRow() != -1) {
                viewDetailsButton.setEnabled(true);
                // Optionally load details immediately, or wait for button click
                int modelRow = returnsTable.convertRowIndexToModel(returnsTable.getSelectedRow());
                String returnId = (String) returnsTableModel.getValueAt(modelRow, 0);
                loadReturnItemsDetails(returnId);
            } else if (returnsTable.getSelectedRow() == -1) {
                viewDetailsButton.setEnabled(false);
                returnItemsTableModel.setRowCount(0);
            }
        });

        viewDetailsButton.addActionListener(e -> {
            int selectedRowInView = returnsTable.getSelectedRow();
            if (selectedRowInView != -1) {
                int modelRow = returnsTable.convertRowIndexToModel(selectedRowInView);
                String returnId = (String) returnsTableModel.getValueAt(modelRow, 0);
                loadReturnItemsDetails(returnId);
            } else {
                JOptionPane.showMessageDialog(this, "Please select a return to view details.", "No Selection", JOptionPane.WARNING_MESSAGE);
            }
        });
    }

    private void loadReturnsData() {
        returnsTableModel.setRowCount(0);
        List<SalesReturn> allReturns = salesReturnManager.getAllSalesReturns();
        for (SalesReturn sr : allReturns) {
            returnsTableModel.addRow(new Object[]{
                    sr.getReturnID(),
                    sr.getOriginalSaleID(),
                    sr.getReturnDateString(),
                    sr.getStatus(),
                    CURRENCY_FORMAT.format(sr.getTotalRefundAmount())
            });
        }
        if (returnsTable.getRowCount() > 0) {
            returnsTable.setRowSelectionInterval(0,0);
            int modelRow = returnsTable.convertRowIndexToModel(0);
            String returnId = (String) returnsTableModel.getValueAt(modelRow, 0);
            loadReturnItemsDetails(returnId); // Load details for first item
        } else {
            viewDetailsButton.setEnabled(false);
            returnItemsTableModel.setRowCount(0);
        }
    }

    private void loadReturnItemsDetails(String returnId){
        returnItemsTableModel.setRowCount(0);
        Optional<SalesReturn> srOpt = salesReturnManager.getSalesReturnById(returnId);
        if(srOpt.isPresent()){
            SalesReturn sr = srOpt.get();
            for(SalesReturnItem sri : sr.getReturnedItems()){
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
    }
}
