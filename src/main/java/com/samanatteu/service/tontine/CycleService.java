package com.samanatteu.service.tontine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.samanatteu.dto.tontine.CycleDTO;
import com.samanatteu.entity.cotisation.Cotisation;
import com.samanatteu.entity.tontine.Cycle;
import com.samanatteu.entity.tontine.Participation;
import com.samanatteu.entity.tontine.Tontine;
import com.samanatteu.enums.cotisation.StatutCotisation;
import com.samanatteu.enums.tontine.StatutCycle;
import com.samanatteu.enums.tontine.StatutParticipation;
import com.samanatteu.enums.tontine.StatutTontine;
import com.samanatteu.enums.utilisateur.RoleUtilisateur;
import com.samanatteu.exception.tontine.CycleDejaEnCoursException;
import com.samanatteu.exception.tontine.CycleIntrouvableException;
import com.samanatteu.exception.tontine.CycleNonEnCoursException;
import com.samanatteu.exception.tontine.NombreCyclesAtteintException;
import com.samanatteu.exception.tontine.TontineIntrouvableException;
import com.samanatteu.exception.tontine.TontineNonActiveException;
import com.samanatteu.repository.cotisation.CotisationRepository;
import com.samanatteu.repository.tontine.CycleRepository;
import com.samanatteu.repository.tontine.ParticipationRepository;
import com.samanatteu.repository.tontine.TontineRepository;
import com.samanatteu.security.UtilisateurConnecte;

@Service
public class CycleService {
    private final CycleRepository cycleRepository;
    private final TontineRepository tontineRepository;
    private final UtilisateurConnecte utilisateurConnecte;
    private final ParticipationRepository participationRepository;
    private final CotisationRepository cotisationRepository;

    public CycleService(CycleRepository cycleRepository, TontineRepository tontineRepository,
            UtilisateurConnecte utilisateurConnecte, ParticipationRepository participationRepository,
            CotisationRepository cotisationRepository) {
        this.cycleRepository = cycleRepository;
        this.tontineRepository = tontineRepository;
        this.utilisateurConnecte = utilisateurConnecte;
        this.participationRepository = participationRepository;
        this.cotisationRepository = cotisationRepository;
    }

    // Lecture filtrée par rôle (règle R6 du CDC), comme listTontine :
    // gestionnaire → cycles de SES tontines ; membre → cycles des tontines
    // où il participe. Un ADMIN est refusé avant (403, SecurityConfig).
    // Membre en 2 temps : ses participations → ids de leurs tontines
    // (stream + map) → tous les cycles de ces tontines (findByTontineIdIn).
    public List<CycleDTO> listCycle() {
        boolean estGestionnaire = utilisateurConnecte.aLeRole(RoleUtilisateur.GESTIONNAIRE);

        List<Cycle> cycles;
        if (estGestionnaire) {
            cycles = cycleRepository.findByTontineGestionnaireTelephone(utilisateurConnecte.telephone());
        } else {
            List<Long> tontineIds = participationRepository.findByMembreTelephone(utilisateurConnecte.telephone())
                    .stream()
                    .map(participation -> participation.getTontine().getId())
                    .toList();
            cycles = cycleRepository.findByTontineIdIn(tontineIds);
        }

        return cycles.stream()
                .map(this::convertiCycleDTO)
                .toList();
    }

    public boolean deleteCycle(Long id) {
        Optional<Cycle> cycle = cycleRepository.findById(id);
        if (cycle.isPresent()) {
            utilisateurConnecte.verifierGestionnaire(cycle.get().getTontine());
            cycleRepository.deleteById(id);
            return true;
        }
        return false;
    }

