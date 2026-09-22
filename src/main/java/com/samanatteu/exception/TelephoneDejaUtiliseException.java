package com.samanatteu.exception;

import org.springframework.http.HttpStatus;

public class TelephoneDejaUtiliseException extends SamanatteuException {
    public TelephoneDejaUtiliseException(String telephone){
        super("Le numéro " + telephone + " existe déjà", HttpStatus.CONFLICT);
    }
}
