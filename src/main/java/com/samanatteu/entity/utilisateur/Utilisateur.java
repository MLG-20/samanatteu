package com.samanatteu.entity.utilisateur;

import java.util.List;

import com.samanatteu.entity.commun.Auditable;
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
import lombok.Setter;

@Entity
@Table(name = "utilisateur")
@Getter
@Setter

public class Utilisateur extends Auditable {
    // Constructeur sans argument exigé par JPA/Hibernate.
    // Écrit à la main plutôt que Lombok @NoArgsConstructor : Lombok 1.18.46
    // génère un constructeur vide (sans super()) pour les filles d'Auditable
    // sur ce JDK, et la JVM refuse la classe au chargement.
    public Utilisateur() {
        super();
    }

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

    // Écrit dans chaque refresh token. L'augmenter de 1 (déconnexion, nouveau
    // mot de passe) rend inutilisables tous les tokens fabriqués avant.
    @Column(name = "version_sessions")
    private int versionSessions;

    // false seulement pour une gestionnaire inscrite par le site public, tant
    // qu'elle n'a pas cliqué le lien reçu : la connexion lui est refusée.
    // Initialisé à true : Hibernate écrit la valeur du champ (le DEFAULT de la
    // base ne joue pas), et tous les autres comptes sont vérifiés d'office.
    @Column(name = "email_verifie")
    private boolean emailVerifie = true;

    // Côté inverse de Tontine.gestionnaire : pas de colonne ici, la clé étrangère
    // gestionnaire_id est dans la table tontine.
    @OneToMany(mappedBy = "gestionnaire")
    private List<Tontine> tontineGerees;
}
