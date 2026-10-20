package com.samanatteu.dto.onboarding;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

// Saisie de la gestionnaire pour une invitation individuelle. Le téléphone est
// obligatoire : c'est lui qui réserve l'invitation à une seule personne.
@Getter
@Setter
public class DemandeInvitationDTO {
    // Constructeur sans argument : Jackson en a besoin pour deserialiser
    // le JSON en DTO. Ecrit a la main, pas genere par Lombok : sur ce JDK,
    // @NoArgsConstructor produit parfois un constructeur VIDE (sans appel a
    // super()), et la JVM refuse alors de charger la classe.
    public DemandeInvitationDTO() {
        super();
    }

    @NotBlank
    private String telephone;

    private String prenom;

    private String nom;

    @Min(1)
    private Integer nombreParts;
}
