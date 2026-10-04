package com.samanatteu.entity.utilisateur;

import java.time.LocalDateTime;

import com.samanatteu.entity.tontine.Tontine;
import com.samanatteu.enums.utilisateur.RoleUtilisateur;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Entity
@Table(name = "utilisateur")
@Getter
@Setter
@NoArgsConstructor

public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom")
    private String nom;

    @Column(name = "prenom")
    private String prenom;

    @Column(name = "telephone")
    private String telephone;

    @Column(name = "email")
    private String email;

    @Column(name = "mot_de_passe")
    private String motDePasse;

    @Enumerated(EnumType.STRING)
    private RoleUtilisateur role;

    @Column(name = "actif")
    private Boolean actif;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Écrit dans chaque refresh token. L'augmenter de 1 (déconnexion, nouveau
    // mot de passe) rend inutilisables tous les tokens fabriqués avant.
    @Column(name = "version_sessions")
    private int versionSessions;

    // Côté inverse de Tontine.gestionnaire : pas de colonne ici, la clé étrangère
    // gestionnaire_id est dans la table tontine.
    @OneToMany(mappedBy = "gestionnaire")
    private List<Tontine> tontineGerees;
}
