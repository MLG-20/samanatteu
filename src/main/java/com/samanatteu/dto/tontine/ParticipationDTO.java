package com.samanatteu.dto.tontine;

import com.samanatteu.enums.StatutParticipation;
import java.sql.Date;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// membreId (Long) au lieu de Utilisateur, tontineId (Long) au lieu de Tontine :
// on n'expose jamais une entité complète imbriquée, seulement son id.
@Getter
@Setter
@NoArgsConstructor
public class ParticipationDTO {
    private Long id;

    private Long membreId;

    private Long tontineId;

    private Integer nombreParts;

    private Date dateAdhesion;

    private StatutParticipation statut;

    private Integer ordreInscription;
}
