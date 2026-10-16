package com.samanatteu.entity.onboarding;

import java.time.LocalDateTime;

import com.samanatteu.entity.commun.Auditable;
import com.samanatteu.entity.tontine.Tontine;
import com.samanatteu.enums.onboarding.StatutInvitation;
import com.samanatteu.enums.onboarding.TypeInvitation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "invitation")
@Getter
@Setter
public class Invitation extends Auditable {
    // Constructeur sans argument exigé par JPA/Hibernate.
    // Écrit à la main plutôt que Lombok @NoArgsConstructor : Lombok 1.18.46
    // génère un constructeur vide (sans super()) pour les filles d'Auditable
    // sur ce JDK, et la JVM refuse la classe au chargement.
    public Invitation() {
        super();
    }
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tontine_id")
    private Tontine tontine;

    @Column(name = "token")
    private String token;

    @Column(name = "telephone")
    private String telephone;

    @Column(name = "email")
    private String email;

    @Column(name = "prenom_pre_rempli")
    private String prenomPreRempli;

    @Column(name = "nom_pre_rempli")
    private String nomPreRempli;

    @Column(name = "nombre_parts")
    private Integer nombreParts;

    @Column(name = "expire_at")
    private LocalDateTime expireAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut")
    private StatutInvitation statut;

    // Lien de groupe ou invitation individuelle (colonne ajoutée en V3)
    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private TypeInvitation type;

}
