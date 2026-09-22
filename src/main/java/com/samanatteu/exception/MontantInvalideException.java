package com.samanatteu.exception;

import java.math.BigDecimal;

import org.springframework.http.HttpStatus;

public class MontantInvalideException extends SamanatteuException{
    public MontantInvalideException(BigDecimal montant){
        super("Le montant " + montant + " est invalide (doit être positif).", HttpStatus.BAD_REQUEST);//erreur 400
    }
}
