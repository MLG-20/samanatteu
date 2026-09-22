package com.samanatteu.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.samanatteu.dto.tontine.TontineDTO;
import com.samanatteu.entity.Tontine;
import com.samanatteu.service.tontine.TontineService;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RequestMapping("/tontine")
@RestController
public class TontineController {

    private final TontineService tontineService;

    public TontineController(TontineService tontineService) {
        this.tontineService = tontineService;
    }

    // --- Lister ---
    @GetMapping
    public List<TontineDTO> listTontines() {
        return tontineService.listTontine();
    }

    // --- Créer ---
    @PostMapping
    /*
     * @RequestBody TontineDTO tontine — dit à Spring "prends le JSON envoyé dans le
     * corps
     * de la requête, et convertis-le automatiquement en objet TontineDTO"
     */
    public TontineDTO createTontine(@RequestBody Tontine tontine) {
        return tontineService.createTontine(tontine);
    }

    // --- Update ---
    @PutMapping("/{id}")
    public ResponseEntity<TontineDTO> updateTontine(@PathVariable Long id, @RequestBody Tontine TontineModifier) {
        return tontineService.updateTontine(id, TontineModifier)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // --- DELETE ---
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTontine(@PathVariable Long id) {
        if (tontineService.deleteTontine(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

}
