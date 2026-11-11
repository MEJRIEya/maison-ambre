package com.maisonambre.order;

import java.time.Instant;
import java.util.List;

import com.maisonambre.common.Money;

public record OrderResponse(
        Long id,
        Long boutiqueId,
        Long customerId,
        OrderStatus status,
        Money total,
        List<LineResponse> lines,
        Instant createdAt,
        Instant completedAt,
        Instant cancelledAt) {

    public record LineResponse(Long productId, String productName, int quantity, Money unitPrice, Money lineTotal) {
    }

    public static OrderResponse from(CustomerOrder order) {
        String currency = order.getCurrency();
        List<LineResponse> lines = order.getLines().stream()
                .map(line -> new LineResponse(
                        line.getProduct().getId(),
                        line.getProduct().getName(),
                        line.getQuantity(),
                        new Money(line.getUnitPrice(), currency),
                        new Money(line.getLineTotal(), currency)))
                .toList();
        return new OrderResponse(
                order.getId(),
                order.getBoutique().getId(),
                order.getProspect().getId(),
                order.getStatus(),
                new Money(order.getTotal(), currency),
                lines,
                order.getCreatedAt(),
                order.getCompletedAt(),
                order.getCancelledAt());
    }
}
