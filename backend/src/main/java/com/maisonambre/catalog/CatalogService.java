package com.maisonambre.catalog;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maisonambre.boutique.Boutique;
import com.maisonambre.boutique.BoutiqueRepository;
import com.maisonambre.common.ApiException;

@Service
@Transactional(readOnly = true)
public class CatalogService {

    private final BoutiqueRepository boutiqueRepository;
    private final InventoryRepository inventoryRepository;

    public CatalogService(BoutiqueRepository boutiqueRepository, InventoryRepository inventoryRepository) {
        this.boutiqueRepository = boutiqueRepository;
        this.inventoryRepository = inventoryRepository;
    }

    public List<ProductResponse> listProducts(Long boutiqueId) {
        Boutique boutique = findBoutique(boutiqueId);
        return inventoryRepository.findCatalogue(boutiqueId).stream()
                .map(inventory -> ProductResponse.from(inventory, boutique.getCurrency()))
                .toList();
    }

    public ProductResponse getProduct(Long boutiqueId, Long productId) {
        Boutique boutique = findBoutique(boutiqueId);
        Inventory inventory = inventoryRepository.findInBoutique(boutiqueId, productId)
                .orElseThrow(() -> ApiException.notFound("PRODUCT_NOT_FOUND",
                        "Product " + productId + " is not sold in " + boutique.getName()));
        return ProductResponse.from(inventory, boutique.getCurrency());
    }

    private Boutique findBoutique(Long boutiqueId) {
        return boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> ApiException.notFound("BOUTIQUE_NOT_FOUND", "Boutique " + boutiqueId + " not found"));
    }
}
