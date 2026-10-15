package com.samanatteu.dto.tontine;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ModificationParticipationDTO {
    @NotNull
    @Min(1)
    private Integer nombreParts;
}
