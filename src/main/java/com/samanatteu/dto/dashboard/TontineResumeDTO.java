package com.samanatteu.dto.dashboard;

import java.math.BigDecimal;

import com.samanatteu.enums.tontine.StatutTontine;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Une ligne « Mes tontines » du tableau de bord gestionnaire. Les chiffres sont
// calculés (aucune table ne les stocke), d'où un DTO dédié et non TontineDTO.
@Getter
@Setter
@NoArgsConstructor
public class TontineResumeDTO {
    private Long id;

    private String nom;

    private StatutTontine statut;

    // Participants au statut ACTIF (Long : c'est le type que renvoie un count SQL).
    private Long nombreMembres;

    // Les trois champs suivants décrivent le cycle EN_COURS ; ils restent null
    // si la tontine n'en a pas (ex. EN_ATTENTE), et le front affiche alors
    // « pas de cycle en cours ».
    private Integer numeroCycleEnCours;

    private BigDecimal montantCollecte;

    private BigDecimal montantAttendu;

    // Argent disponible dans la caisse de prêts de cette tontine (une caisse
    // par tontine). Ne dépend pas du cycle : toujours renseigné.
    private BigDecimal soldeCaissePret;
}
