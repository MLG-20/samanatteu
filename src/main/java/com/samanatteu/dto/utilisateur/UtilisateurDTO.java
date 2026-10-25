package com.samanatteu.dto.utilisateur;

import java.time.LocalDateTime;

import com.samanatteu.enums.utilisateur.RoleUtilisateur;

import lombok.Getter;
import lombok.Setter;


// Champ motDePasse volontairement ABSENT : c'est ce qui empêche physiquement le mot de passe
// de sortir dans les réponses de l'API (OWASP A02 - Cryptographic Failures / Sensitive Data Exposure),
// même si l'entité en base le contient toujours pour le login.
@Getter
@Setter
public class UtilisateurDTO {
    // Constructeur sans argument : Jackson en a besoin pour deserialiser
    // le JSON en DTO. Ecrit a la main, pas genere par Lombok : sur ce JDK,
    // @NoArgsConstructor produit parfois un constructeur VIDE (sans appel a
    // super()), et la JVM refuse alors de charger la classe.
    public UtilisateurDTO() {
        super();
    }

    private Long id;

    private String nom;

    private String prenom;

    private String telephone;

    private String email;

    private RoleUtilisateur role;

    private Boolean actif;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Rempli seulement dans la liste de l'ADMIN, pour un GESTIONNAIRE : le nombre de
    // membres distincts de ses tontines (un nombre, jamais la liste). null ailleurs.
    private Long nbMembres;
}
