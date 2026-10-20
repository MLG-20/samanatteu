package com.samanatteu.dto.dashboard;

import java.math.BigDecimal;

import com.samanatteu.enums.cotisation.StatutTirage;

import lombok.Getter;
import lombok.Setter;

// Une ligne « Gains à verser » du tableau de bord gestionnaire : un tirage
// fait, dont le gagnant n'a pas encore reçu toute sa cagnotte.
@Getter
@Setter
public class GainAVerserDTO {
    // Constructeur sans argument : Jackson en a besoin pour deserialiser
    // le JSON en DTO. Ecrit a la main, pas genere par Lombok : sur ce JDK,
    // @NoArgsConstructor produit parfois un constructeur VIDE (sans appel a
    // super()), et la JVM refuse alors de charger la classe.
    public GainAVerserDTO() {
        super();
    }

    // Id du TIRAGE (il existe, contrairement à TirageAFaireDTO) ; le front
    // s'en sert pour enregistrer un versement.
    private Long tirageId;

    private String tontineNom;

    private Integer numeroCycle;

    // Prénom et nom réunis, comme dans les autres lignes du tableau de bord.
    private String gagnantNom;

    private String gagnantTelephone;

    // Ce que le gagnant doit recevoir au total...
    private BigDecimal montantGagne;

    // ...ce qu'il a déjà reçu...
    private BigDecimal montantVerse;

    // ...et la différence, ce que le gestionnaire lui doit encore.
    private BigDecimal resteAVerser;

    // EN_ATTENTE, PARTIEL ou REPORTE (jamais VERSE : rien à verser).
    private StatutTirage statut;
}
