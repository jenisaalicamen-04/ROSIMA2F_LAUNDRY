package com.scc.dao;

import com.scc.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static com.scc.util.Constants.ROLE_CUSTOMER;

/** UserAccount table: register, login, update and activate or deactivate accounts.
 *  Passwords are stored exactly as typed (plain text, no hashing). */
public class UserDao {

    private static User map(ResultSet rs) throws SQLException {
        int cid = rs.getInt("customer_id");
        Integer customerId = rs.wasNull() ? null : cid;
        return new User(rs.getInt("user_id"), rs.getString("username"),
                rs.getString("role"), customerId, rs.getString("account_status"));
    }

    public static boolean usernameExists(String username) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM UserAccount WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** CREATE: customer profile + login account, saved together (all-or-nothing). */
    public static int registerCustomer(String name, String contact, String address,
                                       String username, String password, String status) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                int customerId;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO Customer(name, contact_number, address) VALUES(?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, name);
                    ps.setString(2, contact);
                    ps.setString(3, address);
                    ps.executeUpdate();
                    try (ResultSet k = ps.getGeneratedKeys()) {
                        k.next();
                        customerId = k.getInt(1);
                    }
                }
                int userId;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO UserAccount(username, password, role, customer_id, account_status) "
                                + "VALUES(?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, username);
                    ps.setString(2, password);
                    ps.setString(3, ROLE_CUSTOMER);
                    ps.setInt(4, customerId);
                    ps.setString(5, status);
                    ps.executeUpdate();
                    try (ResultSet k = ps.getGeneratedKeys()) {
                        k.next();
                        userId = k.getInt(1);
                    }
                }
                c.commit();
                return userId;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** Admin adds a staff or admin account: it is APPROVED straight away. */
    public static int createStaffOrAdmin(String username, String password, String role) throws SQLException {
        return createStaffOrAdmin(username, password, role, "APPROVED");
    }

    /** Creates a staff or admin account with the given status (PENDING for self-registration). */
    public static int createStaffOrAdmin(String username, String password, String role, String status)
            throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO UserAccount(username, password, role, account_status) VALUES(?,?,?,?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, role);
            ps.setString(4, status);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                k.next();
                return k.getInt(1);
            }
        }
    }

    public static User login(String username, String password) throws SQLException {
        String sql = "SELECT user_id, username, role, customer_id, account_status "
                + "FROM UserAccount WHERE username = ? AND password = ?";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public static User findById(int userId) throws SQLException {
        String sql = "SELECT user_id, username, role, customer_id, account_status FROM UserAccount WHERE user_id = ?";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public static List<User> findAll() throws SQLException {
        List<User> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT user_id, username, role, customer_id, account_status FROM UserAccount ORDER BY user_id")) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    /** UPDATE: change the username. */
    public static void updateUsername(int userId, String username) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE UserAccount SET username = ? WHERE user_id = ?")) {
            ps.setString(1, username);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    /** UPDATE: change the password. */
    public static void updatePassword(int userId, String password) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE UserAccount SET password = ? WHERE user_id = ?")) {
            ps.setString(1, password);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    /** DELETE (soft): APPROVED / INACTIVE. The record is never removed. */
    public static void setStatus(int userId, String status) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE UserAccount SET account_status = ? WHERE user_id = ?")) {
            ps.setString(1, status);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    /**
     * UPDATE: change a user's role and status.
     * Both values are converted to upper case before they reach the database.
     * A user switched to CUSTOMER who has no customer profile gets one (name = username, contact and address
     * "N/A"); the admin or the customer can edit it later. An old customer profile is kept, so orders stay linked.
     */
    public static void updateUserRoleAndStatus(int userId, String newRole, String newStatus) throws SQLException {
        String role = newRole.trim().toUpperCase();
        String status = newStatus.trim().toUpperCase();
        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                if (ROLE_CUSTOMER.equals(role)) ensureCustomerProfile(c, userId);
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE UserAccount SET role = ?, account_status = ? WHERE user_id = ?")) {
                    ps.setString(1, role);
                    ps.setString(2, status);
                    ps.setInt(3, userId);
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** Gives the account a customer profile when it has none yet (needed before it can become a CUSTOMER). */
    private static void ensureCustomerProfile(Connection c, int userId) throws SQLException {
        String username;
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT username, customer_id FROM UserAccount WHERE user_id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return;
                username = rs.getString("username");
                rs.getInt("customer_id");
                if (!rs.wasNull()) return; // already has a profile
            }
        }
        int customerId;
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO Customer(name, contact_number, address) VALUES(?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, "N/A");
            ps.setString(3, "N/A");
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                k.next();
                customerId = k.getInt(1);
            }
        }
        try (PreparedStatement ps = c.prepareStatement("UPDATE UserAccount SET customer_id = ? WHERE user_id = ?")) {
            ps.setInt(1, customerId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    /** How many APPROVED admin accounts exist. While it is 0, a newly registered Admin is approved at once. */
    public static int countApprovedAdmins() throws SQLException {
        String sql = "SELECT COUNT(*) FROM UserAccount WHERE role = 'ADMIN' AND account_status = 'APPROVED'";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ---------------------------------------------------------------- approval workflow

    /** Returns every registered account, whatever its role or status. */
    public static List<User> getAllUsers() throws SQLException {
        return findAll();
    }

    /** Returns every account (any role) that is still waiting for admin approval. */
    public static List<User> getPendingUsers() throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT user_id, username, role, customer_id, account_status "
                + "FROM UserAccount WHERE account_status = 'PENDING' ORDER BY user_id";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    /** Changes one account from PENDING to APPROVED. */
    public static void approveUser(int userId) throws SQLException {
        String sql = "UPDATE UserAccount SET account_status = 'APPROVED' WHERE user_id = ? AND account_status = 'PENDING'";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }
}
