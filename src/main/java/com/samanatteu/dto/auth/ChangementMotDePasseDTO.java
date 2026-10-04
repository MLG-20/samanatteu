package com.samanatteu.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Pas d'identifiant dans ce DTO : le compte à modifier est celui du token.
@Getter
@Setter
@NoArgsConstructor
public class ChangementMotDePasseDTO {
    // Preuve que c'est bien le titulaire, pas quelqu'un qui a trouvé une session ouverte.
    @NotBlank
    private String ancienMotDePasse;

    // Même règle qu'à l'inscription : le nouveau ne peut pas être plus faible.
    @NotBlank
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères.")
    private String nouveauMotDePasse;
}
