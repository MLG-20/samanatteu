package com.samanatteu.dto.cotisation;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

// DTO d'ENTRÉE de POST /tirage/{id}/verser : le client n'envoie que le
// montant remis au gagnant. montantVerse, statut, dateVersement sont
// calculés par le serveur, donc infalsifiables.
@Getter
@Setter
public class VersementDTO {
    // Constructeur sans argument : Jackson en a besoin pour deserialiser
    // le JSON en DTO. Ecrit a la main, pas genere par Lombok : sur ce JDK,
    // @NoArgsConstructor produit parfois un constructeur VIDE (sans appel a
    // super()), et la JVM refuse alors de charger la classe.
    public VersementDTO() {
        super();
    }


    // @Positive : refuse 0 et les négatifs, accepte les décimales.
    @NotNull(message = "Le montant est obligatoire.")
    @Positive(message = "Le montant doit être supérieur à 0.")
    private BigDecimal montant;
}
