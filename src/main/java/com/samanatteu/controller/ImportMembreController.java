package com.samanatteu.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.samanatteu.dto.onboarding.ImportMembreDTO;
import com.samanatteu.service.onboarding.ImportMembreService;

@RequestMapping("/importMembre")
@RestController
public class ImportMembreController {
    private final ImportMembreService importMembreService;

    public ImportMembreController(ImportMembreService importMembreService) {
        this.importMembreService = importMembreService;
    }

    // Lecture seule : un rapport d'import est une trace écrite par le serveur,
    // le client ne peut ni le créer, ni le modifier, ni l'effacer.
    @GetMapping
    public List<ImportMembreDTO> listImportMembres() {
        return importMembreService.listImportMembre();
    }
}
