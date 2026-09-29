package com.samanatteu.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.samanatteu.dto.tontine.CycleDTO;
import com.samanatteu.service.tontine.CycleService;

@RequestMapping("/cycle")
@RestController
public class CycleController {

    private final CycleService cycleService;

    public CycleController(CycleService cycleService) {
        this.cycleService = cycleService;
    }

    @GetMapping
    public List<CycleDTO> listCycle() {
        return cycleService.listCycle();
    }

    // Clôture le cycle {id} : pas de corps, tout est décidé par CycleService.
    // Les refus sont des exceptions (404/403/409).
    @PostMapping("/{id}/cloturer")
    public CycleDTO cloturerCycle(@PathVariable Long id) {
        return cycleService.cloturerCycle(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCycle(@PathVariable Long id) {
        if (cycleService.deleteCycle(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

}
