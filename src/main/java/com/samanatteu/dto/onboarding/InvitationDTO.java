package com.samanatteu.dto.onboarding;

import java.time.LocalDateTime;

import com.samanatteu.enums.onboarding.StatutInvitation;
import com.samanatteu.enums.onboarding.TypeInvitation;

import lombok.Getter;
import lombok.Setter;

// tontineId (Long) au lieu de Tontine : on n'expose jamais une entité complète imbriquée, juste son id.
@Getter
@Setter
public class InvitationDTO {
    // Constructeur sans argument : Jackson en a besoin pour deserialiser
    // le JSON en DTO. Ecrit a la main, pas genere par Lombok : sur ce JDK,
    // @NoArgsConstructor produit parfois un constructeur VIDE (sans appel a
    // super()), et la JVM refuse alors de charger la classe.
    public InvitationDTO() {
        super();
    }

    private Long id;

    private Long tontineId;

    private String token;

    private String telephone;

    private String email;

    private String prenomPreRempli;

    private String nomPreRempli;

    private Integer nombreParts;

    private LocalDateTime expireAt;

    private StatutInvitation statut;

    private TypeInvitation type;

    private LocalDateTime createdAt;
}
