package com.samanatteu.dto.onboarding;

import java.time.LocalDateTime;

import com.samanatteu.enums.onboarding.TypeInvitation;

import lombok.Getter;
import lombok.Setter;

// Ce que voit la personne qui clique sur le lien (route publique) : le strict
// nécessaire pour l'écran d'inscription, ni id ni token ni statut interne.
@Getter
@Setter
public class LienInvitationDTO {
    // Constructeur sans argument : Jackson en a besoin pour deserialiser
    // le JSON en DTO. Ecrit a la main, pas genere par Lombok : sur ce JDK,
    // @NoArgsConstructor produit parfois un constructeur VIDE (sans appel a
    // super()), et la JVM refuse alors de charger la classe.
    public LienInvitationDTO() {
        super();
    }


    private String nomTontine;

    private TypeInvitation type;

    private String prenomPreRempli;

    private String nomPreRempli;

    private String telephone;

    private LocalDateTime expireAt;
}
