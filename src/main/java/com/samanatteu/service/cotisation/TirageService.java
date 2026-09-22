package com.samanatteu.service.cotisation;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.samanatteu.dto.cotisation.TirageDTO;
import com.samanatteu.entity.Tirage;
import com.samanatteu.exception.TirageDejaExistantPourCeCycleException;
import com.samanatteu.repository.TirageRepository;

@Service
public class TirageService {
    private final TirageRepository tirageRepository;

    public TirageService(TirageRepository tirageRepository) {
        this.tirageRepository = tirageRepository;
    }

    // Lister
    public List<TirageDTO> listTirage() {
        return tirageRepository.findAll() // 1. List<Tirage> brute depuis la base (avec motDePasse)
                .stream() // 2. transforme la liste en flux traitable élément par élément
                .map(this::convertiTirageDTO) // 3. applique la conversion à CHAQUE Tirage -> TirageDTO
                                              // (sans motDePasse)
                .toList(); // 4. reconstitue une vraie List<TirageDTO> à partir du flux
    }

    // créer
    public TirageDTO createTirage(Tirage tirage) {
        if (tirageRepository.existsByCycleId(tirage.getCycle().getId())) {
            throw new TirageDejaExistantPourCeCycleException(tirage.getCycle().getId());
        }
        Tirage enregistre = tirageRepository.save(tirage);
        return convertiTirageDTO(enregistre);
    }

    // update
    public Optional<TirageDTO> updateTirage(Long id, Tirage tirageModifier) {
        // findById(id) renvoie un Optional<Tirage> : vide si l'id n'existe pas,
        // rempli sinon.
        // .map(...) ne s'exécute QUE si l'Optional est rempli — sinon il reste vide tel
        // quel (pas de NullPointerException).
        return tirageRepository.findById(id).map(tirageExsitant -> {
            // tirageExsitant = l'entité déjà en base (trouvée par findById).
            // tirageModifier = les nouvelles valeurs envoyées par le client (paramètre
            // de la méthode).
            // On recopie les nouvelles valeurs DANS l'entité existante, champ par champ.
            tirageExsitant.setCycle(tirageModifier.getCycle());
            tirageExsitant.setParticipation(tirageModifier.getParticipation());
            tirageExsitant.setMontantGagne(tirageModifier.getMontantGagne());
            tirageExsitant.setDateTirage(tirageModifier.getDateTirage());
            tirageExsitant.setDateVersement(tirageModifier.getDateVersement());
            tirageExsitant.setStatut(tirageModifier.getStatut());
            tirageExsitant.setCreatedAt(tirageModifier.getCreatedAt());

            // save() persiste les changements en base ET renvoie l'entité Tirage à jour
            // (avec motDePasse).
            Tirage enregistre = tirageRepository.save(tirageExsitant);
            // On ne renvoie JAMAIS l'entité brute au client : conversion en DTO juste avant
            // de sortir (sans motDePasse).
            // Comme on est dans un .map(), ce retour devient automatiquement le contenu de
            // l'Optional<TirageDTO>.
            return convertiTirageDTO(enregistre);
        });
    }

    // Delete
    public boolean deleteTirage(Long id) {
        if (tirageRepository.existsById(id)) {
            tirageRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private TirageDTO convertiTirageDTO(Tirage tirage) {
        TirageDTO dto = new TirageDTO();
        dto.setId(tirage.getId());
        dto.setCycleId(tirage.getCycle().getId());
        dto.setParticipationId(tirage.getParticipation().getId());
        dto.setMontantGagne(tirage.getMontantGagne());
        dto.setDateTirage(tirage.getDateTirage());
        dto.setDateVersement(tirage.getDateVersement());
        dto.setStatut(tirage.getStatut());
        dto.setCreatedAt(tirage.getCreatedAt());

        return dto;
    }
}
