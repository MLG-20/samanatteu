package com.samanatteu.security;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.samanatteu.entity.Utilisateur;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

// @Component : Spring doit gérer cette classe (l'instancier, l'injecter ailleurs) pour que
// @PostConstruct et @Value fonctionnent dessus.
@Component
public class JwtUtil {

    // @Value("${...}") : Spring va chercher cette clé dans application.yaml et
    // remplit
    // automatiquement le champ juste après la construction de l'objet.
    @Value("${jwt.secret}")
    String secret;

    @Value("${jwt.expiration-ms}")
    long expirationMs;

    // La clé cryptographique utilisée pour signer/vérifier les tokens.
    // Pas initialisée ici directement : au moment de la construction de l'objet,
    // "secret"
    // vaut encore null (Spring ne l'a pas encore injecté) -> calcul déplacé dans
    // init().
    private SecretKey key;

    // @PostConstruct : Spring appelle cette méthode juste APRÈS avoir injecté tous
    // les @Value.
    // C'est le bon endroit pour calculer "key" à partir de "secret", qui est
    // garanti non-null
    // ici.
    @PostConstruct
    private void init() {
        // Keys.hmacShaKeyFor(byte[]) transforme le secret (texte) en une vraie clé
        // cryptographique HMAC, le format attendu par JJWT pour signer un token.
        key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    // Génère un JWT signé pour cet utilisateur, utilisé à la connexion
    // (/auth/login).
    public String generateToken(Utilisateur utilisateur) {

        return Jwts.builder()
                // subject : le "propriétaire" du token, ici son email (identifiant de
                // connexion).
                .subject(utilisateur.getEmail())
                // claim : donnée custom ajoutée au payload, ici le rôle (utile plus tard pour
                // restreindre certaines routes par rôle).
                .claim("role", utilisateur.getRole())
                // issuedAt : date de création du token, maintenant.
                .issuedAt(new Date())
                // expiration : maintenant + la durée de validité définie dans application.yaml.
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                // signWith : signe le token avec la clé secrète -> garantit que personne ne
                // peut
                // le fabriquer ou le modifier sans connaître "secret".
                .signWith(key)
                // compact : assemble tout en la chaîne finale "xxx.yyy.zzz" renvoyée au client.
                .compact();
    }

    // Lit un token reçu (ex: dans le header Authorization d'une requête) et renvoie
    // l'email qu'il contient, à condition que le token soit valide.
    public String extractEmail(String token) {
        return Jwts.parser()
                // verifyWith : indique avec quelle clé vérifier la signature -> doit être
                // la MÊME clé que celle utilisée pour signer (signWith) dans generateToken.
                .verifyWith(key)
                // build : termine la construction du parseur.
                .build()
                // parseSignedClaims : lit le token donné en argument, vérifie la signature
                // ET l'expiration automatiquement. Si le token est invalide ou expiré,
                // cette ligne lève elle-même une exception (JwtException) -> on n'a pas
                // besoin de vérifier ça nous-mêmes.
                .parseSignedClaims(token)
                // getPayload : récupère le contenu (les "claims") du token, une fois vérifié.
                .getPayload()
                // getSubject : extrait précisément le champ "subject", ici l'email qu'on
                // avait mis avec .subject(...) au moment de la génération.
                .getSubject();

    }

    public String extractRole(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                // get("role", String.class) : lit le claim personnalisé "role" écrit dans
                // generateToken (.claim("role", ...)), en le relisant comme texte.
                // Contrairement à getSubject(), il n'y a pas de méthode dédiée pour "role"
                // car c'est un claim que nous avons inventé, d'où cette méthode générale.
                .get("role", String.class);
    }
}
