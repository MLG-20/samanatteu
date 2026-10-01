package com.samanatteu.service.pret;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.samanatteu.dto.cotisation.VersementDTO;
import com.samanatteu.dto.pret.DemandePretDTO;
import com.samanatteu.dto.pret.PretDTO;
import com.samanatteu.entity.pret.EcheancePret;
import com.samanatteu.entity.pret.Pret;
import com.samanatteu.entity.tontine.Participation;
import com.samanatteu.entity.tontine.Tontine;
import com.samanatteu.enums.pret.StatutEcheancePret;
import com.samanatteu.enums.pret.StatutPret;
import com.samanatteu.enums.pret.TypeTransaction;
import com.samanatteu.enums.tontine.StatutParticipation;
import com.samanatteu.enums.tontine.StatutTontine;
import com.samanatteu.enums.utilisateur.RoleUtilisateur;
import com.samanatteu.exception.cotisation.MontantPayeSuperieurAuDuException;
import com.samanatteu.exception.pret.CaisseInsuffisanteException;
import com.samanatteu.exception.pret.MembreNonParticipantException;
import com.samanatteu.exception.pret.PretDejaEnCoursException;
import com.samanatteu.exception.pret.PretDejaRembourseException;
import com.samanatteu.exception.pret.PretIntrouvableException;
import com.samanatteu.exception.tontine.TontineIntrouvableException;
import com.samanatteu.exception.tontine.TontineNonActiveException;
import com.samanatteu.repository.pret.EcheancePretRepository;
import com.samanatteu.repository.pret.PretRepository;
import com.samanatteu.repository.tontine.ParticipationRepository;
import com.samanatteu.repository.tontine.TontineRepository;
import com.samanatteu.security.UtilisateurConnecte;

@Service
public class PretService {
    private final PretRepository pretRepository;
    private final TontineRepository tontineRepository;
    private final UtilisateurConnecte utilisateurConnecte;
    private final ParticipationRepository participationRepository;
    private final EcheancePretRepository echeancePretRepository;
    private final TransactionService transactionService;

    public PretService(PretRepository pretRepository, TontineRepository tontineRepository,
            UtilisateurConnecte utilisateurConnecte, ParticipationRepository participationRepository,
            EcheancePretRepository echeancePretRepository, TransactionService transactionService) {
        this.pretRepository = pretRepository;
        this.tontineRepository = tontineRepository;
        this.utilisateurConnecte = utilisateurConnecte;
        this.participationRepository = participationRepository;
        this.echeancePretRepository = echeancePretRepository;
        this.transactionService = transactionService;
    }

    // Gestionnaire : prêts de ses tontines. Membre : ses propres prêts
    // seulement (dette = info privée). ADMIN bloqué par SecurityConfig.
    public List<PretDTO> listPret() {
        boolean estGestionnaire = utilisateurConnecte.aLeRole(RoleUtilisateur.GESTIONNAIRE);
        List<Pret> prets = estGestionnaire
                ? pretRepository.findByTontineGestionnaireTelephone(utilisateurConnecte.telephone())
                : pretRepository.findByMembreTelephone(utilisateurConnecte.telephone());

        return prets.stream()
                .map(this::convertiPretDTO)
                .toList();
    }

