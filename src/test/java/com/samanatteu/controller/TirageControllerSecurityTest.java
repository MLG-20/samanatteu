package com.samanatteu.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.samanatteu.config.SecurityConfig;
import com.samanatteu.dto.cotisation.TirageDTO;
import com.samanatteu.security.JwtAccessDeniedHandler;
import com.samanatteu.security.JwtAuthentificationEntryPoint;
import com.samanatteu.security.JwtUtil;
import com.samanatteu.service.cotisation.TirageService;

// Règles d'ACCÈS de SecurityConfig sur /tirage : lecture GESTIONNAIRE ou MEMBRE
// (pas ADMIN), verser et reporter réservés au GESTIONNAIRE. Même montage que
// CycleControllerSecurityTest. (Le tirage lui-même, POST /cycle/{id}/tirage,
// est testé dans CycleControllerSecurityTest.)
@WebMvcTest(TirageController.class)
@Import({ SecurityConfig.class, JwtAuthentificationEntryPoint.class, JwtAccessDeniedHandler.class })
class TirageControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TirageService tirageService;

    @MockitoBean
    private JwtUtil jwtUtil;

    // ---------------------------------------------------------------- lecture

    @Test
    void listeDesTirages_sansToken_donne401() throws Exception {
        mockMvc.perform(get("/tirage"))
                .andExpect(status().isUnauthorized());
    }

    // L'admin de la plateforme ne voit pas le contenu des tontines des gestionnaires.
    @Test
    @WithMockUser(username = "770000099", roles = "ADMIN")
    void listeDesTirages_parUnAdmin_donne403() throws Exception {
        mockMvc.perform(get("/tirage"))
                .andExpect(status().isForbidden());
    }

    // Transparence (US-M03) : le membre voit les résultats des tirages.
    @Test
    @WithMockUser(username = "771234566", roles = "MEMBRE")
    void listeDesTirages_parUnMembre_donne200() throws Exception {
        when(tirageService.listTirage()).thenReturn(List.of());

        mockMvc.perform(get("/tirage"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void listeDesTirages_parUnGestionnaire_donne200() throws Exception {
        when(tirageService.listTirage()).thenReturn(List.of());

        mockMvc.perform(get("/tirage"))
                .andExpect(status().isOk());
    }

    // ----------------------------------------------------------------- verser

    @Test
    @WithMockUser(username = "771234566", roles = "MEMBRE")
    void versement_parUnMembre_donne403() throws Exception {
        mockMvc.perform(post("/tirage/7/verser")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"montant\": 10000}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "770000099", roles = "ADMIN")
    void versement_parUnAdmin_donne403() throws Exception {
        mockMvc.perform(post("/tirage/7/verser")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"montant\": 10000}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void versement_parUnGestionnaire_donne200() throws Exception {
        when(tirageService.verser(eq(7L), any())).thenReturn(new TirageDTO());

        mockMvc.perform(post("/tirage/7/verser")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"montant\": 10000}"))
                .andExpect(status().isOk());
    }

    // @Valid : un montant négatif est refusé AVANT le service (400).
    @Test
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void versement_montantNegatif_donne400SansAppelerLeService() throws Exception {
        mockMvc.perform(post("/tirage/7/verser")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"montant\": -5000}"))
                .andExpect(status().isBadRequest());

        verify(tirageService, never()).verser(any(), any());
    }

    // --------------------------------------------------------------- reporter

    @Test
    @WithMockUser(username = "771234566", roles = "MEMBRE")
    void report_parUnMembre_donne403() throws Exception {
        mockMvc.perform(post("/tirage/7/reporter"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void report_parUnGestionnaire_donne200() throws Exception {
        when(tirageService.reporter(7L)).thenReturn(new TirageDTO());

        mockMvc.perform(post("/tirage/7/reporter"))
                .andExpect(status().isOk());
    }

    // ------------------------------------------- anciennes portes supprimées
    // Un tirage est un fait historique : ni création manuelle (contourner
    // l'urne), ni modification, ni suppression (« supprimer puis retirer »).
    // Même le gestionnaire propriétaire reçoit une erreur CLIENT (4xx).

    // /tirage existe (GET) mais pas en POST : 405 Method Not Allowed.
    @Test
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void creationManuelleDUnTirage_nExistePlus_donne405() throws Exception {
        mockMvc.perform(post("/tirage")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"participationId\": 12}"))
                .andExpect(status().isMethodNotAllowed());
    }

    // /tirage/7 n'existe pour aucune méthode : 404 Not Found.
    @Test
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void modificationDUnTirage_nExistePlus_donne404() throws Exception {
        mockMvc.perform(put("/tirage/7")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void suppressionDUnTirage_nExistePlus_donne404() throws Exception {
        mockMvc.perform(delete("/tirage/7"))
                .andExpect(status().isNotFound());
    }
}
