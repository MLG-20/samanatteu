package com.samanatteu.security;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// AuthenticationEntryPoint : appelé par Spring Security quand une requête non
// authentifiée (pas de token, token invalide/expiré) tape sur une route protégée.
// Sans cette classe, Spring renvoie une page d'erreur HTML par défaut ; ici on
// personnalise pour renvoyer un JSON propre avec le code 401.
@Component
public class JwtAuthentificationEntryPoint implements AuthenticationEntryPoint {

    // commence() = point d'entrée appelé automatiquement par Spring Security
    // (pas nous) dès qu'un accès est refusé faute d'authentification.
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {

        // 401 Unauthorized : "on ne sait pas qui tu es" (à distinguer du 403
        // Forbidden de JwtAccessDeniedHandler = "on sait qui tu es, mais interdit").
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json; charset=UTF-8");
        response.getWriter().write("{\"message\":\"Authentification requise : token manquant, invalide ou expiré\"}");
    }
}
