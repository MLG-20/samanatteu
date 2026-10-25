package com.samanatteu.exception.abonnement;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

// Une gestionnaire s'inscrit par le site public sans choisir de formule : l'essai
// démarrera sur la formule choisie, il en faut une.
public class FormuleObligatoireException extends SamanatteuException {
    public FormuleObligatoireException() {
        super("Choisissez une formule : votre essai gratuit démarrera dessus.", HttpStatus.BAD_REQUEST);
    }
}
