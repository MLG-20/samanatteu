package com.samanatteu.exception;

import org.springframework.http.HttpStatus;

public class InscriptionsFermeesException extends SamanatteuException{
    public InscriptionsFermeesException(String statut){
        super("la tontine est " + statut + " : les inscription sont closes.", HttpStatus.CONFLICT);
    }
}
