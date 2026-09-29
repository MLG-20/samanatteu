package com.samanatteu.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.samanatteu.config.SecurityConfig;
import com.samanatteu.dto.cotisation.TirageDTO;
import com.samanatteu.dto.tontine.CycleDTO;
import com.samanatteu.security.JwtAccessDeniedHandler;
import com.samanatteu.security.JwtAuthentificationEntryPoint;
import com.samanatteu.security.JwtUtil;
import com.samanatteu.service.cotisation.TirageService;
import com.samanatteu.service.tontine.CycleService;

// Règles d'ACCÈS de SecurityConfig sur /cycle : lecture GESTIONNAIRE ou MEMBRE (pas ADMIN),
// clôture et suppression réservées au GESTIONNAIRE. Même montage que ParticipationControllerSecurityTest.
// (L'ouverture d'un cycle, POST /tontine/{id}/cycles, est testée dans TontineControllerSecurityTest.)
@WebMvcTest(CycleController.class)
@Import({ SecurityConfig.class, JwtAuthentificationEntryPoint.class, JwtAccessDeniedHandler.class })
class CycleControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CycleService cycleService;

    // CycleController porte aussi POST /cycle/{id}/tirage : sans ce faux
    // TirageService, le contexte @WebMvcTest ne démarre pas.
    @MockitoBean
    private TirageService tirageService;

    @MockitoBean
    private JwtUtil jwtUtil;

    // ---------------------------------------------------------------- lecture

    @Test
    void listeDesCycles_sansToken_donne401() throws Exception {
        mockMvc.perform(get("/cycle"))
                .andExpect(status().isUnauthorized());
    }

    // L'admin de la plateforme ne voit pas le contenu des tontines des gestionnaires.
    @Test
    @WithMockUser(username = "770000099", roles = "ADMIN")
    void listeDesCycles_parUnAdmin_donne403() throws Exception {
        mockMvc.perform(get("/cycle"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "771234566", roles = "MEMBRE")
    void listeDesCycles_parUnMembre_donne200() throws Exception {
        when(cycleService.listCycle()).thenReturn(List.of());

        mockMvc.perform(get("/cycle"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void listeDesCycles_parUnGestionnaire_donne200() throws Exception {
        when(cycleService.listCycle()).thenReturn(List.of());

        mockMvc.perform(get("/cycle"))
                .andExpect(status().isOk());
    }

    // ---------------------------------------------------------------- clôture

    @Test
    void clotureDUnCycle_sansToken_donne401() throws Exception {
        mockMvc.perform(post("/cycle/5/cloturer"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "771234566", roles = "MEMBRE")
    void clotureDUnCycle_parUnMembre_donne403() throws Exception {
        mockMvc.perform(post("/cycle/5/cloturer"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "770000099", roles = "ADMIN")
    void clotureDUnCycle_parUnAdmin_donne403() throws Exception {
        mockMvc.perform(post("/cycle/5/cloturer"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void clotureDUnCycle_parUnGestionnaire_donne200() throws Exception {
        when(cycleService.cloturerCycle(5L)).thenReturn(new CycleDTO());

        mockMvc.perform(post("/cycle/5/cloturer"))
                .andExpect(status().isOk());
    }

    // ----------------------------------------------------------------- tirage
    // Couvert par la règle POST /cycle/** → GESTIONNAIRE (aucune règle dédiée).

    @Test
    void tirageDUnCycle_sansToken_donne401() throws Exception {
        mockMvc.perform(post("/cycle/5/tirage"))
                .andExpect(status().isUnauthorized());
    }

    // Un membre ne peut pas se lancer un tirage (ni se désigner gagnant).
    @Test
    @WithMockUser(username = "771234566", roles = "MEMBRE")
    void tirageDUnCycle_parUnMembre_donne403() throws Exception {
        mockMvc.perform(post("/cycle/5/tirage"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "770000099", roles = "ADMIN")
    void tirageDUnCycle_parUnAdmin_donne403() throws Exception {
        mockMvc.perform(post("/cycle/5/tirage"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void tirageDUnCycle_parUnGestionnaire_donne200() throws Exception {
        when(tirageService.tirerAuSort(5L)).thenReturn(new TirageDTO());

        mockMvc.perform(post("/cycle/5/tirage"))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------ suppression

    @Test
    @WithMockUser(username = "771234566", roles = "MEMBRE")
    void suppressionDUnCycle_parUnMembre_donne403() throws Exception {
        mockMvc.perform(delete("/cycle/5"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void suppressionDUnCycle_parUnGestionnaire_donne204() throws Exception {
        when(cycleService.deleteCycle(5L)).thenReturn(true);

        mockMvc.perform(delete("/cycle/5"))
                .andExpect(status().isNoContent());
    }
}
