package com.samanatteu.exception;

import org.springframework.http.HttpStatus;

import com.samanatteu.enums.StatutCycle;

public class CycleNonEnCoursException extends SamanatteuException {
    public CycleNonEnCoursException(StatutCycle statut) {
        super("Seul un cycle EN_COURS peut être clôturé (statut actuel : " + statut + ").",
                HttpStatus.CONFLICT);
    }
}
