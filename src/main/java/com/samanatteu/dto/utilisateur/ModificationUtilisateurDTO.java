package com.samanatteu.dto.utilisateur;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ModificationUtilisateurDTO {

    @NotBlank
    private String nom;

    @NotBlank
    private String prenom;

    @Email
    private String email;

}
