package com.samanatteu.exception.abonnement;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

// La formule demandée n'existe pas.
public class FormuleIntrouvableException extends SamanatteuException {
    public FormuleIntrouvableException(Long id) {
        super("Aucune formule ne porte l'identifiant " + id, HttpStatus.NOT_FOUND);
    }
}
