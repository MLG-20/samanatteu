package com.samanatteu.dto.utilisateur;

import com.samanatteu.enums.RoleUtilisateur;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreationUtilisateurDTO {
    @NotBlank
    private String nom;

    @NotBlank
    private String prenom;

    @NotBlank
    private String telephone;

    @Email
    private String email;

    @NotBlank
    @Size(min = 8)
    private String motDePasse;

    @NotNull
    private RoleUtilisateur role;
}
