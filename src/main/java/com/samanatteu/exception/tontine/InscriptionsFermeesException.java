package com.samanatteu.exception.tontine;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

public class InscriptionsFermeesException extends SamanatteuException{
    public InscriptionsFermeesException(String statut){
        super("La tontine est " + statut + " : les inscription sont closes.", HttpStatus.CONFLICT);
    }
}
