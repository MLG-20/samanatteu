package com.samanatteu.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.samanatteu.dto.cotisation.VersementDTO;
import com.samanatteu.dto.pret.PretDTO;
import com.samanatteu.service.pret.PretService;

import jakarta.validation.Valid;

// Plus de POST/PUT/DELETE génériques : un prêt s'accorde par
// POST /tontine/{id}/prets et ne se modifie/supprime pas (fait historique).
@RequestMapping("/pret")
@RestController
public class PretController {
    private final PretService pretService;

    public PretController(PretService pretService) {
        this.pretService = pretService;
    }

    @GetMapping
    public List<PretDTO> listPret() {
        return pretService.listPret();
    }

    // Remboursement réparti sur les échéances les plus anciennes.
    @PostMapping("/{id}/remboursement")
    public PretDTO rembourser(@PathVariable Long id, @Valid @RequestBody VersementDTO versement) {
        return pretService.rembourser(id, versement);
    }
}
