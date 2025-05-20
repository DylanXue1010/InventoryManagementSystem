// SalesReturn.java
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.ZoneOffset;
import java.text.ParseException;


public class SalesReturn {
    private String returnID;
    private String originalSaleID; // 关联的原始销售单ID
    private Date returnDate;
    private List<SalesReturnItem> returnedItems;
    private double totalRefundAmount;
    private String status; // 例如: "Pending", "Approved_Resellable", "Approved_Damaged", "Completed_Refunded", "Rejected"
    private String customerNotes; // 客户备注或退货处理备注

    public static final String STATUS_PENDING = "Pending"; // 等待处理
    public static final String STATUS_APPROVED = "Approved"; // 退货已批准 (后续可能细化为可再销售或损坏)
    public static final String STATUS_COMPLETED = "Completed"; // 退货处理完成 (例如已退款，库存已更新)
    public static final String STATUS_REJECTED = "Rejected"; // 退货被拒绝

    private static final DateTimeFormatter CSV_DATE_FORMATTER = DateTimeFormatter.ISO_INSTANT; // UTC for persistence
    private static final SimpleDateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // 新建退货单
    public SalesReturn(String originalSaleID) {
        if (originalSaleID == null || originalSaleID.trim().isEmpty()) {
            throw new IllegalArgumentException("Original Sale ID cannot be empty for a sales return.");
        }
        this.returnID = generateReturnID();
        this.originalSaleID = originalSaleID;
        this.returnDate = new Date();
        this.returnedItems = new ArrayList<>();
        this.status = STATUS_PENDING;
        this.totalRefundAmount = 0.0;
        this.customerNotes = "";
    }

    // 从CSV加载时使用的构造函数
    public SalesReturn(String returnID, String originalSaleID, Date returnDate, double totalRefundAmount, String status, String customerNotes) {
        this.returnID = returnID;
        this.originalSaleID = originalSaleID;
        this.returnDate = returnDate;
        this.returnedItems = new ArrayList<>(); // 退货项将单独加载
        this.totalRefundAmount = totalRefundAmount; // 这是CSV中存储的总额，之后会根据项重新计算
        this.status = status;
        this.customerNotes = customerNotes;
    }


