package com.samanatteu.controller;

import static org.mockito.ArgumentMatchers.any;
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
import com.samanatteu.dto.onboarding.LienInvitationDTO;
import com.samanatteu.security.JwtAccessDeniedHandler;
import com.samanatteu.security.JwtAuthentificationEntryPoint;
import com.samanatteu.security.JwtUtil;
import com.samanatteu.service.onboarding.InvitationService;

// Règles d'ACCÈS sur /invitation : consultation d'un lien PUBLIQUE (pas encore
// de compte au clic), rejoindre = MEMBRE, liste = GESTIONNAIRE (les tokens sont
// des clés). L'ancien CRUD a disparu. Même montage que TransactionControllerSecurityTest.
@WebMvcTest(InvitationController.class)
@Import({ SecurityConfig.class, JwtAuthentificationEntryPoint.class, JwtAccessDeniedHandler.class })
class InvitationControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InvitationService invitationService;

    @MockitoBean
    private JwtUtil jwtUtil;

    // ------------------------------------------------- consultation du lien

    @Test
    void consulterUnLien_sansToken_donne200() throws Exception {
        when(invitationService.consulterLien("jeton")).thenReturn(new LienInvitationDTO());

        mockMvc.perform(get("/invitation/jeton"))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------ rejoindre

    @Test
    void rejoindre_sansToken_donne401() throws Exception {
        mockMvc.perform(post("/invitation/jeton/rejoindre"))
                .andExpect(status().isUnauthorized());
        verify(invitationService, never()).rejoindre(any());
    }

    @Test
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void rejoindre_parUnGestionnaire_donne403() throws Exception {
        mockMvc.perform(post("/invitation/jeton/rejoindre"))
                .andExpect(status().isForbidden());
        verify(invitationService, never()).rejoindre(any());
    }

    @Test
    @WithMockUser(username = "770000099", roles = "ADMIN")
    void rejoindre_parUnAdmin_donne403() throws Exception {
        mockMvc.perform(post("/invitation/jeton/rejoindre"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "771234566", roles = "MEMBRE")
    void rejoindre_parUnMembre_donne204() throws Exception {
        mockMvc.perform(post("/invitation/jeton/rejoindre"))
                .andExpect(status().isNoContent());
        verify(invitationService).rejoindre("jeton");
    }

    // ---------------------------------------------------------------- liste

    // "/invitation/*" (public) ne doit PAS ouvrir "/invitation" (la liste).
    @Test
    void liste_sansToken_donne401() throws Exception {
        mockMvc.perform(get("/invitation"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "771234566", roles = "MEMBRE")
    void liste_parUnMembre_donne403() throws Exception {
        mockMvc.perform(get("/invitation"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "770000099", roles = "ADMIN")
    void liste_parUnAdmin_donne403() throws Exception {
        mockMvc.perform(get("/invitation"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void liste_parUnGestionnaire_donne200() throws Exception {
        when(invitationService.listInvitation()).thenReturn(List.of());

        mockMvc.perform(get("/invitation"))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------ ancien CRUD supprimé

    // Le client ne peut plus fabriquer une invitation (token, statut choisis par lui).
    @Test
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void ancienneCreation_nExistePlus() throws Exception {
        mockMvc.perform(post("/invitation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\": \"abc123\", \"statut\": \"EN_ATTENTE\"}"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void ancienneModification_nExistePlus() throws Exception {
        mockMvc.perform(put("/invitation/5")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void ancienneSuppression_nExistePlus() throws Exception {
        mockMvc.perform(delete("/invitation/5"))
                .andExpect(status().isMethodNotAllowed());
    }
}
