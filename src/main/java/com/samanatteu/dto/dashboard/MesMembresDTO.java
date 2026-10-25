package com.samanatteu.dto.dashboard;

// Carte « Membres » du tableau de bord gestionnaire. Un record : deux nombres
// en lecture seule, que Jackson écrit tels quels ({"nbMembres": 12, ...}).
// nbMembres : des PERSONNES (un membre dans deux tontines compte une fois).
// nbInscriptions : des PLACES (le même membre compte deux fois).
public record MesMembresDTO(long nbMembres, long nbInscriptions) {
}
