package com.samanatteu.dto.dashboard;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Une ligne « Cotisations en retard » du tableau de bord gestionnaire : qui
// relancer, pour quelle tontine et quel cycle, et combien il reste à payer.
@Getter
@Setter
@NoArgsConstructor
public class CotisationRetardDTO {
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
