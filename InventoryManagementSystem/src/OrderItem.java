// OrderItem.java
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class OrderItem {
    private String itemSKU;
    private String itemName; // Store for convenience, though can be fetched from Inventory
    private int orderedQuantity;
    private int receivedQuantity;
    private double purchasePrice; // Price per unit at the time of order

    public OrderItem(String itemSKU, String itemName, int orderedQuantity, double purchasePrice) {
        if (orderedQuantity <= 0) {
            throw new IllegalArgumentException("Ordered quantity must be positive for SKU: " + itemSKU);
        }
        if (purchasePrice < 0) {
            throw new IllegalArgumentException("Purchase price cannot be negative for SKU: " + itemSKU);
        }
        this.itemSKU = itemSKU;
        this.itemName = itemName; // Good for display in order details without re-fetching
        this.orderedQuantity = orderedQuantity;
        this.purchasePrice = purchasePrice;
        this.receivedQuantity = 0; // Initially, none received
    }

    // Constructor for loading from CSV where receivedQuantity might be set
    public OrderItem(String itemSKU, String itemName, int orderedQuantity, int receivedQuantity, double purchasePrice) {
        this(itemSKU, itemName, orderedQuantity, purchasePrice); // Calls the main constructor
        if (receivedQuantity < 0 || receivedQuantity > orderedQuantity) {
            System.err.println("Warning: Invalid received quantity ("+ receivedQuantity +") for SKU " + itemSKU +
                    ". Must be between 0 and ordered quantity (" + orderedQuantity + "). Setting to 0.");
            this.receivedQuantity = 0;
        } else {
            this.receivedQuantity = receivedQuantity;
        }
    }


    // Getters
    public String getItemSKU() { return itemSKU; }
    public String getItemName() { return itemName; }
    public int getOrderedQuantity() { return orderedQuantity; }
    public double getPurchasePrice() { return purchasePrice; }
    public int getReceivedQuantity() { return receivedQuantity; }

    // Setters
    public void setOrderedQuantity(int orderedQuantity) {
        if (orderedQuantity <= 0) {
            System.err.println("Error: Ordered quantity must be positive for SKU " + itemSKU);
            return;
        }
        this.orderedQuantity = orderedQuantity;
        if (this.receivedQuantity > this.orderedQuantity) { // Adjust if necessary
            this.receivedQuantity = this.orderedQuantity;
        }
    }

    public void setPurchasePrice(double purchasePrice) {
        if (purchasePrice < 0) {
            System.err.println("Error: Purchase price cannot be negative for SKU " + itemSKU);
            return;
        }
        this.purchasePrice = purchasePrice;
    }

    /**
     * Updates the received quantity.
     * @param quantity The number of additional units received.
     * @return The actual number of units recorded as received (cannot exceed ordered quantity).
     */
    public int receiveItems(int quantity) {
        if (quantity < 0) {
            System.err.println("Cannot receive a negative quantity for SKU " + itemSKU);
            return 0;
        }
        int newReceivedQuantity = this.receivedQuantity + quantity;
        if (newReceivedQuantity > this.orderedQuantity) {
            System.err.println("Warning: Receiving " + quantity + " for SKU " + itemSKU +
                    " would exceed ordered quantity. Receiving " + (this.orderedQuantity - this.receivedQuantity) + " instead.");
            quantity = this.orderedQuantity - this.receivedQuantity; // adjust quantity to receive only remaining
            this.receivedQuantity = this.orderedQuantity;
        } else {
            this.receivedQuantity = newReceivedQuantity;
        }
        System.out.println(quantity + " units of SKU " + itemSKU + " marked as received. Total received: " + this.receivedQuantity);
        return quantity; // return actual quantity added to received
    }


    public double getSubtotal() {
        return this.orderedQuantity * this.purchasePrice;
    }

    public double getReceivedSubtotal() {
        return this.receivedQuantity * this.purchasePrice;
    }

    // For CSV persistence
    // orderID,itemSKU,itemName,orderedQuantity,receivedQuantity,purchasePrice
    public String toCsvString() {
        DecimalFormat df = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US));
        return String.join(",",
                Order.escapeCsv(itemSKU),
                Order.escapeCsv(itemName),
                String.valueOf(orderedQuantity),
                String.valueOf(receivedQuantity),
                df.format(purchasePrice)
        );
    }

    public static OrderItem fromCsvParts(String[] parts) {
        if (parts.length < 5) { // Expecting parts for itemSKU, itemName, orderedQty, receivedQty, purchasePrice
            System.err.println("Invalid CSV parts for OrderItem: not enough parts. Expected 5, Got " + parts.length);
            return null;
        }
        try {
            String sku = Order.unescapeCsv(parts[0]);
            String name = Order.unescapeCsv(parts[1]);
            int orderedQty = Integer.parseInt(Order.unescapeCsv(parts[2]));
            int receivedQty = Integer.parseInt(Order.unescapeCsv(parts[3]));
            double price = Double.parseDouble(Order.unescapeCsv(parts[4]));
            return new OrderItem(sku, name, orderedQty, receivedQty, price);
        } catch (NumberFormatException e) {
            System.err.println("Error parsing numeric value for OrderItem from CSV: " + e.getMessage());
            return null;
        } catch (IllegalArgumentException e) {
            System.err.println("Error creating OrderItem from CSV due to invalid arguments: " + e.getMessage());
            return null;
        }
    }

    @Override
    public String toString() {
        DecimalFormat df = new DecimalFormat("#0.00", new DecimalFormatSymbols(Locale.US));
        return "  - SKU: " + itemSKU + ", Name: " + itemName +
                ", Ordered: " + orderedQuantity + ", Received: " + receivedQuantity +
                ", Unit Price: $" + df.format(purchasePrice) +
                ", Subtotal: $" + df.format(getSubtotal());
    }
}