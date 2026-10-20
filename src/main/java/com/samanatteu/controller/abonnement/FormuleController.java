package com.samanatteu.controller.abonnement;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.samanatteu.dto.abonnement.FormuleDTO;
import com.samanatteu.service.abonnement.FormuleService;

import jakarta.validation.Valid;

// Les formules d'abonnement : pour l'ADMIN.
//
// C'est de la DONNÉE : l'ADMIN change les quotas et les prix ici, sans
// redéployer. C'est l'inverse d'un enum, où il faudrait compiler.
@RequestMapping("/formule")
@RestController
public class FormuleController {

    private final FormuleService formuleService;

    public FormuleController(FormuleService formuleService) {
        this.formuleService = formuleService;
    }

    // Toutes les formules (y compris celles retirées de la vente).
    @GetMapping
    public List<FormuleDTO> lister() {
        return formuleService.lister();
    }

    // Les formules proposées à la vente (celles qu'une cliente peut choisir).
    @GetMapping("/disponibles")
    public List<FormuleDTO> listerProposables() {
        return formuleService.listerProposables();
    }

    @PostMapping
    public ResponseEntity<FormuleDTO> creer(@Valid @RequestBody FormuleDTO dto) {
        return ResponseEntity.status(201).body(formuleService.creer(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FormuleDTO> modifier(@PathVariable Long id, @Valid @RequestBody FormuleDTO dto) {
        return ResponseEntity.ok(formuleService.modifier(id, dto));
    }
}
