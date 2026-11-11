package com.maisonambre.boutique;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BoutiqueRepository extends JpaRepository<Boutique, Long> {

    List<Boutique> findAllByOrderByIdAsc();
}
