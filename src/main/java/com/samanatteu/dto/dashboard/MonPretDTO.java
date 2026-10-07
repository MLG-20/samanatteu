package com.samanatteu.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.samanatteu.enums.pret.StatutPret;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Une ligne « Mes prêts » du tableau de bord MEMBRE : la ligne « prêt en
// cours » du gestionnaire, sans nom ni téléphone (c'est le sien).
@Getter
@Setter
@NoArgsConstructor
public class MonPretDTO {
    private Long pretId;

    private String tontineNom;

    private BigDecimal montantTotal;

    private BigDecimal resteARembourser;

    private LocalDate prochaineEcheance;

    private StatutPret statut;
}
