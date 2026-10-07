package com.samanatteu.dto.dashboard;

import java.math.BigDecimal;

import com.samanatteu.enums.tontine.StatutTontine;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Une ligne « Mes tontines » du tableau de bord MEMBRE : une tontine où il
// est inscrit, ce qu'il y paie, et où il en est de ses gains.
@Getter
@Setter
@NoArgsConstructor
public class MaTontineDTO {

    private Long tontineId;

    private String tontineNom;

    private StatutTontine statut;

    // Ses parts dans cette tontine (même nom que dans Participation).
    private Integer nombreParts;

    // Ce qu'il paie à chaque cycle : parts × montant de la part, plus la
    // caisse de prêts (fixe par membre, pas multipliée par les parts).
    private BigDecimal montantParCycle;

    // Combien de fois il a déjà gagné. À lire avec nombreParts : une part =
    // un gain, donc 2 parts et 1 gain = il lui reste un tirage à gagner.
    private Long nombreGains;
}
