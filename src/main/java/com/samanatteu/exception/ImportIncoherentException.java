package com.samanatteu.exception;

import org.springframework.http.HttpStatus;

public class ImportIncoherentException extends SamanatteuException{
    public ImportIncoherentException(int nbMembresTotal, int nbImportes, int nbErreurs){
        super("Import incohérent : " + nbImportes + " importés + " + nbErreurs
                + " erreurs ne correspond pas au total de " + nbMembresTotal + " membres.",
                HttpStatus.BAD_REQUEST);
    }
}
