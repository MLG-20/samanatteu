package com.samanatteu.enums.onboarding;

// GROUPE : un lien par tontine, posté dans le groupe WhatsApp, utilisable par plusieurs membres.
// INDIVIDUELLE : un lien pour une seule personne (hors du groupe), usage unique, pré-rempli.
// Le CDC mélangeait les deux (« lien par tontine » + « usage unique ») : on les sépare.
public enum TypeInvitation {
    GROUPE, INDIVIDUELLE
}
