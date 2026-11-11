package com.maisonambre.customer;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClientRepository extends JpaRepository<Client, Long> {

    /**
     * Turns a prospect into a client. Called from a single place: OrderService.complete().
     *
     * ON CONFLICT DO NOTHING makes it safe to call on every completed order:
     * the first completed order creates the row (and sets converted_at), later ones change nothing.
     *
     * @return 1 if the prospect has just become a client, 0 if they already were one
     */
    @Modifying
    @Query(value = """
            INSERT INTO client (prospect_id, converted_at) VALUES (:prospectId, :convertedAt)
            ON CONFLICT (prospect_id) DO NOTHING
            """, nativeQuery = true)
    int convertToClient(@Param("prospectId") Long prospectId, @Param("convertedAt") Instant convertedAt);
}
