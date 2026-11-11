package com.maisonambre.customer;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProspectRepository extends JpaRepository<Prospect, Long> {

    Optional<Prospect> findByEmail(String email);
}
