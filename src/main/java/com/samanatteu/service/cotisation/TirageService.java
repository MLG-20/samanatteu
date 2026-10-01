package com.samanatteu.service.cotisation;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.samanatteu.dto.cotisation.TirageDTO;
import com.samanatteu.dto.cotisation.VersementDTO;
import com.samanatteu.entity.cotisation.Cotisation;
import com.samanatteu.entity.cotisation.Tirage;
import com.samanatteu.entity.tontine.Cycle;
import com.samanatteu.entity.tontine.Participation;
import com.samanatteu.enums.cotisation.StatutCotisation;
import com.samanatteu.enums.cotisation.StatutTirage;
import com.samanatteu.enums.tontine.StatutCycle;
import com.samanatteu.enums.tontine.StatutParticipation;
import com.samanatteu.enums.utilisateur.RoleUtilisateur;
import com.samanatteu.exception.cotisation.MontantVerseSuperieurAuDisponibleException;
import com.samanatteu.exception.cotisation.TirageDejaExistantPourCeCycleException;
import com.samanatteu.exception.cotisation.TirageIntrouvableException;
import com.samanatteu.exception.cotisation.TirageNonReportableException;
import com.samanatteu.exception.cotisation.UrneVideException;
import com.samanatteu.exception.tontine.CycleIntrouvableException;
import com.samanatteu.exception.tontine.CycleNonClotureException;
import com.samanatteu.repository.cotisation.CotisationRepository;
import com.samanatteu.repository.cotisation.TirageRepository;
import com.samanatteu.repository.tontine.CycleRepository;
import com.samanatteu.repository.tontine.ParticipationRepository;
import com.samanatteu.security.UtilisateurConnecte;

@Service
public class TirageService {
    private final TirageRepository tirageRepository;
    private final CycleRepository cycleRepository;
    private final UtilisateurConnecte utilisateurConnecte;
    private final ParticipationRepository participationRepository;
    private final CotisationRepository cotisationRepository;
    // SecureRandom (imprévisible) et non Random (prévisible) : un tirage
    // d'argent ne doit pas pouvoir être deviné (CDC US-G05, transparence).
    // Déclaré en Random : SecureRandom en hérite (polymorphisme).
    private final Random hasard = new SecureRandom();

    public TirageService(TirageRepository tirageRepository, CycleRepository cycleRepository,
            UtilisateurConnecte utilisateurConnecte, ParticipationRepository participationRepository,
            CotisationRepository cotisationRepository) {
        this.tirageRepository = tirageRepository;
        this.cycleRepository = cycleRepository;
        this.utilisateurConnecte = utilisateurConnecte;
        this.participationRepository = participationRepository;
        this.cotisationRepository = cotisationRepository;
    }

    // Lecture filtrée : le gestionnaire voit les tirages de SES tontines,
    // le membre TOUS les tirages de ses tontines (même gagnés par d'autres :
    // transparence, US-M03). L'ADMIN est bloqué en amont (SecurityConfig),
    // donc ici « pas gestionnaire » = membre.
    public List<TirageDTO> listTirage() {
        boolean estGestionnaire = utilisateurConnecte.aLeRole(RoleUtilisateur.GESTIONNAIRE);

        List<Tirage> tirages;
        if (estGestionnaire) {
            tirages = tirageRepository.findByCycleTontineGestionnaireTelephone(utilisateurConnecte.telephone());
        } else {
            List<Long> tontineIds = participationRepository.findByMembreTelephone(utilisateurConnecte.telephone())
                    .stream()
                    .map(participation -> participation.getTontine().getId())
                    .toList();
            tirages = tirageRepository.findByCycleTontineIdIn(tontineIds);
        }

        return tirages.stream()
                .map(this::convertiTirageDTO)
                .toList();
    }

    // Désigne le gagnant de la cagnotte d'un cycle (modèle B : une part =
    // un gain, chance égale pour chaque membre encore en lice).
    // @Transactional (Spring) : tirage + compensation, tout ou rien.
    @Transactional
    public TirageDTO tirerAuSort(Long cycleId) {
        // Ordre des contrôles : le droit (404, 403) avant la faisabilité
        // (409), pour ne rien révéler du cycle d'un autre gestionnaire.
        Cycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new CycleIntrouvableException());
        utilisateurConnecte.verifierGestionnaire(cycle.getTontine());

