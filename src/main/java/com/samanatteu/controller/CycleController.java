package com.samanatteu.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.samanatteu.dto.tontine.CycleDTO;
import com.samanatteu.entity.Cycle;
import com.samanatteu.service.tontine.CycleService;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RequestMapping("/cycle")
@RestController
public class CycleController {

    private final CycleService cycleService;

    public CycleController(CycleService cycleService) {
        this.cycleService = cycleService;
    }

    // --- Lister ---
    @GetMapping
    public List<CycleDTO> listCycle() {
        return cycleService.listCycle();
    }

    // --- Créer ---
    @PostMapping
    public CycleDTO createCycle(@RequestBody Cycle cycle) {
        return cycleService.createCycle(cycle);
    }

    // --- Update ---
    @PutMapping("/{id}")
    public ResponseEntity<CycleDTO> updateCycle(@PathVariable Long id,
            @RequestBody Cycle cycleModifier) {
        return cycleService.updateCycle(id, cycleModifier)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // --- DELETE ---
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCycle(@PathVariable Long id) {
        if (cycleService.deleteCycle(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

}
