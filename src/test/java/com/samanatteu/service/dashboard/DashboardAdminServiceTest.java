package com.samanatteu.service.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.samanatteu.dto.dashboard.TontinesStatsDTO;
import com.samanatteu.dto.dashboard.UtilisateursStatsDTO;
import com.samanatteu.enums.tontine.StatutTontine;
import com.samanatteu.enums.utilisateur.RoleUtilisateur;
import com.samanatteu.repository.tontine.TontineRepository;
import com.samanatteu.repository.utilisateur.UtilisateurRepository;

// Tableau de bord admin : des compteurs globaux (COUNT en base), sans filtre
// par utilisateur connecté. 10 comptes (1 ADMIN, 3 GESTIONNAIRE, 6 MEMBRE) et
// 7 tontines (2 EN_ATTENTE, 3 ACTIVE, 1 SUSPENDUE, 1 TERMINEE).
@ExtendWith(MockitoExtension.class)
class DashboardAdminServiceTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private TontineRepository tontineRepository;

    @InjectMocks
    private DashboardAdminService dashboardAdminService;

    // Total + détail par rôle : la somme des trois vaut le total.
    @Test
    void statistiquesUtilisateurs_additionneLesComptesParRole() {
        when(utilisateurRepository.count()).thenReturn(10L);
        when(utilisateurRepository.countByRole(RoleUtilisateur.ADMIN)).thenReturn(1L);
        when(utilisateurRepository.countByRole(RoleUtilisateur.GESTIONNAIRE)).thenReturn(3L);
        when(utilisateurRepository.countByRole(RoleUtilisateur.MEMBRE)).thenReturn(6L);

        UtilisateursStatsDTO resultat = dashboardAdminService.statistiquesUtilisateurs();

        assertEquals(10L, resultat.getTotal());
        assertEquals(1L, resultat.getNbAdmins());
        assertEquals(3L, resultat.getNbGestionnaires());
        assertEquals(6L, resultat.getNbMembres());
        // Les chiffres viennent de la base, jamais d'un calcul en mémoire.
        verify(utilisateurRepository).count();
    }

    // Total + détail par statut : la somme des quatre vaut le total.
    @Test
    void statistiquesTontines_additionneLesTontinesParStatut() {
        when(tontineRepository.count()).thenReturn(7L);
        when(tontineRepository.countByStatut(StatutTontine.EN_ATTENTE)).thenReturn(2L);
        when(tontineRepository.countByStatut(StatutTontine.ACTIVE)).thenReturn(3L);
        when(tontineRepository.countByStatut(StatutTontine.SUSPENDUE)).thenReturn(1L);
        when(tontineRepository.countByStatut(StatutTontine.TERMINEE)).thenReturn(1L);

        TontinesStatsDTO resultat = dashboardAdminService.statistiquesTontines();

        assertEquals(7L, resultat.getTotal());
        assertEquals(2L, resultat.getNbEnAttente());
        assertEquals(3L, resultat.getNbActive());
        assertEquals(1L, resultat.getNbSuspendue());
        assertEquals(1L, resultat.getNbTerminee());
        verify(tontineRepository).count();
    }
}
