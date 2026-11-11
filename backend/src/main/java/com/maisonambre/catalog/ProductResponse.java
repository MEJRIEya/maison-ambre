package com.maisonambre.catalog;

import com.maisonambre.common.Money;

/** A product as a customer sees it in one boutique. */
public record ProductResponse(
        Long id,
        String sku,
        String name,
        String description,
        String family,
        int sizeMl,
        Money price,
        int stock,
        boolean available) {

    public static ProductResponse from(Inventory inventory, String currency) {
        Product product = inventory.getProduct();
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getFamily(),
                product.getSizeMl(),
                new Money(inventory.getPrice(), currency),
                inventory.getStock(),
                inventory.getStock() > 0);
    }
}
