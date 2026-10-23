package com.scc.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Roles, order statuses, service types and date helpers shared by the whole program. */
public class Constants {

    public static final String ROLE_CUSTOMER = "Customer";
    public static final String ROLE_STAFF = "Laundry Staff";
    public static final String ROLE_ADMIN = "Admin";

    public static final String PENDING = "Pending";
    public static final String PROCESSING = "Processing";
    public static final String READY = "Ready";
    public static final String COMPLETED = "Completed";
    public static final String CANCELLED = "Cancelled";
    public static final String[] STATUSES = {PENDING, PROCESSING, READY, COMPLETED, CANCELLED};

    public static final String[] SERVICES = {
            "Wash & Fold", "Wash, Dry & Iron", "Dry Cleaning", "Ironing Only", "Bedding / Comforter"
    };

    public static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private Constants() {
    }

    public static String now() {
        return LocalDateTime.now().format(TS);
    }
}
