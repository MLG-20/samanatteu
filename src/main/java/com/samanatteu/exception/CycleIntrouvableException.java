package com.samanatteu.exception;

import org.springframework.http.HttpStatus;

public class CycleIntrouvableException extends SamanatteuException {
    public CycleIntrouvableException() {
        super("Cycle introuvable.", HttpStatus.NOT_FOUND);
    }
}
