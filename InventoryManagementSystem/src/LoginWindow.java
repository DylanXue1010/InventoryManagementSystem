// LoginWindow.java
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Optional;

public class LoginWindow extends JDialog {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton cancelButton;
    private UserManager userManager;
    private MainInventoryWindow mainAppWindow; // 主应用窗口的引用

    private User authenticatedUser = null;

    public LoginWindow(Frame owner, UserManager userManager, MainInventoryWindow mainApp) {
        super(owner, "Login", true); // true 表示模态对话框
        this.userManager = userManager;
        this.mainAppWindow = mainApp;

        initComponents();
        layoutComponents();
        attachEventHandlers();

        pack();
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE); // 或 DO_NOTHING_ON_CLOSE 然后处理 X 按钮
    }

    private void initComponents() {
        usernameField = new JTextField(20);
        passwordField = new JPasswordField(20);
        loginButton = new JButton("Login");
        cancelButton = new JButton("Cancel");
    }

    private void layoutComponents() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; add(usernameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; add(passwordField, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(loginButton);
        buttonPanel.add(cancelButton);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.CENTER;
        add(buttonPanel, gbc);
    }

    private void attachEventHandlers() {
        loginButton.addActionListener(e -> performLogin());
        passwordField.addActionListener(e -> performLogin()); // 允许回车登录
        cancelButton.addActionListener(e -> {
            authenticatedUser = null; // 明确未认证
            dispose();
            // 根据需要，如果取消登录就退出整个应用
            // System.exit(0);
        });
    }

    private void performLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username and password cannot be empty.", "Login Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Optional<User> userOpt = userManager.authenticateUser(username, password);

        if (userOpt.isPresent()) {
            this.authenticatedUser = userOpt.get();
            JOptionPane.showMessageDialog(this, "Login successful! Welcome " + authenticatedUser.getUsername(), "Login Success", JOptionPane.INFORMATION_MESSAGE);
            dispose(); // 关闭登录窗口
            mainAppWindow.onLoginSuccess(this.authenticatedUser); // 通知主窗口登录成功
        } else {
            JOptionPane.showMessageDialog(this, "Invalid username or password.", "Login Failed", JOptionPane.ERROR_MESSAGE);
            passwordField.setText(""); // 清空密码框
        }
    }

    // 公共方法，以便主程序可以检查登录是否成功并获取用户
    public User getAuthenticatedUser() {
        return authenticatedUser;
    }
}
