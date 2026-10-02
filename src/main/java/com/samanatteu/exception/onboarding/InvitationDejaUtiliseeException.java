package com.samanatteu.exception.onboarding;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

public class InvitationDejaUtiliseeException extends SamanatteuException{
    public InvitationDejaUtiliseeException(){
        super("Ce lien a déjà été utilisé.", HttpStatus.CONFLICT);
    }
}
