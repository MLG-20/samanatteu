package com.samanatteu.service.dashboard;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import com.samanatteu.dto.dashboard.CotisationRetardDTO;
import com.samanatteu.dto.dashboard.GainAVerserDTO;
import com.samanatteu.dto.dashboard.PretEnCoursDTO;
import com.samanatteu.dto.dashboard.TirageAFaireDTO;
import com.samanatteu.dto.dashboard.TontineResumeDTO;
import com.samanatteu.entity.cotisation.Cotisation;
import com.samanatteu.entity.cotisation.Tirage;
import com.samanatteu.entity.pret.EcheancePret;
import com.samanatteu.entity.pret.Pret;
import com.samanatteu.entity.tontine.Cycle;
import com.samanatteu.entity.tontine.Tontine;
import com.samanatteu.entity.utilisateur.Utilisateur;
import com.samanatteu.enums.cotisation.StatutCotisation;
import com.samanatteu.enums.cotisation.StatutTirage;
import com.samanatteu.enums.pret.StatutEcheancePret;
import com.samanatteu.enums.pret.StatutPret;
import com.samanatteu.enums.tontine.StatutCycle;
import com.samanatteu.enums.tontine.StatutParticipation;
import com.samanatteu.repository.cotisation.CotisationRepository;
import com.samanatteu.repository.cotisation.TirageRepository;
import com.samanatteu.repository.pret.EcheancePretRepository;
import com.samanatteu.repository.pret.PretRepository;
import com.samanatteu.repository.tontine.CycleRepository;
import com.samanatteu.repository.tontine.ParticipationRepository;
import com.samanatteu.repository.tontine.TontineRepository;
import com.samanatteu.security.UtilisateurConnecte;

// Tableau de bord du gestionnaire : des lectures calculées (rien n'est
// stocké), un bloc = une méthode publique qui rend la liste + une méthode
// privée qui construit une ligne. Le gestionnaire est toujours celui du token.
@Service
public class DashboardService {
    private final TontineRepository tontineRepository;
    private final ParticipationRepository participationRepository;
    private final CycleRepository cycleRepository;
    private final CotisationRepository cotisationRepository;
    private final PretRepository pretRepository;
    private final EcheancePretRepository echeancePretRepository;
    private final TirageRepository tirageRepository;
    private final UtilisateurConnecte utilisateurConnecte;

    public DashboardService(TontineRepository tontineRepository, ParticipationRepository participationRepository,
            CycleRepository cycleRepository, UtilisateurConnecte utilisateurConnecte,
            CotisationRepository cotisationRepository, PretRepository pretRepository,
            EcheancePretRepository echeancePretRepository, TirageRepository tirageRepository) {
        this.tontineRepository = tontineRepository;
        this.participationRepository = participationRepository;
        this.cycleRepository = cycleRepository;
        this.cotisationRepository = cotisationRepository;
        this.pretRepository = pretRepository;
        this.echeancePretRepository = echeancePretRepository;
        this.tirageRepository = tirageRepository;
        this.utilisateurConnecte = utilisateurConnecte;
    }

    // Bloc « Mes tontines » : les tontines du gestionnaire connecté (téléphone
    // lu dans le token, jamais reçu du client), une ligne résumé chacune.
    public List<TontineResumeDTO> mesTontines() {
        List<Tontine> tontines = tontineRepository.findByGestionnaireTelephone(utilisateurConnecte.telephone());

        return tontines.stream()
                .map(this::resume)
                .toList();
    }

    // Bloc « Cotisations en retard » : le repository renvoie des entités
    // Cotisation, c'est le map qui en fait des lignes à afficher.
    public List<CotisationRetardDTO> cotisationRetard() {
        List<Cotisation> cotisations = cotisationRepository
                .findByStatutAndCycleTontineGestionnaireTelephone(StatutCotisation.EN_RETARD,
                        utilisateurConnecte.telephone());
        return cotisations
                .stream()
                .map(this::retard)
                .toList();
    }

