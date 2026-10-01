package com.samanatteu.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import com.samanatteu.entity.utilisateur.Utilisateur;
import com.samanatteu.enums.utilisateur.RoleUtilisateur;

// Teste le filtre avec de VRAIS tokens signés par un vrai JwtUtil (sans Spring, sans base).
// Les tests MockMvc des contrôleurs simulent l'utilisateur avec @WithMockUser et ne traversent
// jamais ce filtre : ils ne pouvaient pas voir qu'un refresh token y était accepté.
class JwtAuthFilterTest {

    private JwtUtil jwtUtil;
    private JwtAuthFilter filtre;
    private Utilisateur membre;

    @BeforeEach
    void preparer() {
        // Hors de Spring, personne n'injecte les @Value ni n'appelle le @PostConstruct :
        // on le fait à la main.
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "une-cle-de-test-d-au-moins-trente-deux-octets");
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 900_000L);
        ReflectionTestUtils.setField(jwtUtil, "refreshExpirationMs", 604_800_000L);
        ReflectionTestUtils.invokeMethod(jwtUtil, "init");

        filtre = new JwtAuthFilter(jwtUtil);

        membre = new Utilisateur();
        membre.setTelephone("771234566");
        membre.setRole(RoleUtilisateur.MEMBRE);
    }

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
    }

    // Passe une requête portant ce token dans le filtre, et renvoie la chaîne pour vérifier
    // que la requête a bien continué.
    private MockFilterChain filtrerAvec(String token) throws Exception {
        MockHttpServletRequest requete = new MockHttpServletRequest();
        requete.addHeader("Authorization", "Bearer " + token);
        MockFilterChain chaine = new MockFilterChain();
        filtre.doFilter(requete, new MockHttpServletResponse(), chaine);
        return chaine;
    }

    @Test
    void unAccessTokenAuthentifieLaRequete() throws Exception {
        filtrerAvec(jwtUtil.generateToken(membre));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("771234566", auth.getName());
        assertEquals("ROLE_MEMBRE", auth.getAuthorities().iterator().next().getAuthority());
    }

    // La faille corrigée : un refresh token (valable 7 jours) ne doit pas servir d'access token.
    @Test
    void unRefreshTokenNAuthentifiePasLaRequete() throws Exception {
        MockFilterChain chaine = filtrerAvec(jwtUtil.generateRefreshToken(membre));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        // La requête continue quand même, en anonyme : c'est SecurityConfig qui renverra 401.
        assertNotNull(chaine.getRequest());
    }

    @Test
    void unTokenFalsifieNAuthentifiePasLaRequete() throws Exception {
        String token = jwtUtil.generateToken(membre);
        String falsifie = token.substring(0, token.length() - 2) + "xx";

        MockFilterChain chaine = filtrerAvec(falsifie);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chaine.getRequest());
    }
}