    // Accorde un prêt sur la caisse de prêts (POST /tontine/{id}/prets).
    // @Transactional : prêt, débit de caisse et échéances, tout ou rien.
    @Transactional
    public PretDTO accorderPret(Long tontineId, DemandePretDTO demande) {
        Tontine tontine = tontineRepository.findById(tontineId)
                .orElseThrow(() -> new TontineIntrouvableException());
        utilisateurConnecte.verifierGestionnaire(tontine);

        // Pas de prêt sur une tontine non ACTIVE.
        if (tontine.getStatut() != StatutTontine.ACTIVE) {
            throw new TontineNonActiveException(tontine.getStatut());
        }

        // Le membre doit participer à CETTE tontine et être ACTIF.
        // filter() vide l'Optional si la condition est fausse → 400.
        Participation participation = participationRepository
                .findByTontineIdAndMembreId(tontineId, demande.getMembreId())
                .filter(p -> p.getStatut() == StatutParticipation.ACTIF)
                .orElseThrow(() -> new MembreNonParticipantException(demande.getMembreId()));

        // Un seul prêt en cours (ACTIF/EN_RETARD) par membre, dans CETTE
        // tontine (chaque tontine a sa caisse ; frontière SaaS).
        if (pretRepository.existsByMembreIdAndTontineIdAndStatutIn(demande.getMembreId(), tontineId,
                List.of(StatutPret.ACTIF, StatutPret.EN_RETARD))) {
            throw new PretDejaEnCoursException(demande.getMembreId());
        }

        // Pas plus que le SOLDE (l'intérêt rentrera plus tard, pas à sortir).
        if (demande.getMontant().compareTo(tontine.getSoldeCaissePret()) > 0) {
            throw new CaisseInsuffisanteException(demande.getMontant(), tontine.getSoldeCaissePret());
        }

        // Intérêt en francs : saisi tel quel, sinon montant × taux / 100,
        // sinon 0. divide() exige décimales + arrondi (sinon plante sur 1/3) ;
        // 0 décimale (FCFA), arrondi au plus proche.
        BigDecimal interet;
        if (demande.getMontantInteret() != null) {
            interet = demande.getMontantInteret();
        } else if (demande.getTauxInteret() != null) {
            interet = demande.getMontant().multiply(demande.getTauxInteret())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        } else {
            interet = BigDecimal.ZERO;
        }

        // Membre et gestionnaire pris en base, jamais dans le JSON.
        // tauxInteret peut rester null (« pas saisi »).
        Pret pret = new Pret();
        pret.setMembre(participation.getMembre());
        pret.setTontine(tontine);
        pret.setGestionnaire(tontine.getGestionnaire());
        pret.setMontant(demande.getMontant());
        pret.setTauxInteret(demande.getTauxInteret());
        pret.setMontantInteret(interet);
        pret.setNbEcheances(demande.getNbEcheances());
        pret.setDateAccord(LocalDate.now());
        pret.setDateDebutRemboursement(demande.getDateDebutRemboursement());
        pret.setStatut(StatutPret.ACTIF);
        pret.setCreatedAt(LocalDateTime.now());

        Pret enregistre = pretRepository.save(pret);

        // L'argent sort de la caisse (jamais négatif : contrôle du solde).
        tontine.setSoldeCaissePret(tontine.getSoldeCaissePret().subtract(demande.getMontant()));
        tontineRepository.save(tontine);

        // Échéances égales arrondies vers le bas (FCFA : pas de centimes),
        // la dernière absorbe le reste pour que le total soit exact.
        BigDecimal total = demande.getMontant().add(interet);
        int nb = demande.getNbEcheances();
        BigDecimal montantEcheance = total.divide(BigDecimal.valueOf(nb), 0, RoundingMode.DOWN);
        BigDecimal derniereEcheance = total.subtract(montantEcheance.multiply(BigDecimal.valueOf(nb - 1)));

        // Une échéance par période au rythme de la tontine, toujours calculée
        // depuis debut (pas de dérive en fin de mois). i commence à 1 → <=.
        LocalDate debut = demande.getDateDebutRemboursement();
        for (int i = 1; i <= nb; i++) {
            EcheancePret echeance = new EcheancePret();
            echeance.setPret(enregistre);
            echeance.setNumeroEcheance(i);
            echeance.setMontantDu(i == nb ? derniereEcheance : montantEcheance);
            echeance.setMontantPaye(BigDecimal.ZERO);

            long decalage = (long) (i - 1) * tontine.getIntervalle();
            LocalDate date = switch (tontine.getFrequence()) {
                case JOUR -> debut.plusDays(decalage);
                case SEMAINE -> debut.plusWeeks(decalage);
                case MOIS -> debut.plusMonths(decalage);
            };
            echeance.setDateEcheance(date);
            echeance.setStatut(StatutEcheancePret.EN_ATTENTE);
            echeancePretRepository.save(echeance);
        }

        // Journal : le capital remis aujourd'hui (comme le subtract du
        // solde) ; l'intérêt rentrera avec les remboursements.
        transactionService.journaliser(participation.getMembre(), tontine, TypeTransaction.PRET, demande.getMontant(),
                null, null, enregistre.getId(), null);

        return convertiPretDTO(enregistre);
    }

