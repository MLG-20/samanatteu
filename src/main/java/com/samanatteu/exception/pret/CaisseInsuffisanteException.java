package com.samanatteu.exception.pret;

import java.math.BigDecimal;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

// 409 : la demande est valide, mais le solde actuel ne suffit pas.
public class CaisseInsuffisanteException extends SamanatteuException {
    public CaisseInsuffisanteException(BigDecimal demande, BigDecimal disponible) {
        super("Caisse de prêts insuffisante : " + demande + " demandé, "
                + disponible + " disponible.", HttpStatus.CONFLICT);
    }
}
