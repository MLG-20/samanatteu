package com.samanatteu.exception.utilisateur;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

// L'utilisateur visé par une action n'existe pas.
public class UtilisateurIntrouvableException extends SamanatteuException {
    public UtilisateurIntrouvableException(Long id) {
        super("Aucun utilisateur ne porte l'identifiant " + id, HttpStatus.NOT_FOUND);
    }
}
