import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

// 假设 Item 和 Inventory 类在同一个包或已正确导入
// import com.yourpackage.Item;
// import com.yourpackage.Inventory;

public class AddProductWindow extends JDialog {

    private JTextField skuField, nameField, categoryField, quantityField, priceField, supplierField, statusField;
    private JButton saveButton, cancelButton;

    private Inventory inventory; // 引用 Inventory 对象
    private MainInventoryWindow ownerWindow; // 引用主窗口，用于刷新

    public AddProductWindow(MainInventoryWindow owner, Inventory inventory) {
        super(owner, "Add New Product", true); // true 表示模态对话框
        this.ownerWindow = owner;
        this.inventory = inventory;

        initComponents();
        layoutComponents();
        attachEventHandlers();

        pack(); // 根据组件自动调整窗口大小
        setLocationRelativeTo(owner); // 相对于父窗口居中显示
    }

    private void initComponents() {
        skuField = new JTextField(20);
        nameField = new JTextField(20);
        categoryField = new JTextField(20);
        quantityField = new JTextField(10);
        priceField = new JTextField(10);
        supplierField = new JTextField(20);
        statusField = new JTextField(10); // 可以考虑使用 JComboBox 提供预设状态

        saveButton = new JButton("Save");
        cancelButton = new JButton("Cancel");
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10)); // 主布局，带边距

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // 组件间距
        gbc.anchor = GridBagConstraints.WEST;

        // SKU
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("SKU:"), gbc);
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
        // 建议: 可以将 Status 改为 JComboBox，例如:
        // String[] statuses = {"Active", "Damaged", "Expired", "Low Stock"};
        // statusComboBox = new JComboBox<>(statuses);
        // formPanel.add(statusComboBox, gbc);


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
                saveProduct();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose(); // 关闭对话框
            }
        });
    }

    private void saveProduct() {
        // 1. 获取输入数据
        String sku = skuField.getText().trim();
        String name = nameField.getText().trim();
        String category = categoryField.getText().trim();
        String quantityStr = quantityField.getText().trim();
        String priceStr = priceField.getText().trim();
        String supplier = supplierField.getText().trim();
        String status = statusField.getText().trim(); // 如果使用 JComboBox, 用 (String) statusComboBox.getSelectedItem();

        // 2. 数据校验 (非常重要)
        if (sku.isEmpty() || name.isEmpty() || category.isEmpty() || quantityStr.isEmpty() || priceStr.isEmpty() || supplier.isEmpty() || status.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
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
            price = Double.parseDouble(priceStr);
            if (price < 0) {
                JOptionPane.showMessageDialog(this, "Price cannot be negative.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid price format. Please enter a number.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 检查SKU是否已存在 (这应该在 Inventory 类中处理，或者在这里调用 Inventory 的检查方法)
        if (inventory.getItem(sku) != null) {
            JOptionPane.showMessageDialog(this, "SKU '" + sku + "' already exists. Please use a unique SKU.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }


        // 3. 创建 Item 对象
        Item newItem = new Item(sku, name, category, quantity, price, supplier, status);

        // 4. 添加到 Inventory
        inventory.addItem(newItem); // 假设 Inventory.addItem 会处理重复SKU的情况 (或者我们已经检查过了)

        // 5. 提示成功并刷新主窗口表格
        JOptionPane.showMessageDialog(this, "Product added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        ownerWindow.loadInventoryData(); // 调用主窗口的方法刷新表格

        // 6. 关闭对话框
        dispose();
    }

    // 这个 AddProductWindow 类不需要自己的 main 方法，它会被 MainInventoryWindow 调用。
    // 但为了独立测试布局，可以临时添加一个：
    /*
    public static void main(String[] args) {
        // 仅用于测试 AddProductWindow 的布局
        // 实际使用时，它会从 MainInventoryWindow 中调用
        Inventory tempInventory = new Inventory(); // 临时的 Inventory 对象
        MainInventoryWindow tempOwner = new MainInventoryWindow(tempInventory); // 临时的 Owner

        SwingUtilities.invokeLater(() -> {
            AddProductWindow dialog = new AddProductWindow(tempOwner, tempInventory);
            dialog.setVisible(true);
        });
    }
    */
}
