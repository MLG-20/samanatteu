package com.samanatteu.service.cotisation;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.samanatteu.dto.cotisation.CotisationDTO;
import com.samanatteu.entity.Cotisation;
import com.samanatteu.exception.MontantPayeSuperieurAuDuException;
import com.samanatteu.repository.CotisationRepository;

@Service
public class CotisationService {
    private final CotisationRepository cotisationRepository;

    public CotisationService(CotisationRepository cotisationRepository) {
        this.cotisationRepository = cotisationRepository;
    }

    // Lister
    public List<CotisationDTO> listCotisations() {
        return cotisationRepository.findAll() // 1. List<Cotisation> brute depuis la base (avec motDePasse)
                .stream() // 2. transforme la liste en flux traitable élément par élément
                .map(this::convertiCotisationDTO) // 3. applique la conversion à CHAQUE Cotisation -> CotisationDTO
                                                  // (sans motDePasse)
                .toList(); // 4. reconstitue une vraie List<CotisationDTO> à partir du flux
    }

    // créer
    public CotisationDTO createCotisation(Cotisation cotisation) {
        if (cotisation.getMontantPaye().compareTo(cotisation.getMontantDu()) > 0){
            throw new MontantPayeSuperieurAuDuException(cotisation.getMontantPaye(), cotisation.getMontantDu());
        }
        Cotisation enregistre = cotisationRepository.save(cotisation);
        return convertiCotisationDTO(enregistre);
    }

    // update
    public Optional<CotisationDTO> updateCotisation(Long id, Cotisation cotisationModifier) {
        // findById(id) renvoie un Optional<Cotisation> : vide si l'id n'existe pas,
        // rempli sinon.
        // .map(...) ne s'exécute QUE si l'Optional est rempli — sinon il reste vide tel
        // quel (pas de NullPointerException).
        return cotisationRepository.findById(id).map(cotisationExsitante -> {
            // cotisationExsitante = l'entité déjà en base (trouvée par findById).
            // cotisationModifier = les nouvelles valeurs envoyées par le client (paramètre
            // de la méthode).
            // On recopie les nouvelles valeurs DANS l'entité existante, champ par champ.
            cotisationExsitante.setParticipation(cotisationModifier.getParticipation());
            cotisationExsitante.setCycle(cotisationModifier.getCycle());
            cotisationExsitante.setMontantDu(cotisationModifier.getMontantDu());
            cotisationExsitante.setMontantPaye(cotisationModifier.getMontantPaye());
            cotisationExsitante.setDatePaiement(cotisationModifier.getDatePaiement());
            cotisationExsitante.setModePaiement(cotisationModifier.getModePaiement());
            cotisationExsitante.setReference(cotisationModifier.getReference());
            cotisationExsitante.setStatut(cotisationModifier.getStatut());
            cotisationExsitante.setCreatedAt(cotisationModifier.getCreatedAt());

            // save() persiste les changements en base ET renvoie l'entité Cotisation à jour
            // (avec motDePasse).
            Cotisation enregistre = cotisationRepository.save(cotisationExsitante);
            // On ne renvoie JAMAIS l'entité brute au client : conversion en DTO juste avant
            // de sortir (sans motDePasse).
            // Comme on est dans un .map(), ce retour devient automatiquement le contenu de
            // l'Optional<CotisationDTO>.
            return convertiCotisationDTO(enregistre);
        });
    }

    // Delete
    public boolean deleteCotisation(Long id) {
        if (cotisationRepository.existsById(id)) {
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
