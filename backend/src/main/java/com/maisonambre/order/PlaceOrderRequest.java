package com.maisonambre.order;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Checkout request. Each line repeats the boutique it was added in, so the API can reject
 * a cart that mixes boutiques instead of silently trusting the frontend.
 * There is no price in the request: prices always come from the boutique's inventory.
 */
public record PlaceOrderRequest(
        @NotNull Long boutiqueId,
        @NotNull Long customerId,
        @NotEmpty @Size(max = 50) List<@Valid @NotNull Line> lines) {

    public record Line(
            @NotNull Long boutiqueId,
            @NotNull Long productId,
            @NotNull @Min(1) @Max(20) Integer quantity) {
    }
}
