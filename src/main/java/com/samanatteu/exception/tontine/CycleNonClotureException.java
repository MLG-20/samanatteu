package com.samanatteu.exception.tontine;

import org.springframework.http.HttpStatus;

import com.samanatteu.enums.StatutCycle;
import com.samanatteu.exception.SamanatteuException;

public class CycleNonClotureException extends SamanatteuException {
    public CycleNonClotureException(StatutCycle statut) {
        super("Le tirage n'est possible que sur un cycle CLOTURE (statut actuel : "
                + statut + ").", HttpStatus.CONFLICT);
    }
}
