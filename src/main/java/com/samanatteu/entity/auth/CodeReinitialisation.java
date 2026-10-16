package com.samanatteu.entity.auth;

import java.time.LocalDateTime;

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

// Code à usage unique envoyé par SMS pour « mot de passe oublié ».
// Reflet exact de la migration V5 (Hibernate le vérifie au démarrage : validate).
@Entity
@Table(name = "code_reinitialisation")
@Getter
@Setter
public class CodeReinitialisation extends Auditable {
    // Constructeur sans argument exigé par JPA/Hibernate.
    // Écrit à la main plutôt que Lombok @NoArgsConstructor : Lombok 1.18.46
    // génère un constructeur vide (sans super()) pour les filles d'Auditable
    // sur ce JDK, et la JVM refuse la classe au chargement.
    public CodeReinitialisation() {
        super();
    }
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    // Haché comme un mot de passe : lire la base ne donne pas les codes.
    @Column(name = "code_hache")
    private String codeHache;

    @Column(name = "expire_at")
    private LocalDateTime expireAt;

    // Essais ratés : sans limite, un programme testerait le million de codes possibles.
    @Column(name = "tentatives")
    private int tentatives;

    @Column(name = "utilise")
    private boolean utilise;

}
