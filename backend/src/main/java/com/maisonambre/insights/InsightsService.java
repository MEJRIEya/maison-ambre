package com.maisonambre.insights;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maisonambre.boutique.Boutique;
import com.maisonambre.boutique.BoutiqueRepository;
import com.maisonambre.common.ApiException;
import com.maisonambre.common.Money;

/**
 * Insights are read-only aggregates, so they are plain SQL queries (easier to read and check than JPA here).
 */
@Service
@Transactional(readOnly = true)
public class InsightsService {

    private final JdbcClient jdbc;
    private final BoutiqueRepository boutiqueRepository;
    private final int lowStockThreshold;

    public InsightsService(JdbcClient jdbc,
                           BoutiqueRepository boutiqueRepository,
                           @Value("${app.insights.low-stock-threshold}") int lowStockThreshold) {
        this.jdbc = jdbc;
        this.boutiqueRepository = boutiqueRepository;
        this.lowStockThreshold = lowStockThreshold;
    }

    public InsightsResponse forBoutique(Long boutiqueId) {
        Boutique boutique = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> ApiException.notFound("BOUTIQUE_NOT_FOUND", "Boutique " + boutiqueId + " not found"));
        String currency = boutique.getCurrency();

        // Sales = completed orders only.
        BigDecimal revenue = jdbc.sql("""
                        SELECT COALESCE(SUM(total), 0) FROM orders
                        WHERE boutique_id = :boutiqueId AND status = 'COMPLETED'
                        """)
                .param("boutiqueId", boutiqueId)
                .query(BigDecimal.class).single();

        long salesCount = jdbc.sql("""
                        SELECT COUNT(*) FROM orders
                        WHERE boutique_id = :boutiqueId AND status = 'COMPLETED'
                        """)
                .param("boutiqueId", boutiqueId)
                .query(Long.class).single();

        long unitsSold = jdbc.sql("""
                        SELECT COALESCE(SUM(l.quantity), 0)
                        FROM order_line l JOIN orders o ON o.id = l.order_id
                        WHERE o.boutique_id = :boutiqueId AND o.status = 'COMPLETED'
                        """)
                .param("boutiqueId", boutiqueId)
                .query(Long.class).single();

        // Top 3 by units sold; ties broken by revenue, then by name so the result is stable.
        List<InsightsResponse.TopProduct> topProducts = jdbc.sql("""
                        SELECT p.id, p.name, SUM(l.quantity) AS units, SUM(l.line_total) AS revenue
                        FROM order_line l
                        JOIN orders o ON o.id = l.order_id
                        JOIN product p ON p.id = l.product_id
                        WHERE o.boutique_id = :boutiqueId AND o.status = 'COMPLETED'
                        GROUP BY p.id, p.name
                        ORDER BY units DESC, revenue DESC, p.name
                        LIMIT 3
                        """)
                .param("boutiqueId", boutiqueId)
                .query((rs, rowNum) -> new InsightsResponse.TopProduct(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getLong("units"),
                        new Money(rs.getBigDecimal("revenue"), currency)))
                .list();

        List<InsightsResponse.LowStockProduct> lowStock = jdbc.sql("""
                        SELECT p.id, p.name, i.stock
                        FROM inventory i JOIN product p ON p.id = i.product_id
                        WHERE i.boutique_id = :boutiqueId AND i.stock <= :threshold
                        ORDER BY i.stock, p.name
                        """)
                .param("boutiqueId", boutiqueId)
                .param("threshold", lowStockThreshold)
                .query((rs, rowNum) -> new InsightsResponse.LowStockProduct(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getInt("stock")))
                .list();

        // Prospects include people who have since become clients (agreed with Kubee).
        long prospects = jdbc.sql("SELECT COUNT(*) FROM prospect WHERE first_seen_boutique_id = :boutiqueId")
                .param("boutiqueId", boutiqueId)
                .query(Long.class).single();

        long clients = jdbc.sql("""
                        SELECT COUNT(*) FROM client c JOIN prospect p ON p.id = c.prospect_id
                        WHERE p.first_seen_boutique_id = :boutiqueId
                        """)
                .param("boutiqueId", boutiqueId)
                .query(Long.class).single();

        // null when there are no prospects yet: "no data" is not the same as 0%.
        BigDecimal conversionRate = prospects == 0 ? null
                : BigDecimal.valueOf(clients).divide(BigDecimal.valueOf(prospects), 4, RoundingMode.HALF_UP);

        return new InsightsResponse(
                boutique.getId(),
                boutique.getName(),
                new Money(revenue, currency),
                salesCount,
                unitsSold,
                topProducts,
                lowStockThreshold,
                lowStock,
                prospects,
                clients,
                conversionRate);
    }
}
