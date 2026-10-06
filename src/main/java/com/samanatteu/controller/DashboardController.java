package com.samanatteu.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.samanatteu.dto.dashboard.CotisationRetardDTO;
import com.samanatteu.dto.dashboard.GainAVerserDTO;
import com.samanatteu.dto.dashboard.PretEnCoursDTO;
import com.samanatteu.dto.dashboard.TirageAFaireDTO;
import com.samanatteu.dto.dashboard.TontineResumeDTO;
import com.samanatteu.service.dashboard.DashboardService;

// Tableau de bord du gestionnaire : un GET par bloc, qui renvoie des lignes
// déjà calculées. Tout est sous /dashboard/gestionnaire, réservé à ce rôle
// par une seule règle de SecurityConfig.
@RequestMapping("/dashboard/gestionnaire")
@RestController
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    // Bloc « Mes tontines » du tableau de bord gestionnaire.
    @GetMapping("/tontines")
    public List<TontineResumeDTO> mesTontines() {
        return dashboardService.mesTontines();
    }

    // Bloc « Cotisations en retard » : qui relancer, et pour combien.
    // Pas de règle à ajouter : /dashboard/gestionnaire/** couvre déjà cette URL.
    @GetMapping("/retards")
    public List<CotisationRetardDTO> cotisationRetard() {
        return dashboardService.cotisationRetard();
    }

    // Bloc « Prêts en cours » : à qui, combien il reste, prochaine échéance.
    @GetMapping("/prets")
    public List<PretEnCoursDTO> pretEnCours() {
        return dashboardService.pretEnCours();
    }

    // Bloc « Tirages à faire » : les cycles clôturés qui attendent leur tirage.
    @GetMapping("/tirages")
    public List<TirageAFaireDTO> tirageAFaire() {
        return dashboardService.tirageAFaire();
    }

    // Bloc « Gains à verser » : les gagnants qui n'ont pas tout reçu. URL
    // distincte de /tirages : deux GET sur la même URL = « Ambiguous
    // mapping », l'application refuse de démarrer.
    @GetMapping("/gains")
    public List<GainAVerserDTO> gainAVerser() {
        return dashboardService.gainAVerse();
    }
}
