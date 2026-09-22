package com.samanatteu.dto.tontine;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.samanatteu.enums.StatutTontine;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// DTO = objet de transfert vers le client, distinct de l'entité JPA Tontine.
// Jamais d'annotations JPA ici : cette classe n'est ni stockée ni lue en base.
// gestionnaireId est un simple Long (pas un Utilisateur complet) : si on mettait l'entité Utilisateur
// ici, son motDePasse serait réexposé en cascade dans le JSON de /tontine - même faille qu'on corrige
// sur UtilisateurDTO, mais imbriquée. Le client n'a besoin que de l'id pour identifier le gestionnaire.
@Getter
@Setter
@NoArgsConstructor
public class TontineDTO {
    private Long id;

    private String nom;

    private BigDecimal montantPart;

    private String frequence;

    private Integer nbCycles;

    private String description;

    private Integer jourCotisation;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private StatutTontine statut;

    private Long gestionnaireId;
}
