package com.samanatteu.service.pret;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.samanatteu.dto.pret.PretDTO;
import com.samanatteu.entity.Pret;
import com.samanatteu.enums.StatutPret;
import com.samanatteu.exception.PretDejaEnCoursException;
import com.samanatteu.repository.PretRepository;

@Service
public class PretService {
    private final PretRepository pretRepository;

    public PretService(PretRepository pretRepository) {
        this.pretRepository = pretRepository;
    }

    // Lister
    public List<PretDTO> listPret() {
        return pretRepository.findAll() // 1. List<Pret> brute depuis la base (avec motDePasse)
                .stream() // 2. transforme la liste en flux traitable élément par élément
                .map(this::convertiPretDTO) // 3. applique la conversion à CHAQUE Pret -> PretDTO
                                            // (sans motDePasse)
                .toList(); // 4. reconstitue une vraie List<PretDTO> à partir du flux
    }

    // créer
    public PretDTO createPret(Pret pret) {
        if (pretRepository.existsByMembreIdAndStatut(pret.getMembre().getId(),StatutPret.ACTIF)) {
            throw new PretDejaEnCoursException(pret.getMembre().getId());
        }
        Pret enregistre = pretRepository.save(pret);
        return convertiPretDTO(enregistre);
    }

    // update
    public Optional<PretDTO> updatePret(Long id, Pret pretModifier) {
        // findById(id) renvoie un Optional<Pret> : vide si l'id n'existe pas,
        // rempli sinon.
        // .map(...) ne s'exécute QUE si l'Optional est rempli — sinon il reste vide tel
        // quel (pas de NullPointerException).
        return pretRepository.findById(id).map(pretExsitant -> {
            // pretExsitant = l'entité déjà en base (trouvée par findById).
            // pretModifier = les nouvelles valeurs envoyées par le client (paramètre
            // de la méthode).
            // On recopie les nouvelles valeurs DANS l'entité existante, champ par champ.
            pretExsitant.setMembre(pretModifier.getMembre());
            pretExsitant.setTontine(pretModifier.getTontine());
            pretExsitant.setGestionnaire(pretModifier.getGestionnaire());
            pretExsitant.setMontant(pretModifier.getMontant());
            pretExsitant.setTauxInteret(pretModifier.getTauxInteret());
            pretExsitant.setNbEcheances(pretModifier.getNbEcheances());
            pretExsitant.setDateAccord(pretModifier.getDateAccord());
            pretExsitant.setStatut(pretModifier.getStatut());
            pretExsitant.setCreatedAt(pretModifier.getCreatedAt());

            // save() persiste les changements en base ET renvoie l'entité Pret à jour
            // (avec motDePasse).
            Pret enregistre = pretRepository.save(pretExsitant);
            // On ne renvoie JAMAIS l'entité brute au client : conversion en DTO juste avant
            // de sortir (sans motDePasse).
            // Comme on est dans un .map(), ce retour devient automatiquement le contenu de
            // l'Optional<PretDTO>.
            return convertiPretDTO(enregistre);
        });
    }

    // Delete
    public boolean deletePret(Long id) {
        if (pretRepository.existsById(id)) {
            pretRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private PretDTO convertiPretDTO(Pret pret) {
        PretDTO dto = new PretDTO();
        dto.setId(pret.getId());
        dto.setMembreId(pret.getMembre().getId());
        dto.setTontineId(pret.getTontine().getId());
        dto.setGestionnaireId(pret.getGestionnaire().getId());
        dto.setMontant(pret.getMontant());
        dto.setTauxInteret(pret.getTauxInteret());
        dto.setNbEcheances(pret.getNbEcheances());
        dto.setDateAccord(pret.getDateAccord());
        dto.setStatut(pret.getStatut());
        dto.setCreatedAt(pret.getCreatedAt());

        return dto;
    }
}
