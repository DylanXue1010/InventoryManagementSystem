import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

// 假设 Item 和 Inventory 类在同一个包或已正确导入
// import com.yourpackage.Item;
// import com.yourpackage.Inventory;

public class EditProductWindow extends JDialog {

    private JTextField skuField, nameField, categoryField, quantityField, priceField, supplierField, statusField;
    private JButton saveButton, cancelButton;

    private Inventory inventory;
    private MainInventoryWindow ownerWindow;
    private Item itemToEdit; // 要编辑的原始 Item 对象

    public EditProductWindow(MainInventoryWindow owner, Inventory inventory, Item itemToEdit) {
        super(owner, "Edit Product - " + itemToEdit.getName(), true); // 模态对话框，标题包含商品名
        this.ownerWindow = owner;
        this.inventory = inventory;
        this.itemToEdit = itemToEdit;

        initComponents();
        populateFields(); // 填充当前商品数据
        layoutComponents();
        attachEventHandlers();

        pack();
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        skuField = new JTextField(20);
        skuField.setEditable(false); // SKU 不可编辑

        nameField = new JTextField(20);
        categoryField = new JTextField(20);
        quantityField = new JTextField(10);
        priceField = new JTextField(10);
        supplierField = new JTextField(20);
        statusField = new JTextField(10); // 同样可以考虑 JComboBox

        saveButton = new JButton("Save Changes");
        cancelButton = new JButton("Cancel");
    }

    private void populateFields() {
        if (itemToEdit != null) {
            skuField.setText(itemToEdit.getSku());
            nameField.setText(itemToEdit.getName());
            categoryField.setText(itemToEdit.getCategory());
            quantityField.setText(String.valueOf(itemToEdit.getQuantity()));
            priceField.setText(String.format("%.2f", itemToEdit.getPrice()).replace(',', '.')); // Ensure dot for decimal
            supplierField.setText(itemToEdit.getSupplier());
            statusField.setText(itemToEdit.getStatus());
        }
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // SKU
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("SKU (Read-only):"), gbc);
        gbc.gridx = 1; gbc.gridy = 0;
        formPanel.add(skuField, gbc);

        // Name
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1;
        formPanel.add(nameField, gbc);

        // Category
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Category:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2;
        formPanel.add(categoryField, gbc);

        // Quantity
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Quantity:"), gbc);
        gbc.gridx = 1; gbc.gridy = 3;
        formPanel.add(quantityField, gbc);

        // Price
        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Price:"), gbc);
        gbc.gridx = 1; gbc.gridy = 4;
        formPanel.add(priceField, gbc);

        // Supplier
        gbc.gridx = 0; gbc.gridy = 5;
        formPanel.add(new JLabel("Supplier:"), gbc);
        gbc.gridx = 1; gbc.gridy = 5;
        formPanel.add(supplierField, gbc);

        // Status
        gbc.gridx = 0; gbc.gridy = 6;
        formPanel.add(new JLabel("Status:"), gbc);
        gbc.gridx = 1; gbc.gridy = 6;
        formPanel.add(statusField, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void attachEventHandlers() {
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveChanges();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }

    private void saveChanges() {
        // 1. 获取输入数据
        String originalSku = itemToEdit.getSku(); // SKU 不会改变
        String name = nameField.getText().trim();
        String category = categoryField.getText().trim();
        String quantityStr = quantityField.getText().trim();
        String priceStr = priceField.getText().trim();
        String supplier = supplierField.getText().trim();
        String status = statusField.getText().trim();

        // 2. 数据校验
        if (name.isEmpty() || category.isEmpty() || quantityStr.isEmpty() || priceStr.isEmpty() || supplier.isEmpty() || status.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields (except SKU) are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(quantityStr);
            if (quantity < 0) {
                JOptionPane.showMessageDialog(this, "Quantity cannot be negative.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid quantity format. Please enter a number.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr.replace(',', '.')); // Handle comma as decimal separator if needed
            if (price < 0) {
                JOptionPane.showMessageDialog(this, "Price cannot be negative.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid price format. Please enter a number.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 3. 创建更新后的 Item 对象
        // 注意：这里我们创建一个新的 Item 对象来代表更新后的状态。
        // Inventory.updateItem() 方法将用这个新对象替换旧对象。
        Item updatedItem = new Item(originalSku, name, category, quantity, price, supplier, status);

        // 4. 更新 Inventory
        boolean success = inventory.updateItem(originalSku, updatedItem);

        // 5. 提示结果并刷新主窗口表格
        if (success) {
            JOptionPane.showMessageDialog(this, "Product updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            ownerWindow.loadInventoryData(); // 调用主窗口的方法刷新表格
            dispose(); // 关闭对话框
        } else {
            // 这种情况理论上不应该发生，如果 getItem(sku) 能找到旧项目
            // 但 updateItem 内部如果还有其他校验失败则可能
            JOptionPane.showMessageDialog(this, "Failed to update product. SKU might not exist or an error occurred.", "Update Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    // No main method needed, will be called from MainInventoryWindow
}