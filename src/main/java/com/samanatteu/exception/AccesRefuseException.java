package com.samanatteu.exception;

import org.springframework.http.HttpStatus;

// Levée par UtilisateurService.updateUtilisateur quand la personne connectée
// n'est ni le propriétaire du profil visé ni un ADMIN (voir la règle "propre
// profil" ajoutée dans updateUtilisateur).
public class AccesRefuseException extends SamanatteuException {
    public AccesRefuseException(){
        super("Vous n'avez pas le droit de modifier ce profil.", HttpStatus.FORBIDDEN); // 403
    }
}
