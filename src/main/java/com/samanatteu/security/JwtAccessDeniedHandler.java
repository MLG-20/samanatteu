package com.samanatteu.security;

import java.io.IOException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// AccessDeniedHandler : appelé par Spring Security quand la requête EST bien
// authentifiée (token valide) mais que le rôle de l'utilisateur ne suffit pas
// pour la route demandée (ex: un MEMBRE qui tape une route réservée à hasRole("ADMIN")).
// Sans cette classe, ce cas tombe par défaut sur un forward interne vers /error
// que JwtAuthFilter ignore, et la requête finit par ressortir en 401 au lieu de 403.
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {

        // 403 Forbidden : "on sait qui tu es, mais tu n'as pas le droit" (à
        // distinguer du 401 de JwtAuthentificationEntryPoint = "on ne sait pas qui tu es").
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json; charset=UTF-8");
        response.getWriter().write("{\"message\":\"Accès refusé : vous n'avez pas les droits pour cette action\"}");
    }
}
