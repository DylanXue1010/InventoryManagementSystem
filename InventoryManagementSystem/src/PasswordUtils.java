// PasswordUtils.java
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordUtils {

    // 生成盐值
    public static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    // 哈希密码
    public static String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            // 将盐值字节和密码字节结合起来哈希，可以增加破解难度
            // 更安全的做法是将盐值混入密码处理的多个环节或使用标准库如 bcrypt
            md.update(Base64.getDecoder().decode(salt));
            byte[] hashedPassword = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashedPassword);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    // 验证密码
    public static boolean verifyPassword(String originalPassword, String storedPasswordHash, String salt) {
        String newHash = hashPassword(originalPassword, salt);
        return newHash.equals(storedPasswordHash);
    }

    /*
    // 示例用法 (可以放在 main 方法中测试)
    public static void main(String[] args) {
        String password = "testpassword";
        String salt = generateSalt();
        String hashedPassword = hashPassword(password, salt);

        System.out.println("Salt: " + salt);
        System.out.println("Hashed Password: " + hashedPassword);
        System.out.println("Verification successful: " + verifyPassword(password, hashedPassword, salt));
        System.out.println("Verification failure: " + verifyPassword("wrongpassword", hashedPassword, salt));
    }
    */
}
