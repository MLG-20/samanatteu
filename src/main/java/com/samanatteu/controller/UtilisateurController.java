package com.samanatteu.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.samanatteu.dto.utilisateur.CreationUtilisateurDTO;
import com.samanatteu.dto.utilisateur.ModificationUtilisateurDTO;
import com.samanatteu.dto.utilisateur.UtilisateurDTO;

import com.samanatteu.service.utilisateur.UtilisateurService;

import jakarta.validation.Valid;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RequestMapping("/utilisateur")
@RestController
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    public UtilisateurController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    // Lister
    @GetMapping
    public List<UtilisateurDTO> listUtilisateurs() {
        return utilisateurService.listUtilisateurs();
    }

    // Créer
    @PostMapping
    public UtilisateurDTO createUtilisateur(@Valid @RequestBody CreationUtilisateurDTO dto) {
        return utilisateurService.createUtilisateur(dto);
    }

    // --- Update ---
    @PutMapping("/{id}")
    public ResponseEntity<UtilisateurDTO> updateUtilisateur(@PathVariable Long id,
            @Valid @RequestBody ModificationUtilisateurDTO modifications) {
        return utilisateurService.updateUtilisateur(id, modifications)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());

    }

    // --- DELETE ---
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUtilisateur(@PathVariable Long id) {
        if (utilisateurService.deleteUtilisateur(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}