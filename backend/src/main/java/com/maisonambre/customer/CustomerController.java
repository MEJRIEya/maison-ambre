package com.maisonambre.customer;

import java.net.URI;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    /**
     * Identify a visitor by email: 201 Created with a new prospect, or 200 OK with the existing prospect or client.
     */
    @PostMapping
    public ResponseEntity<CustomerResponse> identify(@Valid @RequestBody IdentifyRequest request) {
        CustomerService.IdentifyResult result;
        try {
            result = customerService.identify(request);
        } catch (DataIntegrityViolationException e) {
            // The same email was created by another request at the same moment (unique constraint).
            // Retrying in a new transaction finds that prospect.
            result = customerService.identify(request);
        }

        CustomerResponse customer = result.customer();
        if (result.created()) {
            return ResponseEntity.created(URI.create("/api/customers/" + customer.id())).body(customer);
        }
        return ResponseEntity.ok(customer);
    }

    @GetMapping("/{customerId}")
    public CustomerResponse get(@PathVariable Long customerId) {
        return customerService.get(customerId);
    }
}
