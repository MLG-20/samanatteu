package com.samanatteu.dto.dashboard;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Bloc « Tontines » du tableau de bord admin : combien de tontines par
// statut, sans nom ni montant (frontière SaaS : l'ADMIN ne voit pas le contenu
// des tontines). Les chiffres sont calculés en base (COUNT), d'où un DTO
// dédié et non l'entité Tontine.
@Getter
@Setter
@NoArgsConstructor
public class TontinesStatsDTO {

    // Toutes les tontines, tous statuts confondus (count() du repository).
    private Long total;

    // Détail par statut : la somme des quatre vaut total.
    private Long nbEnAttente;

    private Long nbActive;

    private Long nbSuspendue;

    private Long nbTerminee;
}
