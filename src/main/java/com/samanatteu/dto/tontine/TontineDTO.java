package com.samanatteu.dto.tontine;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.samanatteu.enums.tontine.FrequenceTontine;
import com.samanatteu.enums.tontine.StatutTontine;
import lombok.Getter;
import lombok.Setter;

// gestionnaireId (Long) et non l'entité Utilisateur : sinon son motDePasse serait exposé
// en cascade dans le JSON de /tontine.
@Getter
@Setter
public class TontineDTO {
    // Constructeur sans argument : Jackson en a besoin pour deserialiser
    // le JSON en DTO. Ecrit a la main, pas genere par Lombok : sur ce JDK,
    // @NoArgsConstructor produit parfois un constructeur VIDE (sans appel a
    // super()), et la JVM refuse alors de charger la classe.
    public TontineDTO() {
        super();
    }

    private Long id;

    private String nom;

    private BigDecimal montantPart;

    private BigDecimal montantCaissePret;

    private BigDecimal soldeCaissePret;

    private FrequenceTontine frequence;

    private Integer intervalle;

    private Integer nbCycles;

    private String description;

    private Integer jourCotisation;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private StatutTontine statut;

    private Long gestionnaireId;
}
