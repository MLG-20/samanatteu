package com.samanatteu.dto.tontine;

import java.math.BigDecimal;

import com.samanatteu.enums.tontine.FrequenceTontine;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ModificationTontineDTO {
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
}
