package com.samanatteu.handler;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.samanatteu.exception.SamanatteuException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(SamanatteuException.class)
    public ResponseEntity<String> handlerSamanatteuException(SamanatteuException e) {
        return ResponseEntity.status(e.getStatut()).body(e.getMessage());
    }

    // Échec d'une annotation Bean Validation sur un DTO reçu avec @Valid :
    // on renvoie le détail champ par champ, pour que le client sache
    // exactement quoi corriger.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handlerValidationException(MethodArgumentNotValidException e) {
        Map<String, String> erreurs = new HashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(erreur -> erreurs.put(erreur.getField(), erreur.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erreurs);
    }

    // Filet pour les bugs imprévus (Spring choisit toujours le handler le
    // plus précis : une SamanatteuException n'arrive jamais ici).
    // On ne renvoie JAMAIS e.getMessage() : il peut contenir des détails
    // internes (table SQL, chemin de fichier) — OWASP A05.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handlerExceptionGenerique(Exception e) {
        // Erreurs CLIENT signalées par Spring (route inexistante 404,
        // mauvaise méthode 405…) : elles implémentent ErrorResponse, qui
        // connaît le bon code. Sans ce test, le filet les changeait en 500.
        // instanceof avec variable (Java 16+) : teste le type ET caste.
        if (e instanceof ErrorResponse erreurSpring) {
            return ResponseEntity.status(erreurSpring.getStatusCode())
                    .body("Requête invalide : " + erreurSpring.getStatusCode());
        }

        logger.error("Erreur inattendue", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Une erreur inattendue est survenue.");
    }

}
