package com.scc.ui;

import com.scc.dao.LogDao;
import com.scc.dao.OrderDao;
import com.scc.dao.UserDao;
import com.scc.model.ActivityLog;
import com.scc.model.StatusReport;
import com.scc.model.User;
import com.scc.util.ReportExporter;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static com.scc.util.Console.*;
import static com.scc.util.Constants.ROLE_ADMIN;
import static com.scc.util.Constants.ROLE_CUSTOMER;
import static com.scc.util.Constants.ROLE_STAFF;

/** Screen for the ADMIN role: accounts, all orders, audit logs, status report + export, role / status updates. */
public class AdminMenu {

    public static void show(User user) {
        while (true) {
            System.out.println("\n--- ADMIN MENU ---");
            System.out.println("[1] Manage accounts");
            System.out.println("[2] View all laundry orders");
            System.out.println("[3] Search orders");
            System.out.println("[4] View System Audit Logs");
            System.out.println("[5] View Laundry Order Status Report");
            System.out.println("[6] Export Status Report (PDF / CSV / Excel)");
            System.out.println("[7] View all system users");
            System.out.println("[8] View pending registrations");
            System.out.println("[9] Approve user account");
            System.out.println("[10] Update user role and status");
            System.out.println("[0] Logout");
            int ch = askInt("Select: ", 0, 10);
            try {
                switch (ch) {
                    case 1 -> AccountMenu.show(user);
                    case 2 -> printOrders(OrderDao.findAll());
                    case 3 -> OrderSearch.show();
                    case 4 -> viewAuditLogs();
                    case 5 -> printReport(OrderDao.report());
                    case 6 -> exportReport();
                    case 7 -> viewAllUsers();
                    case 8 -> viewPendingUsers();
                    case 9 -> approveUserAccount(user);
                    case 10 -> updateUserRoleAndStatus(user);
                    default -> {
                        return;
                    }
                }
            } catch (SQLException e) {
                dbError(e);
            } catch (IOException e) {
                System.out.println("   Could not write the file: " + e.getMessage());
            }
        }
    }

    private static void viewAllUsers() throws SQLException {
        System.out.println("\n--- ALL SYSTEM USERS ---");
        printUserTable(UserDao.getAllUsers());
    }

    private static void viewPendingUsers() throws SQLException {
        System.out.println("\n--- PENDING REGISTRATIONS ---");
        printUserTable(UserDao.getPendingUsers());
    }

    /** Shows the pending list, asks for an ID (0 = cancel), checks it is pending, then approves it. */
    private static void approveUserAccount(User admin) throws SQLException {
        List<User> pending = UserDao.getPendingUsers();
        System.out.println("\n--- APPROVE USER ACCOUNT ---");
        if (pending.isEmpty()) {
            System.out.println("   No pending registrations.");
            return;
        }
        printUserTable(pending);
        while (true) {
            int id = askInt("Enter User ID to approve (0 to cancel): ", 0, Integer.MAX_VALUE);
            if (id == 0) {
                System.out.println("-> Approval cancelled.");
                return;
            }
            User target = pending.stream().filter(u -> u.userId() == id).findFirst().orElse(null);
            if (target == null) {
                System.out.println("-> ID " + id + " is not in the pending list. Try again or enter 0 to cancel.");
                continue;
            }
            UserDao.approveUser(id);
            LogDao.logAction(admin.userId(), "USER_APPROVED", "Admin [" + admin.userId() + "] approved User [" + id + "]");
            System.out.println("-> Success! " + target.username() + " (User ID " + id + ") is now APPROVED.");
            return;
        }
    }

    // ---------------------------------------------------------------- update role + status

    /** Shows all users, asks for a target ID, then a new role and status. Entering 0 cancels at any step. */
    private static void updateUserRoleAndStatus(User admin) throws SQLException {
        System.out.println("\n--- UPDATE USER ROLE AND STATUS ---");
        List<User> users = UserDao.getAllUsers();
        printUserTable(users);

        // Step 1: target user ID
        User target;
        while (true) {
            int id = readIntInput("Enter User ID to update (0 to cancel): ");
            if (id == 0) {
                System.out.println("-> Update cancelled.");
                return;
            }
            if (id == admin.userId()) {
                System.out.println("WARNING: Modifying the currently active admin account is restricted.");
                continue;
            }
            target = users.stream().filter(u -> u.userId() == id).findFirst().orElse(null);
            if (target == null) {
                System.out.println("-> No user with ID " + id + ". Try again or enter 0 to cancel.");
                continue;
            }
            break;
        }

        // Step 2: new role
        String newRole;
        while (true) {
            newRole = readValidRole();
            if (newRole == null) {
                System.out.println("-> Update cancelled.");
                return;
            }
            break;
        }

        // Step 3: new status
        String newStatus = readValidStatus();
        if (newStatus == null) {
            System.out.println("-> Update cancelled.");
            return;
        }

        // Step 4: confirm
        System.out.printf("%nChange %s (ID %d): %s / %s  ->  %s / %s%n", target.username(), target.userId(),
                target.role(), target.accountStatus(), newRole, newStatus);
        if (readIntInput("Confirm? [1] Yes  [0] Cancel: ") != 1) {
            System.out.println("-> Update cancelled.");
            return;
        }

        UserDao.updateUserRoleAndStatus(target.userId(), newRole, newStatus);
        LogDao.logAction(admin.userId(), "USER_UPDATED", "Admin [" + admin.userId() + "] changed role of User "
                + target.userId() + " to " + newRole + " and status of User " + target.userId() + " to " + newStatus);
        System.out.println("-> Success! " + target.username() + " is now " + newRole + " / " + newStatus + ".");
        printUserTable(List.of(UserDao.findById(target.userId())));
    }

