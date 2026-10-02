package com.samanatteu.repository.onboarding;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.samanatteu.entity.onboarding.Invitation;
import com.samanatteu.enums.onboarding.StatutInvitation;
import com.samanatteu.enums.onboarding.TypeInvitation;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {
      // Sert à retrouver le lien de groupe encore valide d'une tontine pour l'annuler.
      // List et pas Optional : s'il y en avait deux par erreur, on les annule tous.
      List<Invitation> findByTontineIdAndTypeAndStatut(Long tontineId, TypeInvitation type,
                  StatutInvitation statut);

      // Rapide grâce à l'index créé par la contrainte UNIQUE (V3).
      Optional<Invitation> findByToken(String token);

      // Liste de la gestionnaire : seulement les invitations de SES tontines, récentes d'abord.
      List<Invitation> findByTontineGestionnaireTelephoneOrderByCreatedAtDesc(String telephone);

}
