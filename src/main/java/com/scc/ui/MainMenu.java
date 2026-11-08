package com.scc.ui;

import com.scc.dao.LogDao;
import com.scc.dao.UserDao;
import com.scc.model.User;

import java.sql.SQLException;

import static com.scc.util.Console.*;
import static com.scc.util.Constants.ROLE_ADMIN;
import static com.scc.util.Constants.ROLE_CUSTOMER;
import static com.scc.util.Constants.ROLE_STAFF;

/**
 * First screen of the program: Login / Register (Customer, Laundry Staff, or Admin) / Exit.
 * Logging out from any role menu comes back here.
 */
public class MainMenu {

    public static void show() {
        boolean running = true;
        while (running) {
            System.out.println("\n===== LAUNDRY ORDER TRACKING SYSTEM =====");
            System.out.println("[1] Login");
            System.out.println("[2] Register");
            System.out.println("[0] Exit");
            int choice = askInt("Select: ", 0, 2);
            try {
                switch (choice) {
                    case 1 -> loginFlow();
                    case 2 -> registerFlow();
                    default -> running = false;
                }
            } catch (SQLException e) {
                dbError(e);
            }
        }
        System.out.println("Goodbye!");
    }

    /**
     * Register as a Customer, Laundry Staff, or Admin. The account is PENDING until an admin approves it.
     * Exception: while no APPROVED admin exists, a new Admin is approved at once, so the first admin can
     * start the system (otherwise nobody could approve anybody).
     */
    private static void registerFlow() throws SQLException {
        System.out.println("\n--- REGISTRATION ---");
        System.out.println("Register as: [1] Customer  [2] Laundry Staff  [3] Admin  [0] Back");
        int ch = askInt("Select: ", 0, 3);
        if (ch == 0) return;
        String role = switch (ch) {
            case 1 -> ROLE_CUSTOMER;
            case 2 -> ROLE_STAFF;
            default -> ROLE_ADMIN;
        };
        String name = null;
        String contact = null;
        String address = null;
        if (ch == 1) { // only a customer needs a profile (name, contact, address)
            name = askRequired("Full name: ");
            contact = askRequired("Contact number: ");
            address = askRequired("Address: ");
        }
        String username = askNewUsername();
        String password = askPassword("Password: ");

        String status = "PENDING";
        int userId;
        if (ch == 1) {
            userId = UserDao.registerCustomer(name, contact, address, username, password, status);
        } else {
            if (ch == 3 && UserDao.countApprovedAdmins() == 0) status = "APPROVED";
            userId = UserDao.createStaffOrAdmin(username, password, role, status);
        }
        LogDao.logAction(userId, "USER_REGISTERED", "Account registered: " + username + " (" + role + ", " + status + ")");
        if ("APPROVED".equals(status)) {
            System.out.println("\n-> Successful Registration! There was no approved admin yet, so this Admin account is APPROVED. You can log in now.");
        } else {
            System.out.println("\n-> Successful Registration! Your account is PENDING - an admin must approve it before you can log in.");
        }
    }

    private static void loginFlow() throws SQLException {
        System.out.println("\n--- LOGIN ---");
        String username = askRequired("Username: ");
        String password = askRequired("Password: ");
        User user = UserDao.login(username, password);
        if (user == null) {
            System.out.println("-> Invalid username or password.");
            return;
        }
        if ("PENDING".equals(user.accountStatus())) {
            System.out.println("-> Your account is still awaiting admin approval.");
            return;
        }
        if (!"APPROVED".equals(user.accountStatus())) {
            System.out.println("-> This account has been deactivated. Please contact the admin.");
            return;
        }
        LogDao.logAction(user.userId(), "USER_LOGIN", "User " + user.username() + " (ID: " + user.userId() + ") logged in.");
        System.out.println("\n-> Welcome, " + user.username() + " (" + user.role() + ")");
        switch (user.role()) {
            case ROLE_CUSTOMER -> CustomerMenu.show(user);
            case ROLE_STAFF -> StaffMenu.show(user);
            default -> AdminMenu.show(user);
        }
        // The role menu returns here when the user chooses Logout.
        LogDao.logAction(user.userId(), "USER_LOGOUT", "User " + user.username() + " (ID: " + user.userId() + ") logged out.");
    }
}
