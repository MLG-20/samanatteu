package com.samanatteu.exception.cotisation;

import java.math.BigDecimal;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

public class MontantInvalideException extends SamanatteuException{
    public MontantInvalideException(BigDecimal montant){
        super("Le montant " + montant + " est invalide (doit être positif).", HttpStatus.BAD_REQUEST);
    }
}