    // Bloc « Prêts en cours » : les prêts ACTIF ou EN_RETARD (pas les
    // REMBOURSE) des tontines du gestionnaire connecté. Même nom que la
    // méthode privée plus bas : Java les distingue par leurs paramètres
    // (surcharge), celle-ci renvoie la liste, l'autre construit une ligne.
    public List<PretEnCoursDTO> pretEnCours() {
        List<Pret> prets = pretRepository
                .findByStatutInAndTontineGestionnaireTelephone(List.of(StatutPret.ACTIF, StatutPret.EN_RETARD),
                        utilisateurConnecte.telephone());

        return prets
                .stream()
                .map(this::pretEnCours)
                .toList();
    }

    // Bloc « Tirages à faire » : les cycles clôturés sans tirage. On part des
    // CYCLES (le tirage n'existe pas encore). filter garde, map transforme :
    // d'abord les deux tris, ensuite cycle -> ligne.
    public List<TirageAFaireDTO> tirageAFaire() {
        List<Cycle> cycles = cycleRepository
                .findByTontineGestionnaireTelephone(utilisateurConnecte.telephone());

        return cycles.stream()
                // On ne tire qu'après la clôture (règle de TirageService).
                .filter(cycle -> cycle.getStatut() == StatutCycle.CLOTURE)
                // « ! » = non : on garde les cycles qui n'ont PAS de tirage.
                .filter(cycle -> !tirageRepository.existsByCycleId(cycle.getId()))
                .map(this::tirageAFaire)
                .toList();
    }

    // Bloc « Gains à verser » : les tirages faits dont le gagnant n'a pas
    // tout reçu. On part des TIRAGES (ils existent), et on écarte les VERSE
    // (« != » = différent de) : il ne reste que EN_ATTENTE, PARTIEL, REPORTE.
    public List<GainAVerserDTO> gainAVerse() {
        List<Tirage> tirages = tirageRepository
                .findByCycleTontineGestionnaireTelephone(utilisateurConnecte.telephone());

        return tirages.stream()
                .filter(tirage -> tirage.getStatut() != StatutTirage.VERSE)
                .map(this::gainAVerse)
                .toList();
    }

    // Construit une ligne « Mes tontines » : infos de la tontine + chiffres calculés.
    private TontineResumeDTO resume(Tontine tontine) {
        TontineResumeDTO dto = new TontineResumeDTO();
        dto.setId(tontine.getId());
        dto.setSoldeCaissePret(tontine.getSoldeCaissePret());
        dto.setNom(tontine.getNom());
        dto.setStatut(tontine.getStatut());
        dto.setNombreMembres(
                participationRepository.countByTontineIdAndStatut(tontine.getId(), StatutParticipation.ACTIF));

        // Le cycle EN_COURS n'existe pas toujours (tontine EN_ATTENTE) :
        // ifPresent ne remplit les champs que s'il y en a un, sinon ils restent null.
        cycleRepository.findByTontineIdAndStatut(tontine.getId(), StatutCycle.EN_COURS)
                .ifPresent(cycle -> {
                    dto.setNumeroCycleEnCours(cycle.getNumeroCycle());
                    dto.setMontantCollecte(cycle.getMontantCollecte());
                    dto.setMontantAttendu(cycle.getMontantAttendu());
                });

        return dto;
    }

    // Construit une ligne « Cotisations en retard » : qui relancer, et pour combien.
    private CotisationRetardDTO retard(Cotisation cotisation) {
        CotisationRetardDTO dto = new CotisationRetardDTO();
        dto.setCotisationId(cotisation.getId());
        dto.setTontineNom(cotisation.getCycle().getTontine().getNom());
        dto.setNumeroCycle(cotisation.getCycle().getNumeroCycle());

        Utilisateur membre = cotisation.getParticipation().getMembre();
        dto.setMembreNom(membre.getPrenom() + " " + membre.getNom());
        dto.setMembreTelephone(membre.getTelephone());

        // Reste dû = reste de la part + reste de la caisse de prêts : même
        // formule que CotisationService.enregistrerPaiement, pour que le montant affiché
        // soit exactement celui que le paiement acceptera.
        BigDecimal resteDuPart = cotisation.getMontantDu().subtract(cotisation.getMontantPaye());
        BigDecimal resteDuCaisse = cotisation.getMontantCaisseDu().subtract(cotisation.getMontantCaissePaye());
        dto.setResteDu(resteDuPart.add(resteDuCaisse));

        return dto;
    }

