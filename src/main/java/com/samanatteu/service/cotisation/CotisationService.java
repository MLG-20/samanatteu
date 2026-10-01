package com.samanatteu.service.cotisation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.samanatteu.dto.cotisation.CotisationDTO;
import com.samanatteu.dto.cotisation.PaiementDTO;
import com.samanatteu.entity.cotisation.Cotisation;
import com.samanatteu.entity.tontine.Cycle;
import com.samanatteu.entity.tontine.Tontine;
import com.samanatteu.enums.cotisation.StatutCotisation;
import com.samanatteu.enums.pret.TypeTransaction;
import com.samanatteu.enums.utilisateur.RoleUtilisateur;
import com.samanatteu.exception.cotisation.CotisationDejaPayeeException;
import com.samanatteu.exception.cotisation.CotisationIntrouvableException;
import com.samanatteu.exception.cotisation.MontantPayeSuperieurAuDuException;
import com.samanatteu.repository.cotisation.CotisationRepository;
import com.samanatteu.repository.tontine.CycleRepository;
import com.samanatteu.repository.tontine.TontineRepository;
import com.samanatteu.security.UtilisateurConnecte;
import com.samanatteu.service.pret.TransactionService;

@Service
public class CotisationService {
    private final CycleRepository cycleRepository;
    private final CotisationRepository cotisationRepository;
    private final UtilisateurConnecte utilisateurConnecte;
    private final TontineRepository tontineRepository;
    private final TransactionService transactionService;

    public CotisationService(CotisationRepository cotisationRepository, UtilisateurConnecte utilisateurConnecte,
            CycleRepository cycleRepository, TontineRepository tontineRepository,
            TransactionService transactionsService) {
        this.cotisationRepository = cotisationRepository;
        this.utilisateurConnecte = utilisateurConnecte;
        this.cycleRepository = cycleRepository;
        this.tontineRepository = tontineRepository;
        this.transactionService = transactionsService;
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

        // On ne paie pas plus que ce qui reste dû. Un seul paiement couvre la
        // part ET la caisse de prêts : le plafond est la somme des deux
        // restes (ex. part 2 000 + caisse 500 → 2 500 max, 3 000 refusé 400).
        // resteDuPart resservira pour le partage plus bas.
        // BigDecimal : subtract() pour « - », compareTo() pour comparer
        // (> 0 = plus grand). Jamais equals() : 10.0 et 10.00 seraient
        // « différents » pour equals, égaux pour compareTo.
        BigDecimal resteDuPart = cotisation.getMontantDu().subtract(cotisation.getMontantPaye());
        BigDecimal resteDuCaisse = cotisation.getMontantCaisseDu().subtract(cotisation.getMontantCaissePaye());
        BigDecimal resteDu = resteDuPart.add(resteDuCaisse);

        if (paiement.getMontant().compareTo(resteDu) > 0) {
            throw new MontantPayeSuperieurAuDuException(paiement.getMontant(), resteDu);
        }

        // Partage du paiement, la PART est prioritaire : min() donne le plus
        // petit des deux, donc la part prend tout jusqu'à être remplie, et
        // le surplus va à la caisse (ex. 5 500 pour un reste de 5 000 →
        // part 5 000, caisse 500 ; 3 000 → part 3 000, caisse 0).
        // Pas de contrôle de débordement caisse : le plafond l'empêche déjà.
        // add() RENVOIE un nouveau BigDecimal (il ne modifie pas l'ancien) :
        // on le garde dans une variable, il sert aussi au choix du statut.
        // Date du paiement fixée par le serveur, pas par le client.
        BigDecimal versePart = paiement.getMontant().min(resteDuPart);
        BigDecimal verseCaisse = paiement.getMontant().subtract(versePart);
        BigDecimal nouveauMontantPaye = cotisation.getMontantPaye().add(versePart);
        cotisation.setMontantCaissePaye(cotisation.getMontantCaissePaye().add(verseCaisse));
        cotisation.setMontantPaye(nouveauMontantPaye);
        cotisation.setModePaiement(paiement.getModePaiement());
        cotisation.setReference(paiement.getReference());
        cotisation.setDatePaiement(LocalDateTime.now());

        // COMPLET seulement si la part ET la caisse sont payées (&& : les deux
        // vraies). Sinon un membre à 5 000 / 5 500 serait COMPLET et ne
        // pourrait plus verser ses 500 (paiement sur COMPLET refusé, 409).
        // Sinon PARTIEL, SAUF si la cotisation était EN_RETARD : un paiement
        // partiel n'efface pas le retard (utile pour les rappels, phase 6).
        // « Plus que le dû » est impossible ici : refusé juste au-dessus.
        // getMontantCaissePaye() contient déjà le nouveau total (mis à jour
        // lors du partage, plus haut).
        boolean partComplete = nouveauMontantPaye.compareTo(cotisation.getMontantDu()) == 0;
        boolean caisseComplete = cotisation.getMontantCaissePaye().compareTo(cotisation.getMontantCaisseDu()) == 0;
        if (partComplete && caisseComplete) {
            cotisation.setStatut(StatutCotisation.COMPLET);
        } else if (cotisation.getStatut() != StatutCotisation.EN_RETARD) {
            cotisation.setStatut(StatutCotisation.PARTIEL);
        }

        // Le cycle encaisse seulement l'argent qui vient d'arriver (pas
        // nouveauMontantPaye, qui recompterait les paiements précédents),
        // et seulement la PART (versePart) : montantCollecte est la cagnotte
        // du tirage, l'argent de la caisse de prêts ne doit pas y entrer.
        // Deux save() : grâce à @Transactional, les deux réussissent ou aucun.
        Cycle cycle = cotisation.getCycle();
        cycle.setMontantCollecte(cycle.getMontantCollecte().add(versePart));
        cycleRepository.save(cycle);

        // Le surplus (verseCaisse) crédite la caisse de prêts de la tontine
        // (cotisation → cycle → tontine, pas de lien direct). Si verseCaisse
        // vaut 0, add(0) ne change rien : pas besoin de if. @Transactional :
        // cycle, tontine et cotisation sont enregistrés tous ou aucun.
        Tontine tontine = cycle.getTontine();
        tontine.setSoldeCaissePret(tontine.getSoldeCaissePret().add(verseCaisse));
        tontineRepository.save(tontine);

        // Journal : une ligne par paiement réel, montant TOTAL (part + caisse,
        // option 1). Même @Transactional : pas de paiement sans sa ligne.
        transactionService.journaliser(cotisation.getParticipation().getMembre(), tontine,
                TypeTransaction.COTISATION, paiement.getMontant(), paiement.getModePaiement(),
                paiement.getReference(), cotisation.getId(), null);

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
        dto.setMontantCaisseDu(cotisation.getMontantCaisseDu());
        dto.setMontantCaissePaye(cotisation.getMontantCaissePaye());
        dto.setDatePaiement(cotisation.getDatePaiement());
        dto.setModePaiement(cotisation.getModePaiement());
        dto.setReference(cotisation.getReference());
        dto.setStatut(cotisation.getStatut());
        dto.setCreatedAt(cotisation.getCreatedAt());

        return dto;
    }
}
