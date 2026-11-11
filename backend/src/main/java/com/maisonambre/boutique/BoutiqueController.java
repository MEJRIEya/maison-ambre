package com.maisonambre.boutique;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/boutiques")
public class BoutiqueController {

    private final BoutiqueRepository boutiqueRepository;

    public BoutiqueController(BoutiqueRepository boutiqueRepository) {
        this.boutiqueRepository = boutiqueRepository;
    }

    @GetMapping
    public List<BoutiqueResponse> list() {
        return boutiqueRepository.findAllByOrderByIdAsc().stream()
                .map(BoutiqueResponse::from)
                .toList();
    }
}
