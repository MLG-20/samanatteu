package com.samanatteu.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// Pas de token ici : la preuve d'identité est le code reçu par SMS sur ce téléphone.
@Getter
@Setter
public class ReinitialisationMotDePasseDTO {
    // Constructeur sans argument : Jackson en a besoin pour deserialiser
    // le JSON en DTO. Ecrit a la main, pas genere par Lombok : sur ce JDK,
    // @NoArgsConstructor produit parfois un constructeur VIDE (sans appel a
    // super()), et la JVM refuse alors de charger la classe.
    public ReinitialisationMotDePasseDTO() {
        super();
    }

    @NotBlank
    private String telephone;

    @NotBlank
    private String code;

    @NotBlank
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères.")
    private String nouveauMotDePasse;
}
