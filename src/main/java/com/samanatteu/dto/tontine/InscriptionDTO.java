package com.samanatteu.dto.tontine;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class InscriptionDTO {
    @NotNull(message = "Le membre est obligatoire.")
    private Long membreId;

    @NotNull(message = "La tontine est obligatoire.")
    private Long tontineId;

    @NotNull
    @Min(1)
    private Integer nombreParts;
    // PAS de statut, ordreInscription, dateAdhesion
}
