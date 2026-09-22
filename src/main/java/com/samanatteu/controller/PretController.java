package com.samanatteu.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.samanatteu.dto.pret.PretDTO;
import com.samanatteu.entity.Pret;
import com.samanatteu.service.pret.PretService;

@RequestMapping("/pret")
@RestController
public class PretController {
    private final PretService pretService;

    public PretController(PretService pretService) {
        this.pretService = pretService;
    }

    // --- Lister ---
    @GetMapping
    public List<PretDTO> listPret() {
        return pretService.listPret();
    }

    // --- Créer ---
    @PostMapping
    public PretDTO createPret(@RequestBody Pret pret) {
        return pretService.createPret(pret);
    }

    // --- Update ---
    @PutMapping("/{id}")
    public ResponseEntity<PretDTO> updatePret(@PathVariable Long id,
            @RequestBody Pret pretModifier) {
        return pretService.updatePret(id, pretModifier)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // --- DELETE ---
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePret(@PathVariable Long id) {
        if (pretService.deletePret(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
