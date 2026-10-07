package com.samanatteu.controller.dashboard;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.samanatteu.dto.dashboard.CotisationAPayerDTO;
import com.samanatteu.dto.dashboard.MaTontineDTO;
import com.samanatteu.dto.dashboard.MonGainDTO;
import com.samanatteu.dto.dashboard.MonPretDTO;
import com.samanatteu.service.dashboard.DashboardMembreService;

// Tableau de bord du membre : un GET par bloc, tout sous /dashboard/membre,
// réservé au rôle MEMBRE par une seule règle de SecurityConfig. Le membre
// n'est jamais passé en paramètre : il est lu dans le token.
@RequestMapping("/dashboard/membre")
@RestController
public class DashboardMembreController {

    private final DashboardMembreService dashboardMembreService;

    public DashboardMembreController(DashboardMembreService dashboardMembreService) {
        this.dashboardMembreService = dashboardMembreService;
    }

    // Bloc « Mes tontines » : ses parts, ce qu'il paie par cycle, ses gains.
    @GetMapping("/tontines")
    public List<MaTontineDTO> maTontine() {
        return dashboardMembreService.maTontine();
    }

    // Bloc « À payer » : ses cotisations non soldées, reste dû et date limite.
    @GetMapping("/cotisations")
    public List<CotisationAPayerDTO> cotisationAPayer() {
        return dashboardMembreService.cotisationAPayer();
    }

    // Bloc « Mes prêts » : reste à rembourser et prochaine échéance.
    @GetMapping("/prets")
    public List<MonPretDTO> monPret() {
        return dashboardMembreService.monPret();
    }

    // Bloc « Mes gains » : gagné, déjà reçu, reste à recevoir (même mot
    // que /dashboard/gestionnaire/gains).
    @GetMapping("/gains")
    public List<MonGainDTO> monGain() {
        return dashboardMembreService.monGain();
    }
}
