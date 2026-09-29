package com.samanatteu.exception.tontine;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

public class NombreCyclesAtteintException extends SamanatteuException {
    public NombreCyclesAtteintException(Integer nbCycles) {
        super("Les " + nbCycles + " cycles prévus pour cette tontine ont déjà été ouverts.",
                HttpStatus.CONFLICT);
    }
}
