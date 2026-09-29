package com.samanatteu.exception;

import org.springframework.http.HttpStatus;

public class CycleDejaEnCoursException extends SamanatteuException {
    public CycleDejaEnCoursException() {
        super("Un cycle est déjà en cours pour cette tontine : clôturez-le avant d'en ouvrir un autre.",
                HttpStatus.CONFLICT);
    }
}
