package com.scc.ui;

import com.scc.dao.CustomerDao;
import com.scc.dao.LogDao;
import com.scc.dao.OrderDao;
import com.scc.dao.UserDao;
import com.scc.model.Customer;
import com.scc.model.LaundryOrder;
import com.scc.model.User;

import java.sql.SQLException;
import java.util.List;

import static com.scc.util.Console.*;
import static com.scc.util.Constants.*;

/** Screen for the CUSTOMER role: create / view / cancel orders and update own account. */
public class CustomerMenu {

    public static void show(User user) {
        while (true) {
            System.out.println("\n--- CUSTOMER MENU ---");
            System.out.println("[1] Create laundry order");
            System.out.println("[2] View my current orders (status)");
            System.out.println("[3] View my order history");
            System.out.println("[4] Cancel a pending order");
            System.out.println("[5] Update my account information");
            System.out.println("[0] Logout");
            int ch = askInt("Select: ", 0, 5);
            try {
                switch (ch) {
                    case 1 -> customerCreateOrder(user);
                    case 2 -> printOrders(OrderDao.findByCustomer(user.customerId()).stream()
                            .filter(o -> !COMPLETED.equals(o.status()) && !CANCELLED.equals(o.status())).toList());
                    case 3 -> printOrders(OrderDao.findByCustomer(user.customerId()).stream()
                            .filter(o -> COMPLETED.equals(o.status()) || CANCELLED.equals(o.status())).toList());
                    case 4 -> customerCancelOrder(user);
                    case 5 -> customerUpdateAccount(user);
                    default -> {
                        return;
                    }
                }
            } catch (SQLException e) {
                dbError(e);
            }
        }
    }

    private static void customerCreateOrder(User user) throws SQLException {
        String service = chooseService(false);
        int orderId = OrderDao.create(user.customerId(), service);
        LogDao.logAction(user.userId(), orderId, "ORDER_CREATED", "Submitted laundry order #" + orderId
                + " by Customer " + user.username() + " - Service: " + service);
        System.out.println("-> Order #" + orderId + " created. Status: " + PENDING);
    }

    private static void customerCancelOrder(User user) throws SQLException {
        List<LaundryOrder> pending = OrderDao.findByCustomer(user.customerId()).stream()
                .filter(o -> PENDING.equals(o.status())).toList();
        if (pending.isEmpty()) {
            System.out.println("   You have no Pending orders to cancel.");
            return;
        }
        printOrders(pending);
        int id = askId("Order ID to cancel: ");
        LaundryOrder o = OrderDao.findById(id);
        if (o == null || o.customerId() != user.customerId()) {
            System.out.println("-> Order not found.");
        } else if (!PENDING.equals(o.status())) {
            System.out.println("-> Only Pending orders can be cancelled.");
        } else {
            OrderDao.cancel(id);
            LogDao.logAction(user.userId(), id, "ORDER_CANCELLED", "Order #" + id + " cancelled by Customer " + user.username());
            System.out.println("-> Order #" + id + " cancelled (record kept in your history).");
        }
    }

    private static void customerUpdateAccount(User user) throws SQLException {
        Customer c = CustomerDao.find(user.customerId());
        System.out.println("Leave blank to keep the current value.");
        String contact = ask("Contact number [" + c.contactNumber() + "]: ");
        String address = ask("Address [" + c.address() + "]: ");
        String password = ask("New password (any text, blank = keep): ");
        CustomerDao.update(c.customerId(), c.name(),
                contact.isEmpty() ? c.contactNumber() : contact,
                address.isEmpty() ? c.address() : address);
        if (!password.isEmpty()) UserDao.updatePassword(user.userId(), password);
        LogDao.logAction(user.userId(), "PROFILE_UPDATED", "Customer " + user.username() + " (ID: " + user.userId()
                + ") updated their account information");
        System.out.println("-> Account updated.");
    }
}
