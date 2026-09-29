package com.samanatteu.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

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
import com.samanatteu.dto.cotisation.CotisationDTO;
import com.samanatteu.security.JwtAccessDeniedHandler;
import com.samanatteu.security.JwtAuthentificationEntryPoint;
import com.samanatteu.security.JwtUtil;
import com.samanatteu.service.cotisation.CotisationService;

// Règles d'ACCÈS de SecurityConfig sur /cotisation (lecture GESTIONNAIRE ou MEMBRE, paiement et
// suppression réservés au GESTIONNAIRE) + validation @Valid du PaiementDTO (400 avant le service).
// Même montage que ParticipationControllerSecurityTest.
@WebMvcTest(CotisationController.class)
@Import({ SecurityConfig.class, JwtAuthentificationEntryPoint.class, JwtAccessDeniedHandler.class })
class CotisationControllerSecurityTest {

    private static final String PAIEMENT_VALIDE = "{\"montant\": 5000, \"modePaiement\": \"WAVE\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CotisationService cotisationService;

    @MockitoBean
    private JwtUtil jwtUtil;

    // ---------------------------------------------------------------- lecture

    @Test
    void listeDesCotisations_sansToken_donne401() throws Exception {
        mockMvc.perform(get("/cotisation"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "770000099", roles = "ADMIN")
    void listeDesCotisations_parUnAdmin_donne403() throws Exception {
        mockMvc.perform(get("/cotisation"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "771234566", roles = "MEMBRE")
    void listeDesCotisations_parUnMembre_donne200() throws Exception {
        when(cotisationService.listCotisations()).thenReturn(List.of());

        mockMvc.perform(get("/cotisation"))
                .andExpect(status().isOk());
    }

    // --------------------------------------------------------------- paiement

    @Test
    void paiement_sansToken_donne401() throws Exception {
        mockMvc.perform(post("/cotisation/5/paiement")
                .contentType(MediaType.APPLICATION_JSON)
                .content(PAIEMENT_VALIDE))
                .andExpect(status().isUnauthorized());
    }

    // Un membre n'enregistre pas lui-même son paiement : c'est le gestionnaire qui l'encaisse.
    @Test
    @WithMockUser(username = "771234566", roles = "MEMBRE")
    void paiement_parUnMembre_donne403() throws Exception {
        mockMvc.perform(post("/cotisation/5/paiement")
                .contentType(MediaType.APPLICATION_JSON)
                .content(PAIEMENT_VALIDE))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "770000099", roles = "ADMIN")
    void paiement_parUnAdmin_donne403() throws Exception {
        mockMvc.perform(post("/cotisation/5/paiement")
                .contentType(MediaType.APPLICATION_JSON)
                .content(PAIEMENT_VALIDE))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void paiement_parUnGestionnaire_donne200() throws Exception {
        when(cotisationService.enregistrerPaiement(eq(5L), any())).thenReturn(new CotisationDTO());

        mockMvc.perform(post("/cotisation/5/paiement")
                .contentType(MediaType.APPLICATION_JSON)
                .content(PAIEMENT_VALIDE))
                .andExpect(status().isOk());
    }

    // @Valid : montant absent, nul, négatif ou mode absent → 400, et le service n'est jamais appelé.
    @ParameterizedTest
    @ValueSource(strings = {
            "{\"modePaiement\": \"CASH\"}",
            "{\"montant\": 0, \"modePaiement\": \"CASH\"}",
            "{\"montant\": -500, \"modePaiement\": \"CASH\"}",
            "{\"montant\": 5000}" })
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void paiement_invalide_donne400SansAppelerLeService(String corps) throws Exception {
        mockMvc.perform(post("/cotisation/5/paiement")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corps))
                .andExpect(status().isBadRequest());

        verify(cotisationService, never()).enregistrerPaiement(any(), any());
    }

    // ------------------------------------------------------------ suppression

    @Test
    @WithMockUser(username = "771234566", roles = "MEMBRE")
    void suppressionDUneCotisation_parUnMembre_donne403() throws Exception {
        mockMvc.perform(delete("/cotisation/5"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void suppressionDUneCotisation_parUnGestionnaire_donne204() throws Exception {
        when(cotisationService.deleteCotisation(5L)).thenReturn(true);

        mockMvc.perform(delete("/cotisation/5"))
                .andExpect(status().isNoContent());
    }
}
