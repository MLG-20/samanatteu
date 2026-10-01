package com.samanatteu.dto.pret;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

// DTO d'ENTRÉE pour accorder un prêt : seulement ce que la gestionnaire
// décide. Tontine, gestionnaire, statut, date d'accord et échéancier sont
// fixés par le serveur (on ne croit jamais le JSON pour ça).
@Getter
@Setter
public class DemandePretDTO {

    // Un simple id, pas un objet Utilisateur : le service rechargera le
    // membre en base (même règle que pour les participations).
    @NotNull(message = "Le membre est obligatoire.")
    private Long membreId;

    @NotNull(message = "Le montant est obligatoire.")
    @Positive(message = "Le montant doit être positif.")
    private BigDecimal montant;

    @NotNull(message = "Le nombre d'échéances est obligatoire.")
    @Min(value = 1, message = "Il faut au moins une échéance.")
    private Integer nbEcheances;

    // Date passée permise exprès (pas de @FutureOrPresent) : la gestionnaire
    // doit pouvoir saisir un prêt déjà en cours avant d'utiliser l'appli.
    @NotNull(message = "La date du premier remboursement est obligatoire.")
    private LocalDate dateDebutRemboursement;

    // Deux façons de saisir l'intérêt (au choix de la gestionnaire) : en %
    // ou directement en francs. Les deux sont facultatifs : aucun des deux
    // = prêt sans intérêt.
    @PositiveOrZero(message = "Le taux ne peut pas être négatif.")
    private BigDecimal tauxInteret;

    @PositiveOrZero(message = "L'intérêt ne peut pas être négatif.")
    private BigDecimal montantInteret;

    // Règle entre deux champs (comme LoginDTO) : Bean Validation appelle
    // cette méthode ; false → 400 avec le message. Le nom doit commencer
    // par « is ». Vrai si au moins un des deux est vide : seul le cas
    // « les deux remplis » est refusé.
    @AssertTrue(message = "Donnez l'intérêt en taux OU en montant, pas les deux.")
    public boolean isUneSeuleFormeDInteret() {
        return tauxInteret == null || montantInteret == null;
    }
}
