package com.samanatteu.exception;

import org.springframework.http.HttpStatus;

import com.samanatteu.enums.StatutTontine;

public class TransitionStatutInvalideException extends SamanatteuException {
    public TransitionStatutInvalideException(StatutTontine actuel, StatutTontine voulu){
        super("Impossible de passer la tontine de " + actuel + " à " + voulu + ".", HttpStatus.CONFLICT);
    }
}
