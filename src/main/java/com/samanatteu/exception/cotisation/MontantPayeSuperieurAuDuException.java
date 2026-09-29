package com.samanatteu.exception.cotisation;

import java.math.BigDecimal;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

public class MontantPayeSuperieurAuDuException extends SamanatteuException{
    public MontantPayeSuperieurAuDuException(BigDecimal montantPaye, BigDecimal montantDu){
        super("Le montant payé (" + montantPaye + ") dépasse le montant dû (" + montantDu + ").", HttpStatus.BAD_REQUEST);
    }
}
