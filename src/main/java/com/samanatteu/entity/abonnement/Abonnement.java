package com.samanatteu.entity.abonnement;

import java.time.LocalDate;

import com.samanatteu.entity.utilisateur.Utilisateur;
import com.samanatteu.enums.abonnement.StatutAbonnement;

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

// L'abonnement d'une gestionnaire : c'est ELLE qui paie, pas les membres.
//
// L'ADMIN voit les abonnements (données commerciales de la plateforme), mais
// pas le contenu des tontines : même frontière que pour le reste.
@Entity
@Table(name = "abonnement")
@Getter
@Setter
public class Abonnement {

    // Constructeur sans argument exigé par JPA/Hibernate.
    // Écrit à la main plutôt que Lombok @NoArgsConstructor : sur ce JDK, Lombok
    // produit parfois un constructeur VIDE (sans appel à super()), et la JVM
    // refuse alors de charger la classe.
    public Abonnement() {
        super();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // La cliente : c'est ELLE qui s'abonne, pas la tontine. Une gestionnaire
    // qui gère 10 tontines a UN abonnement.
    @ManyToOne
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    // La formule : une DONNÉE (table formule), pas un enum. L'ADMIN peut
    // changer ses quotas et son prix sans redéployer l'application.
    @ManyToOne
    @JoinColumn(name = "formule_id", nullable = false)
    private Formule formule;

    // Les décisions de la plateforme. L'EXPIRATION, elle, n'est pas un statut :
    // elle se déduit de la date de fin (voir estActif()).
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutAbonnement statut;

    // La période couverte. dateFin est le DERNIER JOUR couvert : le jour même,
    // l'abonnement est encore actif (voir estActif()).
    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    // Compteur de SMS envoyés sur le mois en cours (le maximum vient de la
    // formule). On le stocke dès maintenant, on l'applique plus tard.
    @Column(name = "sms_envoyes_mois", nullable = false)
    private Integer smsEnvoyesMois;

    // Le mois du compteur : au changement de mois, il repart à zéro.
    @Column(name = "mois_compteur", nullable = false)
    private Integer moisCompteur;

    // L'abonnement donne-t-il le droit d'agir ?
    // - ACTIF (payé) ou ESSAI (gratuit) : les deux ouvrent les mêmes droits,
    //   seule la date de fin les distingue ;
    // - et la période n'est pas dépassée (la date est calculée, pas stockée :
    //   aucun job ne peut être en retard, et l'état est toujours juste).
    //
    // dateFin étant le DERNIER JOUR couvert, l'abonnement est encore actif ce
    // jour-là : isBefore (strictement avant) et non isAfter.
    public boolean estActif() {
        return (statut == StatutAbonnement.ACTIF || statut == StatutAbonnement.ESSAI)
                && !dateFin.isBefore(LocalDate.now());
    }
}