    // Ouvre le cycle suivant d'une tontine (POST /tontine/{id}/cycles, sans corps).
    // Le client n'envoie que l'id de la tontine : numéro, dates, statut et montants
    // sont décidés ici, côté serveur, pour qu'il ne puisse rien falsifier.
    // Ordre des contrôles : d'abord « qui a le droit » (404, 403), ensuite
    // « est-ce possible » (409), comme dans ParticipationService.
    // @Transactional : le cycle + ses N cotisations forment un bloc « tout ou
    // rien ». Si une écriture échoue (ou si une RuntimeException est levée),
    // Spring annule tout (rollback) : pas de cycle EN_COURS à moitié créé qui
    // bloquerait la tontine. Ne marche que si l'appel vient d'une autre classe
    // (ici le contrôleur) : Spring intercepte l'appel à l'entrée du service.
    @Transactional
    public CycleDTO ouvrirCycle(Long tontineId) {
        Tontine tontine = tontineRepository.findById(tontineId)
                .orElseThrow(() -> new TontineIntrouvableException());
        utilisateurConnecte.verifierGestionnaire(tontine);

        // On ne collecte pas d'argent dans une tontine en attente, suspendue
        // ou terminée.
        if (tontine.getStatut() != StatutTontine.ACTIVE) {
            throw new TontineNonActiveException(tontine.getStatut());
        }
        // Un seul cycle ouvert à la fois : sinon les paiements de deux cycles
        // se mélangeraient.
        if (cycleRepository.existsByTontineIdAndStatut(tontineId, StatutCycle.EN_COURS)) {
            throw new CycleDejaEnCoursException();
        }

        // Numéro = dernier numéro + 1, ou 1 si la tontine n'a encore aucun cycle
        // (Optional vide → orElse). Même calcul que ordreInscription.
        Integer numero = cycleRepository.findFirstByTontineIdOrderByNumeroCycleDesc(tontineId)
                .map(dernier -> dernier.getNumeroCycle() + 1)
                .orElse(1);

        // nbCycles fixe la durée de la tontine : pas de cycle au-delà.
        if (numero > tontine.getNbCycles()) {
            throw new NombreCyclesAtteintException(tontine.getNbCycles());
        }

        // montantAttendu est calculé plus bas, avec les cotisations.
        Cycle cycle = new Cycle();
        cycle.setTontine(tontine);
        cycle.setNumeroCycle(numero);

        // Fin prévue = début + intervalle unités (ex. MOIS, 2 → +2 mois).
        // switch « expression » (->) : il RENVOIE une valeur, pas de break.
        // Sur un enum, sans default : si une valeur est ajoutée à
        // FrequenceTontine sans case ici, le code ne compile plus.
        // plusMonths gère les fins de mois (31 janv. + 1 mois = 28/29 févr.)
        // et, comme BigDecimal, LocalDate renvoie une NOUVELLE date.
        LocalDate dateDebut = LocalDate.now();
        LocalDate dateFinPrevue = switch (tontine.getFrequence()) {
            case JOUR -> dateDebut.plusDays(tontine.getIntervalle());
            case SEMAINE -> dateDebut.plusWeeks(tontine.getIntervalle());
            case MOIS -> dateDebut.plusMonths(tontine.getIntervalle());
        };
        cycle.setDateDebut(dateDebut);
        cycle.setDateFinPrevue(dateFinPrevue);
        cycle.setStatut(StatutCycle.EN_COURS);
        cycle.setMontantCollecte(BigDecimal.ZERO);
        cycle.setCreatedAt(LocalDateTime.now());

        // On sauvegarde AVANT de créer les cotisations : c'est save() qui donne
        // son id au cycle, et chaque cotisation doit pointer vers cet id en base.
        Cycle enregistre = cycleRepository.save(cycle);

        // Une cotisation par membre ACTIF (les SUSPENDU / SORTI ne doivent rien).
        // Règle R1 du CDC : montantDu = nombreParts × montantPart.
        // BigDecimal (argent exact) : pas de « * », on utilise multiply(), et
        // valueOf() convertit le nombre de parts (Integer) en BigDecimal.
        List<Participation> participations = participationRepository.findByTontineIdAndStatut(tontineId,
                StatutParticipation.ACTIF);

        // Total du cycle = somme des montantDu, accumulée dans la boucle.
        // BigDecimal ne se modifie jamais : add() RENVOIE un nouveau nombre,
        // d'où « total = total.add(...) » (sans l'affectation, rien ne change).
        BigDecimal montantAttendu = BigDecimal.ZERO;

        for (Participation participation : participations) {
            Cotisation cotisation = new Cotisation();
            cotisation.setCycle(enregistre);
            cotisation.setParticipation(participation);
            BigDecimal montantDu = tontine.getMontantPart()
                    .multiply(BigDecimal.valueOf(participation.getNombreParts()));
            cotisation.setMontantDu(montantDu);
            montantAttendu = montantAttendu.add(montantDu);
            cotisation.setMontantPaye(BigDecimal.ZERO);
            // Caisse de prêts : montant FIXE par membre (pas de multiply par
            // les parts), copié pour garder la trace de ce cycle. Rien de
            // versé au départ (ZERO, jamais null : NOT NULL + add() plus tard).
            // Pas ajouté à montantAttendu : ce total est la cagnotte du tirage.
            cotisation.setMontantCaisseDu(tontine.getMontantCaissePret());
            cotisation.setMontantCaissePaye(BigDecimal.ZERO);
            cotisation.setStatut(StatutCotisation.EN_ATTENTE);
            cotisation.setCreatedAt(LocalDateTime.now());
            cotisationRepository.save(cotisation);
        }

        // Second save() : le cycle a déjà un id, donc JPA fait un UPDATE de la
        // même ligne (pas de doublon).
        enregistre.setMontantAttendu(montantAttendu);
        cycleRepository.save(enregistre);

        return convertiCycleDTO(enregistre);
    }

