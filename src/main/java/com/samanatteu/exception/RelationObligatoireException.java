package com.samanatteu.exception;

import org.springframework.http.HttpStatus;

public class RelationObligatoireException extends SamanatteuException {
    public RelationObligatoireException(String nomChamp) {
        super("Le champ " + nomChamp + " est obligatoire.", HttpStatus.BAD_REQUEST);
    }
}
