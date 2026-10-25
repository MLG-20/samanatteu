package com.samanatteu.exception.onboarding;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

// Un visiteur tente de s'inscrire comme MEMBRE sans lien d'invitation : un
// membre sans tontine n'aurait rien à faire sur la plateforme.
public class InvitationRequiseException extends SamanatteuException {
    public InvitationRequiseException() {
        super("On devient membre sur invitation : ouvrez le lien envoyé par votre gestionnaire.",
                HttpStatus.FORBIDDEN);
    }
}
