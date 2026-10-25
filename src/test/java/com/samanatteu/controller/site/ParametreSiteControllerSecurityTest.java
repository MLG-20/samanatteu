package com.samanatteu.controller.site;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.samanatteu.config.SecurityConfig;
import com.samanatteu.dto.site.ParametreSiteDTO;
import com.samanatteu.security.JwtAccessDeniedHandler;
import com.samanatteu.security.JwtAuthentificationEntryPoint;
import com.samanatteu.security.JwtUtil;
import com.samanatteu.service.site.ParametreSiteService;

// Règles d'ACCÈS sur /site : lecture publique (footer, pages Contact et
// À propos), modification réservée à l'ADMIN. Plus la validation des liens.
// Même montage que InvitationControllerSecurityTest.
@WebMvcTest(ParametreSiteController.class)
@Import({ SecurityConfig.class, JwtAuthentificationEntryPoint.class, JwtAccessDeniedHandler.class })
class ParametreSiteControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ParametreSiteService parametreSiteService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    void lire_sansToken_donne200() throws Exception {
        when(parametreSiteService.lire()).thenReturn(new ParametreSiteDTO());

        mockMvc.perform(get("/site"))
                .andExpect(status().isOk());
    }

    @Test
    void modifier_sansToken_donne401() throws Exception {
        mockMvc.perform(put("/site")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());
        verify(parametreSiteService, never()).modifier(any());
    }

    @Test
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void modifier_parUnGestionnaire_donne403() throws Exception {
        mockMvc.perform(put("/site")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());
        verify(parametreSiteService, never()).modifier(any());
    }

    @Test
    @WithMockUser(username = "770000099", roles = "ADMIN")
    void modifier_parUnAdmin_donne200() throws Exception {
        when(parametreSiteService.modifier(any())).thenReturn(new ParametreSiteDTO());

        mockMvc.perform(put("/site")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"facebook\": \"https://facebook.com/samanatteu\", \"x\": \"\"}"))
                .andExpect(status().isOk());
    }

    // Un lien javascript:... dans le footer serait une faille : refusé.
    @Test
    @WithMockUser(username = "770000099", roles = "ADMIN")
    void modifier_avecUnLienNonHttps_donne400() throws Exception {
        mockMvc.perform(put("/site")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"facebook\": \"javascript:alert(1)\"}"))
                .andExpect(status().isBadRequest());
        verify(parametreSiteService, never()).modifier(any());
    }
}