    // Clôture le cycle (POST /cycle/{id}/cloturer). Option B (écart assumé
    // avec la règle R4 du CDC) : clôture permise même avec des impayés, qui
    // passent EN_RETARD et restent payables ensuite.
    // @Transactional : le cycle et ses cotisations changent ensemble.
    @Transactional
    public CycleDTO cloturerCycle(Long cycleId) {
        Cycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new CycleIntrouvableException());
        utilisateurConnecte.verifierGestionnaire(cycle.getTontine());

        // On ne clôture qu'un cycle EN_COURS (pas deux fois le même).
        if (cycle.getStatut() != StatutCycle.EN_COURS) {
            throw new CycleNonEnCoursException(cycle.getStatut());
        }

        // Tout ce qui n'est pas COMPLET (EN_ATTENTE ou PARTIEL) passe
        // EN_RETARD. save() seulement pour les cotisations modifiées.
        List<Cotisation> cotisations = cotisationRepository.findByCycleId(cycleId);

        for (Cotisation cotisation : cotisations) {
            if (cotisation.getStatut() != StatutCotisation.COMPLET) {
                cotisation.setStatut(StatutCotisation.EN_RETARD);
                cotisationRepository.save(cotisation);
            }
        }

        // CLOTURE libère la place : ouvrirCycle acceptera le cycle suivant.
        // dateFinReelle vs dateFinPrevue montre si la tontine a pris du retard.
        cycle.setStatut(StatutCycle.CLOTURE);
        cycle.setDateFinReelle(LocalDate.now());

        Cycle enregistre = cycleRepository.save(cycle);
        return convertiCycleDTO(enregistre);
    }

    private CycleDTO convertiCycleDTO(Cycle cycle) {
        CycleDTO dto = new CycleDTO();
        dto.setId(cycle.getId());
        dto.setTontineId(cycle.getTontine().getId());
        dto.setNumeroCycle(cycle.getNumeroCycle());
        dto.setDateDebut(cycle.getDateDebut());
        dto.setDateFinPrevue(cycle.getDateFinPrevue());
        dto.setDateFinReelle(cycle.getDateFinReelle());
        dto.setMontantAttendu(cycle.getMontantAttendu());
        dto.setMontantCollecte(cycle.getMontantCollecte());
        dto.setStatut(cycle.getStatut());
        dto.setCreatedAt(cycle.getCreatedAt());

        return dto;
    }
}
