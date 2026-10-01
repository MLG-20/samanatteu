package com.samanatteu.exception.pret;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

// 400 : c'est la demande qui est fausse (membre absent de la tontine,
// inconnu, SUSPENDU ou SORTI), pas l'état du serveur.
public class MembreNonParticipantException extends SamanatteuException {
    public MembreNonParticipantException(Long membreId) {
        super("Le membre " + membreId + " ne participe pas (ou plus) à cette tontine.",
                HttpStatus.BAD_REQUEST);
    }
}

