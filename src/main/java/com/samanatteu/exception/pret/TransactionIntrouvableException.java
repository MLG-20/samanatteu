package com.samanatteu.exception.pret;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

// Aucune ligne de journal ne porte cet identifiant : le reçu PDF n'existe pas.
public class TransactionIntrouvableException extends SamanatteuException {
    public TransactionIntrouvableException(Long id) {
        super("Aucune operation du journal ne porte l'identifiant " + id, HttpStatus.NOT_FOUND);
    }
}
