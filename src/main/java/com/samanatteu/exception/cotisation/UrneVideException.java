package com.samanatteu.exception.cotisation;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

public class UrneVideException extends SamanatteuException {
    public UrneVideException(Long cycleId) {
        super("Aucun membre en lice pour le cycle " + cycleId
                + " : chacun a déjà gagné autant de fois qu'il a de parts.",
                HttpStatus.CONFLICT);
    }
}
