package com.samanatteu.exception;

import org.springframework.http.HttpStatus;

public class TontineNonModifiableException extends SamanatteuException {
    public TontineNonModifiableException(String statut){
        super("La tontine est " + statut + " : elle ne peut plus être modifiée.", HttpStatus.CONFLICT);
    }
}
