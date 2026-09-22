package com.samanatteu.dto.utilisateur;

import java.time.LocalDateTime;

import com.samanatteu.enums.RoleUtilisateur;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


// DTO = objet de transfert vers le client, distinct de l'entité JPA Utilisateur.
// Jamais d'annotations JPA ici : cette classe n'est ni stockée ni lue en base, juste sérialisée en JSON.
// Champ motDePasse volontairement ABSENT : c'est ce qui empêche physiquement le mot de passe
// de sortir dans les réponses de l'API (OWASP A02 - Cryptographic Failures / Sensitive Data Exposure),
// même si l'entité en base le contient toujours pour le login.
@Getter
@Setter
@NoArgsConstructor
public class UtilisateurDTO {
    private Long id;

    private String nom;

    private String prenom;

    private String telephone;

    private String email;

    private RoleUtilisateur role;

    private Boolean actif;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
