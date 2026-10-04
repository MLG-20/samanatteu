package com.samanatteu.exception.auth;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

// Un seul message pour tous les refus (numéro inconnu, code faux, expiré, déjà
// utilisé, trop d'essais) : un message précis renseignerait celui qui devine.
public class CodeInvalideException extends SamanatteuException {
    public CodeInvalideException() {
        super("Code invalide ou expiré. Demandez un nouveau code.", HttpStatus.BAD_REQUEST);
    }
}
