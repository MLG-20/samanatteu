package com.samanatteu.dto.onboarding;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Saisie de la gestionnaire pour une invitation individuelle. Le téléphone est
// obligatoire : c'est lui qui réserve l'invitation à une seule personne.
@Getter
@Setter
@NoArgsConstructor
public class DemandeInvitationDTO {
    @NotBlank
    private String telephone;

    private String prenom;

    private String nom;

    @Min(1)
    private Integer nombreParts;
}
