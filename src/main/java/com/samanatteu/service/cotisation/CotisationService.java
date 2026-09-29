package com.samanatteu.service.cotisation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.samanatteu.dto.cotisation.CotisationDTO;
import com.samanatteu.dto.cotisation.PaiementDTO;
import com.samanatteu.entity.Cotisation;
import com.samanatteu.entity.Cycle;
import com.samanatteu.enums.RoleUtilisateur;
import com.samanatteu.enums.StatutCotisation;
import com.samanatteu.exception.CotisationDejaPayeeException;
import com.samanatteu.exception.CotisationIntrouvableException;
import com.samanatteu.exception.MontantPayeSuperieurAuDuException;
import com.samanatteu.repository.CotisationRepository;
import com.samanatteu.repository.CycleRepository;
import com.samanatteu.security.UtilisateurConnecte;

@Service
public class CotisationService {
    private final CycleRepository cycleRepository;
    private final CotisationRepository cotisationRepository;
    private final UtilisateurConnecte utilisateurConnecte;

    public CotisationService(CotisationRepository cotisationRepository, UtilisateurConnecte utilisateurConnecte,
            CycleRepository cycleRepository) {
        this.cotisationRepository = cotisationRepository;
        this.utilisateurConnecte = utilisateurConnecte;
        this.cycleRepository = cycleRepository;
    }

    // Lecture filtrée par rôle (règle R6), comme listParticipation :
    // gestionnaire → cotisations de SES tontines ; membre → SES cotisations
    // seulement. ADMIN refusé avant (403, SecurityConfig).
    public List<CotisationDTO> listCotisations() {
        boolean estGestionnaire = utilisateurConnecte.aLeRole(RoleUtilisateur.GESTIONNAIRE);

        List<Cotisation> cotisations = estGestionnaire
                ? cotisationRepository.findByCycleTontineGestionnaireTelephone(utilisateurConnecte.telephone())
                : cotisationRepository.findByParticipationMembreTelephone(utilisateurConnecte.telephone());

        return cotisations.stream()
                .map(this::convertiCotisationDTO)
                .toList();
    }

    // Enregistre un paiement (POST /cotisation/{id}/paiement) : le client envoie
    // seulement montant + mode + référence (PaiementDTO) ; montantPaye, statut
    // et montantCollecte du cycle sont calculés ici.
    // @Transactional (celui de Spring, comme CycleService) : la cotisation ET
    // le cycle sont modifiés, les deux réussissent ensemble ou aucun.
    @Transactional
    public CotisationDTO enregistrerPaiement(Long cotisationId, PaiementDTO paiement) {
        Cotisation cotisation = cotisationRepository.findById(cotisationId)
                .orElseThrow(() -> new CotisationIntrouvableException());
        // Une cotisation n'a pas de lien direct vers sa tontine : on passe
        // par son cycle.
        utilisateurConnecte.verifierGestionnaire(cotisation.getCycle().getTontine());

        // Pas de contrôle « cycle EN_COURS » (option B) : un membre EN_RETARD
        // peut encore payer après la clôture. Seul COMPLET est refusé.
        if (cotisation.getStatut() == StatutCotisation.COMPLET) {
            throw new CotisationDejaPayeeException();
        }

        // On ne paie pas plus que ce qui reste dû (ex. dû 10 000, déjà payé
        // 6 000 → reste 4 000 : un paiement de 5 000 est refusé, 400).
        // BigDecimal : subtract() pour « - », compareTo() pour comparer
        // (> 0 = plus grand). Jamais equals() : 10.0 et 10.00 seraient
        // « différents » pour equals, égaux pour compareTo.
        BigDecimal resteDu = cotisation.getMontantDu().subtract(cotisation.getMontantPaye());
        if (paiement.getMontant().compareTo(resteDu) > 0) {
            throw new MontantPayeSuperieurAuDuException(paiement.getMontant(), resteDu);
        }

        // add() RENVOIE un nouveau BigDecimal (il ne modifie pas l'ancien) :
        // on le garde dans une variable, il sert aussi au choix du statut.
        // Date du paiement fixée par le serveur, pas par le client.
        BigDecimal nouveauMontantPaye = cotisation.getMontantPaye().add(paiement.getMontant());
        cotisation.setMontantPaye(nouveauMontantPaye);
        cotisation.setModePaiement(paiement.getModePaiement());
        cotisation.setReference(paiement.getReference());
        cotisation.setDatePaiement(LocalDateTime.now());

        // Tout payé (compareTo == 0) → COMPLET. Sinon PARTIEL, SAUF si la
        // cotisation était EN_RETARD : un paiement partiel n'efface pas le
        // retard (utile pour les rappels, phase 6). « Plus que le dû » est
        // impossible ici : refusé juste au-dessus.
        if (nouveauMontantPaye.compareTo(cotisation.getMontantDu()) == 0) {
            cotisation.setStatut(StatutCotisation.COMPLET);
        } else if (cotisation.getStatut() != StatutCotisation.EN_RETARD) {
            cotisation.setStatut(StatutCotisation.PARTIEL);
        }

        // Le cycle encaisse seulement l'argent qui vient d'arriver (pas
        // nouveauMontantPaye, qui recompterait les paiements précédents).
        // Deux save() : grâce à @Transactional, les deux réussissent ou aucun.
        Cycle cycle = cotisation.getCycle();
        cycle.setMontantCollecte(cycle.getMontantCollecte().add(paiement.getMontant()));
        cycleRepository.save(cycle);

        Cotisation enregistree = cotisationRepository.save(cotisation);
        return convertiCotisationDTO(enregistree);
    }

    public boolean deleteCotisation(Long id) {
        Optional<Cotisation> cotisation = cotisationRepository.findById(id);
        if (cotisation.isPresent()) {
            utilisateurConnecte.verifierGestionnaire(cotisation.get().getCycle().getTontine());
            cotisationRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private CotisationDTO convertiCotisationDTO(Cotisation cotisation) {
        CotisationDTO dto = new CotisationDTO();
        dto.setId(cotisation.getId());
        dto.setParticipationId(cotisation.getParticipation().getId());
        dto.setCycleId(cotisation.getCycle().getId());
        dto.setMontantDu(cotisation.getMontantDu());
        dto.setMontantPaye(cotisation.getMontantPaye());
        dto.setDatePaiement(cotisation.getDatePaiement());
        dto.setModePaiement(cotisation.getModePaiement());
        dto.setReference(cotisation.getReference());
        dto.setStatut(cotisation.getStatut());
        dto.setCreatedAt(cotisation.getCreatedAt());

        return dto;
    }
}
