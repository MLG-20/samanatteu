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

import com.samanatteu.dto.onboarding.ImportMembreDTO;
import com.samanatteu.entity.onboarding.ImportMembre;
import com.samanatteu.service.onboarding.ImportMembreService;

@RequestMapping("/importMembre")
@RestController
public class ImportMembreController {
    private final ImportMembreService importMembreService;

    public ImportMembreController(ImportMembreService importMembreService) {
        this.importMembreService = importMembreService;
    }

    @GetMapping
    public List<ImportMembreDTO> listImportMembres() {
        return importMembreService.listImportMembre();
    }

    @PostMapping

    public ImportMembreDTO createImportMembre(@RequestBody ImportMembre importMembre) {
        return importMembreService.createImportMembre(importMembre);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ImportMembreDTO> updateImportMembre(@PathVariable Long id,
            @RequestBody ImportMembre importMembreModifier) {
        return importMembreService.updateImportMembre(id, importMembreModifier)
               .map(ResponseEntity::ok)
               .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteImportMembre(@PathVariable Long id) {
        if (importMembreService.deleteImportMembre(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
