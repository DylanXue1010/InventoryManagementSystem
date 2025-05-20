// User.java
public class User {
    private String username;
    private String passwordHash; // 存储加密后的密码
    private String salt;         // 用于密码哈希的盐值
    private String role;         // 例如："Admin", "Staff"

    public User(String username, String passwordHash, String salt, String role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.role = role;
    }

    // Getters
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getSalt() { return salt; }
    public String getRole() { return role; }

    // 可选的 Setters (例如管理员修改用户信息时)
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setSalt(String salt) { this.salt = salt; }
    public void setRole(String role) { this.role = role; }

    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}
