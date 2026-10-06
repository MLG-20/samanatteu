package com.samanatteu.controller.notification;

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
import com.samanatteu.service.notification.NotificationService;

// Règles d'ACCÈS sur /notification : lecture GESTIONNAIRE ou MEMBRE (ses propres
// notifications, filtrées par le service), pas ADMIN. Plus aucune écriture :
// les notifications sont produites par le serveur. Même montage que TransactionControllerSecurityTest.
@WebMvcTest(NotificationController.class)
@Import({ SecurityConfig.class, JwtAuthentificationEntryPoint.class, JwtAccessDeniedHandler.class })
class NotificationControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    void mesNotifications_sansToken_donne401() throws Exception {
        mockMvc.perform(get("/notification"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "770000099", roles = "ADMIN")
    void mesNotifications_parUnAdmin_donne403() throws Exception {
        mockMvc.perform(get("/notification"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "771234566", roles = "MEMBRE")
    void mesNotifications_parUnMembre_donne200() throws Exception {
        when(notificationService.listNotification()).thenReturn(List.of());

        mockMvc.perform(get("/notification"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void mesNotifications_parUnGestionnaire_donne200() throws Exception {
        when(notificationService.listNotification()).thenReturn(List.of());

        mockMvc.perform(get("/notification"))
                .andExpect(status().isOk());
    }

    // On ne fabrique pas une notification « envoyée » à la main.
    @Test
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void ancienneCreation_nExistePlus() throws Exception {
        mockMvc.perform(post("/notification")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\": \"faux\", \"statut\": \"ENVOYE\"}"))
                .andExpect(status().isMethodNotAllowed());
    }

    // Une preuve d'envoi ne s'efface pas.
    @Test
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void ancienneSuppression_nExistePlus() throws Exception {
        mockMvc.perform(delete("/notification/5"))
                .andExpect(status().isNotFound());
    }
}
