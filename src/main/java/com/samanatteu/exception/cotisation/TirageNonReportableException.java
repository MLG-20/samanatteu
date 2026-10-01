package com.samanatteu.exception.cotisation;

import org.springframework.http.HttpStatus;

import com.samanatteu.enums.cotisation.StatutTirage;
import com.samanatteu.exception.SamanatteuException;

public class TirageNonReportableException extends SamanatteuException {
    public TirageNonReportableException(StatutTirage statut) {
        super("Seul un tirage EN_ATTENTE ou PARTIEL peut être reporté (statut actuel : "
                + statut + ").", HttpStatus.CONFLICT);
    }
}
