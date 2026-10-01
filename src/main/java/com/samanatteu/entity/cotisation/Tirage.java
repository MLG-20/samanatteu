package com.samanatteu.entity.cotisation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.samanatteu.entity.tontine.Cycle;
import com.samanatteu.entity.tontine.Participation;
import com.samanatteu.enums.cotisation.StatutTirage;

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
@Table(name = "tirage")
@Getter
@Setter
@NoArgsConstructor
public class Tirage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cycle_id")
    private Cycle cycle;

    @ManyToOne
    @JoinColumn(name = "participation_id")
    private Participation participation;

    @Column(name = "montant_gagne")
    private BigDecimal montantGagne;

    @Column(name = "montant_verse")
    private BigDecimal montantVerse;

    @Column(name = "date_tirage")
    private LocalDateTime dateTirage;

    @Column(name = "date_versement")
    private LocalDateTime dateVersement;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut")
    private StatutTirage statut;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
