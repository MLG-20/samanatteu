package com.samanatteu.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.samanatteu.enums.pret.StatutPret;

import lombok.Getter;
import lombok.Setter;

// Une ligne « Mes prêts » du tableau de bord MEMBRE : la ligne « prêt en
// cours » du gestionnaire, sans nom ni téléphone (c'est le sien).
@Getter
@Setter
public class MonPretDTO {
    // Constructeur sans argument : Jackson en a besoin pour deserialiser
    // le JSON en DTO. Ecrit a la main, pas genere par Lombok : sur ce JDK,
    // @NoArgsConstructor produit parfois un constructeur VIDE (sans appel a
    // super()), et la JVM refuse alors de charger la classe.
    public MonPretDTO() {
        super();
    }

    private Long pretId;

    private String tontineNom;

    private BigDecimal montantTotal;

    private BigDecimal resteARembourser;

    private LocalDate prochaineEcheance;

    private StatutPret statut;
}
