package com.samanatteu.entity.onboarding;

import java.time.LocalDateTime;

import com.samanatteu.entity.tontine.Tontine;
import com.samanatteu.enums.onboarding.StatutInvitation;

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
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "invitation")
@Getter
@Setter
@NoArgsConstructor
public class Invitation {
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

    @Column(name = "created_at")
    private LocalDateTime createdAt;

}
