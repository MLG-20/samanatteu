package com.samanatteu.service.tontine;

import java.util.List;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.samanatteu.dto.tontine.TontineDTO;
import com.samanatteu.entity.Tontine;
import com.samanatteu.entity.Utilisateur;
import com.samanatteu.exception.AccesRefuseException;
import com.samanatteu.repository.TontineRepository;
import com.samanatteu.repository.UtilisateurRepository;

@Service
public class TontineService {
    private final TontineRepository tontineRepository;
    private final UtilisateurRepository utilisateurRepository;

    public TontineService(TontineRepository tontineRepository, UtilisateurRepository utilisateurRepository) {
        this.tontineRepository = tontineRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    // Lister
    public List<TontineDTO> listTontine() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return tontineRepository.findByGestionnaireTelephone(auth.getName()) // 1. List<Tontine> brute depuis la base (avec motDePasse)
                .stream() // 2. transforme la liste en flux traitable élément par élément
                .map(this::convertiTontineDTO) // 3. applique la conversion à CHAQUE Tontine -> TontineDTO
                                               // (sans motDePasse)
                .toList(); // 4. reconstitue une vraie List<TontineDTO> à partir du flux
    }

    // créer
    public TontineDTO createTontine(Tontine tontine) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Utilisateur gestionnaire = utilisateurRepository.findByTelephone(auth.getName())
        .orElseThrow(() -> new AccesRefuseException());
        tontine.setGestionnaire(gestionnaire);
        Tontine enregistre = tontineRepository.save(tontine);
        return convertiTontineDTO(enregistre);
    }

    // update
    public Optional<TontineDTO> updateTontine(Long id, Tontine tontineModifier) {
        // findById(id) renvoie un Optional<Tontine> : vide si l'id n'existe pas,
        // rempli sinon.
        // .map(...) ne s'exécute QUE si l'Optional est rempli — sinon il reste vide tel
        // quel (pas de NullPointerException).
        return tontineRepository.findById(id).map(tontineExsitant -> {
            verifierProprietaire(tontineExsitant);
            // tontineExsitant = l'entité déjà en base (trouvée par findById).
            // tontineModifier = les nouvelles valeurs envoyées par le client (paramètre
            // de la méthode).
            // On recopie les nouvelles valeurs DANS l'entité existante, champ par champ.
            tontineExsitant.setNom(tontineModifier.getNom());
            tontineExsitant.setMontantPart(tontineModifier.getMontantPart());
            tontineExsitant.setFrequence(tontineModifier.getFrequence());
            tontineExsitant.setNbCycles(tontineModifier.getNbCycles());
            tontineExsitant.setDescription(tontineModifier.getDescription());
            tontineExsitant.setJourCotisation(tontineModifier.getJourCotisation());
            tontineExsitant.setCreatedAt(tontineModifier.getCreatedAt());
            tontineExsitant.setUpdatedAt(tontineModifier.getUpdatedAt());
            tontineExsitant.setStatut(tontineModifier.getStatut());
            tontineExsitant.setGestionnaire(tontineModifier.getGestionnaire());

            // save() persiste les changements en base ET renvoie l'entité Tontine à jour
            // (avec motDePasse).
            Tontine enregistre = tontineRepository.save(tontineExsitant);
            // On ne renvoie JAMAIS l'entité brute au client : conversion en DTO juste avant
            // de sortir (sans motDePasse).
            // Comme on est dans un .map(), ce retour devient automatiquement le contenu de
            // l'Optional<TontineDTO>.
            return convertiTontineDTO(enregistre);
        });
    }

    // Delete
    public boolean deleteTontine(Long id) {
        Optional<Tontine> tontine = tontineRepository.findById(id);
        if (tontine.isPresent()) {
            verifierProprietaire(tontine.get());
            tontineRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private void verifierProprietaire(Tontine tontine) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!auth.getName().equals(tontine.getGestionnaire().getTelephone())) {
            throw new AccesRefuseException();
        }
    }

    private TontineDTO convertiTontineDTO(Tontine tontine) {
        TontineDTO dto = new TontineDTO();
        dto.setId(tontine.getId());
        dto.setNom(tontine.getNom());
        dto.setMontantPart(tontine.getMontantPart());
        dto.setFrequence(tontine.getFrequence());
        dto.setNbCycles(tontine.getNbCycles());
        dto.setDescription(tontine.getDescription());
        dto.setJourCotisation(tontine.getJourCotisation());
        dto.setCreatedAt(tontine.getCreatedAt());
        dto.setUpdatedAt(tontine.getUpdatedAt());
        dto.setStatut(tontine.getStatut());
        dto.setGestionnaireId(tontine.getGestionnaire().getId());

        return dto;
    }
}
