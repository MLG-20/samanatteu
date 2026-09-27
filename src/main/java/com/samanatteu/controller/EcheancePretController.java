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

import com.samanatteu.dto.pret.EcheancePretDTO;
import com.samanatteu.entity.EcheancePret;


import com.samanatteu.service.pret.EcheancePretService;

@RequestMapping("/echeancePret")
@RestController
public class EcheancePretController {
    private final EcheancePretService echeancePretService;

    public EcheancePretController(EcheancePretService echeancePretService) {
        this.echeancePretService = echeancePretService;
    }

    @GetMapping
    public List<EcheancePretDTO> listEcheancePrets() {
        return echeancePretService.listEcheancePret();
    }

    @PostMapping
    public EcheancePretDTO createEcheancePret(@RequestBody EcheancePret echeancePret) {
        return echeancePretService.createEcheancePret(echeancePret);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EcheancePretDTO> updateEcheancePret(@PathVariable Long id,
            @RequestBody EcheancePret echeancePretModifier) {
        return echeancePretService.updateEcheancePret(id, echeancePretModifier)
               .map(ResponseEntity::ok)
               .orElse(ResponseEntity.notFound().build());
     
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEcheancePret(@PathVariable Long id) {
        if (echeancePretService.deleteEcheancePret(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

}
