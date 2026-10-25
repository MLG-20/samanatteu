package com.samanatteu.controller.utilisateur;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import com.samanatteu.exception.AccesRefuseException;
import com.samanatteu.security.JwtAccessDeniedHandler;
import com.samanatteu.security.JwtAuthentificationEntryPoint;
import com.samanatteu.security.JwtUtil;
import com.samanatteu.service.utilisateur.UtilisateurService;

// Règles d'ACCÈS sur /utilisateur : la liste est réservée à l'ADMIN, et le refus du
// service (un ADMIN qui vise un MEMBRE) sort bien en 403.
// Même montage que MessageContactControllerSecurityTest.
@WebMvcTest(UtilisateurController.class)
@Import({ SecurityConfig.class, JwtAuthentificationEntryPoint.class, JwtAccessDeniedHandler.class })
class UtilisateurControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UtilisateurService utilisateurService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    void lister_sansToken_donne401() throws Exception {
        mockMvc.perform(get("/utilisateur"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void lister_parUnGestionnaire_donne403() throws Exception {
        mockMvc.perform(get("/utilisateur"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "771234566", roles = "MEMBRE")
    void lister_parUnMembre_donne403() throws Exception {
        mockMvc.perform(get("/utilisateur"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "770000099", roles = "ADMIN")
    void lister_parUnAdmin_donne200() throws Exception {
        when(utilisateurService.listUtilisateurs()).thenReturn(List.of());

        mockMvc.perform(get("/utilisateur"))
                .andExpect(status().isOk());
    }

    // Le service refuse (compte MEMBRE) : AccesRefuseException -> 403, pas 500.
    @Test
    @WithMockUser(username = "770000099", roles = "ADMIN")
    void supprimer_unMembreParUnAdmin_donne403() throws Exception {
        when(utilisateurService.deleteUtilisateur(17L)).thenThrow(new AccesRefuseException());

        mockMvc.perform(delete("/utilisateur/17"))
                .andExpect(status().isForbidden());
    }
}
