package com.samanatteu.controller.dashboard;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.samanatteu.dto.dashboard.TontinesStatsDTO;
import com.samanatteu.dto.dashboard.UtilisateursStatsDTO;
import com.samanatteu.service.dashboard.DashboardAdminService;

@RequestMapping("/dashboard/admin")
@RestController
public class DashboardAdminController {
    private final DashboardAdminService dashboardAdminService;

    public DashboardAdminController(DashboardAdminService dashboardAdminService) {
        this.dashboardAdminService = dashboardAdminService;
    }

    @GetMapping("/utilisateurs")
    public UtilisateursStatsDTO utilisateurs() {
        return dashboardAdminService.statistiquesUtilisateurs();
    }

    @GetMapping("/tontines")
    public TontinesStatsDTO tontines() {
        return dashboardAdminService.statistiquesTontines();
    }
}
