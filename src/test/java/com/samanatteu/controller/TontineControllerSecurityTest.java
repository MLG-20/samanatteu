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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.samanatteu.config.SecurityConfig;
import com.samanatteu.security.JwtAccessDeniedHandler;
import com.samanatteu.security.JwtAuthentificationEntryPoint;
import com.samanatteu.security.JwtUtil;
import com.samanatteu.service.tontine.TontineService;

// Teste les RÈGLES D'ACCÈS de SecurityConfig sur /tontine (401, 403, 200), pas la logique du service.
//
// @WebMvcTest(TontineController.class) : ne démarre que la couche web (ce contrôleur, les filtres,
// la sécurité). Pas de base de données, pas de vrai service : c'est rapide.
//
// @Import : un test "slice" ne charge PAS les @Configuration ni les @Component ordinaires. On importe donc
// à la main SecurityConfig et les deux gestionnaires d'erreur qu'elle réclame (401 et 403 personnalisés).
// JwtAuthFilter, lui, est un Filter : le slice web le charge tout seul.
@WebMvcTest(TontineController.class)
@Import({ SecurityConfig.class, JwtAuthentificationEntryPoint.class, JwtAccessDeniedHandler.class })
class TontineControllerSecurityTest {

    // Le faux client HTTP : envoie des requêtes à l'application sans lancer de serveur.
    @Autowired
    private MockMvc mockMvc;

    // Faux TontineService : le contrôleur en a besoin pour se construire, mais on ne teste pas sa
    // logique ici (elle a déjà ses tests unitaires). On ne fait que fixer ce qu'il répond.
    @MockitoBean
    private TontineService tontineService;

    // JwtAuthFilter a besoin d'un JwtUtil pour se construire. On le remplace par un faux : dans ces tests
    // on n'envoie jamais de vrai token, l'utilisateur est simulé par @WithMockUser.
    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    void listeDesTontines_sansToken_donne401() throws Exception {
        // Aucun utilisateur connecté : SecurityConfig doit renvoyer 401.
        mockMvc.perform(get("/tontine"))
                .andExpect(status().isUnauthorized());
    }

    // @WithMockUser pose un faux utilisateur connecté avec ce rôle (Spring ajoute lui-même "ROLE_").
    @Test
    @WithMockUser(username = "770000099", roles = "ADMIN")
    void listeDesTontines_parUnAdmin_donne403() throws Exception {
        // Règle métier : l'admin de la plateforme ne voit pas le contenu des tontines.
        mockMvc.perform(get("/tontine"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "771234566", roles = "MEMBRE")
    void listeDesTontines_parUnMembre_donne403() throws Exception {
        // Le cas MEMBRE est refusé par défaut, tant que "mes tontines de membre" n'est pas construit.
        mockMvc.perform(get("/tontine"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void listeDesTontines_parUnGestionnaire_donne200() throws Exception {
        // Le vrai service est remplacé par un faux : on lui fait renvoyer une liste vide.
        when(tontineService.listTontine()).thenReturn(List.of());

        mockMvc.perform(get("/tontine"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "771234566", roles = "MEMBRE")
    void creationDUneTontine_parUnMembre_donne403() throws Exception {
        // Un MEMBRE ne peut pas créer de tontine. Le corps est vide ({}) : c'est SecurityConfig qui doit
        // refuser AVANT que le contrôleur ne regarde le contenu.
        mockMvc.perform(post("/tontine")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "770000099", roles = "ADMIN")
    void suppressionDUneTontine_parUnAdmin_donne403() throws Exception {
        // L'admin ne peut pas non plus supprimer la tontine d'un gestionnaire.
        mockMvc.perform(delete("/tontine/6"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void suppressionDUneTontine_parUnGestionnaire_donne204() throws Exception {
        // Un gestionnaire passe SecurityConfig. Le faux service dit "supprimée" (true) : le contrôleur
        // répond alors 204 (No Content).
        when(tontineService.deleteTontine(6L)).thenReturn(true);

        mockMvc.perform(delete("/tontine/6"))
                .andExpect(status().isNoContent());
    }
}
