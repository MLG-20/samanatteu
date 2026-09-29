package com.samanatteu.exception.tontine;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

public class RelationObligatoireException extends SamanatteuException {
    public RelationObligatoireException(String nomChamp) {
        super("Le champ " + nomChamp + " est obligatoire.", HttpStatus.BAD_REQUEST);
    }
}
