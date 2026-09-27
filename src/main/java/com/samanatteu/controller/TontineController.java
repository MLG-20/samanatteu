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

    @GetMapping
    public List<TontineDTO> listTontines() {
        return tontineService.listTontine();
    }

    @PostMapping
    public TontineDTO createTontine(@RequestBody Tontine tontine) {
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

    @PutMapping("/{id}")
    public ResponseEntity<TontineDTO> updateTontine(@PathVariable Long id, @RequestBody Tontine TontineModifier) {
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
