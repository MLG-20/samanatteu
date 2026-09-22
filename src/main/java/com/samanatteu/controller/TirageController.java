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

import com.samanatteu.dto.cotisation.TirageDTO;
import com.samanatteu.entity.Tirage;
import com.samanatteu.service.cotisation.TirageService;

@RequestMapping("/tirage")
@RestController
public class TirageController {
    private final TirageService tirageService;

    public TirageController(TirageService tirageService) {
        this.tirageService = tirageService;
    }

    // --- Lister ---
    @GetMapping
    public List<TirageDTO> listTirages() {
        return tirageService.listTirage();
    }

    // --- Créer ---
    @PostMapping
    public TirageDTO createTirage(@RequestBody Tirage tirage) {
        return tirageService.createTirage(tirage);
    }

    // --- Update ---
    @PutMapping("/{id}")
    public ResponseEntity<TirageDTO> updatePret(@PathVariable Long id,
            @RequestBody Tirage tirageModifier) {
        return tirageService.updateTirage(id, tirageModifier)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // --- DELETE ---
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTirage(@PathVariable Long id) {
        if (tirageService.deleteTirage(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
