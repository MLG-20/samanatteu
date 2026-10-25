package com.samanatteu.exception.abonnement;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

// Une formule sans prix (0 = « sur devis ») ne s'achète pas en ligne : on passe
// par la page Contact. Dès que l'ADMIN lui fixe un prix, elle devient choisissable.
public class FormuleSurDevisException extends SamanatteuException {
    public FormuleSurDevisException(String nom) {
        super("La formule " + nom + " est sur devis : écrivez-nous depuis la page Contact.", HttpStatus.BAD_REQUEST);
    }
}
