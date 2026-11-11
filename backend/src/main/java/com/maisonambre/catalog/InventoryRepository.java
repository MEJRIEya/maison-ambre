package com.maisonambre.catalog;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    @Query("""
            SELECT i FROM Inventory i JOIN FETCH i.product p
            WHERE i.boutique.id = :boutiqueId
            ORDER BY p.name
            """)
    List<Inventory> findCatalogue(@Param("boutiqueId") Long boutiqueId);

    @Query("""
            SELECT i FROM Inventory i JOIN FETCH i.product
            WHERE i.boutique.id = :boutiqueId AND i.product.id = :productId
            """)
    Optional<Inventory> findInBoutique(@Param("boutiqueId") Long boutiqueId, @Param("productId") Long productId);

    /**
     * Takes stock only if there is enough of it, in one SQL statement.
     *
     * Why this is safe with two orders at the same time: PostgreSQL locks the row during the UPDATE.
     * The second order waits for the first one to commit, then checks "stock >= quantity" again
     * against the new value. So the stock can never go below zero, and the losing order sees 0 rows updated.
     *
     * @return 1 if the stock was taken, 0 if there was not enough stock
     */
    @Modifying
    @Query(value = """
            UPDATE inventory SET stock = stock - :quantity
            WHERE id = :inventoryId AND stock >= :quantity
            """, nativeQuery = true)
    int decrementStock(@Param("inventoryId") Long inventoryId, @Param("quantity") int quantity);

    /** Puts stock back, used when a pending order is cancelled. */
    @Modifying
    @Query(value = """
            UPDATE inventory SET stock = stock + :quantity
            WHERE boutique_id = :boutiqueId AND product_id = :productId
            """, nativeQuery = true)
    int incrementStock(@Param("boutiqueId") Long boutiqueId,
                       @Param("productId") Long productId,
                       @Param("quantity") int quantity);
}
