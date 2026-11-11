package com.maisonambre.customer;

import java.time.Instant;

/**
 * The API talks about "customers": a customer is a prospect, and is a client once converted.
 * status is PROSPECT or CLIENT; convertedAt is null for prospects.
 */
public record CustomerResponse(
        Long id,
        String email,
        String status,
        Long firstSeenBoutiqueId,
        Instant createdAt,
        Instant convertedAt) {
}
