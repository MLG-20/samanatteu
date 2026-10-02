package com.samanatteu.exception.onboarding;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

public class InvitationIntrouvableException extends SamanatteuException {
    public InvitationIntrouvableException() {
        super("Ce lien d'invitation n'existe pas.", HttpStatus.NOT_FOUND);
    }
}
