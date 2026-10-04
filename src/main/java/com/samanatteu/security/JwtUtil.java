package com.samanatteu.security;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.samanatteu.entity.utilisateur.Utilisateur;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    String secret;

    @Value("${jwt.expiration-ms}")
    long expirationMs;

    @Value("${jwt.refresh-expiration-ms}")
    long refreshExpirationMs;

    private SecretKey key;

    // La clé est calculée ici et pas à la déclaration du champ : pendant la construction
    // de l'objet, les @Value ne sont pas encore injectés et "secret" vaut null.
    @PostConstruct
    private void init() {
        key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    // L'identité (subject) est le téléphone, toujours présent ; l'email est optionnel.
    public String generateToken(Utilisateur utilisateur) {
        return Jwts.builder()
                .subject(utilisateur.getTelephone())
                .claim("role", utilisateur.getRole())
                .claim("type", "access")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(Utilisateur utilisateur) {
        return Jwts.builder()
                .subject(utilisateur.getTelephone())
                // Marque le token comme "refresh" pour qu'il ne soit pas confondu avec
                // un access token.
                .claim("type", "refresh")
                // Numéro de version du compte au moment de la fabrication : comparé à
                // celui de la base lors du refresh, pour refuser un token révoqué.
                .claim("version", utilisateur.getVersionSessions())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpirationMs))
                .signWith(key)
                .compact();
    }

    // parseSignedClaims vérifie la signature ET l'expiration : un token invalide ou expiré
    // lève une JwtException, à attraper par l'appelant.
    public String extractTelephone(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public String extractRole(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);
    }

    public String extractType(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("type", String.class);
    }

    // Integer et non int : un token fabriqué avant l'ajout de ce claim renvoie null.
    public Integer extractVersion(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("version", Integer.class);
    }

}
