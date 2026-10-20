package com.samanatteu.dto.dashboard;

import java.math.BigDecimal;

import com.samanatteu.enums.tontine.StatutTontine;

import lombok.Getter;
import lombok.Setter;

// Une ligne « Mes tontines » du tableau de bord MEMBRE : une tontine où il
// est inscrit, ce qu'il y paie, et où il en est de ses gains.
@Getter
@Setter
public class MaTontineDTO {
    // Constructeur sans argument : Jackson en a besoin pour deserialiser
    // le JSON en DTO. Ecrit a la main, pas genere par Lombok : sur ce JDK,
    // @NoArgsConstructor produit parfois un constructeur VIDE (sans appel a
    // super()), et la JVM refuse alors de charger la classe.
    public MaTontineDTO() {
        super();
    }


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
