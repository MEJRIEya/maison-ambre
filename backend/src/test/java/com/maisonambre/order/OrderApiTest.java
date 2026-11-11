package com.maisonambre.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

/**
 * Tests the order endpoint over real HTTP against PostgreSQL.
 * Each test starts from empty tables and creates its own boutiques, product and stock.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@SuppressWarnings({"rawtypes", "unchecked"})
class OrderApiTest {

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private JdbcClient jdbc;

    private long parisId;
    private long dubaiId;
    private long productId;

    @BeforeEach
    void resetDatabase() {
        jdbc.sql("TRUNCATE order_line, orders, client, prospect, inventory, product, boutique RESTART IDENTITY CASCADE")
                .update();
        parisId = insertBoutique("PAR", "Paris", "EUR");
        dubaiId = insertBoutique("DXB", "Dubai", "AED");
        productId = jdbc.sql("""
                        INSERT INTO product (sku, name, description, family, size_ml)
                        VALUES ('TEST-1', 'Test Rose', 'A product for tests', 'floral', 50) RETURNING id
                        """)
                .query(Long.class).single();
    }

    @Test
    void completedOrderConvertsProspectIntoClient() {
        setStock(parisId, "120.00", 10);
        long customerId = identify("new.visitor@example.com", parisId);
        assertThat(customer(customerId).get("status")).isEqualTo("PROSPECT");

        ResponseEntity<Map> response = placeOrder(customerId, parisId, line(parisId, 2));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().get("status")).isEqualTo("COMPLETED");
        Map customer = customer(customerId);
        assertThat(customer.get("status")).isEqualTo("CLIENT");
        assertThat(customer.get("convertedAt")).isNotNull();
        assertThat(stock(parisId)).isEqualTo(8);
    }

    @Test
    void concurrentOrdersNeverSellMoreThanTheStock() throws Exception {
        setStock(parisId, "120.00", 5);
        int buyers = 10;
        List<Long> customerIds = new ArrayList<>();
        for (int i = 0; i < buyers; i++) {
            customerIds.add(identify("buyer" + i + "@example.com", parisId));
        }

        // 10 buyers want 1 bottle each, there are 5 bottles, and they all click "buy" at the same moment.
        ExecutorService pool = Executors.newFixedThreadPool(buyers);
        CountDownLatch startSignal = new CountDownLatch(1);
        List<Future<HttpStatusCode>> results = new ArrayList<>();
        for (Long customerId : customerIds) {
            results.add(pool.submit(() -> {
                startSignal.await();
                return placeOrder(customerId, parisId, line(parisId, 1)).getStatusCode();
            }));
        }
        startSignal.countDown();

        List<Integer> statuses = new ArrayList<>();
        for (Future<HttpStatusCode> result : results) {
            statuses.add(result.get(30, TimeUnit.SECONDS).value());
        }
        pool.shutdown();

        assertThat(statuses).filteredOn(status -> status == 201).hasSize(5);
        assertThat(statuses).filteredOn(status -> status == 409).hasSize(5);
        assertThat(stock(parisId)).isZero();
    }

    @Test
    void cartMixingBoutiquesIsRejected() {
        setStock(parisId, "120.00", 5);
        setStock(dubaiId, "490.00", 5);
        long customerId = identify("mixed.cart@example.com", parisId);

        ResponseEntity<Map> response = placeOrder(customerId, parisId, line(parisId, 1), line(dubaiId, 1));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().get("code")).isEqualTo("MIXED_BOUTIQUE_CART");
        assertThat(stock(parisId)).isEqualTo(5);
        assertThat(stock(dubaiId)).isEqualTo(5);
    }

    // --- helpers ---

    private long insertBoutique(String code, String city, String currency) {
        return jdbc.sql("""
                        INSERT INTO boutique (code, name, city, currency)
                        VALUES (:code, :name, :city, :currency) RETURNING id
                        """)
                .param("code", code)
                .param("name", "Maison Ambre " + city)
                .param("city", city)
                .param("currency", currency)
                .query(Long.class).single();
    }

    private void setStock(long boutiqueId, String price, int stock) {
        jdbc.sql("""
                        INSERT INTO inventory (boutique_id, product_id, price, stock)
                        VALUES (:boutiqueId, :productId, CAST(:price AS NUMERIC), :stock)
                        """)
                .param("boutiqueId", boutiqueId)
                .param("productId", productId)
                .param("price", price)
                .param("stock", stock)
                .update();
    }

    private int stock(long boutiqueId) {
        return jdbc.sql("SELECT stock FROM inventory WHERE boutique_id = :boutiqueId AND product_id = :productId")
                .param("boutiqueId", boutiqueId)
                .param("productId", productId)
                .query(Integer.class).single();
    }

    private long identify(String email, long boutiqueId) {
        Map body = http.postForObject("/api/customers", Map.of("email", email, "boutiqueId", boutiqueId), Map.class);
        return ((Number) body.get("id")).longValue();
    }

    private Map customer(long customerId) {
        return http.getForObject("/api/customers/" + customerId, Map.class);
    }

    private Map<String, Object> line(long boutiqueId, int quantity) {
        return Map.of("boutiqueId", boutiqueId, "productId", productId, "quantity", quantity);
    }

    @SafeVarargs
    private ResponseEntity<Map> placeOrder(long customerId, long boutiqueId, Map<String, Object>... lines) {
        Map<String, Object> request = Map.of(
                "boutiqueId", boutiqueId,
                "customerId", customerId,
                "lines", List.of(lines));
        return http.postForEntity("/api/orders", request, Map.class);
    }
}
