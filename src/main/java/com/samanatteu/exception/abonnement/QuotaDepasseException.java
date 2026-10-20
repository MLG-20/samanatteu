package com.samanatteu.exception.abonnement;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

// La formule est atteinte : la gestionnaire a déjà autant de tontines (ou de
// membres) que sa formule en permet.
//
// 403 comme pour un abonnement inactif : c'est la même famille de refus
// (le compte n'a pas le droit de faire cette action).
public class QuotaDepasseException extends SamanatteuException {
    public QuotaDepasseException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
