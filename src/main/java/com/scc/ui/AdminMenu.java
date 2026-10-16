package com.scc.ui;

import com.scc.dao.LogDao;
import com.scc.dao.OrderDao;
import com.scc.model.ActivityLog;
import com.scc.model.StatusReport;
import com.scc.model.User;
import com.scc.util.ReportExporter;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

import static com.scc.util.Console.*;

/** Screen for the ADMIN role: accounts, all orders, activity log, status report + export. */
public class AdminMenu {

    public static void show(User user) {
        while (true) {
            System.out.println("\n--- ADMIN MENU ---");
            System.out.println("[1] Manage accounts");
            System.out.println("[2] View all laundry orders");
            System.out.println("[3] Search orders");
            System.out.println("[4] View Laundry Activity Log");
            System.out.println("[5] View Laundry Order Status Report");
            System.out.println("[6] Export Status Report (PDF / CSV / Excel)");
            System.out.println("[0] Logout");
            int ch = askInt("Select: ", 0, 6);
            try {
                switch (ch) {
                    case 1 -> AccountMenu.show(user);
                    case 2 -> printOrders(OrderDao.findAll());
                    case 3 -> OrderSearch.show();
                    case 4 -> viewActivityLog();
                    case 5 -> printReport(OrderDao.report());
                    case 6 -> exportReport();
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

    private static void viewActivityLog() throws SQLException {
        List<ActivityLog> logs = LogDao.findAll();
        if (logs.isEmpty()) {
            System.out.println("   No activity recorded yet.");
            return;
        }
        System.out.printf("%-5s %-14s %-8s %-52s %-19s%n", "Log", "User", "Order", "Action", "Date & Time");
        System.out.println("-".repeat(102));
        for (ActivityLog l : logs) {
            System.out.printf("%-5d %-14s %-8s %-52s %-19s%n", l.logId(), l.username(),
                    l.orderId() == null ? "-" : "#" + l.orderId(), l.action(), l.logDatetime());
        }
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
