package com.samanatteu.repository.tontine;

// Une ligne du résultat de ParticipationRepository.compterMembresParGestionnaire :
// un gestionnaire et le nombre de personnes distinctes inscrites dans ses tontines.
// Un record : champs final, constructeur et accesseurs (gestionnaireId(), nbMembres())
// générés par Java.
public record NbMembresParGestionnaire(Long gestionnaireId, Long nbMembres) {
}
