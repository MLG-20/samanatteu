package com.samanatteu.exception;

import org.springframework.http.HttpStatus;

public class InvitationDejaUtiliseeException extends SamanatteuException{
    public InvitationDejaUtiliseeException(String token){
        super("L'invitation " + token + " a déjà été utilisée.", HttpStatus.CONFLICT);
    }
}
