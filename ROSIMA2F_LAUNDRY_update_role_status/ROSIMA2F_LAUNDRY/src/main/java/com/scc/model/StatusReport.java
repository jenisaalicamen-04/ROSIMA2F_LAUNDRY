package com.scc.model;

/** Numbers shown in the Laundry Order Status Report. */
public record StatusReport(int total, int pending, int processing, int ready, int completed, int cancelled) {
}
