package com.maisonambre.order;

/**
 * PENDING -> COMPLETED (a sale) or PENDING -> CANCELLED.
 * Only COMPLETED orders count in the insights.
 */
public enum OrderStatus {
    PENDING,
    COMPLETED,
    CANCELLED
}
