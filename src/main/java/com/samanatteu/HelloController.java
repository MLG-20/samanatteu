package com.samanatteu;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/*
@RestController — dit à Spring "cette classe contient 
des méthodes qui répondent directement à des requêtes HTTP, 
et retournent des données (pas des pages HTML)".
 */
@RestController
public class HelloController {
    //dit "quand une requête GET arrive sur l'URL /hello, exécute cette méthode"
    @GetMapping("/hello")
    public String direBonjour(){
        return "Bonjour depuis samanatteu !";
    }
}
