package com.maisonambre.common;

import java.math.BigDecimal;

/**
 * An amount always travels with its currency, so a price in AED can never be
 * mistaken for a price in EUR. BigDecimal keeps amounts exact (no floating point).
 */
public record Money(BigDecimal amount, String currency) {
}
