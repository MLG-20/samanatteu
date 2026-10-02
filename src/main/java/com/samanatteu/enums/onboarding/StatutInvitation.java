package com.samanatteu.enums.onboarding;

// EN_ATTENTE : lien valide. ACCEPTE : invitation individuelle utilisée.
// EXPIRE : 7 jours passés. ANNULEE : lien de groupe remplacé par un nouveau
// (gardé en base plutôt que supprimé : historique + message clair si on clique dessus).
public enum StatutInvitation {
    EN_ATTENTE, ACCEPTE, EXPIRE, ANNULEE
}
