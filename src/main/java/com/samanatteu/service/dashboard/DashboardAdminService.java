package com.samanatteu.service.dashboard;

import org.springframework.stereotype.Service;

import com.samanatteu.dto.dashboard.TontinesStatsDTO;
import com.samanatteu.dto.dashboard.UtilisateursStatsDTO;
import com.samanatteu.enums.tontine.StatutTontine;
import com.samanatteu.enums.utilisateur.RoleUtilisateur;
import com.samanatteu.repository.tontine.TontineRepository;
import com.samanatteu.repository.utilisateur.UtilisateurRepository;

@Service
public class DashboardAdminService {
    private final UtilisateurRepository utilisateurRepository;
    private final TontineRepository tontineRepository;

    public DashboardAdminService(UtilisateurRepository utilisateurRepository,
            TontineRepository tontineRepository) {
        this.utilisateurRepository = utilisateurRepository;
        this.tontineRepository = tontineRepository;
    }

    public UtilisateursStatsDTO statistiquesUtilisateurs() {
        UtilisateursStatsDTO dto = new UtilisateursStatsDTO();

        dto.setTotal(utilisateurRepository.count());
        dto.setNbAdmins(utilisateurRepository.countByRole(RoleUtilisateur.ADMIN));
        dto.setNbGestionnaires(utilisateurRepository.countByRole(RoleUtilisateur.GESTIONNAIRE));
        dto.setNbMembres(utilisateurRepository.countByRole(RoleUtilisateur.MEMBRE));

        return dto;
    }

    public TontinesStatsDTO statistiquesTontines() {
        TontinesStatsDTO dto = new TontinesStatsDTO();
        dto.setTotal(tontineRepository.count());
        dto.setNbEnAttente(tontineRepository.countByStatut(StatutTontine.EN_ATTENTE));
        dto.setNbActive(tontineRepository.countByStatut(StatutTontine.ACTIVE));
        dto.setNbSuspendue(tontineRepository.countByStatut(StatutTontine.SUSPENDUE));
        dto.setNbTerminee(tontineRepository.countByStatut(StatutTontine.TERMINEE));

        return dto;
    }
}
