package com.samanatteu.entity.auth;

import java.time.LocalDateTime;

import com.samanatteu.entity.abonnement.Formule;
import com.samanatteu.entity.commun.Auditable;
import com.samanatteu.entity.utilisateur.Utilisateur;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

// Un lien « Confirmez votre adresse » envoyé par email à une gestionnaire qui
// vient de s'inscrire. Reflet exact de la migration V12.
@Entity
@Table(name = "verification_email")
@Getter
@Setter
public class VerificationEmail extends Auditable {
    // Constructeur sans argument exigé par JPA/Hibernate.
    // Écrit à la main plutôt que Lombok @NoArgsConstructor : Lombok 1.18.46
    // génère un constructeur vide (sans super()) pour les filles d'Auditable
    // sur ce JDK, et la JVM refuse la classe au chargement.
    public VerificationEmail() {
        super();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    // La formule choisie à l'inscription : l'essai démarre dessus au clic.
    @ManyToOne
    @JoinColumn(name = "formule_id")
    private Formule formule;

    // Empreinte SHA-256 du jeton du lien, jamais le jeton : lire la base ne
    // donne pas de lien utilisable.
    @Column(name = "jeton_hache")
    private String jetonHache;

    @Column(name = "expire_at")
    private LocalDateTime expireAt;

    // Usage unique : un lien déjà cliqué ne vérifie plus rien.
    @Column(name = "utilise")
    private boolean utilise;
}
