package com.maisonambre.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.maisonambre.boutique.Boutique;
import com.maisonambre.catalog.Product;
import com.maisonambre.common.ApiException;
import com.maisonambre.customer.Prospect;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/** An order in one boutique, in that boutique's currency. Named CustomerOrder because ORDER is a SQL keyword. */
@Entity
@Table(name = "orders")
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "boutique_id")
    private Boutique boutique;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prospect_id")
    private Prospect prospect;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private BigDecimal total;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<OrderLine> lines = new ArrayList<>();

    protected CustomerOrder() {
        // for JPA
    }

    /** A new order starts PENDING, in the currency of its boutique. */
    public CustomerOrder(Boutique boutique, Prospect prospect, Instant createdAt) {
        this.boutique = boutique;
        this.prospect = prospect;
        this.currency = boutique.getCurrency();
        this.status = OrderStatus.PENDING;
        this.total = BigDecimal.ZERO;
        this.createdAt = createdAt;
    }

    /** unitPrice comes from the boutique's inventory, never from the request. */
    public void addLine(Product product, int quantity, BigDecimal unitPrice) {
        OrderLine line = new OrderLine(this, product, quantity, unitPrice);
        lines.add(line);
        total = total.add(line.getLineTotal());
    }

    /** Only called by OrderService.complete(), which also converts the prospect. */
    void markCompleted(Instant at) {
        requirePending("complete");
        status = OrderStatus.COMPLETED;
        completedAt = at;
    }

    void markCancelled(Instant at) {
        requirePending("cancel");
        status = OrderStatus.CANCELLED;
        cancelledAt = at;
    }

    private void requirePending(String action) {
        if (status != OrderStatus.PENDING) {
            throw ApiException.conflict("ORDER_NOT_PENDING",
                    "Cannot " + action + " order " + id + " because it is " + status);
        }
    }

    public Long getId() {
        return id;
    }

    public Boutique getBoutique() {
        return boutique;
    }

    public Prospect getProspect() {
        return prospect;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public List<OrderLine> getLines() {
        return lines;
    }
}
