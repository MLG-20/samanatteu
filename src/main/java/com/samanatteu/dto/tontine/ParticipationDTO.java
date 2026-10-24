package com.samanatteu.dto.tontine;

import com.samanatteu.enums.tontine.StatutParticipation;
import java.sql.Date;
import lombok.Getter;
import lombok.Setter;

// membreId (Long) au lieu de Utilisateur, tontineId (Long) au lieu de Tontine :
// on n'expose jamais une entité complète imbriquée, seulement son id.
@Getter
@Setter
public class ParticipationDTO {
    // Constructeur sans argument : Jackson en a besoin pour deserialiser
    // le JSON en DTO. Ecrit a la main, pas genere par Lombok : sur ce JDK,
    // @NoArgsConstructor produit parfois un constructeur VIDE (sans appel a
    // super()), et la JVM refuse alors de charger la classe.
    public ParticipationDTO() {
        super();
    }

    private Long id;

    private Long membreId;

    // Recopiés depuis le membre (pas l'entité Utilisateur entière) : le
    // gestionnaire voit qui participe sans un 2e appel, et rien de sensible
    // (mot de passe, rôle) ne sort.
    private String membrePrenom;

    private String membreNom;

    private String membreTelephone;

    private Long tontineId;

    private Integer nombreParts;

    private Date dateAdhesion;

    private StatutParticipation statut;

    private Integer ordreInscription;
}
