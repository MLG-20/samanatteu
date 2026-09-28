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

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    // Ce filtre ne refuse jamais rien lui-même : il authentifie la requête si le token est
    // valide, et dans tous les cas la laisse continuer. C'est SecurityConfig qui décide
    // ensuite si la route exige d'être connecté ou d'avoir un rôle.
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String autHeader = request.getHeader("Authorization");

        if (autHeader != null && autHeader.startsWith("Bearer ")) {
            String token = autHeader.substring(7);

            // Sans ce try/catch, une JwtException (token falsifié, expiré, mal formé)
            // remonterait jusqu'au conteneur : GlobalExceptionHandler ne voit que les
            // contrôleurs, pas les filtres, et le client recevrait une erreur vide.
            try {
                // Seul un access token authentifie une requête : un refresh token (7 jours)
                // ne doit servir qu'à /auth/refresh.
                if ("access".equals(jwtUtil.extractType(token))) {
                    String telephone = jwtUtil.extractTelephone(token);
                    SimpleGrantedAuthority authorite = new SimpleGrantedAuthority("ROLE_" + jwtUtil.extractRole(token));
                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                            telephone, null, List.of(authorite));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            } catch (io.jsonwebtoken.JwtException e) {
                // Token invalide : la requête continue en anonyme.
            }
            filterChain.doFilter(request, response);

        } else {
            filterChain.doFilter(request, response);
            return;
        }

    }

}
