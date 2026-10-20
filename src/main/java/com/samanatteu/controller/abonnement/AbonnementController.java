package com.samanatteu.controller.abonnement;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.samanatteu.dto.abonnement.AbonnementDTO;
import com.samanatteu.dto.abonnement.ActivationAbonnementDTO;
import com.samanatteu.security.UtilisateurConnecte;
import com.samanatteu.service.abonnement.AbonnementService;

import jakarta.validation.Valid;

// Les abonnements.
//
// Deux usages, deux rôles :
// - l'ADMIN voit et gère les abonnements (c'est la donnée commerciale de la
//   plateforme) ;
// - la GESTIONNAIRE voit le sien (sa formule, son échéance, ses quotas).
// Le MEMBRE n'a rien à voir là-dedans.
@RequestMapping("/abonnement")
@RestController
public class AbonnementController {

    private final AbonnementService abonnementService;
    private final UtilisateurConnecte utilisateurConnecte;

    public AbonnementController(AbonnementService abonnementService,
            UtilisateurConnecte utilisateurConnecte) {
        this.abonnementService = abonnementService;
        this.utilisateurConnecte = utilisateurConnecte;
    }

    // Tous les abonnements de la plateforme : pour l'ADMIN.
    @GetMapping
    public List<AbonnementDTO> lister() {
        return abonnementService.lister();
    }

    // L'abonnement de la cliente connectée. 404 si elle n'en a pas.
    @GetMapping("/mon")
    public ResponseEntity<AbonnementDTO> monAbonnement() {
        return abonnementService.monAbonnement(utilisateurConnecte.telephone())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Activation manuelle d'un abonnement : pour l'ADMIN (pas de paiement réel
    // pour l'instant). 201 = créé ou prolongé.
    @PostMapping
    public ResponseEntity<AbonnementDTO> activer(@Valid @RequestBody ActivationAbonnementDTO dto) {
        return ResponseEntity.status(201).body(abonnementService.activer(dto));
    }
}
