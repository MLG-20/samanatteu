package com.samanatteu.controller.site;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.samanatteu.dto.site.ParametreSiteDTO;
import com.samanatteu.service.site.ParametreSiteService;

import jakarta.validation.Valid;

// Les paramètres du site public : réseaux sociaux, contact, « À propos ».
//
// Lecture PUBLIQUE (le footer et les pages Contact / À propos d'un visiteur
// non connecté) ; modification réservée à l'ADMIN (SecurityConfig).
@RequestMapping("/site")
@RestController
public class ParametreSiteController {

    private final ParametreSiteService parametreSiteService;

    public ParametreSiteController(ParametreSiteService parametreSiteService) {
        this.parametreSiteService = parametreSiteService;
    }

    @GetMapping
    public ParametreSiteDTO lire() {
        return parametreSiteService.lire();
    }

    @PutMapping
    public ParametreSiteDTO modifier(@Valid @RequestBody ParametreSiteDTO dto) {
        return parametreSiteService.modifier(dto);
    }
}
