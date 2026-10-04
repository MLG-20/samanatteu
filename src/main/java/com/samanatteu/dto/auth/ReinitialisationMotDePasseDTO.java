package com.samanatteu.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Pas de token ici : la preuve d'identité est le code reçu par SMS sur ce téléphone.
@Getter
@Setter
@NoArgsConstructor
public class ReinitialisationMotDePasseDTO {
    @NotBlank
    private String telephone;

    @NotBlank
    private String code;

    @NotBlank
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères.")
    private String nouveauMotDePasse;
}
