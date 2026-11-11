package com.maisonambre.customer;

import java.time.Instant;

import com.maisonambre.boutique.Boutique;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Anyone who has given their email. A prospect who buys keeps this row and gains a {@link Client} row,
 * so "number of prospects" naturally includes clients.
 */
@Entity
@Table(name = "prospect")
public class Prospect {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "first_seen_boutique_id")
    private Boutique firstSeenBoutique;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Prospect() {
        // for JPA
    }

    public Prospect(String email, Boutique firstSeenBoutique, Instant createdAt) {
        this.email = email;
        this.firstSeenBoutique = firstSeenBoutique;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public Boutique getFirstSeenBoutique() {
        return firstSeenBoutique;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
