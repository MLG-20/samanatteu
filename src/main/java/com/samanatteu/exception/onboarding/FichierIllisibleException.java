package com.samanatteu.exception.onboarding;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

public class FichierIllisibleException extends SamanatteuException{
    public FichierIllisibleException(){
        super("Le fichier est illisible : envoyez un fichier CSV valide.", HttpStatus.BAD_REQUEST);
    }
}
