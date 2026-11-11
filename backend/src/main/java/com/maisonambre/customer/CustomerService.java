package com.maisonambre.customer;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maisonambre.boutique.Boutique;
import com.maisonambre.boutique.BoutiqueRepository;
import com.maisonambre.common.ApiException;

@Service
public class CustomerService {

    /** Result of identifying a visitor: the customer, and whether we have just created them. */
    public record IdentifyResult(CustomerResponse customer, boolean created) {
    }

    private final ProspectRepository prospectRepository;
    private final ClientRepository clientRepository;
    private final BoutiqueRepository boutiqueRepository;

    public CustomerService(ProspectRepository prospectRepository,
                           ClientRepository clientRepository,
                           BoutiqueRepository boutiqueRepository) {
        this.prospectRepository = prospectRepository;
        this.clientRepository = clientRepository;
        this.boutiqueRepository = boutiqueRepository;
    }

    /** Returns the existing prospect or client for this email, or creates a new prospect. */
    @Transactional
    public IdentifyResult identify(IdentifyRequest request) {
        String email = normalise(request.email());

        Optional<Prospect> existing = prospectRepository.findByEmail(email);
        if (existing.isPresent()) {
            return new IdentifyResult(toResponse(existing.get()), false);
        }

        Boutique boutique = boutiqueRepository.findById(request.boutiqueId())
                .orElseThrow(() -> ApiException.unprocessable("UNKNOWN_BOUTIQUE",
                        "Boutique " + request.boutiqueId() + " does not exist"));
        Prospect prospect = prospectRepository.save(new Prospect(email, boutique, Instant.now()));
        return new IdentifyResult(toResponse(prospect), true);
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(Long customerId) {
        Prospect prospect = prospectRepository.findById(customerId)
                .orElseThrow(() -> ApiException.notFound("CUSTOMER_NOT_FOUND", "Customer " + customerId + " not found"));
        return toResponse(prospect);
    }

    private CustomerResponse toResponse(Prospect prospect) {
        Optional<Client> client = clientRepository.findById(prospect.getId());
        return new CustomerResponse(
                prospect.getId(),
                prospect.getEmail(),
                client.isPresent() ? "CLIENT" : "PROSPECT",
                prospect.getFirstSeenBoutique().getId(),
                prospect.getCreatedAt(),
                client.map(Client::getConvertedAt).orElse(null));
    }

    /** "Sara@Example.com " and "sara@example.com" are the same person. */
    private static String normalise(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
