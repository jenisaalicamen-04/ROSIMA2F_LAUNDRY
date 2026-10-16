package com.scc.ui;

import com.scc.dao.CustomerDao;
import com.scc.dao.LogDao;
import com.scc.dao.UserDao;
import com.scc.model.Customer;
import com.scc.model.User;

import java.sql.SQLException;

import static com.scc.util.Console.*;
import static com.scc.util.Constants.*;

/** Admin sub-screen: list, add, edit, deactivate and reactivate accounts. */
public class AccountMenu {

    public static void show(User admin) throws SQLException {
        while (true) {
            System.out.println("\n--- MANAGE ACCOUNTS ---");
            System.out.println("[1] List accounts");
            System.out.println("[2] Add account");
            System.out.println("[3] Edit account");
            System.out.println("[4] Deactivate account");
            System.out.println("[5] Reactivate account");
            System.out.println("[0] Back");
            int ch = askInt("Select: ", 0, 5);
            switch (ch) {
                case 1 -> listAccounts();
                case 2 -> addAccount(admin);
                case 3 -> editAccount(admin);
                case 4 -> changeAccountStatus(admin, "Inactive");
                case 5 -> changeAccountStatus(admin, "Active");
                default -> {
                    return;
                }
            }
        }
    }

    private static void listAccounts() throws SQLException {
        System.out.printf("%-5s %-18s %-14s %-10s %-12s%n", "ID", "Username", "Role", "Status", "Customer ID");
        System.out.println("-".repeat(64));
        for (User u : UserDao.findAll()) {
            System.out.printf("%-5d %-18s %-14s %-10s %-12s%n", u.userId(), u.username(), u.role(),
                    u.accountStatus(), u.customerId() == null ? "-" : String.valueOf(u.customerId()));
        }
    }

    private static void addAccount(User admin) throws SQLException {
        System.out.println("Role: [1] Customer  [2] Laundry Staff  [3] Admin  [0] Back");
        int ch = askInt("Select: ", 0, 3);
        if (ch == 0) return;
        String role = switch (ch) {
            case 1 -> ROLE_CUSTOMER;
            case 2 -> ROLE_STAFF;
            default -> ROLE_ADMIN;
        };
        String username;
        String password;
        if (ch == 1) {
            String name = askRequired("Full name: ");
            String contact = askRequired("Contact number: ");
            String address = askRequired("Address: ");
            username = askNewUsername();
            password = askPassword("Password (min 4 chars): ");
            UserDao.registerCustomer(name, contact, address, username, password);
        } else {
            username = askNewUsername();
            password = askPassword("Password (min 4 chars): ");
            UserDao.createStaffOrAdmin(username, password, role);
        }
        LogDao.log(admin.userId(), null, "Account created: " + username + " (" + role + ")");
        System.out.println("-> Account created.");
    }

    private static void editAccount(User admin) throws SQLException {
        listAccounts();
        User u = UserDao.findById(askId("User ID to edit: "));
        if (u == null) {
            System.out.println("-> Account not found.");
            return;
        }
        System.out.println("Editing '" + u.username() + "'. Leave blank to keep the current value.");

        String newUser = ask("New username: ");
        if (!newUser.isEmpty() && !newUser.equals(u.username())) {
            if (UserDao.usernameExists(newUser)) System.out.println("   Username taken - not changed.");
            else UserDao.updateUsername(u.userId(), newUser);
        }
        String newPass = ask("New password (min 4 chars): ");
        if (newPass.length() >= 4) UserDao.updatePassword(u.userId(), newPass);
        else if (!newPass.isEmpty()) System.out.println("   Password too short - not changed.");

        if (u.customerId() != null) {
            Customer c = CustomerDao.find(u.customerId());
            String name = ask("Full name [" + c.name() + "]: ");
            String contact = ask("Contact number [" + c.contactNumber() + "]: ");
            String address = ask("Address [" + c.address() + "]: ");
            CustomerDao.update(c.customerId(),
                    name.isEmpty() ? c.name() : name,
                    contact.isEmpty() ? c.contactNumber() : contact,
                    address.isEmpty() ? c.address() : address);
        }
        LogDao.log(admin.userId(), null, "Account information updated: " + u.username());
        System.out.println("-> Account updated.");
    }

    private static void changeAccountStatus(User admin, String status) throws SQLException {
        listAccounts();
        User u = UserDao.findById(askId("User ID: "));
        if (u == null) {
            System.out.println("-> Account not found.");
        } else if (u.userId() == admin.userId() && "Inactive".equals(status)) {
            System.out.println("-> You cannot deactivate your own account.");
        } else {
            UserDao.setStatus(u.userId(), status);
            LogDao.log(admin.userId(), null,
                    ("Inactive".equals(status) ? "Account deactivated: " : "Account reactivated: ") + u.username());
            System.out.println("-> " + u.username() + " is now " + status + ".");
        }
    }
}
