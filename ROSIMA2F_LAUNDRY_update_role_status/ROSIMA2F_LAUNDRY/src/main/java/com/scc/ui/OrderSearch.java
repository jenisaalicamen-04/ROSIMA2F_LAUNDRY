package com.scc.ui;

import com.scc.dao.OrderDao;
import com.scc.model.LaundryOrder;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static com.scc.util.Console.*;
import static com.scc.util.Constants.STATUSES;

/** Search screen shared by Staff and Admin: by order ID, customer name or status. */
public class OrderSearch {

    public static void show() throws SQLException {
        System.out.println("Search by: [1] Order ID  [2] Customer name  [3] Status  [0] Back");
        int ch = askInt("Select: ", 0, 3);
        switch (ch) {
            case 1 -> {
                LaundryOrder o = OrderDao.findById(askId("Order ID: "));
                printOrders(o == null ? new ArrayList<LaundryOrder>() : List.of(o));
            }
            case 2 -> printOrders(OrderDao.findByCustomerName(askRequired("Customer name: ")));
            case 3 -> {
                System.out.println("[1] Pending  [2] Processing  [3] Ready  [4] Completed  [5] Cancelled");
                printOrders(OrderDao.findByStatus(STATUSES[askInt("Status: ", 1, 5) - 1]));
            }
            default -> {
                // back
            }
        }
    }
}
