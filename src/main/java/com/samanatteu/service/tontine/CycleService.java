package com.samanatteu.service.tontine;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.samanatteu.dto.tontine.CycleDTO;
import com.samanatteu.entity.Cycle;
import com.samanatteu.exception.NumeroCycleDejaExistantException;
import com.samanatteu.repository.CycleRepository;

@Service 
public class CycleService {
    private final CycleRepository cycleRepository;

    public CycleService(CycleRepository cycleRepository) {
        this.cycleRepository = cycleRepository;
    }

    // Lister
    public List<CycleDTO> listCycle() {
        return cycleRepository.findAll() // 1. List<Cycle> brute depuis la base (avec motDePasse)
                .stream() // 2. transforme la liste en flux traitable élément par élément
                .map(this::convertiCycleDTO) // 3. applique la conversion à CHAQUE Cycle -> CycleDTO
                                                  // (sans motDePasse)
                .toList(); // 4. reconstitue une vraie List<CycleDTO> à partir du flux
    }

    // créer
    public CycleDTO createCycle(Cycle cycle) {
        if (cycleRepository.existsByNumeroCycleAndTontineId(cycle.getNumeroCycle(), cycle.getTontine().getId())) {
            throw new NumeroCycleDejaExistantException(cycle.getNumeroCycle(), cycle.getTontine().getId());
        }
        Cycle enregistre = cycleRepository.save(cycle);
        return convertiCycleDTO(enregistre);
    }

    // update
    public Optional<CycleDTO> updateCycle(Long id, Cycle cycleModifier) {
        // findById(id) renvoie un Optional<Cycle> : vide si l'id n'existe pas,
        // rempli sinon.
        // .map(...) ne s'exécute QUE si l'Optional est rempli — sinon il reste vide tel
        // quel (pas de NullPointerException).
        return cycleRepository.findById(id).map(cycleExsitant -> {
            // cycleExsitant = l'entité déjà en base (trouvée par findById).
            // cycleModifier = les nouvelles valeurs envoyées par le client (paramètre
            // de la méthode).
            // On recopie les nouvelles valeurs DANS l'entité existante, champ par champ.
            cycleExsitant.setTontine(cycleModifier.getTontine());
            cycleExsitant.setNumeroCycle(cycleModifier.getNumeroCycle());
            cycleExsitant.setDateDebut(cycleModifier.getDateDebut());
            cycleExsitant.setDateFinPrevue(cycleModifier.getDateFinPrevue());
            cycleExsitant.setDateFinReelle(cycleModifier.getDateFinReelle());
            cycleExsitant.setMontantAttendu(cycleModifier.getMontantAttendu());
            cycleExsitant.setMontantCollecte(cycleModifier.getMontantCollecte());
            cycleExsitant.setStatut(cycleModifier.getStatut());
            cycleExsitant.setCreatedAt(cycleModifier.getCreatedAt());

            // save() persiste les changements en base ET renvoie l'entité Cycle à jour
            // (avec motDePasse).
            Cycle enregistre = cycleRepository.save(cycleExsitant);
            // On ne renvoie JAMAIS l'entité brute au client : conversion en DTO juste avant
            // de sortir (sans motDePasse).
            // Comme on est dans un .map(), ce retour devient automatiquement le contenu de
            // l'Optional<CycleDTO>.
            return convertiCycleDTO(enregistre);
        });
    }

    // Delete
    public boolean deleteCycle(Long id) {
        if (cycleRepository.existsById(id)) {
            cycleRepository.deleteById(id);
            return true;
        }
        return false;
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
