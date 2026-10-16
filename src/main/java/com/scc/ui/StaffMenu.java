package com.scc.ui;

import com.scc.dao.LogDao;
import com.scc.dao.OrderDao;
import com.scc.model.LaundryOrder;
import com.scc.model.User;

import java.sql.SQLException;
import java.util.List;

import static com.scc.util.Console.*;
import static com.scc.util.Constants.*;

/** Screen for the LAUNDRY STAFF role: view / search orders, update info and status. */
public class StaffMenu {

    public static void show(User user) {
        while (true) {
            System.out.println("\n--- LAUNDRY STAFF MENU ---");
            System.out.println("[1] View all laundry orders");
            System.out.println("[2] Search orders (ID / customer name / status)");
            System.out.println("[3] Update order information (service type, total amount)");
            System.out.println("[4] Update order status (Pending / Processing / Ready)");
            System.out.println("[5] Mark order as Completed");
            System.out.println("[0] Logout");
            int ch = askInt("Select: ", 0, 5);
            try {
                switch (ch) {
                    case 1 -> printOrders(OrderDao.findAll());
                    case 2 -> OrderSearch.show();
                    case 3 -> staffUpdateInfo(user);
                    case 4 -> staffUpdateStatus(user);
                    case 5 -> staffMarkCompleted(user);
                    default -> {
                        return;
                    }
                }
            } catch (SQLException e) {
                dbError(e);
            }
        }
    }

    /** Returns the order only if it exists and is still open (not Completed / Cancelled). */
    private static LaundryOrder pickOpenOrder() throws SQLException {
        int id = askId("Order ID: ");
        LaundryOrder o = OrderDao.findById(id);
        if (o == null) {
            System.out.println("-> Order not found.");
            return null;
        }
        if (COMPLETED.equals(o.status()) || CANCELLED.equals(o.status())) {
            System.out.println("-> Order #" + id + " is " + o.status() + " and can no longer be changed.");
            return null;
        }
        return o;
    }

    private static void staffUpdateInfo(User user) throws SQLException {
        LaundryOrder o = pickOpenOrder();
        if (o == null) return;
        printOrders(List.of(o));
        String service = chooseService(true);
        Double amount = askOptionalAmount("New total amount [" + String.format("%.2f", o.totalAmount()) + "] (blank = keep): ");
        OrderDao.updateInfo(o.orderId(), service == null ? o.serviceType() : service,
                amount == null ? o.totalAmount() : amount, user.userId());
        LogDao.log(user.userId(), o.orderId(), "Order updated");
        System.out.println("-> Order #" + o.orderId() + " updated.");
    }

    private static void staffUpdateStatus(User user) throws SQLException {
        LaundryOrder o = pickOpenOrder();
        if (o == null) return;
        System.out.println("Current status: " + o.status());
        System.out.println("[1] Pending  [2] Processing  [3] Ready  [0] Back");
        int ch = askInt("New status: ", 0, 3);
        if (ch == 0) return;
        String newStatus = STATUSES[ch - 1];
        if (newStatus.equals(o.status())) {
            System.out.println("-> Order is already " + newStatus + ".");
            return;
        }
        OrderDao.updateStatus(o.orderId(), newStatus, user.userId());
        LogDao.log(user.userId(), o.orderId(), "Order status changed: " + o.status() + " -> " + newStatus);
        System.out.println("-> Order #" + o.orderId() + " is now " + newStatus + ".");
    }

    private static void staffMarkCompleted(User user) throws SQLException {
        LaundryOrder o = pickOpenOrder();
        if (o == null) return;
        if (!READY.equals(o.status())) {
            System.out.println("-> Only Ready orders can be marked Completed (current: " + o.status() + ").");
            return;
        }
        OrderDao.updateStatus(o.orderId(), COMPLETED, user.userId());
        LogDao.log(user.userId(), o.orderId(), "Order completed (picked up)");
        System.out.println("-> Order #" + o.orderId() + " marked Completed.");
    }
}
