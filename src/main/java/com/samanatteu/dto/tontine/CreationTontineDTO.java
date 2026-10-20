package com.samanatteu.dto.tontine;

import java.math.BigDecimal;

import com.samanatteu.enums.tontine.FrequenceTontine;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreationTontineDTO {
    // Constructeur sans argument : Jackson en a besoin pour deserialiser
    // le JSON en DTO. Ecrit a la main, pas genere par Lombok : sur ce JDK,
    // @NoArgsConstructor produit parfois un constructeur VIDE (sans appel a
    // super()), et la JVM refuse alors de charger la classe.
    public CreationTontineDTO() {
        super();
    }

    private String nom;

    private BigDecimal montantPart;

    @NotNull(message = "Le montant de la caisse de prêts est obligatoire (0 si pas de caisse).")
    @PositiveOrZero
    private BigDecimal montantCaissePret;

    @NotNull
    private FrequenceTontine frequence;

    @NotNull
    @Min(1)
    private Integer intervalle;

    private String description;

    private Integer jourCotisation;

    // PAS de id, statut, soldeCaissePret, nbCycles, gestionnaire, createdAt
}
