package com.samanatteu.controller.dashboard;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.samanatteu.config.SecurityConfig;
import com.samanatteu.exception.utilisateur.UtilisateurIntrouvableException;
import com.samanatteu.security.JwtAccessDeniedHandler;
import com.samanatteu.security.JwtAuthentificationEntryPoint;
import com.samanatteu.security.JwtUtil;
import com.samanatteu.service.dashboard.DashboardAdminService;

// Règles d'ACCÈS sur /dashboard/admin/** : réservé à l'ADMIN (pilotage
// plateforme, compteurs anonymes). Le GESTIONNAIRE et le MEMBRE ont chacun
// leur tableau de bord, sans passerelle. Une ligne de @ValueSource par bloc :
// un nouveau bloc oublié dans la règle ferait passer ces tests au rouge.
// Même montage que DashboardControllerSecurityTest.
@WebMvcTest(DashboardAdminController.class)
@Import({ SecurityConfig.class, JwtAuthentificationEntryPoint.class, JwtAccessDeniedHandler.class })
class DashboardAdminControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardAdminService dashboardAdminService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @ParameterizedTest
    @ValueSource(strings = { "utilisateurs", "tontines", "gestionnaire/5" })
    void bloc_sansToken_donne401(String bloc) throws Exception {
        mockMvc.perform(get("/dashboard/admin/" + bloc))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = { "utilisateurs", "tontines", "gestionnaire/5" })
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void bloc_parUnGestionnaire_donne403(String bloc) throws Exception {
        mockMvc.perform(get("/dashboard/admin/" + bloc))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = { "utilisateurs", "tontines", "gestionnaire/5" })
    @WithMockUser(username = "771110002", roles = "MEMBRE")
    void bloc_parUnMembre_donne403(String bloc) throws Exception {
        mockMvc.perform(get("/dashboard/admin/" + bloc))
                .andExpect(status().isForbidden());
    }

    // Le service mocké rend null par défaut : seul l'accès est jugé.
    @ParameterizedTest
    @ValueSource(strings = { "utilisateurs", "tontines", "gestionnaire/5" })
    @WithMockUser(username = "770000099", roles = "ADMIN")
    void bloc_parUnAdmin_donne200(String bloc) throws Exception {
        mockMvc.perform(get("/dashboard/admin/" + bloc))
                .andExpect(status().isOk());
    }

    // Pas une gestionnaire (membre, admin, id inconnu) : l'exception du service sort en 404.
    @Test
    @WithMockUser(username = "770000099", roles = "ADMIN")
    void fiche_dUnCompteQuiNEstPasGestionnaire_donne404() throws Exception {
        when(dashboardAdminService.ficheGestionnaire(17L)).thenThrow(new UtilisateurIntrouvableException(17L));

        mockMvc.perform(get("/dashboard/admin/gestionnaire/17"))
                .andExpect(status().isNotFound());
    }
}
