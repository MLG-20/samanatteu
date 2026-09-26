package com.samanatteu.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.samanatteu.dto.tontine.ParticipationDTO;
import com.samanatteu.entity.Participation;
import com.samanatteu.service.tontine.ParticipationService;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RequestMapping("/participation")
@RestController
public class ParticipationController {

    private final ParticipationService participationService;

    public ParticipationController(ParticipationService participationService) {
        this.participationService = participationService;
    }

    // --- Lister ---
    @GetMapping
    public List<ParticipationDTO> listParticipation() {
        return participationService.listParticipation();
    }

    // --- Créer ---
    @PostMapping
    /*
     * @RequestBody ParticipationDTO participation — dit à Spring "prends le JSON
     * envoyé dans le
     * corps
     * de la requête, et convertis-le automatiquement en objet Tontine"
     */
    public ParticipationDTO createParticipation(@RequestBody Participation participation) {
        return participationService.createParticipation(participation);
    }

    // --- Update ---
    @PutMapping("/{id}")
    public ResponseEntity<ParticipationDTO> updateParticipation(@PathVariable Long id,
            @RequestBody Participation participationModifier) {
        return participationService.updateParticipation(id, participationModifier)
               .map(ResponseEntity::ok)
               .orElse(ResponseEntity.notFound().build());
    }

    // --- DELETE ---
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteParticipation(@PathVariable Long id) {
        if (participationService.deleteParticipation(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

}
