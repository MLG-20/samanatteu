package com.samanatteu.service.tontine;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.samanatteu.dto.tontine.TontineDTO;
import com.samanatteu.entity.Participation;
import com.samanatteu.entity.Tontine;
import com.samanatteu.entity.Utilisateur;
import com.samanatteu.enums.StatutTontine;
import com.samanatteu.exception.AccesRefuseException;
import com.samanatteu.exception.TontineNonModifiableException;
import com.samanatteu.exception.TransitionStatutInvalideException;
import com.samanatteu.repository.ParticipationRepository;
import com.samanatteu.repository.TontineRepository;
import com.samanatteu.repository.UtilisateurRepository;

@Service
public class TontineService {
    private final TontineRepository tontineRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ParticipationRepository participationRepository;

    public TontineService(TontineRepository tontineRepository, UtilisateurRepository utilisateurRepository,
            ParticipationRepository participationRepository) {
        this.tontineRepository = tontineRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.participationRepository = participationRepository;
    }

    // Lister
    public List<TontineDTO> listTontine() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean estGestionnaire = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_GESTIONNAIRE"));

        List<Tontine> tontines = estGestionnaire
                ? tontineRepository.findByGestionnaireTelephone(auth.getName())
                : participationRepository.findByMembreTelephone(auth.getName())
                        .stream()
                        .map(Participation::getTontine)
                        .toList();

        return tontines.stream()
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
        tontine.setStatut(StatutTontine.EN_ATTENTE);
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

            if (tontineExsitant.getStatut() != StatutTontine.EN_ATTENTE) {
                throw new TontineNonModifiableException(tontineExsitant.getStatut().toString());
            }
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

    public Optional<TontineDTO> activerTontine(Long id) {
        return changerStatut(id, StatutTontine.ACTIVE, StatutTontine.EN_ATTENTE, StatutTontine.SUSPENDUE);
    }

    public Optional<TontineDTO> suspendreTontine(Long id) {
        return changerStatut(id, StatutTontine.SUSPENDUE, StatutTontine.ACTIVE);
    }

    public Optional<TontineDTO> cloturerTontine(Long id) {
        return changerStatut(id, StatutTontine.TERMINEE, StatutTontine.ACTIVE);
    }

    private Optional<TontineDTO> changerStatut(Long id, StatutTontine nouveau, StatutTontine... autorises) {
        return tontineRepository.findById(id).map(tontine -> {
            verifierProprietaire(tontine);
            if (!Arrays.asList(autorises).contains(tontine.getStatut())) {
                throw new TransitionStatutInvalideException(tontine.getStatut(), nouveau);
            }
            tontine.setStatut(nouveau);
            return convertiTontineDTO(tontineRepository.save(tontine));
        });
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
