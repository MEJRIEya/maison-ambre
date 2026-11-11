package com.maisonambre.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A fragrance in the shared catalogue. Price and stock depend on the boutique, see {@link Inventory}. */
@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    /** Fragrance family: floral, woody or oriental. */
    @Column(nullable = false)
    private String family;

    @Column(name = "size_ml", nullable = false)
    private int sizeMl;

    protected Product() {
        // for JPA
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getFamily() {
        return family;
    }

    public int getSizeMl() {
        return sizeMl;
    }
}
