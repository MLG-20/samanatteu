package com.samanatteu.controller.site;

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
import com.samanatteu.dto.site.MessageContactDTO;
import com.samanatteu.security.JwtAccessDeniedHandler;
import com.samanatteu.security.JwtAuthentificationEntryPoint;
import com.samanatteu.security.JwtUtil;
import com.samanatteu.service.site.MessageContactService;

// Règles d'ACCÈS sur /messageContact : un visiteur ENVOIE (public), seul l'ADMIN
// lit, marque lu et supprime. Plus la validation du formulaire.
// Même montage que InvitationControllerSecurityTest.
@WebMvcTest(MessageContactController.class)
@Import({ SecurityConfig.class, JwtAuthentificationEntryPoint.class, JwtAccessDeniedHandler.class })
class MessageContactControllerSecurityTest {

    private static final String MESSAGE_VALIDE =
            "{\"nom\": \"Awa Diop\", \"email\": \"awa@exemple.sn\", \"sujet\": \"Formule PRO\", \"message\": \"Bonjour\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MessageContactService messageContactService;

    @MockitoBean
    private JwtUtil jwtUtil;

    // ---------------------------------------------------------------- envoi

    @Test
    void envoyer_sansToken_donne204() throws Exception {
        mockMvc.perform(post("/messageContact")
                .contentType(MediaType.APPLICATION_JSON)
                .content(MESSAGE_VALIDE))
                .andExpect(status().isNoContent());
        verify(messageContactService).envoyer(any());
    }

    @Test
    void envoyer_sansSujetNiMessage_donne400() throws Exception {
        mockMvc.perform(post("/messageContact")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nom\": \"Awa Diop\", \"email\": \"awa@exemple.sn\"}"))
                .andExpect(status().isBadRequest());
        verify(messageContactService, never()).envoyer(any());
    }

    // ------------------------------------------------- lecture par l'ADMIN

    // POST /messageContact (public) ne doit PAS ouvrir GET /messageContact (les messages).
    @Test
    void lister_sansToken_donne401() throws Exception {
        mockMvc.perform(get("/messageContact"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void lister_parUnGestionnaire_donne403() throws Exception {
        mockMvc.perform(get("/messageContact"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "770000099", roles = "ADMIN")
    void lister_parUnAdmin_donne200() throws Exception {
        when(messageContactService.lister()).thenReturn(List.of());

        mockMvc.perform(get("/messageContact"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "771234566", roles = "MEMBRE")
    void marquerLu_parUnMembre_donne403() throws Exception {
        mockMvc.perform(put("/messageContact/5/lu"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "770000099", roles = "ADMIN")
    void marquerLu_parUnAdmin_donne200() throws Exception {
        when(messageContactService.marquerLu(5L, false)).thenReturn(new MessageContactDTO());

        mockMvc.perform(put("/messageContact/5/lu").param("valeur", "false"))
                .andExpect(status().isOk());
        verify(messageContactService).marquerLu(5L, false);
    }

    @Test
    void supprimer_sansToken_donne401() throws Exception {
        mockMvc.perform(delete("/messageContact/5"))
                .andExpect(status().isUnauthorized());
        verify(messageContactService, never()).supprimer(any());
    }

    @Test
    @WithMockUser(username = "770000099", roles = "ADMIN")
    void supprimer_parUnAdmin_donne204() throws Exception {
        mockMvc.perform(delete("/messageContact/5"))
                .andExpect(status().isNoContent());
        verify(messageContactService).supprimer(5L);
    }
}
