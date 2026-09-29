package com.samanatteu.exception.utilisateur;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

public class EmailDejaUtiliseException extends SamanatteuException {
    public EmailDejaUtiliseException(String email){
        super("L'email " + email + " est déjà utilisé.", HttpStatus.CONFLICT);
    }
}
