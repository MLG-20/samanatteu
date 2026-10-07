package com.samanatteu.service.dashboard;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import com.samanatteu.dto.dashboard.CotisationAPayerDTO;
import com.samanatteu.dto.dashboard.MaTontineDTO;
import com.samanatteu.dto.dashboard.MonGainDTO;
import com.samanatteu.dto.dashboard.MonPretDTO;
import com.samanatteu.entity.cotisation.Cotisation;
import com.samanatteu.entity.cotisation.Tirage;
import com.samanatteu.entity.pret.EcheancePret;
import com.samanatteu.entity.pret.Pret;
import com.samanatteu.entity.tontine.Participation;
import com.samanatteu.entity.tontine.Tontine;
import com.samanatteu.enums.cotisation.StatutCotisation;
import com.samanatteu.enums.pret.StatutEcheancePret;
import com.samanatteu.enums.pret.StatutPret;
import com.samanatteu.repository.cotisation.CotisationRepository;
import com.samanatteu.repository.cotisation.TirageRepository;
import com.samanatteu.repository.pret.EcheancePretRepository;
import com.samanatteu.repository.pret.PretRepository;
import com.samanatteu.repository.tontine.ParticipationRepository;
import com.samanatteu.security.UtilisateurConnecte;

// Tableau de bord du membre : même mécanisme que DashboardService, mais tout
// est filtré sur le téléphone du MEMBRE connecté (lu dans le token) : il ne
// voit que ce qui le concerne.
@Service
public class DashboardMembreService {

    private final ParticipationRepository participationRepository;
    private final TirageRepository tirageRepository;
    private final CotisationRepository cotisationRepository;
    private final PretRepository pretRepository;
    private final EcheancePretRepository echeancePretRepository;
    private final UtilisateurConnecte utilisateurConnecte;

    // public : c'est Spring, de l'extérieur, qui crée le service.
    public DashboardMembreService(ParticipationRepository participationRepository, TirageRepository tirageRepository,
            UtilisateurConnecte utilisateurConnecte, CotisationRepository cotisationRepository,
            PretRepository pretRepository, EcheancePretRepository echeancePretRepository) {
        this.participationRepository = participationRepository;
        this.tirageRepository = tirageRepository;
        this.cotisationRepository = cotisationRepository;
        this.utilisateurConnecte = utilisateurConnecte;
        this.pretRepository = pretRepository;
        this.echeancePretRepository = echeancePretRepository;
    }

    // Bloc « Mes tontines » : on part des PARTICIPATIONS du membre (c'est
    // elles qui le relient à une tontine et portent ses parts).
    public List<MaTontineDTO> maTontine() {
        List<Participation> participations = participationRepository
                .findByMembreTelephone(utilisateurConnecte.telephone());

        return participations
                .stream()
                .map(this::maTontine)
                .toList();
    }

    // Bloc « À payer » : ses cotisations pas encore soldées. La requête rend
    // TOUTES ses cotisations ; le filtre écarte les COMPLET. Ordre obligé :
    // stream() d'abord (une List ne connaît pas filter), puis filter, puis map.
    public List<CotisationAPayerDTO> cotisationAPayer() {
        List<Cotisation> cotisations = cotisationRepository
                .findByParticipationMembreTelephone(utilisateurConnecte.telephone());

        return cotisations
                .stream()
                .filter(cotisation -> cotisation.getStatut() != StatutCotisation.COMPLET)
                .map(this::cotisationAPayer)
                .toList();
    }

    // Bloc « Mes prêts » : ses prêts encore en cours. La requête rend TOUS
    // ses prêts ; le filtre écarte les REMBOURSE (plus rien à rendre).
    public List<MonPretDTO> monPret() {
        List<Pret> prets = pretRepository
                .findByMembreTelephone(utilisateurConnecte.telephone());

        return prets
                .stream()
                .filter(pret -> pret.getStatut() != StatutPret.REMBOURSE)
                .map(this::monPret)
                .toList();
    }

    // Bloc « Mes gains » : les tirages GAGNÉS par ce membre (requête par la
    // participation, pas par le gestionnaire). Pas de filtre : les gains
    // entièrement versés restent, c'est aussi son historique.
    public List<MonGainDTO> monGain() {
        List<Tirage> tirages = tirageRepository
                .findByParticipationMembreTelephone(utilisateurConnecte.telephone());

        return tirages
                .stream()
                .map(this::monGain)
                .toList();
    }

