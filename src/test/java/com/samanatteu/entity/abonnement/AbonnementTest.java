package com.samanatteu.entity.abonnement;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.samanatteu.enums.abonnement.StatutAbonnement;

// Tests de la règle d'expiration d'un abonnement.
//
// L'expiration n'est PAS un statut stocké : elle se calcule sur la date de fin.
// C'est ce qui garantit qu'aucun job en retard ne puisse laisser un abonnement
// expiré "ACTIF" (et donc actif) pendant des heures.
class AbonnementTest {

    private Abonnement abonnement(StatutAbonnement statut, LocalDate dateFin) {
        Abonnement abonnement = new Abonnement();
        abonnement.setStatut(statut);
        abonnement.setDateDebut(dateFin.minusDays(30));
        abonnement.setDateFin(dateFin);
        return abonnement;
    }

    // Règle : ACTIF et période non dépassée → actif.
    @Test
    void estActif_statutActifEtPeriodeValide_estVrai() {
        Abonnement abonnement = abonnement(StatutAbonnement.ACTIF, LocalDate.now().plusDays(1));

        assertTrue(abonnement.estActif());
    }

    // Règle : le DERNIER JOUR couvert est encore actif (dateFin = le dernier
    // jour, pas le jour d'après).
    @Test
    void estActif_leDernierJourCouvert_estEncoreActif() {
        Abonnement abonnement = abonnement(StatutAbonnement.ACTIF, LocalDate.now());

        assertTrue(abonnement.estActif());
    }

    // Règle : la période dépassée → plus actif, même si le statut dit ACTIF.
    @Test
    void estActif_periodeDepassee_estFaux() {
        Abonnement abonnement = abonnement(StatutAbonnement.ACTIF, LocalDate.now().minusDays(1));

        assertFalse(abonnement.estActif());
    }

    // Règle : une décision de la plateforme (suspendu, résilié) l'emporte
    // sur la date, même si la période est valide.
    @Test
    void estActif_statutSuspenduOuResilie_estFaux() {
        assertFalse(abonnement(StatutAbonnement.SUSPENDU, LocalDate.now().plusDays(10)).estActif());
        assertFalse(abonnement(StatutAbonnement.RESILIE, LocalDate.now().plusDays(10)).estActif());
    }
}