    // Remboursement sur le prêt entier, réparti sur les échéances les plus
    // anciennes d'abord. @Transactional : échéances, prêt et caisse ensemble.
    @Transactional
    public PretDTO rembourser(Long pretId, VersementDTO versement) {
        Pret pret = pretRepository.findById(pretId)
                .orElseThrow(() -> new PretIntrouvableException());
        utilisateurConnecte.verifierGestionnaire(pret.getTontine());

        if (pret.getStatut() == StatutPret.REMBOURSE) {
            throw new PretDejaRembourseException();
        }

        // Échéances non payées, de la plus ancienne à la plus récente.
        // reduce() additionne leurs restes → plafond du paiement (400).
        List<EcheancePret> aPayer = echeancePretRepository
                .findByPretIdAndStatutNotOrderByNumeroEcheanceAsc(pretId, StatutEcheancePret.PAYE);

        BigDecimal resteDu = aPayer.stream()
                .map(e -> e.getMontantDu().subtract(e.getMontantPaye()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (versement.getMontant().compareTo(resteDu) > 0) {
            throw new MontantPayeSuperieurAuDuException(versement.getMontant(), resteDu);
        }

        // Chaque échéance prend ce qui lui manque (min), dans la limite du
        // paiement ; break dès que le paiement est épuisé.
        BigDecimal restant = versement.getMontant();
        for (EcheancePret echeance : aPayer) {
            if (restant.compareTo(BigDecimal.ZERO) == 0) {
                break;
            }
            BigDecimal resteEcheance = echeance.getMontantDu().subtract(echeance.getMontantPaye());
            BigDecimal verse = restant.min(resteEcheance);

            echeance.setMontantPaye(echeance.getMontantPaye().add(verse));
            echeance.setDatePaiement(LocalDateTime.now());
            if (echeance.getMontantPaye().compareTo(echeance.getMontantDu()) == 0) {
                echeance.setStatut(StatutEcheancePret.PAYE);
            }
            echeancePretRepository.save(echeance);

            restant = restant.subtract(verse);
        }

        // Tout le remboursement (intérêt compris) retourne dans la caisse.
        Tontine tontine = pret.getTontine();
        tontine.setSoldeCaissePret(tontine.getSoldeCaissePret().add(versement.getMontant()));
        tontineRepository.save(tontine);

        // Journal : tout ce qui entre (capital + part d'intérêt), comme l'add
        // du solde. Pas de if : @Positive sur VersementDTO.
        transactionService.journaliser(pret.getMembre(), tontine, TypeTransaction.REMBOURSEMENT,
                versement.getMontant(), null, null, pret.getId(), null);

        // Paiement == tout ce qui restait → prêt soldé (statut final).
        // Sinon, plus aucune échéance EN_RETARD → le prêt redevient ACTIF.
        if (versement.getMontant().compareTo(resteDu) == 0) {
            pret.setStatut(StatutPret.REMBOURSE);
            pretRepository.save(pret);
        } else if (pret.getStatut() == StatutPret.EN_RETARD
                && !echeancePretRepository.existsByPretIdAndStatut(pretId, StatutEcheancePret.EN_RETARD)) {
            pret.setStatut(StatutPret.ACTIF);
            pretRepository.save(pret);
        }

        return convertiPretDTO(pret);
    }

    private PretDTO convertiPretDTO(Pret pret) {
        PretDTO dto = new PretDTO();
        dto.setId(pret.getId());
        dto.setMembreId(pret.getMembre().getId());
        dto.setTontineId(pret.getTontine().getId());
        dto.setGestionnaireId(pret.getGestionnaire().getId());
        dto.setMontant(pret.getMontant());
        dto.setTauxInteret(pret.getTauxInteret());
        dto.setMontantInteret(pret.getMontantInteret());
        dto.setNbEcheances(pret.getNbEcheances());
        dto.setDateAccord(pret.getDateAccord());
        dto.setDateDebutRemboursement(pret.getDateDebutRemboursement());
        dto.setStatut(pret.getStatut());
        dto.setCreatedAt(pret.getCreatedAt());

        return dto;
    }
}
