package com.samanatteu.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.samanatteu.config.SecurityConfig;
import com.samanatteu.dto.tontine.ParticipationDTO;
import com.samanatteu.security.JwtAccessDeniedHandler;
import com.samanatteu.security.JwtAuthentificationEntryPoint;
import com.samanatteu.security.JwtUtil;
import com.samanatteu.service.tontine.ParticipationService;

// Règles d'ACCÈS de SecurityConfig sur /participation (401, 403, 200), pas la logique du service.
// Même montage que TontineControllerSecurityTest : couche web seule, faux service, faux JwtUtil.
@WebMvcTest(ParticipationController.class)
@Import({ SecurityConfig.class, JwtAuthentificationEntryPoint.class, JwtAccessDeniedHandler.class })
class ParticipationControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ParticipationService participationService;

    @MockitoBean
    private JwtUtil jwtUtil;

    // ---------------------------------------------------------------- lecture

    @Test
    void listeDesParticipations_sansToken_donne401() throws Exception {
        mockMvc.perform(get("/participation"))
                .andExpect(status().isUnauthorized());
    }

    // L'admin de la plateforme ne voit pas le contenu des tontines des gestionnaires.
    @Test
    @WithMockUser(username = "770000099", roles = "ADMIN")
    void listeDesParticipations_parUnAdmin_donne403() throws Exception {
        mockMvc.perform(get("/participation"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void listeDesParticipations_parUnGestionnaire_donne200() throws Exception {
        when(participationService.listParticipation()).thenReturn(List.of());

        mockMvc.perform(get("/participation"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "771234566", roles = "MEMBRE")
    void listeDesParticipations_parUnMembre_donne200() throws Exception {
        when(participationService.listParticipation()).thenReturn(List.of());

        mockMvc.perform(get("/participation"))
                .andExpect(status().isOk());
    }

    // --------------------------------------------------------------- écriture

    @Test
    @WithMockUser(username = "771234566", roles = "MEMBRE")
    void inscription_parUnMembre_donne403() throws Exception {
        // Un membre ne s'inscrit pas lui-même : SecurityConfig refuse AVANT de lire le corps.
        mockMvc.perform(post("/participation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "770000099", roles = "ADMIN")
    void inscription_parUnAdmin_donne403() throws Exception {
        mockMvc.perform(post("/participation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void inscription_parUnGestionnaire_passeLaSecurite() throws Exception {
        // Le faux service répond "null" : ce qui compte ici, c'est de ne pas recevoir 401 ni 403.
        mockMvc.perform(post("/participation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "771234566", roles = "MEMBRE")
    void modificationDUneParticipation_parUnMembre_donne403() throws Exception {
        mockMvc.perform(put("/participation/9")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void modificationDUneParticipation_parUnGestionnaire_donne200() throws Exception {
        when(participationService.updateParticipation(eq(9L), any()))
                .thenReturn(Optional.of(new ParticipationDTO()));

        mockMvc.perform(put("/participation/9")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "771234566", roles = "MEMBRE")
    void suppressionDUneParticipation_parUnMembre_donne403() throws Exception {
        mockMvc.perform(delete("/participation/9"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void suppressionDUneParticipation_parUnGestionnaire_donne204() throws Exception {
        when(participationService.deleteParticipation(9L)).thenReturn(true);

        mockMvc.perform(delete("/participation/9"))
                .andExpect(status().isNoContent());
    }
}
