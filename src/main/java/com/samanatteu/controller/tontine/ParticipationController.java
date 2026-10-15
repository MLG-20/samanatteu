package com.samanatteu.controller.tontine;

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

import com.samanatteu.dto.tontine.InscriptionDTO;
import com.samanatteu.dto.tontine.ModificationParticipationDTO;
import com.samanatteu.dto.tontine.ParticipationDTO;
import com.samanatteu.service.tontine.ParticipationService;

@RequestMapping("/participation")
@RestController
public class ParticipationController {

    private final ParticipationService participationService;

    public ParticipationController(ParticipationService participationService) {
        this.participationService = participationService;
    }

    @GetMapping
    public List<ParticipationDTO> listParticipation() {
        return participationService.listParticipation();
    }

    @PostMapping
    public ParticipationDTO createParticipation(@RequestBody InscriptionDTO demande) {
        return participationService.createParticipation(demande);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ParticipationDTO> updateParticipation(@PathVariable Long id,
            @RequestBody ModificationParticipationDTO modification) {
        return participationService.updateParticipation(id, modification)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteParticipation(@PathVariable Long id) {
        if (participationService.deleteParticipation(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

}
