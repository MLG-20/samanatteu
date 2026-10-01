package com.samanatteu.enums.tontine;

// UNITÉ du rythme des cycles ; le nombre d'unités est Tontine.intervalle.
// Ex. (MOIS, 2) = tous les 2 mois, (JOUR, 15) = tous les 15 jours.
// Écart assumé avec le CDC (qui fixait HEBDOMADAIRE/MENSUEL/TRIMESTRIEL) :
// le gestionnaire règle le rythme selon le fonctionnement de sa tontine.
// Un enum plutôt qu'un String : la liste est fermée, donc le code peut
// calculer une date pour chaque valeur (ex. la fin prévue d'un cycle).
public enum FrequenceTontine {
    JOUR, SEMAINE, MOIS
}
