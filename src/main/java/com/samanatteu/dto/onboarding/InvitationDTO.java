package com.samanatteu.dto.onboarding;

import java.time.LocalDateTime;

import com.samanatteu.enums.StatutInvitation;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// DTO : jamais d'annotations JPA, cette classe n'est ni stockée ni lue en base.
// tontineId (Long) au lieu de Tontine : on n'expose jamais une entité complète imbriquée, juste son id.
@Getter
@Setter
@NoArgsConstructor
public class InvitationDTO {
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

    private LocalDateTime createdAt;
}
