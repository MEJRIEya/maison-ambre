package com.maisonambre.insights;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Manager view of one boutique. No access control: authentication is out of scope for this exercise. */
@RestController
public class InsightsController {

    private final InsightsService insightsService;

    public InsightsController(InsightsService insightsService) {
        this.insightsService = insightsService;
    }

    @GetMapping("/api/boutiques/{boutiqueId}/insights")
    public InsightsResponse get(@PathVariable Long boutiqueId) {
        return insightsService.forBoutique(boutiqueId);
    }
}