    private String generateReturnID() {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyyMMddHHmmss");
        return "RTN-" + sdfDate.format(new Date()) + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    // Getters
    public String getReturnID() { return returnID; }
    public String getOriginalSaleID() { return originalSaleID; }
    public Date getReturnDate() { return returnDate; }
    public String getReturnDateString() { return DISPLAY_DATE_FORMAT.format(this.returnDate); }
    public List<SalesReturnItem> getReturnedItems() { return new ArrayList<>(returnedItems); } // 返回副本
    public double getTotalRefundAmount() {
        calculateTotalRefundAmount(); // 确保总是最新的
        return totalRefundAmount;
    }
    public String getStatus() { return status; }
    public String getCustomerNotes() { return customerNotes; }

    // Setters
    public void setStatus(String status) { this.status = status; }
    public void setCustomerNotes(String customerNotes) { this.customerNotes = customerNotes; }
    public void setOriginalSaleID(String originalSaleID) { this.originalSaleID = originalSaleID; }


    public void addReturnItem(SalesReturnItem item) {
        if (item != null) {
            // 可选：检查是否已存在相同SKU的退货项，如果存在是合并数量还是作为新项？
            // 为简单起见，这里直接添加
            this.returnedItems.add(item);
            calculateTotalRefundAmount();
        }
    }

    public void addLoadedReturnItem(SalesReturnItem item) { // 从CSV加载项时使用，避免重复计算总额
        if (item != null) {
            this.returnedItems.add(item);
        }
    }

    public void removeReturnItem(String sku) {
        boolean removed = this.returnedItems.removeIf(item -> item.getItemSKU().equals(sku));
        if (removed) {
            calculateTotalRefundAmount();
        }
    }

    public void calculateTotalRefundAmount() {
        this.totalRefundAmount = 0.0;
        for (SalesReturnItem item : this.returnedItems) {
            this.totalRefundAmount += item.getSubtotalRefund();
        }
    }

    // CSV Persistence for sales_returns.csv
    // returnID,originalSaleID,returnDate,totalRefundAmount,status,customerNotes
    public String toSalesReturnCsvString() {
        DecimalFormat df = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US));
        String isoReturnDate = CSV_DATE_FORMATTER.format(this.returnDate.toInstant().atZone(ZoneOffset.UTC));
        return String.join(",",
                escapeCsv(returnID),
                escapeCsv(originalSaleID),
                escapeCsv(isoReturnDate),
                df.format(totalRefundAmount), // 保存计算后的总额
                escapeCsv(status),
                escapeCsv(customerNotes)
        );
    }

    public static Date parseIsoDateString(String dateString) {
        // (与Order.java中的方法相同，可以提取到公共工具类)
        if (dateString == null || dateString.trim().isEmpty()) return null;
        try {
            return Date.from(Instant.parse(dateString.trim()));
        } catch (Exception e) {
            System.err.println("Error parsing ISO date string for SalesReturn: '" + dateString + "' - " + e.getMessage());
            try { // Fallback for older Date.toString() format or other common formats
                SimpleDateFormat legacySdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
                return legacySdf.parse(dateString.trim());
            } catch (ParseException pe) {
                System.err.println("Error parsing legacy/common date string for SalesReturn after ISO fail: '" + dateString + "' - " + pe.getMessage());
                return null;
            }
        }
    }

    // CSV Helper methods (可以提取到公共工具类)
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

    public String getReturnDetails() {
        DecimalFormat df = new DecimalFormat("#0.00", new DecimalFormatSymbols(Locale.US));
        StringBuilder sb = new StringBuilder();
        sb.append("---------------- SALES RETURN RECEIPT ----------------\n");
        sb.append("Return ID: ").append(returnID).append("\n");
        sb.append("Original Sale ID: ").append(originalSaleID).append("\n");
        sb.append("Return Date: ").append(getReturnDateString()).append("\n");
        sb.append("Status: ").append(status).append("\n");
        sb.append("Customer Notes: ").append(customerNotes.isEmpty() ? "N/A" : customerNotes).append("\n");
        sb.append("Total Refund Amount: $").append(df.format(getTotalRefundAmount())).append("\n");
        sb.append("Returned Items (").append(returnedItems.size()).append("):\n");
        if (returnedItems.isEmpty()) {
            sb.append("  (No items in this return)\n");
        } else {
            for (SalesReturnItem item : returnedItems) {
                sb.append(item.toString()).append("\n");
            }
        }
        sb.append("-----------------------------------------------------\n");
        return sb.toString();
    }

    public static void main(String[] args) {
        SalesReturn sr = new SalesReturn("SALE-20250101-TEST1");
        System.out.println("Created Sales Return: " + sr.getReturnID());

        SalesReturnItem item1 = new SalesReturnItem("SKU001", "Laptop", 1, 1200.00, SalesReturnItem.CONDITION_RESELLABLE, "Customer changed mind");
        SalesReturnItem item2 = new SalesReturnItem("SKU002", "Mouse", 1, 25.00, SalesReturnItem.CONDITION_DAMAGED, "Box crushed");
        sr.addReturnItem(item1);
        sr.addReturnItem(item2);
        sr.setCustomerNotes("Customer requested full refund.");
        sr.setStatus(SalesReturn.STATUS_APPROVED);

        System.out.println(sr.getReturnDetails());
        System.out.println("SalesReturn CSV: " + sr.toSalesReturnCsvString());
        for(SalesReturnItem sri : sr.getReturnedItems()){
            System.out.println("SalesReturnItem CSV for Return " + sr.getReturnID() + ": " + sri.toCsvString());
        }
    }
}
