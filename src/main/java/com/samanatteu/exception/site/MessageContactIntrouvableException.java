package com.samanatteu.exception.site;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

// Le message de contact demandé n'existe pas (ou a déjà été supprimé).
public class MessageContactIntrouvableException extends SamanatteuException {
    public MessageContactIntrouvableException(Long id) {
        super("Aucun message ne porte l'identifiant " + id, HttpStatus.NOT_FOUND);
    }
}
