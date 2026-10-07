package com.samanatteu.dto.dashboard;

import java.math.BigDecimal;

import com.samanatteu.enums.cotisation.StatutTirage;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Une ligne « Mes gains » du tableau de bord MEMBRE : un tirage qu'il a
// gagné, ce qu'il a déjà reçu et ce qu'on lui doit encore. Les gains
// entièrement versés restent affichés (historique).
@Getter
@Setter
@NoArgsConstructor
public class MonGainDTO {

    private Long tirageId;

    private String tontineNom;

    private Integer numeroCycle;

    private BigDecimal montantGagne;

    private BigDecimal montantVerse;

    private BigDecimal resteARecevoir;

    private StatutTirage statut;
}
