package com.samanatteu.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.samanatteu.enums.cotisation.StatutCotisation;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Une ligne « À payer » du tableau de bord MEMBRE : une de ses cotisations
// pas encore soldée. Ni nom ni téléphone : ce sont les siennes.
@Getter
@Setter
@NoArgsConstructor
public class CotisationAPayerDTO {

    private Long cotisationId;

    private String tontineNom;

    // Distingue deux cotisations d'une même tontine (cycle 1 en retard,
    // cycle 2 en cours).
    private Integer numeroCycle;

    // Reste de la part + reste de la caisse de prêts : ce que le paiement
    // acceptera au maximum.
    private BigDecimal resteDu;

    // dateFinPrevue du cycle : après, la cotisation passe EN_RETARD.
    private LocalDate dateLimite;

    // EN_ATTENTE, PARTIEL ou EN_RETARD (jamais COMPLET : rien à payer).
    private StatutCotisation statut;
}
