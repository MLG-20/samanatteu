package com.samanatteu.dto.dashboard;

import java.math.BigDecimal;

import com.samanatteu.enums.cotisation.StatutTirage;

import lombok.Getter;
import lombok.Setter;

// Une ligne « Mes gains » du tableau de bord MEMBRE : un tirage qu'il a
// gagné, ce qu'il a déjà reçu et ce qu'on lui doit encore. Les gains
// entièrement versés restent affichés (historique).
@Getter
@Setter
public class MonGainDTO {
    // Constructeur sans argument : Jackson en a besoin pour deserialiser
    // le JSON en DTO. Ecrit a la main, pas genere par Lombok : sur ce JDK,
    // @NoArgsConstructor produit parfois un constructeur VIDE (sans appel a
    // super()), et la JVM refuse alors de charger la classe.
    public MonGainDTO() {
        super();
    }


    private Long tirageId;

    private String tontineNom;

    private Integer numeroCycle;

    private BigDecimal montantGagne;

    private BigDecimal montantVerse;

    private BigDecimal resteARecevoir;

    private StatutTirage statut;
}