        // On ne tire qu'après la clôture : la collecte du tour est finie
        // (pratique réelle : on rassemble l'argent, puis on tire).
        if (cycle.getStatut() != StatutCycle.CLOTURE) {
            throw new CycleNonClotureException(cycle.getStatut());
        }

        // CDC : un cycle a exactement un tirage (cycle_id UNIQUE).
        if (tirageRepository.existsByCycleId(cycleId)) {
            throw new TirageDejaExistantPourCeCycleException(cycleId);
        }

        // Seuls les membres ACTIF participent (même règle que ouvrirCycle).
        List<Participation> participations = participationRepository
                .findByTontineIdAndStatut(cycle.getTontine().getId(), StatutParticipation.ACTIF);

        // L'urne n'est pas stockée : on la recalcule depuis l'historique
        // (tickets restants = parts - tirages déjà gagnés), une seule
        // source de vérité. Chaque membre en lice y figure UNE fois :
        // chance égale ; ses parts fixent seulement combien de fois il gagne.
        List<Participation> urne = new ArrayList<>();
        for (Participation participation : participations) {
            // Integer - long : Java convertit l'Integer, le résultat est long.
            long ticketsRestants = participation.getNombreParts()
                    - tirageRepository.countByParticipationId(participation.getId());
            if (ticketsRestants > 0) {
                urne.add(participation);
            }
        }

        // Plus personne en lice (plus de cycles que de parts, ou membres
        // SORTI) : 409 clair, avant que nextInt(0) ne lève une 500.
        if (urne.isEmpty()) {
            throw new UrneVideException(cycleId);
        }

        // nextInt(n) rend 0..n-1 = les indices valides de l'urne,
        // chacun avec la même probabilité : 1 chance sur n par membre.
        Participation gagnant = urne.get(hasard.nextInt(urne.size()));

        // Compensation : si le gagnant est lui-même EN_RETARD sur ce cycle,
        // il paie sa dette avec son propre gain (inutile de payer puis de se
        // faire rendre l'argent). Il recotise normalement aux cycles suivants.
        Optional<Cotisation> cotisationGagnant = cotisationRepository
                .findByCycleIdAndParticipationId(cycleId, gagnant.getId());

        // && s'arrête dès la 1re partie fausse : get() n'est appelé
        // que si l'Optional contient bien une cotisation.
        if (cotisationGagnant.isPresent()
                && cotisationGagnant.get().getStatut() == StatutCotisation.EN_RETARD) {
            Cotisation cotisation = cotisationGagnant.get();
            // resteDu et non montantDu : il a pu payer en partie (PARTIEL
            // avant la clôture), on ne compense que ce qui manque.
            BigDecimal resteDu = cotisation.getMontantDu().subtract(cotisation.getMontantPaye());

            cotisation.setMontantPaye(cotisation.getMontantDu());

            // Le gain n'efface que la dette de PART : la caisse de prêts est
            // à part, jamais compensée. COMPLET seulement si la caisse est
            // déjà payée ; sinon il reste EN_RETARD (pas de else) et ses 500
            // restent payables : enregistrerPaiement les mettra en caisse.
            if (cotisation.getMontantCaissePaye().compareTo(cotisation.getMontantCaisseDu()) == 0) {
                cotisation.setStatut(StatutCotisation.COMPLET);
            }

            cotisationRepository.save(cotisation);

            // BigDecimal est immuable : add() renvoie un NOUVEL objet, d'où
            // le set. Après ça, la cagnotte du cycle (montantCollecte, à ne
            // pas confondre avec la caisse de prêts) = ce que reçoit le gagnant.
            cycle.setMontantCollecte(cycle.getMontantCollecte().add(resteDu));
            cycleRepository.save(cycle);
        }

        // Le gagnant a droit à toute la cagnotte attendue ; on lui remet
        // tout de suite ce qu'il y a en caisse. Le reste (retards des
        // autres) lui sera reversé au fil des paiements.
        BigDecimal montantGagne = cycle.getMontantAttendu();
        BigDecimal montantVerse = cycle.getMontantCollecte();

        Tirage tirage = new Tirage();
        tirage.setCycle(cycle);
        tirage.setParticipation(gagnant);
        tirage.setMontantGagne(montantGagne);
        tirage.setMontantVerse(montantVerse);
        tirage.setDateTirage(LocalDateTime.now());
        tirage.setCreatedAt(LocalDateTime.now());

