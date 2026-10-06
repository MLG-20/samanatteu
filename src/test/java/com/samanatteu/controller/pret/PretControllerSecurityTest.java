package com.samanatteu.controller.pret;

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
import com.samanatteu.dto.pret.PretDTO;
import com.samanatteu.security.JwtAccessDeniedHandler;
import com.samanatteu.security.JwtAuthentificationEntryPoint;
import com.samanatteu.security.JwtUtil;
import com.samanatteu.service.pret.PretService;

// Règles d'ACCÈS sur /pret : lecture GESTIONNAIRE ou MEMBRE (pas ADMIN),
// remboursement réservé au GESTIONNAIRE, plus de CRUD générique.
// (Accorder un prêt, POST /tontine/{id}/prets : TontineControllerSecurityTest.)
@WebMvcTest(PretController.class)
@Import({ SecurityConfig.class, JwtAuthentificationEntryPoint.class, JwtAccessDeniedHandler.class })
class PretControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PretService pretService;

    @MockitoBean
    private JwtUtil jwtUtil;

    // ---------------------------------------------------------------- lecture

    @Test
    void listeDesPrets_sansToken_donne401() throws Exception {
        mockMvc.perform(get("/pret"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "770000099", roles = "ADMIN")
    void listeDesPrets_parUnAdmin_donne403() throws Exception {
        mockMvc.perform(get("/pret"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "771234566", roles = "MEMBRE")
    void listeDesPrets_parUnMembre_donne200() throws Exception {
        when(pretService.listPret()).thenReturn(List.of());

        mockMvc.perform(get("/pret"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void listeDesPrets_parUnGestionnaire_donne200() throws Exception {
        when(pretService.listPret()).thenReturn(List.of());

        mockMvc.perform(get("/pret"))
                .andExpect(status().isOk());
    }

    // ----------------------------------------------------------- rembourser

    @Test
    @WithMockUser(username = "771234566", roles = "MEMBRE")
    void remboursement_parUnMembre_donne403() throws Exception {
        mockMvc.perform(post("/pret/20/remboursement")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"montant\": 10000}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "770000099", roles = "ADMIN")
    void remboursement_parUnAdmin_donne403() throws Exception {
        mockMvc.perform(post("/pret/20/remboursement")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"montant\": 10000}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void remboursement_parUnGestionnaire_donne200() throws Exception {
        when(pretService.rembourser(eq(20L), any())).thenReturn(new PretDTO());

        mockMvc.perform(post("/pret/20/remboursement")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"montant\": 10000}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void remboursement_montantNul_donne400SansAppelerLeService() throws Exception {
        mockMvc.perform(post("/pret/20/remboursement")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"montant\": 0}"))
                .andExpect(status().isBadRequest());

        verify(pretService, never()).rembourser(any(), any());
    }

    // ------------------------------------------- anciennes portes supprimées
    // POST /pret contournait tous les contrôles, PUT désynchronisait
    // l'échéancier, DELETE effaçait une dette.

    @Test
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void creationManuelleDUnPret_nExistePlus_donne405() throws Exception {
        mockMvc.perform(post("/pret")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"montant\": 50000}"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void modificationDUnPret_nExistePlus_donne404() throws Exception {
        mockMvc.perform(put("/pret/20")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void suppressionDUnPret_nExistePlus_donne404() throws Exception {
        mockMvc.perform(delete("/pret/20"))
                .andExpect(status().isNotFound());
    }
}
