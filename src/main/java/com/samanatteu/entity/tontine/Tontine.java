package com.samanatteu.entity.tontine;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.samanatteu.entity.utilisateur.Utilisateur;
import com.samanatteu.enums.tontine.FrequenceTontine;
import com.samanatteu.enums.tontine.StatutTontine;

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
import jakarta.validation.constraints.PositiveOrZero;
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

    // Somme FIXE que chaque membre verse à la caisse de prêts à chaque
    // cycle (même montant pour tous, pas multiplié par les parts).
    // Règle fixée par la gestionnaire : 0 = pas de caisse de prêts.
    // @PositiveOrZero : comme @Positive, mais accepte 0.
    @NotNull(message = "Le montant de la caisse de prêts est obligatoire (0 si pas de caisse).")
    @PositiveOrZero(message = "Le montant de la caisse de prêts ne peut pas être négatif.")
    @Column(name = "montant_caisse_pret")
    private BigDecimal montantCaissePret;

    // Argent disponible dans la caisse de prêts. Calculé par le serveur,
    // jamais saisi par le client (même principe que nbCycles).
    @Column(name = "solde_caisse_pret")
    private BigDecimal soldeCaissePret;

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
