package com.samanatteu.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.samanatteu.config.SecurityConfig;
import com.samanatteu.security.JwtAccessDeniedHandler;
import com.samanatteu.security.JwtAuthentificationEntryPoint;
import com.samanatteu.security.JwtUtil;
import com.samanatteu.service.auth.AuthService;

// Règles d'ACCÈS sur /auth/mot-de-passe : il faut être connecté, quel que soit le
// rôle (la route n'a pas de règle propre, elle tombe sous anyRequest().authenticated()).
// Même montage que les autres *ControllerSecurityTest.
@WebMvcTest(AuthController.class)
@Import({ SecurityConfig.class, JwtAuthentificationEntryPoint.class, JwtAccessDeniedHandler.class })
class AuthControllerSecurityTest {

    private static final String CORPS_VALIDE = """
            {"ancienMotDePasse": "motdepasse123", "nouveauMotDePasse": "nouveaupasse456"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    void changerMotDePasse_sansToken_donne401() throws Exception {
        mockMvc.perform(put("/auth/mot-de-passe").contentType(MediaType.APPLICATION_JSON).content(CORPS_VALIDE))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(authService);
    }

    // Tout utilisateur connecté change SON mot de passe : aucun rôle n'est exclu.
    @ParameterizedTest
    @ValueSource(strings = { "MEMBRE", "GESTIONNAIRE", "ADMIN" })
    void changerMotDePasse_connecte_donne204(String role) throws Exception {
        mockMvc.perform(put("/auth/mot-de-passe").contentType(MediaType.APPLICATION_JSON).content(CORPS_VALIDE)
                .with(user("771234566").roles(role)))
                .andExpect(status().isNoContent());

        verify(authService).changerMotDePasse(any());
    }

    // Même règle qu'à l'inscription (8 caractères), avec un message lisible.
    @Test
    @WithMockUser(username = "771234566", roles = "MEMBRE")
    void changerMotDePasse_nouveauTropCourt_donne400AvecUnMessageLisible() throws Exception {
        mockMvc.perform(put("/auth/mot-de-passe").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"ancienMotDePasse": "motdepasse123", "nouveauMotDePasse": "abc"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.nouveauMotDePasse")
                        .value("Le mot de passe doit contenir au moins 8 caractères."));

        verifyNoInteractions(authService);
    }

    @Test
    @WithMockUser(username = "771234566", roles = "MEMBRE")
    void changerMotDePasse_sansAncienMotDePasse_donne400() throws Exception {
        mockMvc.perform(put("/auth/mot-de-passe").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nouveauMotDePasse": "nouveaupasse456"}
                        """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    // La route est en PUT (on remplace une chose qui existe), pas en POST.
    @Test
    @WithMockUser(username = "771234566", roles = "MEMBRE")
    void changerMotDePasse_enPost_donne405() throws Exception {
        mockMvc.perform(post("/auth/mot-de-passe").contentType(MediaType.APPLICATION_JSON).content(CORPS_VALIDE))
                .andExpect(status().isMethodNotAllowed());
    }
}
