package com.samanatteu.exception.abonnement;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

// La gestionnaire n'a pas d'abonnement actif : elle ne peut pas commencer une
// nouvelle activité (créer une tontine, inviter des membres, importer).
//
// 403 : c'est un droit manquant, pas une erreur de saisie. Les tontines DÉJÀ
// créées continuent de fonctionner (les membres ont de l'argent en jeu).
public class AbonnementInactifException extends SamanatteuException {
    public AbonnementInactifException(String telephone) {
        super("Le compte " + telephone + " n'a pas d'abonnement actif.", HttpStatus.FORBIDDEN);
    }
}
