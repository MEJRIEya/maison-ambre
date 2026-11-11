package com.maisonambre.catalog;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Products are always read in the context of a boutique, because price and stock are local. */
@RestController
@RequestMapping("/api/boutiques/{boutiqueId}/products")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public List<ProductResponse> list(@PathVariable Long boutiqueId) {
        return catalogService.listProducts(boutiqueId);
    }

    @GetMapping("/{productId}")
    public ProductResponse get(@PathVariable Long boutiqueId, @PathVariable Long productId) {
        return catalogService.getProduct(boutiqueId, productId);
    }
}
