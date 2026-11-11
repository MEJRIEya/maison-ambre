package com.maisonambre.customer;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The fact that a prospect has completed at least one order.
 * The id is the prospect's id, so a prospect can only be converted once.
 * Rows are only created by {@link ClientRepository#convertToClient}.
 */
@Entity
@Table(name = "client")
public class Client {

    @Id
    @Column(name = "prospect_id")
    private Long prospectId;

    @Column(name = "converted_at", nullable = false)
    private Instant convertedAt;

    protected Client() {
        // for JPA
    }

    public Long getProspectId() {
        return prospectId;
    }

    public Instant getConvertedAt() {
        return convertedAt;
    }
}
