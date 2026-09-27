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

    public List<InvitationDTO> listInvitation() {
        return invitationRepository.findAll()
                .stream()
                .map(this::convertiInvitationDTO)
                .toList();
    }

    public InvitationDTO createInvitation(Invitation invitation) {
        Invitation enregistree = invitationRepository.save(invitation);
        return convertiInvitationDTO(enregistree);
    }

    public Optional<InvitationDTO> updateInvitation(Long id, Invitation invitationModifier) {
        return invitationRepository.findById(id).map(invitationExsitante -> {
            if (invitationExsitante.getStatut() == StatutInvitation.ACCEPTE) {
                throw new InvitationDejaUtiliseeException(invitationExsitante.getToken());
            } else if (invitationExsitante.getExpireAt().isBefore(LocalDateTime.now())) {
                throw new InvitationExpireeException(invitationExsitante.getToken());
            }
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

            Invitation enregistre = invitationRepository.save(invitationExsitante);
            return convertiInvitationDTO(enregistre);
        });
    }

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
