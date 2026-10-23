package com.scc.dao;

import com.scc.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static com.scc.util.Constants.ROLE_ADMIN;
import static com.scc.util.Constants.ROLE_CUSTOMER;
import static com.scc.util.Constants.ROLE_STAFF;

/** UserAccount table: register, login, update and activate or deactivate accounts.
 *  Passwords are stored exactly as typed (plain text, no hashing). */
public class UserDao {

    public static void seedDefaultAccounts() throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM UserAccount")) {
            if (rs.next() && rs.getInt(1) > 0) return;
        }
        createStaffOrAdmin("admin", "admin123", ROLE_ADMIN);
        createStaffOrAdmin("staff", "staff123", ROLE_STAFF);
    }

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

    public static int createStaffOrAdmin(String username, String password, String role) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO UserAccount(username, password, role, account_status) VALUES(?,?,?,'APPROVED')",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, role);
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

    /** DELETE (soft): APPROVED / Inactive. The record is never removed. */
    public static void setStatus(int userId, String status) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE UserAccount SET account_status = ? WHERE user_id = ?")) {
            ps.setString(1, status);
            ps.setInt(2, userId);
            ps.executeUpdate();
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
