package com.samanatteu.dto.onboarding;

import java.time.LocalDateTime;

import com.samanatteu.enums.onboarding.TypeInvitation;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Ce que voit la personne qui clique sur le lien (route publique) : le strict
// nécessaire pour l'écran d'inscription, ni id ni token ni statut interne.
@Getter
@Setter
@NoArgsConstructor
public class LienInvitationDTO {

    private String nomTontine;

    private TypeInvitation type;

    private String prenomPreRempli;

    private String nomPreRempli;

    private String telephone;

    private LocalDateTime expireAt;
}
