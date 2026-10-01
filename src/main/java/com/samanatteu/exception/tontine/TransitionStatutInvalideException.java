package com.samanatteu.exception.tontine;

import org.springframework.http.HttpStatus;

import com.samanatteu.enums.tontine.StatutTontine;
import com.samanatteu.exception.SamanatteuException;

public class TransitionStatutInvalideException extends SamanatteuException {
    public TransitionStatutInvalideException(StatutTontine actuel, StatutTontine voulu){
        super("Impossible de passer la tontine de " + actuel + " à " + voulu + ".", HttpStatus.CONFLICT);
    }
}
