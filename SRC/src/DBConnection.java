import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DBConnection provides JDBC database connectivity for Kongu Engineering College (KEC)
 * R2024 Academic Evaluation System using MySQL.
 */
public final class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/kec_r2024?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata";
    private static final String USER = "root";
    private static final String PASSWORD = "Mysql@12345";

    private DBConnection() {
        // Prevent instantiation
    }

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("[ERROR] MySQL JDBC Driver not found on classpath: " + e.getMessage());
        }
    }

    /**
     * Gets a new JDBC database connection.
     * @return Connection object
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /**
     * Tests whether the database connection can be established.
     * @return true if successful, false otherwise
     */
    public static boolean testConnection() {
        try (Connection con = getConnection()) {
            return con != null && !con.isClosed();
        } catch (SQLException e) {
            System.err.println("[DBConnection Test Failed] " + e.getMessage());
            return false;
        }
    }
}
