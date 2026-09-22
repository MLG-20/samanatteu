package com.samanatteu.entity;

import java.time.LocalDateTime;

import com.samanatteu.enums.RoleUtilisateur;

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

    // Pas de colonne SQL pour ce champ : mappedBy = "gestionnaire" dit que la relation
    // est déjà gérée par l'attribut "gestionnaire" dans Tontine.java (côté propriétaire).
    // La vraie clé étrangère (gestionnaire_id) vit dans la table "tontine", pas ici.
    // Ce champ n'est qu'une vue de confort côté Java pour faire utilisateur.getTontineGerees().
    @OneToMany(mappedBy = "gestionnaire")
    private List<Tontine> tontineGerees;
}
