package com.samanatteu.exception.tontine;

import org.springframework.http.HttpStatus;

import com.samanatteu.enums.tontine.StatutTontine;
import com.samanatteu.exception.SamanatteuException;

public class TontineNonActiveException extends SamanatteuException {
    public TontineNonActiveException(StatutTontine statut) {
        super("La tontine doit être ACTIVE pour ouvrir un cycle (statut actuel : " + statut + ").",
                HttpStatus.CONFLICT);
    }
}
