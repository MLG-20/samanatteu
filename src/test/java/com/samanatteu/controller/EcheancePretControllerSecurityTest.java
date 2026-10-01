package com.samanatteu.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.samanatteu.security.JwtAccessDeniedHandler;
import com.samanatteu.security.JwtAuthentificationEntryPoint;
import com.samanatteu.security.JwtUtil;
import com.samanatteu.service.pret.EcheancePretService;

// /echeancePret : lecture seule (GESTIONNAIRE ou MEMBRE, pas ADMIN). Un PUT
// permettait de se marquer « payé » sans verser l'argent : supprimé.
@WebMvcTest(EcheancePretController.class)
@Import({ SecurityConfig.class, JwtAuthentificationEntryPoint.class, JwtAccessDeniedHandler.class })
class EcheancePretControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EcheancePretService echeancePretService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    void listeDesEcheances_sansToken_donne401() throws Exception {
        mockMvc.perform(get("/echeancePret"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "770000099", roles = "ADMIN")
    void listeDesEcheances_parUnAdmin_donne403() throws Exception {
        mockMvc.perform(get("/echeancePret"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "771234566", roles = "MEMBRE")
    void listeDesEcheances_parUnMembre_donne200() throws Exception {
        when(echeancePretService.listEcheancePret()).thenReturn(List.of());

        mockMvc.perform(get("/echeancePret"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void listeDesEcheances_parUnGestionnaire_donne200() throws Exception {
        when(echeancePretService.listEcheancePret()).thenReturn(List.of());

        mockMvc.perform(get("/echeancePret"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void modificationDUneEcheance_nExistePlus_donne404() throws Exception {
        mockMvc.perform(put("/echeancePret/5")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"montantPaye\": 36666}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void suppressionDUneEcheance_nExistePlus_donne404() throws Exception {
        mockMvc.perform(delete("/echeancePret/5"))
                .andExpect(status().isNotFound());
    }
}