    // ---------------------------------------------------------------- input validation wrappers

    /** Reads a whole number. Letters or symbols never crash the program - it just asks again. */
    private static int readIntInput(String prompt) {
        while (true) {
            String s = ask(prompt);
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                System.out.println("-> Invalid input. Please enter a number.");
            }
        }
    }

    /** Keeps asking until a valid role is typed. Returns the role in upper case, or null if the admin enters 0. */
    private static String readValidRole() {
        while (true) {
            String s = ask("Enter new role [" + ROLE_CUSTOMER + " / " + ROLE_STAFF + " / " + ROLE_ADMIN
                    + "] (0 to cancel): ").toUpperCase();
            if (s.equals("0")) return null;
            if (s.equals("STAFF")) s = ROLE_STAFF;        // short form
            if (s.equals(ROLE_CUSTOMER) || s.equals(ROLE_STAFF) || s.equals(ROLE_ADMIN)) return s;
            System.out.println("-> Invalid role. Choose CUSTOMER, LAUNDRY STAFF or ADMIN.");
        }
    }

    /** Keeps asking until APPROVED or PENDING is typed. Returns it in upper case, or null if the admin enters 0. */
    private static String readValidStatus() {
        while (true) {
            String s = ask("Enter new status [APPROVED / PENDING] (0 to cancel): ").toUpperCase();
            if (s.equals("0")) return null;
            if (s.equals("APPROVED") || s.equals("PENDING")) return s;
            System.out.println("-> Invalid status. Choose APPROVED or PENDING.");
        }
    }

    // ---------------------------------------------------------------- system audit logs

    private static final int DESC_WIDTH = 35;
    private static final String LOG_ROW = "%-5s | %-8s | %-18s | %-35s | %-20s";

    /** Audit log screen: view every log, or only the logs of one user. */
    private static void viewAuditLogs() throws SQLException {
        while (true) {
            System.out.println("\n--- SYSTEM AUDIT LOGS ---");
            System.out.println("[1] View All System Logs");
            System.out.println("[2] Filter Logs by User ID");
            System.out.println("[0] Back");
            int ch = askInt("Select: ", 0, 2);
            switch (ch) {
                case 1 -> printLogs(LogDao.getAllLogs());
                case 2 -> printLogs(LogDao.getLogsByUserId(askId("Enter User ID: ")));
                default -> {
                    return;
                }
            }
        }
    }

    /** Aligned table. A long description continues on the next line(s) so nothing is cut off. */
    private static void printLogs(List<ActivityLog> logs) {
        if (logs.isEmpty()) {
            System.out.println("   No activity recorded yet.");
            return;
        }
        String divider = "-".repeat(98);
        System.out.println();
        System.out.println(String.format(LOG_ROW, "ID", "User ID", "Action Type", "Description", "Timestamp"));
        System.out.println(divider);
        for (ActivityLog l : logs) {
            String text = l.description();
            if (l.orderId() != null && !text.contains("#" + l.orderId())) text += " [Order #" + l.orderId() + "]";
            List<String> lines = wrap(text, DESC_WIDTH);
            for (int i = 0; i < lines.size(); i++) {
                if (i == 0) {
                    System.out.println(String.format(LOG_ROW, l.logId(), l.userId(), l.actionType(),
                            lines.get(i), l.timestamp()));
                } else {
                    System.out.println(String.format(LOG_ROW, "", "", "", lines.get(i), ""));
                }
            }
        }
        System.out.println(divider);
        System.out.println("Records shown: " + logs.size() + " (most recent first)");
    }

    /** Splits text into lines of at most "width" characters, breaking at spaces when possible. */
    private static List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            while (word.length() > width) { // a single very long word is cut
                if (line.length() > 0) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                lines.add(word.substring(0, width));
                word = word.substring(width);
            }
            if (line.length() > 0 && line.length() + 1 + word.length() > width) {
                lines.add(line.toString());
                line.setLength(0);
            }
            if (line.length() > 0) line.append(' ');
            line.append(word);
        }
        if (line.length() > 0 || lines.isEmpty()) lines.add(line.toString());
        return lines;
    }

    private static void exportReport() throws SQLException, IOException {
        StatusReport r = OrderDao.report();
        printReport(r);
        System.out.println("\nExport as: [1] PDF  [2] CSV  [3] Excel (XLSX)  [0] Back");
        int ch = askInt("Select: ", 0, 3);
        if (ch == 0) return;
        Path file = switch (ch) {
            case 1 -> ReportExporter.exportPdf(r);
            case 2 -> ReportExporter.exportCsv(r);
            default -> ReportExporter.exportXlsx(r);
        };
        System.out.println("-> Report saved: " + file.toAbsolutePath());
    }
}
