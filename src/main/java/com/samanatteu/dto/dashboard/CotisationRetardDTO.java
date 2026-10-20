package com.samanatteu.dto.dashboard;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

// Une ligne « Cotisations en retard » du tableau de bord gestionnaire : qui
// relancer, pour quelle tontine et quel cycle, et combien il reste à payer.
@Getter
@Setter
public class CotisationRetardDTO {
    // Constructeur sans argument : Jackson en a besoin pour deserialiser
    // le JSON en DTO. Ecrit a la main, pas genere par Lombok : sur ce JDK,
    // @NoArgsConstructor produit parfois un constructeur VIDE (sans appel a
    // super()), et la JVM refuse alors de charger la classe.
    public CotisationRetardDTO() {
        super();
    }

    // Pour que le front ouvre directement le paiement de cette cotisation.
    private Long cotisationId;

    private String tontineNom;

    private Integer numeroCycle;

    // Prénom et nom réunis : le front n'a qu'à l'afficher.
    private String membreNom;

    private String membreTelephone;

    // Reste de la part + reste de la caisse de prêts (même calcul que
    // CotisationService au moment d'un paiement).
    private BigDecimal resteDu;
}
