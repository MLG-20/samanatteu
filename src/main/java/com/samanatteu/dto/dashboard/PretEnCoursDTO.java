package com.samanatteu.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.samanatteu.enums.pret.StatutPret;

import lombok.Getter;
import lombok.Setter;

// Une ligne « Prêts en cours » du tableau de bord gestionnaire : à qui il a
// prêté, combien il reste à récupérer, et quand tombe la prochaine échéance.
@Getter
@Setter
public class PretEnCoursDTO {
    // Constructeur sans argument : Jackson en a besoin pour deserialiser
    // le JSON en DTO. Ecrit a la main, pas genere par Lombok : sur ce JDK,
    // @NoArgsConstructor produit parfois un constructeur VIDE (sans appel a
    // super()), et la JVM refuse alors de charger la classe.
    public PretEnCoursDTO() {
        super();
    }

    private Long pretId;

    private String tontineNom;

    // Prénom et nom réunis, comme dans CotisationRetardDTO.
    private String membreNom;

    private String membreTelephone;

    // Montant prêté + intérêt : ce que le membre doit rendre au total.
    private BigDecimal montantTotal;

    // Somme des restes des échéances pas encore payées.
    private BigDecimal resteARembourser;

    // Date de la première échéance non payée. LocalDate et non LocalDateTime :
    // une échéance tombe un jour, pas à une heure.
    private LocalDate prochaineEcheance;

    // ACTIF ou EN_RETARD (au moins une échéance dépassée et non payée).
    private StatutPret statut;
}
