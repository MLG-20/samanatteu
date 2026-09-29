package com.samanatteu.exception.tontine;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

public class CycleIntrouvableException extends SamanatteuException {
    public CycleIntrouvableException() {
        super("Cycle introuvable.", HttpStatus.NOT_FOUND);
    }
}
