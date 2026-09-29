package com.samanatteu.exception;

import org.springframework.http.HttpStatus;

import com.samanatteu.enums.StatutTontine;

public class TontineNonActiveException extends SamanatteuException {
    public TontineNonActiveException(StatutTontine statut) {
        super("La tontine doit être ACTIVE pour ouvrir un cycle (statut actuel : " + statut + ").",
                HttpStatus.CONFLICT);
    }
}
