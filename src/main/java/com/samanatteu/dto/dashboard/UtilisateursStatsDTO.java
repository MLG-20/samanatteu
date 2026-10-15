package com.samanatteu.dto.dashboard;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Bloc « Utilisateurs » du tableau de bord admin : des compteurs anonymes
// (aucun nom, aucun téléphone), pour piloter la plateforme sans lire le
// contenu des tontines. Les chiffres sont calculés en base (COUNT), d'où un
// DTO dédié et non l'entité Utilisateur.
@Getter
@Setter
@NoArgsConstructor
public class UtilisateursStatsDTO {

    // Tous les comptes, tous rôles confondus (count() du repository).
    private Long total;

    // Détail par rôle : la somme des trois vaut total.
    private Long nbAdmins;

    private Long nbGestionnaires;

    private Long nbMembres;
}
