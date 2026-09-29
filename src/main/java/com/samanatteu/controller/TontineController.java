package com.samanatteu.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.samanatteu.dto.tontine.CycleDTO;
import com.samanatteu.dto.tontine.TontineDTO;
import com.samanatteu.entity.Tontine;
import com.samanatteu.service.tontine.CycleService;
import com.samanatteu.service.tontine.TontineService;

import jakarta.validation.Valid;

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
    private final CycleService cycleService;

    public TontineController(TontineService tontineService, CycleService cycleService) {
        this.tontineService = tontineService;
        this.cycleService = cycleService;
    }

    @GetMapping
    public List<TontineDTO> listTontines() {
        return tontineService.listTontine();
    }

    // @Valid : Spring vérifie les annotations de Tontine (@NotNull, @Min…)
    // AVANT d'appeler le service ; en cas d'échec, 400 avec le champ fautif
    // (GlobalExceptionHandler). Aussi sur updateTontine, sinon on pourrait
    // créer une tontine valide puis la modifier avec intervalle = 0.
    @PostMapping
    public TontineDTO createTontine(@Valid @RequestBody Tontine tontine) {
        return tontineService.createTontine(tontine);
    }

    @PostMapping("/{id}/activer")
    public ResponseEntity<TontineDTO> activerTontine(@PathVariable Long id) {
        return tontineService.activerTontine(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/suspendre")
    public ResponseEntity<TontineDTO> suspendreTontine(@PathVariable Long id) {
        return tontineService.suspendreTontine(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/cloturer")
    public ResponseEntity<TontineDTO> cloturerTontine(@PathVariable Long id) {
        return tontineService.cloturerTontine(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Ouvre le cycle suivant : POST sur la collection des cycles de la tontine.
    // Pas de @RequestBody : le client n'envoie que l'id (URL), tout le reste
    // est calculé par CycleService. Pas d'Optional ni de ResponseEntity : les
    // refus sont des exceptions (404/403/409) traduites par
    // GlobalExceptionHandler. Sécurité : couvert par POST /tontine/** =
    // GESTIONNAIRE dans SecurityConfig.
    @PostMapping("/{id}/cycles")
    public CycleDTO ouvrirCycle(@PathVariable Long id) {
        return cycleService.ouvrirCycle(id);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TontineDTO> updateTontine(@PathVariable Long id, @Valid @RequestBody Tontine TontineModifier) {
        return tontineService.updateTontine(id, TontineModifier)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTontine(@PathVariable Long id) {
        if (tontineService.deleteTontine(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

}
