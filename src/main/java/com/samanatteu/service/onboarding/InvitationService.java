package com.samanatteu.service.onboarding;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.samanatteu.dto.onboarding.InvitationDTO;
import com.samanatteu.entity.Invitation;
import com.samanatteu.enums.StatutInvitation;
import com.samanatteu.exception.InvitationDejaUtiliseeException;
import com.samanatteu.exception.InvitationExpireeException;
import com.samanatteu.repository.InvitationRepository;

@Service
public class InvitationService {
    private final InvitationRepository invitationRepository;

    public InvitationService(InvitationRepository invitationRepository) {
        this.invitationRepository = invitationRepository;
    }

    // Lister
    public List<InvitationDTO> listInvitation() {
        return invitationRepository.findAll() // 1. List<Invitation> brute depuis la base (avec motDePasse)
                .stream() // 2. transforme la liste en flux traitable élément par élément
                .map(this::convertiInvitationDTO) // 3. applique la conversion à CHAQUE Invitation -> InvitationDTO
                                                  // (sans motDePasse)
                .toList(); // 4. reconstitue une vraie List<InvitationDTO> à partir du flux
    }

    // créer
    public InvitationDTO createInvitation(Invitation invitation) {
        Invitation enregistree = invitationRepository.save(invitation);
        return convertiInvitationDTO(enregistree);
    }

    // update
    public Optional<InvitationDTO> updateInvitation(Long id, Invitation invitationModifier) {
        // findById(id) renvoie un Optional<Invitation> : vide si l'id n'existe pas,
        // rempli sinon.
        // .map(...) ne s'exécute QUE si l'Optional est rempli — sinon il reste vide tel
        // quel (pas de NullPointerException).
        return invitationRepository.findById(id).map(invitationExsitante -> {
            if (invitationExsitante.getStatut() == StatutInvitation.ACCEPTE) {
                throw new InvitationDejaUtiliseeException(invitationExsitante.getToken());
            } else if (invitationExsitante.getExpireAt().isBefore(LocalDateTime.now())) {
                throw new InvitationExpireeException(invitationExsitante.getToken());
            }
            // invitationExsitante = l'entité déjà en base (trouvée par findById).
            // invitationModifier = les nouvelles valeurs envoyées par le client (paramètre
            // de la méthode).
            // On recopie les nouvelles valeurs DANS l'entité existante, champ par champ.
            invitationExsitante.setTontine(invitationModifier.getTontine());
            invitationExsitante.setToken(invitationModifier.getToken());
            invitationExsitante.setTelephone(invitationModifier.getTelephone());
            invitationExsitante.setEmail(invitationModifier.getEmail());
            invitationExsitante.setPrenomPreRempli(invitationModifier.getPrenomPreRempli());
            invitationExsitante.setNomPreRempli(invitationModifier.getNomPreRempli());
            invitationExsitante.setNombreParts(invitationModifier.getNombreParts());
            invitationExsitante.setExpireAt(invitationModifier.getExpireAt());
            invitationExsitante.setStatut(invitationModifier.getStatut());
            invitationExsitante.setCreatedAt(invitationModifier.getCreatedAt());

            // save() persiste les changements en base ET renvoie l'entité Invitation à jour
            // (avec motDePasse).
            Invitation enregistre = invitationRepository.save(invitationExsitante);
            // On ne renvoie JAMAIS l'entité brute au client : conversion en DTO juste avant
            // de sortir (sans motDePasse).
            // Comme on est dans un .map(), ce retour devient automatiquement le contenu de
            // l'Optional<InvitationDTO>.
            return convertiInvitationDTO(enregistre);
        });
    }

    // Delete
    public boolean deleteInvitation(Long id) {
        if (invitationRepository.existsById(id)) {
            invitationRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private InvitationDTO convertiInvitationDTO(Invitation invitation) {
        InvitationDTO dto = new InvitationDTO();
        dto.setId(invitation.getId());
        dto.setTontineId(invitation.getTontine().getId());
        dto.setToken(invitation.getToken());
        dto.setTelephone(invitation.getTelephone());
        dto.setEmail(invitation.getEmail());
        dto.setPrenomPreRempli(invitation.getPrenomPreRempli());
        dto.setNomPreRempli(invitation.getNomPreRempli());
        dto.setNombreParts(invitation.getNombreParts());
        dto.setExpireAt(invitation.getExpireAt());
        dto.setStatut(invitation.getStatut());
        dto.setCreatedAt(invitation.getCreatedAt());

        return dto;
    }
}
