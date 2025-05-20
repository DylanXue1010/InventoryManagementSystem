import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserManager {
    private List<User> users;
    // 统一数据目录路径
    public static final String DATA_DIRECTORY = "data/";
    private static final String USERS_FILE_PATH = DATA_DIRECTORY + "users.csv";
    private static final String CSV_HEADER = "username,passwordHash,salt,role";

    public UserManager() {
        this.users = new ArrayList<>();
        loadUsersFromFile();
    }

    private void loadUsersFromFile() {
        File file = new File(USERS_FILE_PATH);
        if (!file.exists()) {
            System.out.println("Users file not found: " + USERS_FILE_PATH + ". Attempting to create default admin.");
            // Ensure data directory exists for potential save operations later
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            createDefaultAdminUserIfNotExists(true);
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line = br.readLine();
            if (line == null || !line.trim().equalsIgnoreCase(CSV_HEADER)) {
                System.err.println("Warning: Users CSV file header mismatch or file is empty. Expected: '" + CSV_HEADER + "'. Got: '" + (line != null ? line.trim() : "null") + "'");
                if (this.users.isEmpty()) {
                    System.out.println("Attempting to create default admin due to header issue or empty user list after file check.");
                    createDefaultAdminUserIfNotExists(true);
                }
                return;
            }

            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split(",", -1);
                if (parts.length >= 4) {
                    users.add(new User(parts[0].trim(), parts[1].trim(), parts[2].trim(), parts[3].trim()));
                } else {
                    System.err.println("Skipping malformed user line (not enough parts): " + line);
                }
            }
            System.out.println(users.size() + " users loaded successfully from " + USERS_FILE_PATH);

            if (this.users.isEmpty()) {
                System.out.println("No users found in file after loading. Attempting to create default admin.");
                createDefaultAdminUserIfNotExists(true);
            }

        } catch (IOException e) {
            System.err.println("Error loading users from file " + USERS_FILE_PATH + ": " + e.getMessage());
            if (this.users.isEmpty()) {
                System.out.println("Attempting to create default admin due to IO error during load.");
                createDefaultAdminUserIfNotExists(true);
            }
        }
    }

    public void saveUsersToFile() {
        File file = new File(USERS_FILE_PATH);
        File parentDir = file.getParentFile(); // Should be "data" directory
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                System.err.println("Could not create directory for users file: " + parentDir.getPath());
                return;
            }
        }

        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file)))) {
            out.println(CSV_HEADER);
            for (User user : users) {
                out.println(String.join(",",
                        user.getUsername(),
                        user.getPasswordHash(),
                        user.getSalt(),
                        user.getRole()
                ));
            }
            System.out.println(users.size() + " users saved successfully to " + USERS_FILE_PATH);
        } catch (IOException e) {
            System.err.println("Error saving users to file " + USERS_FILE_PATH + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Optional<User> findUser(String username) {
        if (username == null || username.trim().isEmpty()) {
            return Optional.empty();
        }
        return users.stream()
                .filter(user -> username.equals(user.getUsername()))
                .findFirst();
    }

    public Optional<User> authenticateUser(String username, String password) {
        Optional<User> userOpt = findUser(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (PasswordUtils.verifyPassword(password, user.getPasswordHash(), user.getSalt())) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    public boolean addUser(String username, String password, String role) {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty() || role == null || role.trim().isEmpty()) {
            System.err.println("Error: Username, password, and role cannot be empty when adding a new user.");
            return false;
        }
        if (findUser(username).isPresent()) {
            System.err.println("Error: User '" + username + "' already exists.");
            return false;
        }
        String salt = PasswordUtils.generateSalt();
        String passwordHash = PasswordUtils.hashPassword(password, salt);
        User newUser = new User(username, passwordHash, salt, role);
        boolean addedToList = this.users.add(newUser);
        if (addedToList) {
            System.out.println("User '" + username + "' (Role: " + role + ") added to internal list.");
        } else {
            System.err.println("Failed to add user '" + username + "' to internal list.");
        }
        return addedToList;
    }

    public void createDefaultAdminUserIfNotExists(boolean saveToFileAfterCreation) {
        if (findUser("admin").isEmpty()) {
            System.out.println("Default admin user 'admin' not found in memory. Attempting to create with password 'admin'.");
            boolean addedSuccessfully = addUser("admin", "admin", "Admin");

            if (addedSuccessfully && saveToFileAfterCreation) {
                System.out.println("Default admin user created locally, now saving all users to file.");
                saveUsersToFile();
            } else if (!addedSuccessfully) {
                System.err.println("Failed to create default admin user 'admin' locally (addUser returned false).");
            } else { // addedSuccessfully && !saveToFileAfterCreation
                System.out.println("Default admin user 'admin' created locally but not saved to file immediately (saveToFileAfterCreation was false).");
            }
        } else {
            System.out.println("Default admin user 'admin' already exists in memory.");
        }
    }

    public static void main(String[] args) {
        System.out.println("Testing UserManager (data in " + DATA_DIRECTORY + ")...");
        UserManager userManager = new UserManager();

        if (userManager.findUser("admin").isEmpty()) {
            System.out.println("Admin user still not found after UserManager construction, explicitly calling createDefaultAdminUserIfNotExists again.");
            userManager.createDefaultAdminUserIfNotExists(true);
        }

        System.out.println("\n--- Testing Authentication ---");
        Optional<User> authUser = userManager.authenticateUser("admin", "admin");
        if (authUser.isPresent()) {
            System.out.println("SUCCESS: Authentication for 'admin'/'admin' successful. Role: " + authUser.get().getRole());
        } else {
            System.err.println("FAILURE: Authentication for 'admin'/'admin' failed.");
        }

        Optional<User> authFailUser = userManager.authenticateUser("admin", "wrongpassword");
        if (authFailUser.isEmpty()) {
            System.out.println("SUCCESS: Authentication for 'admin'/'wrongpassword' failed as expected.");
        } else {
            System.err.println("FAILURE: Authentication for 'admin'/'wrongpassword' unexpectedly succeeded.");
        }

        Optional<User> noSuchUser = userManager.authenticateUser("nouser", "anypassword");
        if (noSuchUser.isEmpty()) {
            System.out.println("SUCCESS: Authentication for 'nouser' failed as expected (user does not exist).");
        } else {
            System.err.println("FAILURE: Authentication for 'nouser' unexpectedly succeeded.");
        }

        System.out.println("\nUserManager test finished.");
    }
}
