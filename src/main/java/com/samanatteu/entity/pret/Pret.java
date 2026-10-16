package com.samanatteu.entity.pret;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.samanatteu.entity.commun.Auditable;
import com.samanatteu.entity.tontine.Tontine;
import com.samanatteu.entity.utilisateur.Utilisateur;
import com.samanatteu.enums.pret.StatutPret;

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
@Table(name = "pret")
@Getter
@Setter
public class Pret extends Auditable {
    // Constructeur sans argument exigé par JPA/Hibernate.
    // Écrit à la main plutôt que Lombok @NoArgsConstructor : Lombok 1.18.46
    // génère un constructeur vide (sans super()) pour les filles d'Auditable
    // sur ce JDK, et la JVM refuse la classe au chargement.
    public Pret() {
        super();
    }
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "membre_id")
    private Utilisateur membre;

    @ManyToOne
    @JoinColumn(name = "tontine_id")
    private Tontine tontine;

    @ManyToOne
    @JoinColumn(name = "gestionnaire_id")
    private Utilisateur gestionnaire;

    @Column(name = "montant")
    private BigDecimal montant;

    @Column(name = "taux_interet")
    private BigDecimal tauxInteret;

    // Intérêt en FRANCS, fixé par la gestionnaire (l'appli n'impose rien).
    // Saisi directement, ou calculé une fois depuis tauxInteret (intérêt
    // simple : montant × taux / 100). Seule valeur utile à l'échéancier ;
    // tauxInteret est gardé pour savoir ce qu'elle avait saisi.
    @Column(name = "montant_interet")
    private BigDecimal montantInteret;

    @Column(name = "nb_echeances")
    private Integer nbEcheances;

    @Column(name = "date_accord")
    private LocalDate dateAccord;

    // Date de la 1re échéance, choisie par la gestionnaire. Les suivantes
    // suivent le rythme de la tontine (frequence + intervalle).
    @Column(name = "date_debut_remb")
    private LocalDate dateDebutRemboursement;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut")
    private StatutPret statut;

}
