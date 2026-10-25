package com.samanatteu.service.dashboard;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.samanatteu.dto.dashboard.FicheGestionnaireDTO;
import com.samanatteu.dto.dashboard.TontineFicheDTO;
import com.samanatteu.dto.dashboard.TontinesStatsDTO;
import com.samanatteu.dto.dashboard.UtilisateursStatsDTO;
import com.samanatteu.entity.tontine.Tontine;
import com.samanatteu.entity.utilisateur.Utilisateur;
import com.samanatteu.enums.tontine.StatutParticipation;
import com.samanatteu.enums.tontine.StatutTontine;
import com.samanatteu.enums.utilisateur.RoleUtilisateur;
import com.samanatteu.exception.utilisateur.UtilisateurIntrouvableException;
import com.samanatteu.repository.tontine.NbMembresParTontine;
import com.samanatteu.repository.tontine.ParticipationRepository;
import com.samanatteu.repository.tontine.TontineRepository;
import com.samanatteu.repository.utilisateur.UtilisateurRepository;
import com.samanatteu.service.abonnement.AbonnementService;

@Service
public class DashboardAdminService {
    private final UtilisateurRepository utilisateurRepository;
    private final TontineRepository tontineRepository;
    private final ParticipationRepository participationRepository;
    private final AbonnementService abonnementService;

    public DashboardAdminService(UtilisateurRepository utilisateurRepository,
            TontineRepository tontineRepository, ParticipationRepository participationRepository,
            AbonnementService abonnementService) {
        this.utilisateurRepository = utilisateurRepository;
        this.tontineRepository = tontineRepository;
        this.participationRepository = participationRepository;
        this.abonnementService = abonnementService;
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

    // La fiche d'une gestionnaire : identité, abonnement, utilisation, tontines
    // (sans argent ni membre nommé). 404 si l'id n'est pas celui d'une
    // gestionnaire : un MEMBRE ou un ADMIN n'a pas de fiche, et la réponse ne
    // dit même pas qu'un compte existe à cet id.
    public FicheGestionnaireDTO ficheGestionnaire(Long id) {
        Utilisateur gestionnaire = utilisateurRepository.findById(id)
                .filter(u -> u.getRole() == RoleUtilisateur.GESTIONNAIRE)
                .orElseThrow(() -> new UtilisateurIntrouvableException(id));

        FicheGestionnaireDTO fiche = new FicheGestionnaireDTO();
        fiche.setId(gestionnaire.getId());
        fiche.setNom(gestionnaire.getNom());
        fiche.setPrenom(gestionnaire.getPrenom());
        fiche.setTelephone(gestionnaire.getTelephone());
        fiche.setEmail(gestionnaire.getEmail());
        fiche.setActif(gestionnaire.getActif());
        fiche.setCreatedAt(gestionnaire.getCreatedAt());

        // orElse(null) : pas d'abonnement n'est pas une erreur, la fiche le montre.
        fiche.setAbonnement(abonnementService.abonnementDe(id).orElse(null));
        fiche.setNbMembres(participationRepository.compterMembresDistincts(gestionnaire.getTelephone(),
                StatutParticipation.SORTI));

        // Une requête pour les nombres de membres de toutes ses tontines, rangée en
        // Map tontineId -> nombre (même principe que la liste des gestionnaires).
        Map<Long, Long> membresParTontine = participationRepository
                .compterMembresParTontine(id, StatutParticipation.SORTI)
                .stream()
                .collect(Collectors.toMap(NbMembresParTontine::tontineId, NbMembresParTontine::nbMembres));

        List<TontineFicheDTO> tontines = tontineRepository.findByGestionnaireIdOrderByCreatedAtDesc(id)
                .stream()
                .map(tontine -> versLigneFiche(tontine, membresParTontine.getOrDefault(tontine.getId(), 0L)))
                .toList();
        fiche.setTontines(tontines);
        // Même règle que le quota (TontineService) : une tontine TERMINEE ne compte plus.
        fiche.setNbTontinesEnCours(tontines.stream()
                .filter(t -> t.getStatut() != StatutTontine.TERMINEE)
                .count());

        return fiche;
    }

    // Ce qu'on recopie d'une tontine : rien qui touche à l'argent.
    private TontineFicheDTO versLigneFiche(Tontine tontine, Long nbMembres) {
        TontineFicheDTO ligne = new TontineFicheDTO();
        ligne.setId(tontine.getId());
        ligne.setNom(tontine.getNom());
        ligne.setStatut(tontine.getStatut());
        ligne.setFrequence(tontine.getFrequence());
        ligne.setIntervalle(tontine.getIntervalle());
        ligne.setNbMembres(nbMembres);
        ligne.setCreatedAt(tontine.getCreatedAt());
        return ligne;
    }
}
