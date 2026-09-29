package com.samanatteu.exception.onboarding;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

public class InvitationExpireeException extends SamanatteuException{
    public InvitationExpireeException(String token){
        super("L'invitation " + token + " a expiré.", HttpStatus.GONE);
    }
}
