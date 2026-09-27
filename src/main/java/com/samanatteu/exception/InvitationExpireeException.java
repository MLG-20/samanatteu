package com.samanatteu.exception;

import org.springframework.http.HttpStatus;

public class InvitationExpireeException extends SamanatteuException{
    public InvitationExpireeException(String token){
        super("L'invitation " + token + " a expiré.", HttpStatus.GONE);
    }
}
