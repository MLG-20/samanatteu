package com.samanatteu.dto.cotisation;

import java.math.BigDecimal;

import com.samanatteu.enums.ModePaiementCotisation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// DTO d'ENTRÉE de POST /cotisation/{id}/paiement : uniquement ce que le client
// a le droit d'envoyer. montantPaye, statut, montantDu… n'existent pas ici,
// donc il ne peut pas les falsifier : le serveur les calcule.
@Getter
@Setter
@NoArgsConstructor
public class PaiementDTO {

    // @Positive (pas @Min) : refuse 0 et les négatifs, et accepte les
    // décimales (2500.50) ; @Min est fait pour les nombres entiers.
    @NotNull(message = "Le montant est obligatoire.")
    @Positive(message = "Le montant doit être supérieur à 0.")
    private BigDecimal montant;

    @NotNull(message = "Le mode de paiement est obligatoire (CASH, WAVE, ORANGE ou FREE).")
    private ModePaiementCotisation modePaiement;

    // Facultative : n° de transaction Wave/Orange/Free ; rien pour du CASH.
    private String reference;

}
