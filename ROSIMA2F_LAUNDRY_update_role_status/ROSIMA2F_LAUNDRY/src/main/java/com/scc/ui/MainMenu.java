package com.scc.ui;

import com.scc.dao.LogDao;
import com.scc.dao.UserDao;
import com.scc.model.User;

import java.sql.SQLException;

import static com.scc.util.Console.*;
import static com.scc.util.Constants.ROLE_CUSTOMER;
import static com.scc.util.Constants.ROLE_STAFF;

/**
 * First screen of the program: Login / Register / Exit.
 * Logging out from any role menu comes back here.
 */
public class MainMenu {

    public static void show() {
        boolean running = true;
        while (running) {
            System.out.println("\n===== LAUNDRY ORDER TRACKING SYSTEM =====");
            System.out.println("[1] Login");
            System.out.println("[2] Register as Customer");
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

    private static void registerFlow() throws SQLException {
        System.out.println("\n--- CUSTOMER REGISTRATION ---");
        String name = askRequired("Full name: ");
        String contact = askRequired("Contact number: ");
        String address = askRequired("Address: ");
        String username = askNewUsername();
        String password = askPassword("Password (min 4 chars): ");
        int userId = UserDao.registerCustomer(name, contact, address, username, password, "PENDING");
        LogDao.log(userId, null, "Customer account created: " + username);
        System.out.println("\n-> Successful Registration! Your account is PENDING - an admin must approve it before you can log in.");
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
        System.out.println("\n-> Welcome, " + user.username() + " (" + user.role() + ")");
        switch (user.role()) {
            case ROLE_CUSTOMER -> CustomerMenu.show(user);
            case ROLE_STAFF -> StaffMenu.show(user);
            default -> AdminMenu.show(user);
        }
    }
}
