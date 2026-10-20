package com.samanatteu.dto.tontine;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InscriptionDTO {
    // Constructeur sans argument : Jackson en a besoin pour deserialiser
    // le JSON en DTO. Ecrit a la main, pas genere par Lombok : sur ce JDK,
    // @NoArgsConstructor produit parfois un constructeur VIDE (sans appel a
    // super()), et la JVM refuse alors de charger la classe.
    public InscriptionDTO() {
        super();
    }

    @NotNull(message = "Le membre est obligatoire.")
    private Long membreId;

    @NotNull(message = "La tontine est obligatoire.")
    private Long tontineId;

    @NotNull
    @Min(1)
    private Integer nombreParts;
    // PAS de statut, ordreInscription, dateAdhesion
}
