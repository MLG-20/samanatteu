package com.samanatteu.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.samanatteu.enums.FrequenceTontine;
import com.samanatteu.enums.StatutTontine;

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
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tontine")
@Getter
@Setter
@NoArgsConstructor

public class Tontine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom")
    private String nom;

    @Column(name = "montant_part")
    private BigDecimal montantPart;

    // STRING : la colonne (varchar) contient le NOM de la valeur, ex. "MOIS".
    // Lombok nomme getter/setter d'après le champ : getFrequence(), pas
    // getFrequenceTontine().
    @NotNull(message = "La fréquence est obligatoire (JOUR, SEMAINE ou MOIS).")
    @Enumerated(EnumType.STRING)
    @Column(name = "frequence")
    private FrequenceTontine frequence;

    // Nombre d'unités entre deux cycles : (frequence MOIS, intervalle 2) =
    // tous les 2 mois. En base : NOT NULL DEFAULT 1.
    // @NotNull (pas @NotBlank, réservé au texte) + @Min(1) : un intervalle de
    // 0 ferait finir le cycle le jour même. Vérifié grâce à @Valid dans
    // TontineController. Sans frequence, le switch de ouvrirCycle planterait.
    @NotNull(message = "L'intervalle est obligatoire.")
    @Min(value = 1, message = "L'intervalle doit être d'au moins 1.")
    @Column(name = "intervalle")
    private Integer intervalle;

    @Column(name = "nb_cycles_total")
    private Integer nbCycles;

    @Column(name = "description")
    private String description;

    @Column(name = "jour_cotisation")
    private Integer jourCotisation;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut")
    private StatutTontine statut;

    @ManyToOne
    @JoinColumn(name = "gestionnaire_id")
    private Utilisateur gestionnaire;

    public boolean estGereePar(String telephone) {
        return gestionnaire.getTelephone().equals(telephone);
    }
}
