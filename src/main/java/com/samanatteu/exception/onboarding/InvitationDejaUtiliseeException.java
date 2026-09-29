package com.samanatteu.exception.onboarding;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

public class InvitationDejaUtiliseeException extends SamanatteuException{
    public InvitationDejaUtiliseeException(String token){
        super("L'invitation " + token + " a déjà été utilisée.", HttpStatus.CONFLICT);
    }
}
