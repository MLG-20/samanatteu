package com.samanatteu.exception.auth;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

// Un seul message pour tous les refus (jeton inconnu, expiré, déjà cliqué, compte
// déjà vérifié), comme CodeInvalideException : la réponse ne renseigne pas celui
// qui essaie des jetons au hasard.
public class LienVerificationInvalideException extends SamanatteuException {
    public LienVerificationInvalideException() {
        super("Ce lien n'est plus valable. Si votre adresse est déjà confirmée, connectez-vous ; "
                + "sinon, demandez un nouvel email.", HttpStatus.BAD_REQUEST);
    }
}
