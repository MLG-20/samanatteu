package com.samanatteu.exception.tontine;

import org.springframework.http.HttpStatus;

import com.samanatteu.enums.tontine.StatutCycle;
import com.samanatteu.exception.SamanatteuException;

public class CycleNonEnCoursException extends SamanatteuException {
    public CycleNonEnCoursException(StatutCycle statut) {
        super("Seul un cycle EN_COURS peut être clôturé (statut actuel : " + statut + ").",
                HttpStatus.CONFLICT);
    }
}
