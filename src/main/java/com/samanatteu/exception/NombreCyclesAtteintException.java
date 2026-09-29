package com.samanatteu.exception;

import org.springframework.http.HttpStatus;

public class NombreCyclesAtteintException extends SamanatteuException {
    public NombreCyclesAtteintException(Integer nbCycles) {
        super("Les " + nbCycles + " cycles prévus pour cette tontine ont déjà été ouverts.",
                HttpStatus.CONFLICT);
    }
}
