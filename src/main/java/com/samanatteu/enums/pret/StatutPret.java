package com.samanatteu.enums.pret;

// ACTIF : accordé, le membre rembourse (un prêt naît ACTIF : c'est la
// gestionnaire qui le crée, pas de demande à valider).
// EN_RETARD : au moins une échéance dépassée et non payée.
// REMBOURSE : toutes les échéances payées (final).
public enum StatutPret {
    ACTIF, EN_RETARD, REMBOURSE
}
