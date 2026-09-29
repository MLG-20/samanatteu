package com.samanatteu.exception.cotisation;

import java.math.BigDecimal;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

public class MontantVerseSuperieurAuDisponibleException extends SamanatteuException{
     public MontantVerseSuperieurAuDisponibleException(BigDecimal montant, BigDecimal disponible) {
        super("Le montant à verser (" + montant + ") dépasse l'argent disponible en caisse ("
                + disponible + ").", HttpStatus.BAD_REQUEST);
    }
}