        // compareTo et non equals : equals compare aussi l'échelle
        // (40000.00 != 40000), compareTo ne compare que la valeur.
        if (montantVerse.compareTo(montantGagne) == 0) {
            tirage.setStatut(StatutTirage.VERSE);
        } else if (montantVerse.compareTo(BigDecimal.ZERO) == 0) {
            tirage.setStatut(StatutTirage.EN_ATTENTE);
        } else {
            tirage.setStatut(StatutTirage.PARTIEL);
        }

        // Pas de date de versement si rien n'a été remis (EN_ATTENTE).
        if (tirage.getStatut() != StatutTirage.EN_ATTENTE) {
            tirage.setDateVersement(LocalDateTime.now());
        }

        Tirage enregistre = tirageRepository.save(tirage);
        return convertiTirageDTO(enregistre);

    }

    // Enregistre une remise d'argent au gagnant, quand elle a RÉELLEMENT
    // lieu (décision : pas automatique au paiement d'un retard), pour que
    // montantVerse dise toujours la vérité aux membres.
    // Pas de @Transactional : un seul save, déjà « tout ou rien ».
    public TirageDTO verser(Long tirageId, VersementDTO versement) {
        // tirage → cycle → tontine : on remonte les relations pour
        // vérifier le propriétaire (frontière entre gestionnaires, SaaS).
        Tirage tirage = tirageRepository.findById(tirageId)
                .orElseThrow(() -> new TirageIntrouvableException());
        utilisateurConnecte.verifierGestionnaire(tirage.getCycle().getTontine());

        // Disponible = en caisse mais pas encore remis. compareTo > 0 :
        // strictement plus ; verser exactement le disponible est permis.
        // Un tirage VERSE a 0 de disponible : tout versement est refusé.
        BigDecimal disponible = tirage.getCycle().getMontantCollecte()
                .subtract(tirage.getMontantVerse());
        if (versement.getMontant().compareTo(disponible) > 0) {
            throw new MontantVerseSuperieurAuDisponibleException(versement.getMontant(), disponible);
        }

        tirage.setMontantVerse(tirage.getMontantVerse().add(versement.getMontant()));
        tirage.setDateVersement(LocalDateTime.now());

        // Pas d'EN_ATTENTE possible ici : le montant est > 0 (@Positive).
        if (tirage.getMontantVerse().compareTo(tirage.getMontantGagne()) == 0) {
            tirage.setStatut(StatutTirage.VERSE);
        } else {
            tirage.setStatut(StatutTirage.PARTIEL);
        }

        Tirage enregistre = tirageRepository.save(tirage);
        return convertiTirageDTO(enregistre);
    }

    // Met en pause le versement (arrangement entre membres hors plateforme).
    // Le gagnant ne change JAMAIS : le résultat du tirage est définitif.
    // Pour sortir de REPORTE : verser(), qui repasse en PARTIEL ou VERSE.
    public TirageDTO reporter(Long tirageId) {
        Tirage tirage = tirageRepository.findById(tirageId)
                .orElseThrow(() -> new TirageIntrouvableException());
        utilisateurConnecte.verifierGestionnaire(tirage.getCycle().getTontine());

        // « ni EN_ATTENTE ni PARTIEL » = pas A ET pas B (avec ||, tout
        // serait refusé). VERSE : rien à reporter ; REPORTE : déjà fait.
        if (tirage.getStatut() != StatutTirage.EN_ATTENTE
                && tirage.getStatut() != StatutTirage.PARTIEL) {
            throw new TirageNonReportableException(tirage.getStatut());
        }

        tirage.setStatut(StatutTirage.REPORTE);
        Tirage enregistre = tirageRepository.save(tirage);
        return convertiTirageDTO(enregistre);
    }

    private TirageDTO convertiTirageDTO(Tirage tirage) {
        TirageDTO dto = new TirageDTO();
        dto.setId(tirage.getId());
        dto.setCycleId(tirage.getCycle().getId());
        dto.setParticipationId(tirage.getParticipation().getId());
        dto.setMontantGagne(tirage.getMontantGagne());
        dto.setMontantVerse(tirage.getMontantVerse());
        dto.setDateTirage(tirage.getDateTirage());
        dto.setDateVersement(tirage.getDateVersement());
        dto.setStatut(tirage.getStatut());
        dto.setCreatedAt(tirage.getCreatedAt());

        return dto;
    }
}
