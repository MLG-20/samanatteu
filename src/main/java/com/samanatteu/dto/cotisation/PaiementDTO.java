package com.samanatteu.dto.cotisation;

import java.math.BigDecimal;

import com.samanatteu.enums.ModePaiement;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

// DTO d'ENTRÉE de POST /cotisation/{id}/paiement : uniquement ce que le client
// a le droit d'envoyer. montantPaye, statut, montantDu… n'existent pas ici,
// donc il ne peut pas les falsifier : le serveur les calcule.
@Getter
@Setter
public class PaiementDTO {
    // Constructeur sans argument : Jackson en a besoin pour deserialiser
    // le JSON en DTO. Ecrit a la main, pas genere par Lombok : sur ce JDK,
    // @NoArgsConstructor produit parfois un constructeur VIDE (sans appel a
    // super()), et la JVM refuse alors de charger la classe.
    public PaiementDTO() {
        super();
    }


    // @Positive (pas @Min) : refuse 0 et les négatifs, et accepte les
    // décimales (2500.50) ; @Min est fait pour les nombres entiers.
    @NotNull(message = "Le montant est obligatoire.")
    @Positive(message = "Le montant doit être supérieur à 0.")
    private BigDecimal montant;

    @NotNull(message = "Le mode de paiement est obligatoire (CASH, WAVE, ORANGE ou FREE).")
    private ModePaiement modePaiement;

    // Facultative : n° de transaction Wave/Orange/Free ; rien pour du CASH.
    private String reference;

}
