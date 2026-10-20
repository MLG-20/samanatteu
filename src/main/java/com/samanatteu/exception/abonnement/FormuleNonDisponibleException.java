package com.samanatteu.exception.abonnement;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

// La formule existe mais n'est plus proposée à la vente (retirée par l'ADMIN).
// On ne peut pas activer un abonnement dessus.
public class FormuleNonDisponibleException extends SamanatteuException {
    public FormuleNonDisponibleException(String nom) {
        super("La formule " + nom + " n'est plus disponible.", HttpStatus.CONFLICT);
    }
}
