package com.samanatteu.controller.abonnement;

import static org.mockito.Mockito.when;
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
import com.samanatteu.security.JwtAccessDeniedHandler;
import com.samanatteu.security.JwtAuthentificationEntryPoint;
import com.samanatteu.security.JwtUtil;
import com.samanatteu.service.abonnement.FormuleService;

// Règles d'ACCÈS sur /formule : les formules À LA VENTE sont publiques (la page
// d'accueil affiche les tarifs), tout le reste est réservé à l'ADMIN.
// Même montage que InvitationControllerSecurityTest.
@WebMvcTest(FormuleController.class)
@Import({ SecurityConfig.class, JwtAuthentificationEntryPoint.class, JwtAccessDeniedHandler.class })
class FormuleControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FormuleService formuleService;

    @MockitoBean
    private JwtUtil jwtUtil;

    // ------------------------------------------------ formules à la vente

    @Test
    void disponibles_sansToken_donne200() throws Exception {
        when(formuleService.listerProposables()).thenReturn(List.of());

        mockMvc.perform(get("/formule/disponibles"))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------- toutes les formules

    // "/formule/disponibles" (public) ne doit PAS ouvrir "/formule" (y compris
    // les formules retirées de la vente).
    @Test
    void liste_sansToken_donne401() throws Exception {
        mockMvc.perform(get("/formule"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void liste_parUnGestionnaire_donne403() throws Exception {
        mockMvc.perform(get("/formule"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "770000099", roles = "ADMIN")
    void liste_parUnAdmin_donne200() throws Exception {
        when(formuleService.lister()).thenReturn(List.of());

        mockMvc.perform(get("/formule"))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------- modification

    @Test
    void creation_sansToken_donne401() throws Exception {
        mockMvc.perform(post("/formule")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void modification_parUnGestionnaire_donne403() throws Exception {
        mockMvc.perform(put("/formule/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());
    }
}
