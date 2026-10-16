package com.scc.util;

import com.scc.dao.UserDao;
import com.scc.model.LaundryOrder;
import com.scc.model.StatusReport;

import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

import static com.scc.util.Constants.SERVICES;
import static com.scc.util.Constants.now;

/** Small helpers for reading input and printing tables in the console. */
public class Console {

    static final Scanner in = new Scanner(System.in);

    public static String ask(String prompt) {
        System.out.print(prompt);
        return in.nextLine().trim();
    }

    public static String askRequired(String prompt) {
        while (true) {
            String s = ask(prompt);
            if (!s.isEmpty()) return s;
            System.out.println("   Input cannot be empty.");
        }
    }

    public static String askPassword(String prompt) {
        while (true) {
            String s = ask(prompt);
            if (s.length() >= 4) return s;
            System.out.println("   Password must be at least 4 characters.");
        }
    }

    public static int askInt(String prompt, int min, int max) {
        while (true) {
            try {
                int v = Integer.parseInt(ask(prompt));
                if (v >= min && v <= max) return v;
            } catch (NumberFormatException ignored) {
                // fall through to the message below
            }
            System.out.println("   Please enter a number from " + min + " to " + max + ".");
        }
    }

    public static int askId(String prompt) {
        return askInt(prompt, 1, Integer.MAX_VALUE);
    }

    /** Blank = keep the old value (returns null). */
    public static Double askOptionalAmount(String prompt) {
        while (true) {
            String s = ask(prompt);
            if (s.isEmpty()) return null;
            try {
                double v = Double.parseDouble(s);
                if (v >= 0) return v;
            } catch (NumberFormatException ignored) {
                // fall through to the message below
            }
            System.out.println("   Enter a valid amount (0 or more), or leave blank.");
        }
    }

    public static String askNewUsername() throws SQLException {
        while (true) {
            String u = askRequired("Username: ");
            if (!UserDao.usernameExists(u)) return u;
            System.out.println("   That username is already taken.");
        }
    }

    public static String chooseService(boolean allowKeep) {
        System.out.println("Service type:");
        for (int i = 0; i < SERVICES.length; i++) System.out.println("  [" + (i + 1) + "] " + SERVICES[i]);
        if (allowKeep) System.out.println("  [0] Keep current");
        int ch = askInt("Select: ", allowKeep ? 0 : 1, SERVICES.length);
        return ch == 0 ? null : SERVICES[ch - 1];
    }

    public static void printOrders(List<LaundryOrder> orders) {
        if (orders.isEmpty()) {
            System.out.println("   No orders found.");
            return;
        }
        System.out.printf("%-6s %-18s %-20s %-19s %-11s %10s%n",
                "ID", "Customer", "Service", "Order Date", "Status", "Amount");
        System.out.println("-".repeat(90));
        for (LaundryOrder o : orders) {
            String name = o.customerName().length() > 18 ? o.customerName().substring(0, 17) + "~" : o.customerName();
            System.out.printf("%-6d %-18s %-20s %-19s %-11s %10.2f%n",
                    o.orderId(), name, o.serviceType(), o.orderDate(), o.status(), o.totalAmount());
        }
    }

    public static void printReport(StatusReport r) {
        System.out.println("\n===== LAUNDRY ORDER STATUS REPORT =====");
        System.out.println("Generated  : " + now());
        System.out.printf("Total orders : %d%n", r.total());
        System.out.printf("Pending      : %d%n", r.pending());
        System.out.printf("Processing   : %d%n", r.processing());
        System.out.printf("Ready        : %d%n", r.ready());
        System.out.printf("Completed    : %d%n", r.completed());
        System.out.printf("Cancelled    : %d%n", r.cancelled());
    }

    public static void dbError(SQLException e) {
        System.out.println("   Database error: " + e.getMessage());
    }
}
