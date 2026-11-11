package com.maisonambre.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** A visitor gives their email in a boutique. boutiqueId is recorded as "first seen" for new prospects. */
public record IdentifyRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotNull Long boutiqueId) {
}