    // Construit une ligne « Mes tontines » depuis une participation.
    private MaTontineDTO maTontine(Participation participation) {
        MaTontineDTO dto = new MaTontineDTO();
        Tontine tontine = participation.getTontine();

        dto.setTontineId(tontine.getId());
        dto.setTontineNom(tontine.getNom());
        dto.setStatut(tontine.getStatut());
        dto.setNombreParts(participation.getNombreParts());
        // Ses gains = les tirages gagnés par CETTE participation.
        dto.setNombreGains(tirageRepository.countByParticipationId(participation.getId()));

        // Même formule que CycleService.ouvrirCycle : parts × montant de la
        // part (multiply, pas « * »), plus la caisse, fixe par membre.
        BigDecimal parts = tontine.getMontantPart()
                .multiply(BigDecimal.valueOf(participation.getNombreParts()));
        dto.setMontantParCycle(parts.add(tontine.getMontantCaissePret()));

        return dto;
    }

    // Construit une ligne « À payer » depuis une cotisation du membre.
    private CotisationAPayerDTO cotisationAPayer(Cotisation cotisation) {
        CotisationAPayerDTO dto = new CotisationAPayerDTO();

        dto.setCotisationId(cotisation.getId());
        dto.setTontineNom(cotisation.getCycle().getTontine().getNom());
        dto.setNumeroCycle(cotisation.getCycle().getNumeroCycle());
        // Date limite = fin PRÉVUE du cycle (après, la cotisation est en retard).
        dto.setDateLimite(cotisation.getCycle().getDateFinPrevue());
        dto.setStatut(cotisation.getStatut());

        // Même calcul que la ligne de retard du gestionnaire : part + caisse.
        BigDecimal resteDuPart = cotisation.getMontantDu().subtract(cotisation.getMontantPaye());
        BigDecimal resteDuCaisse = cotisation.getMontantCaisseDu().subtract(cotisation.getMontantCaissePaye());
        dto.setResteDu(resteDuPart.add(resteDuCaisse));

        return dto;
    }

    // Construit une ligne « Mes prêts ». Même calcul que la ligne « prêt en
    // cours » du gestionnaire (DashboardService), sans le nom du membre.
    private MonPretDTO monPret(Pret pret) {
        MonPretDTO dto = new MonPretDTO();

        dto.setPretId(pret.getId());
        dto.setTontineNom(pret.getTontine().getNom());
        // Capital + intérêt : ce qu'il doit rendre au total.
        dto.setMontantTotal(pret.getMontant().add(pret.getMontantInteret()));
        dto.setStatut(pret.getStatut());

        // Les échéances pas encore payées de CE prêt, triées par numéro.
        List<EcheancePret> restantes = echeancePretRepository
                .findByPretIdAndStatutNotOrderByNumeroEcheanceAsc(pret.getId(), StatutEcheancePret.PAYE);

        // On part de zéro et on ajoute le reste de chaque échéance.
        BigDecimal reste = BigDecimal.ZERO;
        for (EcheancePret echeance : restantes) {
            reste = reste.add(echeance.getMontantDu().subtract(echeance.getMontantPaye()));
        }
        dto.setResteARembourser(reste);

        // La première de la liste triée est la prochaine.
        if (!restantes.isEmpty()) {
            dto.setProchaineEcheance(restantes.get(0).getDateEcheance());
        }

        return dto;
    }

    // Construit une ligne « Mes gains » depuis un tirage qu'il a gagné.
    private MonGainDTO monGain(Tirage tirage) {
        MonGainDTO dto = new MonGainDTO();

        dto.setTirageId(tirage.getId());
        dto.setTontineNom(tirage.getCycle().getTontine().getNom());
        dto.setNumeroCycle(tirage.getCycle().getNumeroCycle());
        dto.setMontantGagne(tirage.getMontantGagne());
        dto.setMontantVerse(tirage.getMontantVerse());
        dto.setResteARecevoir(tirage.getMontantGagne().subtract(tirage.getMontantVerse()));
        dto.setStatut(tirage.getStatut());

        return dto;
    }
}
