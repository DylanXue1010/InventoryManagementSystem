// SalesReturnItem.java
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class SalesReturnItem {
    private String itemSKU;
    private String itemName; // 为方便显示，尽管可以从Inventory获取
    private int returnedQuantity;
    private double unitPriceAtSale; // 退货时参考的原销售单价
    private String condition;     // 商品状况，例如 "Resellable", "Damaged", "Defective"
    private String reason;        // 退货原因 (可选)

    public static final String CONDITION_RESELLABLE = "Resellable";
    public static final String CONDITION_DAMAGED = "Damaged";
    public static final String CONDITION_DEFECTIVE = "Defective";


    public SalesReturnItem(String itemSKU, String itemName, int returnedQuantity, double unitPriceAtSale, String condition, String reason) {
        if (returnedQuantity <= 0) {
            throw new IllegalArgumentException("Returned quantity must be positive for SKU: " + itemSKU);
        }
        if (unitPriceAtSale < 0) {
            throw new IllegalArgumentException("Unit price at sale cannot be negative for SKU: " + itemSKU);
        }
        this.itemSKU = itemSKU;
        this.itemName = itemName;
        this.returnedQuantity = returnedQuantity;
        this.unitPriceAtSale = unitPriceAtSale;
        this.condition = (condition == null || condition.trim().isEmpty()) ? CONDITION_RESELLABLE : condition.trim();
        this.reason = reason != null ? reason.trim() : "";
    }

    // Getters
    public String getItemSKU() { return itemSKU; }
    public String getItemName() { return itemName; }
    public int getReturnedQuantity() { return returnedQuantity; }
    public double getUnitPriceAtSale() { return unitPriceAtSale; }
    public String getCondition() { return condition; }
    public String getReason() { return reason; }

    // Setters (如果允许修改)
    public void setCondition(String condition) { this.condition = condition; }
    public void setReason(String reason) { this.reason = reason; }


    public double getSubtotalRefund() {
        return this.returnedQuantity * this.unitPriceAtSale;
    }

    // CSV persistence: returnID,itemSKU,itemName,returnedQuantity,unitPriceAtSale,condition,reason
    public String toCsvString() {
        DecimalFormat df = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US));
        return String.join(",",
                SalesReturn.escapeCsv(itemSKU),
                SalesReturn.escapeCsv(itemName),
                String.valueOf(returnedQuantity),
                df.format(unitPriceAtSale),
                SalesReturn.escapeCsv(condition),
                SalesReturn.escapeCsv(reason)
        );
    }

    public static SalesReturnItem fromCsvParts(String[] parts) { // parts不包含returnID
        if (parts.length < 6) {
            System.err.println("Invalid CSV parts for SalesReturnItem: not enough parts. Expected 6, Got " + parts.length);
            return null;
        }
        try {
            String sku = SalesReturn.unescapeCsv(parts[0]);
            String name = SalesReturn.unescapeCsv(parts[1]);
            int qty = Integer.parseInt(SalesReturn.unescapeCsv(parts[2]));
            double price = Double.parseDouble(SalesReturn.unescapeCsv(parts[3]));
            String condition = SalesReturn.unescapeCsv(parts[4]);
            String reason = SalesReturn.unescapeCsv(parts[5]);
            return new SalesReturnItem(sku, name, qty, price, condition, reason);
        } catch (NumberFormatException e) {
            System.err.println("Error parsing numeric value for SalesReturnItem from CSV: " + e.getMessage());
            return null;
        } catch (IllegalArgumentException e) {
            System.err.println("Error creating SalesReturnItem from CSV due to invalid arguments: " + e.getMessage());
            return null;
        }
    }

    @Override
    public String toString() {
        DecimalFormat df = new DecimalFormat("#0.00", new DecimalFormatSymbols(Locale.US));
        return "  - SKU: " + itemSKU + ", Name: " + itemName +
                ", Qty Returned: " + returnedQuantity + ", Condition: " + condition +
                ", Unit Price: $" + df.format(unitPriceAtSale) +
                ", Subtotal Refund: $" + df.format(getSubtotalRefund()) +
                (reason.isEmpty() ? "" : ", Reason: " + reason);
    }
}
