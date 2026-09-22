package com.samanatteu.service.tontine;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.samanatteu.dto.tontine.ParticipationDTO;
import com.samanatteu.entity.Participation;
import com.samanatteu.exception.RelationObligatoireException;
import com.samanatteu.repository.ParticipationRepository;

@Service
public class ParticipationService {
    private final ParticipationRepository participationRepository;

    public ParticipationService(ParticipationRepository participationRepository) {
        this.participationRepository = participationRepository;
    }

    // Lister
    public List<ParticipationDTO> listNotification() {
        return participationRepository.findAll() // 1. List<Participation> brute depuis la base (avec motDePasse)
                .stream() // 2. transforme la liste en flux traitable élément par élément
                .map(this::convertiParticipationDTO) // 3. applique la conversion à CHAQUE Participation ->
                                                     // ParticipationDTO
                                                     // (sans motDePasse)
                .toList(); // 4. reconstitue une vraie List<ParticipationDTO> à partir du flux
    }

    // créer
    public ParticipationDTO createParticipation(Participation participation) {
        if (participation.getMembre() == null) {
            throw new RelationObligatoireException("membre");
        }
        if(participation.getTontine() == null){
            throw new RelationObligatoireException("tontine");
        }
        Participation enregistree = participationRepository.save(participation);
        return convertiParticipationDTO(enregistree);
    }

    // update
    public Optional<ParticipationDTO> updateParticipation(Long id, Participation participationModifier) {
        // findById(id) renvoie un Optional<Participation> : vide si l'id n'existe pas,
        // rempli sinon.
        // .map(...) ne s'exécute QUE si l'Optional est rempli — sinon il reste vide tel
        // quel (pas de NullPointerException).
        return participationRepository.findById(id).map(ParticipationExsitante -> {
            // ParticipationExsitante = l'entité déjà en base (trouvée par findById).
            // participationModifier = les nouvelles valeurs envoyées par le client
            // (paramètre
            // de la méthode).
            // On recopie les nouvelles valeurs DANS l'entité existante, champ par champ.
            ParticipationExsitante.setMembre(participationModifier.getMembre());
            ParticipationExsitante.setTontine(participationModifier.getTontine());
            ParticipationExsitante.setNombreParts(participationModifier.getNombreParts());
            ParticipationExsitante.setDateAdhesion(participationModifier.getDateAdhesion());
            ParticipationExsitante.setStatut(participationModifier.getStatut());
            ParticipationExsitante.setOrdreInscription(participationModifier.getOrdreInscription());

            // save() persiste les changements en base ET renvoie l'entité Participation à
            // jour
            // (avec motDePasse).
            Participation enregistre = participationRepository.save(ParticipationExsitante);
            // On ne renvoie JAMAIS l'entité brute au client : conversion en DTO juste avant
            // de sortir (sans motDePasse).
            // Comme on est dans un .map(), ce retour devient automatiquement le contenu de
            // l'Optional<ParticipationDTO>.
            return convertiParticipationDTO(enregistre);
        });
    }

    // Delete
    public boolean deleteParticipation(Long id) {
        if (participationRepository.existsById(id)) {
            participationRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private ParticipationDTO convertiParticipationDTO(Participation Participation) {
        ParticipationDTO dto = new ParticipationDTO();
        dto.setId(Participation.getId());
        dto.setMembreId(Participation.getMembre().getId());
        dto.setTontineId(Participation.getTontine().getId());
        dto.setNombreParts(Participation.getNombreParts());
        dto.setDateAdhesion(Participation.getDateAdhesion());
        dto.setStatut(Participation.getStatut());
        dto.setOrdreInscription(Participation.getOrdreInscription());

        return dto;
    }
}
