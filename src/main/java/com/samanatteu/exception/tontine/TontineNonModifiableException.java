package com.samanatteu.exception.tontine;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

public class TontineNonModifiableException extends SamanatteuException {
    public TontineNonModifiableException(String statut){
        super("La tontine est " + statut + " : elle ne peut plus être modifiée.", HttpStatus.CONFLICT);
    }
}
