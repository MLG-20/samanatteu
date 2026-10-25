package com.samanatteu.service.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.samanatteu.dto.abonnement.AbonnementDTO;
import com.samanatteu.dto.dashboard.FicheGestionnaireDTO;
import com.samanatteu.dto.dashboard.TontinesStatsDTO;
import com.samanatteu.dto.dashboard.UtilisateursStatsDTO;
import com.samanatteu.entity.tontine.Tontine;
import com.samanatteu.entity.utilisateur.Utilisateur;
import com.samanatteu.enums.tontine.FrequenceTontine;
import com.samanatteu.enums.tontine.StatutParticipation;
import com.samanatteu.enums.tontine.StatutTontine;
import com.samanatteu.enums.utilisateur.RoleUtilisateur;
import com.samanatteu.exception.utilisateur.UtilisateurIntrouvableException;
import com.samanatteu.repository.tontine.NbMembresParTontine;
import com.samanatteu.repository.tontine.ParticipationRepository;
import com.samanatteu.repository.tontine.TontineRepository;
import com.samanatteu.repository.utilisateur.UtilisateurRepository;
import com.samanatteu.service.abonnement.AbonnementService;

// Tableau de bord admin : des compteurs globaux (COUNT en base), sans filtre
// par utilisateur connecté. 10 comptes (1 ADMIN, 3 GESTIONNAIRE, 6 MEMBRE) et
// 7 tontines (2 EN_ATTENTE, 3 ACTIVE, 1 SUSPENDUE, 1 TERMINEE).
@ExtendWith(MockitoExtension.class)
class DashboardAdminServiceTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private TontineRepository tontineRepository;
    @Mock
    private ParticipationRepository participationRepository;
    @Mock
    private AbonnementService abonnementService;

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

    // ------------------------------------------------ fiche gestionnaire

    private Utilisateur compte(Long id, RoleUtilisateur role) {
        Utilisateur compte = new Utilisateur();
        compte.setId(id);
        compte.setNom("Sarr");
        compte.setPrenom("Fatou");
        compte.setTelephone("+221770000005");
        compte.setEmail("fatou@mail.sn");
        compte.setRole(role);
        compte.setActif(true);
        return compte;
    }

    private Tontine tontine(Long id, String nom, StatutTontine statut) {
        Tontine tontine = new Tontine();
        tontine.setId(id);
        tontine.setNom(nom);
        tontine.setStatut(statut);
        tontine.setFrequence(FrequenceTontine.MOIS);
        tontine.setIntervalle(1);
        return tontine;
    }

    // Une gestionnaire avec 3 tontines (dont 1 TERMINEE) et 15 membres distincts.
    // La tontine 12 n'a aucun membre : pas de ligne dans le comptage, donc 0.
    @Test
    void ficheGestionnaire_rassembleIdentiteAbonnementEtTontines() {
        when(utilisateurRepository.findById(5L)).thenReturn(Optional.of(compte(5L, RoleUtilisateur.GESTIONNAIRE)));
        AbonnementDTO abonnement = new AbonnementDTO();
        abonnement.setFormule("PRO");
        when(abonnementService.abonnementDe(5L)).thenReturn(Optional.of(abonnement));
        when(participationRepository.compterMembresDistincts("+221770000005", StatutParticipation.SORTI))
                .thenReturn(15L);
        when(participationRepository.compterMembresParTontine(5L, StatutParticipation.SORTI))
                .thenReturn(List.of(new NbMembresParTontine(10L, 12L), new NbMembresParTontine(11L, 5L)));
        when(tontineRepository.findByGestionnaireIdOrderByCreatedAtDesc(5L)).thenReturn(List.of(
                tontine(10L, "Tontine Dakar", StatutTontine.ACTIVE),
                tontine(11L, "Tontine 2025", StatutTontine.TERMINEE),
                tontine(12L, "Épargne Thiès", StatutTontine.EN_ATTENTE)));

        FicheGestionnaireDTO fiche = dashboardAdminService.ficheGestionnaire(5L);

        assertEquals("Fatou", fiche.getPrenom());
        assertEquals("fatou@mail.sn", fiche.getEmail());
        assertEquals("PRO", fiche.getAbonnement().getFormule());
        // 15 et non 12 + 5 = 17 : un membre inscrit dans deux tontines compte une fois.
        assertEquals(15L, fiche.getNbMembres());
        // TERMINEE ne compte pas dans le quota.
        assertEquals(2L, fiche.getNbTontinesEnCours());
        assertEquals(3, fiche.getTontines().size());
        assertEquals(12L, fiche.getTontines().get(0).getNbMembres());
        assertEquals(0L, fiche.getTontines().get(2).getNbMembres());
    }

    // Pas d'abonnement n'est pas une erreur : la fiche s'affiche, abonnement à null.
    @Test
    void ficheGestionnaire_sansAbonnement_laisseLAbonnementANull() {
        when(utilisateurRepository.findById(5L)).thenReturn(Optional.of(compte(5L, RoleUtilisateur.GESTIONNAIRE)));
        when(abonnementService.abonnementDe(5L)).thenReturn(Optional.empty());
        when(tontineRepository.findByGestionnaireIdOrderByCreatedAtDesc(5L)).thenReturn(List.of());

        FicheGestionnaireDTO fiche = dashboardAdminService.ficheGestionnaire(5L);

        assertNull(fiche.getAbonnement());
        assertEquals(0L, fiche.getNbTontinesEnCours());
    }

    // Un MEMBRE n'a pas de fiche : 404, et rien d'autre n'est lu.
    @Test
    void ficheGestionnaire_dUnMembre_estIntrouvable() {
        when(utilisateurRepository.findById(17L)).thenReturn(Optional.of(compte(17L, RoleUtilisateur.MEMBRE)));

        assertThrows(UtilisateurIntrouvableException.class, () -> dashboardAdminService.ficheGestionnaire(17L));

        verifyNoInteractions(tontineRepository, participationRepository, abonnementService);
    }

    @Test
    void ficheGestionnaire_dUnIdInconnu_estIntrouvable() {
        when(utilisateurRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UtilisateurIntrouvableException.class, () -> dashboardAdminService.ficheGestionnaire(99L));
    }
}
