package com.samanatteu.exception.utilisateur;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

public class TelephoneDejaUtiliseException extends SamanatteuException {
    public TelephoneDejaUtiliseException(String telephone){
        super("Le numéro " + telephone + " existe déjà", HttpStatus.CONFLICT);
    }
}
