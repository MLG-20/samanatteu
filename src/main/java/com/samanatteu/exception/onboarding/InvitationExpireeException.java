package com.samanatteu.exception.onboarding;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

public class InvitationExpireeException extends SamanatteuException{
    public InvitationExpireeException(){
        super("Ce lien a expiré. Demandez un nouveau lien à votre gestionnaire.", HttpStatus.GONE);
    }
}