    // Construit une ligne « Prêts en cours » : à qui, combien il reste, et
    // quand tombe la prochaine échéance.
    private PretEnCoursDTO pretEnCours(Pret pret) {
        PretEnCoursDTO dto = new PretEnCoursDTO();
        dto.setPretId(pret.getId());
        dto.setTontineNom(pret.getTontine().getNom());

        Utilisateur membre = pret.getMembre();
        dto.setMembreNom(membre.getPrenom() + " " + membre.getNom());
        dto.setMembreTelephone(membre.getTelephone());

        // Le statut réel du prêt (jamais une valeur fixe) : c'est lui qui
        // montre un retard de remboursement au gestionnaire.
        dto.setStatut(pret.getStatut());
        dto.setMontantTotal(pret.getMontant().add(pret.getMontantInteret()));

        // Les échéances pas encore payées (EN_ATTENTE ou EN_RETARD), triées
        // par numéro : elles servent au reste ET à la prochaine échéance.
        List<EcheancePret> restantes = echeancePretRepository
                .findByPretIdAndStatutNotOrderByNumeroEcheanceAsc(pret.getId(), StatutEcheancePret.PAYE);

        // Le reste n'est stocké nulle part : on l'additionne. add() renvoie
        // un nouveau BigDecimal, d'où « reste = reste.add(...) ».
        BigDecimal reste = BigDecimal.ZERO;
        for (EcheancePret echeance : restantes) {
            reste = reste.add(echeance.getMontantDu().subtract(echeance.getMontantPaye()));
        }
        dto.setResteARembourser(reste);

        // Liste triée : la première est la prochaine. isEmpty() d'abord,
        // sinon get(0) plante sur une liste vide.
        if (!restantes.isEmpty()) {
            dto.setProchaineEcheance(restantes.get(0).getDateEcheance());
        }

        return dto;
    }

    // Construit une ligne « Tirages à faire » depuis un cycle clôturé.
    private TirageAFaireDTO tirageAFaire(Cycle cycle) {
        TirageAFaireDTO dto = new TirageAFaireDTO();
        dto.setCycleId(cycle.getId());
        dto.setTontineNom(cycle.getTontine().getNom());
        dto.setNumeroCycle(cycle.getNumeroCycle());
        // dateFinReelle (jour réel de la clôture), pas dateFinPrevue.
        dto.setDateCloture(cycle.getDateFinReelle());
        dto.setMontantCollecte(cycle.getMontantCollecte());
        dto.setMontantAttendu(cycle.getMontantAttendu());

        return dto;
    }

    // Construit une ligne « Gains à verser » depuis un tirage déjà fait.
    // Getters enchaînés : chaque « . » est un pas (du tirage, prends le
    // cycle ; de ce cycle, prends la tontine ; de cette tontine, le nom).
    private GainAVerserDTO gainAVerse(Tirage tirage) {
        GainAVerserDTO dto = new GainAVerserDTO();
        dto.setTirageId(tirage.getId());
        dto.setTontineNom(tirage.getCycle().getTontine().getNom());
        dto.setNumeroCycle(tirage.getCycle().getNumeroCycle());

        // tirage -> participation (celle du gagnant) -> membre.
        Utilisateur gagnant = tirage.getParticipation().getMembre();
        dto.setGagnantNom(gagnant.getPrenom() + " " + gagnant.getNom());
        dto.setGagnantTelephone(gagnant.getTelephone());
        dto.setMontantGagne(tirage.getMontantGagne());
        dto.setMontantVerse(tirage.getMontantVerse());
        dto.setResteAVerser(tirage.getMontantGagne().subtract(tirage.getMontantVerse()));
        dto.setStatut(tirage.getStatut());

        return dto;
    }
}
