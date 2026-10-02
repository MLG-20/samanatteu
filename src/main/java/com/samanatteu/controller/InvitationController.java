package com.samanatteu.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.samanatteu.dto.onboarding.InvitationDTO;
import com.samanatteu.dto.onboarding.LienInvitationDTO;
import com.samanatteu.service.onboarding.InvitationService;

@RequestMapping("/invitation")
@RestController
public class InvitationController {
    private final InvitationService invitationService;

    public InvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @GetMapping
    public List<InvitationDTO> listInvitation() {
        return invitationService.listInvitation();
    }

    // Publique : la personne n'a pas encore de compte quand elle clique sur le lien.
    @GetMapping("/{token}")
    public LienInvitationDTO consulterLien(@PathVariable String token) {
        return invitationService.consulterLien(token);
    }

    // 204 : rien à renvoyer, l'écran affiche simplement « Bienvenue ».
    @PostMapping("/{token}/rejoindre")
    public ResponseEntity<Void> rejoindre(@PathVariable String token) {
        invitationService.rejoindre(token);
        return ResponseEntity.noContent().build();
    }

}
