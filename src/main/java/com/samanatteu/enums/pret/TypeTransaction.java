package com.samanatteu.enums.pret;

// Nature d'un mouvement d'argent du journal (une ligne par mouvement).
// Enum plutôt que String : une faute de frappe ne compile plus.
public enum TypeTransaction {
    COTISATION, GAIN, PRET, REMBOURSEMENT, PENALITE
}
