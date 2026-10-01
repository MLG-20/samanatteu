package com.samanatteu.exception.pret;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

public class PretDejaRembourseException extends SamanatteuException {
    public PretDejaRembourseException() {
        super("Ce prêt est déjà entièrement remboursé.", HttpStatus.CONFLICT);
    }
}
