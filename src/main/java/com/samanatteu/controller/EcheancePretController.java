package com.samanatteu.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.samanatteu.dto.pret.EcheancePretDTO;
import com.samanatteu.service.pret.EcheancePretService;

// Échéances créées seulement par accorderPret ; plus de POST/PUT/DELETE
// (un PUT permettait de se marquer « payé » sans verser l'argent).
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
}
