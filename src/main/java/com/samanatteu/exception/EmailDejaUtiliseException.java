package com.samanatteu.exception;

import org.springframework.http.HttpStatus;

public class EmailDejaUtiliseException extends SamanatteuException {
    public EmailDejaUtiliseException(String email){
        super("L'email " + email + " est déjà utilisé.", HttpStatus.CONFLICT);
    }
}
