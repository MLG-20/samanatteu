package com.samanatteu.controller.cotisation;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.samanatteu.dto.cotisation.TirageDTO;
import com.samanatteu.dto.cotisation.VersementDTO;
import com.samanatteu.service.cotisation.TirageService;

import jakarta.validation.Valid;

@RequestMapping("/tirage")
@RestController
public class TirageController {
    private final TirageService tirageService;

    public TirageController(TirageService tirageService) {
        this.tirageService = tirageService;
    }

    @GetMapping
    public List<TirageDTO> listTirages() {
        return tirageService.listTirage();
    }

    // Remise d'argent au gagnant du tirage {id} (id de TIRAGE : d'où /tirage).
    // @RequestBody lit le JSON ; @Valid applique @NotNull/@Positive du DTO
    // AVANT le service (sinon 400). Refus = exceptions (404/403/400).
    @PostMapping("/{id}/verser")
    public TirageDTO verser(@PathVariable Long id,
            @Valid @RequestBody VersementDTO versement) {
        return tirageService.verser(id, versement);
    }

    // Reporte le versement du tirage {id}. Pas de corps : rien à choisir
    // côté client. Refus = exceptions (404/403/409).
    @PostMapping("/{id}/reporter")
    public TirageDTO reporter(@PathVariable Long id) {
        return tirageService.reporter(id);
    }

}
