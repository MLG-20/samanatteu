package com.samanatteu.exception.onboarding;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

public class ImportIncoherentException extends SamanatteuException{
    public ImportIncoherentException(int nbMembresTotal, int nbImportes, int nbErreurs){
        super("Import incohérent : " + nbImportes + " importés + " + nbErreurs
                + " erreurs ne correspond pas au total de " + nbMembresTotal + " membres.",
                HttpStatus.BAD_REQUEST);
    }
}
