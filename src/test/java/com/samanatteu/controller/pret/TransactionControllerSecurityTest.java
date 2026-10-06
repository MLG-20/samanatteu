package com.samanatteu.controller.pret;

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
import com.samanatteu.security.JwtAccessDeniedHandler;
import com.samanatteu.security.JwtAuthentificationEntryPoint;
import com.samanatteu.security.JwtUtil;
import com.samanatteu.service.pret.TransactionService;

// Règles d'ACCÈS sur /transaction : lecture GESTIONNAIRE ou MEMBRE (pas
// ADMIN, frontière SaaS). Le journal ne s'écrit que par le serveur
// (journaliser) : aucune route d'écriture. Même montage que
// TirageControllerSecurityTest.
@WebMvcTest(TransactionController.class)
@Import({ SecurityConfig.class, JwtAuthentificationEntryPoint.class, JwtAccessDeniedHandler.class })
class TransactionControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private JwtUtil jwtUtil;

    // ---------------------------------------------------------------- lecture

    @Test
    void journal_sansToken_donne401() throws Exception {
        mockMvc.perform(get("/transaction"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "770000099", roles = "ADMIN")
    void journal_parUnAdmin_donne403() throws Exception {
        mockMvc.perform(get("/transaction"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void journal_parUnGestionnaire_donne200() throws Exception {
        when(transactionService.listTransactions()).thenReturn(List.of());

        mockMvc.perform(get("/transaction"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "771234566", roles = "MEMBRE")
    void journal_parUnMembre_donne200() throws Exception {
        when(transactionService.listTransactions()).thenReturn(List.of());

        mockMvc.perform(get("/transaction"))
                .andExpect(status().isOk());
    }

    // ------------------------------------------- anciennes portes supprimées
    // POST inventait une ligne sans mouvement réel, PUT la falsifiait,
    // DELETE effaçait une preuve. Même le gestionnaire reçoit une erreur 4xx.

    // /transaction existe (GET) mais pas en POST : 405.
    @Test
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void creationManuelleDUneLigne_nExistePlus_donne405() throws Exception {
        mockMvc.perform(post("/transaction")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"montant\": 10000}"))
                .andExpect(status().isMethodNotAllowed());
    }

    // /transaction/7 n'existe pour aucune méthode : 404.
    @Test
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void modificationDUneLigne_nExistePlus_donne404() throws Exception {
        mockMvc.perform(put("/transaction/7")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "770000101", roles = "GESTIONNAIRE")
    void suppressionDUneLigne_nExistePlus_donne404() throws Exception {
        mockMvc.perform(delete("/transaction/7"))
                .andExpect(status().isNotFound());
    }
}
