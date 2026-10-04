package com.samanatteu.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.samanatteu.security.JwtAccessDeniedHandler;
import com.samanatteu.security.JwtAuthentificationEntryPoint;
import com.samanatteu.security.JwtUtil;
import com.samanatteu.service.onboarding.ImportMembreService;

// Règles d'ACCÈS sur /importMembre : lecture réservée au GESTIONNAIRE (ses propres
// rapports, filtrés par le service). Plus aucune écriture : un rapport d'import est
// produit par le serveur. Même montage que NotificationControllerSecurityTest.
@WebMvcTest(ImportMembreController.class)
@Import({ SecurityConfig.class, JwtAuthentificationEntryPoint.class, JwtAccessDeniedHandler.class })
class ImportMembreControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImportMembreService importMembreService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    void mesImports_sansToken_donne401() throws Exception {
        mockMvc.perform(get("/importMembre"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "770000099", roles = "ADMIN")
    void mesImports_parUnAdmin_donne403() throws Exception {
        mockMvc.perform(get("/importMembre"))
                .andExpect(status().isForbidden());
    }

    // Les rapports contiennent des téléphones et des noms : pas pour un MEMBRE.
    @Test
    @WithMockUser(username = "771234566", roles = "MEMBRE")
    void mesImports_parUnMembre_donne403() throws Exception {
        mockMvc.perform(get("/importMembre"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void mesImports_parUnGestionnaire_donne200() throws Exception {
        when(importMembreService.listImportMembre()).thenReturn(List.of());

        mockMvc.perform(get("/importMembre"))
                .andExpect(status().isOk());
    }
}
