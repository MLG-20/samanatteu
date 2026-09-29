package com.samanatteu.dto.cotisation;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// DTO d'ENTRÉE de POST /tirage/{id}/verser : le client n'envoie que le
// montant remis au gagnant. montantVerse, statut, dateVersement sont
// calculés par le serveur, donc infalsifiables.
@Getter
@Setter
@NoArgsConstructor
public class VersementDTO {

    // @Positive : refuse 0 et les négatifs, accepte les décimales.
    @NotNull(message = "Le montant est obligatoire.")
    @Positive(message = "Le montant doit être supérieur à 0.")
    private BigDecimal montant;
}
