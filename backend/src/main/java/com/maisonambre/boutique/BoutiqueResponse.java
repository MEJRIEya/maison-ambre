package com.maisonambre.boutique;

public record BoutiqueResponse(Long id, String code, String name, String city, String currency) {

    public static BoutiqueResponse from(Boutique boutique) {
        return new BoutiqueResponse(boutique.getId(), boutique.getCode(), boutique.getName(),
                boutique.getCity(), boutique.getCurrency());
    }
}
