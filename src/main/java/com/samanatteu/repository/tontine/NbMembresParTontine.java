package com.samanatteu.repository.tontine;

// Une ligne du résultat de ParticipationRepository.compterMembresParTontine :
// une tontine et son nombre de membres. Même principe que NbMembresParGestionnaire.
public record NbMembresParTontine(Long tontineId, Long nbMembres) {
}
