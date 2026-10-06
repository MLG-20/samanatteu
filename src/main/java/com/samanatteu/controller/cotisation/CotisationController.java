package com.samanatteu.controller.cotisation;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.samanatteu.dto.cotisation.CotisationDTO;
import com.samanatteu.dto.cotisation.PaiementDTO;
import com.samanatteu.service.cotisation.CotisationService;

import jakarta.validation.Valid;

@RequestMapping("/cotisation")
@RestController
public class CotisationController {
    private final CotisationService cotisationService;

    public CotisationController(CotisationService cotisationService) {
        this.cotisationService = cotisationService;
    }

    @GetMapping
    public List<CotisationDTO> listCotisation() {
        return cotisationService.listCotisations();
    }

    // Enregistre un paiement sur la cotisation {id}. @Valid vérifie PaiementDTO
    // (@NotNull, @Positive) AVANT le service : sinon 400. Pas de
    // ResponseEntity : les refus sont des exceptions (404/403/409/400).
    @PostMapping("/{id}/paiement")
    public CotisationDTO enregistrerPaiement(@PathVariable Long id,
            @Valid @RequestBody PaiementDTO paiement) {
        return cotisationService.enregistrerPaiement(id, paiement);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCotisation(@PathVariable Long id) {
        if (cotisationService.deleteCotisation(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
