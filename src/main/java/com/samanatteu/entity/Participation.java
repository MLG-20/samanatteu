package com.samanatteu.entity;


import java.sql.Date;
import com.samanatteu.enums.StatutParticipation;
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
@Table(name = "participation")
@Getter
@Setter
@NoArgsConstructor
public class Participation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "membre_id")
    private Utilisateur membre;

    @ManyToOne
    @JoinColumn(name = "tontine_id")
    private Tontine tontine;

    @Column(name = "nombre_parts")
    private Integer nombreParts;

    @Column(name = "date_adhesion")
    private Date dateAdhesion;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut")
    private StatutParticipation statut;

    @Column(name = "ordre_inscription")
    private Integer ordreInscription;

}
