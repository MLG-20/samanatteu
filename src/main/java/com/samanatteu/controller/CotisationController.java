package com.samanatteu.controller;

import java.util.List;

import com.samanatteu.entity.Cotisation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.samanatteu.dto.cotisation.CotisationDTO;
import com.samanatteu.service.cotisation.CotisationService;

@RequestMapping("/cotisation")
@RestController
public class CotisationController {
    private final CotisationService cotisationService;

    public CotisationController(CotisationService cotisationService) {
        this.cotisationService = cotisationService;
    }

    // --- Lister ---
    @GetMapping
    public List<CotisationDTO> listCotisation() {
        return cotisationService.listCotisations();
    }

    // --- Créer ---
    @PostMapping
    /*
     * @RequestBody CotisationDTO cotisation — dit à Spring "prends le JSON
     * envoyé dans le
     * corps
     * de la requête, et convertis-le automatiquement en objet CotisationDTO"
     */
    public CotisationDTO createCotisation(@RequestBody Cotisation cotisation) {
        return cotisationService.createCotisation(cotisation);
    }

    // --- Update ---
    @PutMapping("/{id}")
    public ResponseEntity<CotisationDTO> updateCotisation(@PathVariable Long id,
            @RequestBody Cotisation cotisationModifier) {
        return cotisationService.updateCotisation(id, cotisationModifier)
               .map(ResponseEntity::ok)
               .orElse(ResponseEntity.notFound().build());
        }

    // --- DELETE ---
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCotisation(@PathVariable Long id) {
        if (cotisationService.deleteCotisation(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

}
