package com.samanatteu.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// @Component : Spring doit gérer cette classe pour l'injecter dans SecurityConfig.
// OncePerRequestFilter : classe de base Spring Security, garantit que ce filtre
// s'exécute une seule fois par requête HTTP.
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    // Dépendance : le filtre a besoin de JwtUtil pour lire/vérifier le token.
    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    // Méthode appelée automatiquement par Spring Security sur CHAQUE requête,
    // avant qu'elle n'atteigne le controller.
    // request : la requête entrante (on y lit le header "Authorization")
    // response : la réponse HTTP (pas touchée ici)
    // filterChain : représente "la suite du traitement" -> il faut TOUJOURS
    // appeler filterChain.doFilter(request, response), sinon la requête reste
    // bloquée indéfiniment.
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // Le header ressemble à "Bearer xxx.yyy.zzz" s'il est présent.
        String autHeader = request.getHeader("Authorization");

        if (autHeader != null && autHeader.startsWith("Bearer ")) {
            // "Bearer " fait 7 caractères -> substring(7) garde uniquement le vrai token.
            String token = autHeader.substring(7);

            // try/catch : extractEmail lève une JwtException si le token est invalide
            // (signature falsifiée, expiré, mal formé). Sans ce try/catch, l'exception
            // remonterait jusqu'au conteneur, court-circuiterait GlobalExceptionHandler
            // (qui ne voit que les controllers, pas les filtres) et donnerait un 403 vide.
            try {
                // Vérifie la signature + l'expiration (dans JwtUtil), et renvoie l'email
                // contenu dans le token si tout est valide.
                String email = jwtUtil.extractEmail(token);

                // Construit l'objet qui dit à Spring Security "cette requête est authentifiée".
                // 1er argument : l'identité (l'email) 2e : le mot de passe (null, pas besoin,
                // on a déjà vérifié via le token) 3e : les rôles/autorités (vide pour
                // l'instant, géré plus tard à l'étape autorisation par rôle).
                SimpleGrantedAuthority authorite = new SimpleGrantedAuthority("ROLE_" + jwtUtil.extractRole(token));
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(email,
                        null, List.of(authorite));

                // Dépose l'authentification dans le contexte de sécurité de la requête en
                // cours.
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            } catch (io.jsonwebtoken.JwtException e) {
                // Le token est invalide ou expiré : on n'authentifie PAS la requête, et on
                // n'interrompt rien ici. Elle continue en anonyme, et c'est SecurityConfig
                // qui décide ensuite : route publique (ex: /auth/login) -> elle passe,
                // route protégée -> elle est refusée.
            }

            // Laisse la requête continuer : authentifiée si le token était valide,
            // anonyme s'il était invalide.
            filterChain.doFilter(request, response);

        } else {
            // Pas de token présent ou mal formé : on laisse quand même la requête
            // continuer, mais SANS authentification. C'est SecurityConfig (règles
            // d'accès) qui décidera ensuite si la route exige d'être connecté.
            filterChain.doFilter(request, response);
            return;
        }

    }

}
