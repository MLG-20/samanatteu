package com.samanatteu.handler;


import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.samanatteu.exception.SamanatteuException;

// @RestControllerAdvice = cette classe est un point central qui surveille TOUS les Controllers
// de l'application. Pas besoin de try/catch dans chaque méthode de chaque Controller :
// Spring redirige automatiquement ici toute exception levée pendant le traitement d'une requête.
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);


    @ExceptionHandler(SamanatteuException.class)
    public ResponseEntity<String> handlerSamanatteuException(SamanatteuException e){
        return ResponseEntity.status(e.getStatut()).body(e.getMessage());
    }

    // Se déclenche quand une annotation Bean Validation (@NotBlank, @Email, @Size...)
    // échoue sur un DTO reçu avec @Valid. Spring lève ce type d'exception AVANT même
    // que le code du Controller ne s'exécute.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handlerValidationException(MethodArgumentNotValidException e) {
        // erreurs va contenir, pour chaque champ invalide, la paire "nom du champ" -> "message d'erreur".
        // Ex: "email" -> "must be a well-formed email address"
        Map<String, String> erreurs = new HashMap<>();

        // getBindingResult() = l'objet qui contient le détail de TOUTES les violations trouvées.
        // getFieldErrors() = la liste des champs en échec (un utilisateur peut violer plusieurs
        // règles à la fois, ex: nom vide ET email invalide en même temps).
        // "erreur" (le paramètre de la lambda) représente UN champ fautif à la fois ; pour chacun,
        // on ajoute une entrée dans la Map avec .put(clé, valeur).
        e.getBindingResult().getFieldErrors().forEach(erreur ->
            erreurs.put(erreur.getField(), erreur.getDefaultMessage())
        );

        // On renvoie un 400 (mauvaise requête côté client) avec le détail de chaque champ fautif,
        // pour que le client sache exactement quoi corriger — contrairement au filet de sécurité
        // générique plus bas, qui cache volontairement les détails techniques.
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erreurs);
    }

    // Filet de sécurité : Exception (java.lang.Exception) est la classe mère de TOUTE
    // exception Java, y compris celles qu'on n'a pas prévues (NullPointerException,
    // erreur SQL...). Spring choisit toujours le handler le PLUS PRÉCIS pour le type levé :
    // une SamanatteuException ira dans handlerSamanatteuException ci-dessus, pas ici.
    // Cette méthode ne s'active que pour tout le reste, les bugs imprévus.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handlerExceptionGenerique(Exception e) {
        logger.error("Erreur inattendue", e);
        // On ne renvoie JAMAIS e.getMessage() ici : un bug imprévu peut contenir des
        // détails techniques internes (nom de table SQL, chemin de fichier...) qu'il ne
        // faut pas exposer au client (OWASP A05:2021 - fuite d'informations sensibles via
        // une erreur mal gérée, déjà vu sur l'exposition du motDePasse).
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Une erreur inattendue est survenue.");
    }

}
