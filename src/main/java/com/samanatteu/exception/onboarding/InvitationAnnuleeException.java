package com.samanatteu.exception.onboarding;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

public class InvitationAnnuleeException extends SamanatteuException{
    public InvitationAnnuleeException() {
        super("Ce lien a été remplacé. Demandez un nouveau lien à votre gestionnaire.",
                HttpStatus.GONE);
    }
}
