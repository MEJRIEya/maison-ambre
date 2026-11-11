package com.maisonambre.insights;

import java.math.BigDecimal;
import java.util.List;

import com.maisonambre.common.Money;

/**
 * Key figures for one boutique. Sales figures only count COMPLETED orders.
 * Prospects and clients are counted by the boutique where the person was first seen,
 * so clients are a subset of prospects and conversionRate stays between 0 and 1.
 */
public record InsightsResponse(
        Long boutiqueId,
        String boutiqueName,
        Money salesRevenue,
        long salesCount,
        long unitsSold,
        List<TopProduct> topProducts,
        int lowStockThreshold,
        List<LowStockProduct> lowStock,
        long prospects,
        long clients,
        BigDecimal conversionRate) {

    public record TopProduct(Long productId, String name, long unitsSold, Money revenue) {
    }

    public record LowStockProduct(Long productId, String name, int stock) {
    }
}
