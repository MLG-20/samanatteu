package com.samanatteu.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import com.samanatteu.service.dashboard.DashboardService;

// Règles d'ACCÈS sur /dashboard/gestionnaire/** : réservé au GESTIONNAIRE.
// Sans la règle de SecurityConfig, anyRequest().authenticated() laisserait
// entrer un MEMBRE ou un ADMIN connecté. Une ligne de @ValueSource par bloc :
// un nouveau bloc oublié dans la règle ferait passer ces tests au rouge.
// Même montage que TransactionControllerSecurityTest.
@WebMvcTest(DashboardController.class)
@Import({ SecurityConfig.class, JwtAuthentificationEntryPoint.class, JwtAccessDeniedHandler.class })
class DashboardControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @ParameterizedTest
    @ValueSource(strings = { "tontines", "retards", "prets", "tirages", "gains" })
    void bloc_sansToken_donne401(String bloc) throws Exception {
        mockMvc.perform(get("/dashboard/gestionnaire/" + bloc))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = { "tontines", "retards", "prets", "tirages", "gains" })
    @WithMockUser(username = "771234566", roles = "MEMBRE")
    void bloc_parUnMembre_donne403(String bloc) throws Exception {
        mockMvc.perform(get("/dashboard/gestionnaire/" + bloc))
                .andExpect(status().isForbidden());
    }

    // Frontière SaaS : l'ADMIN ne voit pas le contenu des tontines des clients.
    @ParameterizedTest
    @ValueSource(strings = { "tontines", "retards", "prets", "tirages", "gains" })
    @WithMockUser(username = "770000099", roles = "ADMIN")
    void bloc_parUnAdmin_donne403(String bloc) throws Exception {
        mockMvc.perform(get("/dashboard/gestionnaire/" + bloc))
                .andExpect(status().isForbidden());
    }

    // Le service mocké rend une liste vide par défaut : seul l'accès est jugé.
    @ParameterizedTest
    @ValueSource(strings = { "tontines", "retards", "prets", "tirages", "gains" })
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void bloc_parUnGestionnaire_donne200(String bloc) throws Exception {
        mockMvc.perform(get("/dashboard/gestionnaire/" + bloc))
                .andExpect(status().isOk());
    }
}
