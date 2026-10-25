package com.samanatteu.dto.utilisateur;

import com.samanatteu.enums.utilisateur.RoleUtilisateur;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreationUtilisateurDTO {
    // Constructeur sans argument : Jackson en a besoin pour deserialiser
    // le JSON en DTO. Ecrit a la main, pas genere par Lombok : sur ce JDK,
    // @NoArgsConstructor produit parfois un constructeur VIDE (sans appel a
    // super()), et la JVM refuse alors de charger la classe.
    public CreationUtilisateurDTO() {
        super();
    }

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

    // Le jeton du lien d'invitation (/rejoindre/{jeton}). OBLIGATOIRE pour un
    // visiteur qui s'inscrit comme MEMBRE : on ne devient membre que sur
    // invitation. Ignoré pour un gestionnaire, ou si une gestionnaire connectée
    // crée le compte de l'un de ses membres.
    private String jetonInvitation;
}
