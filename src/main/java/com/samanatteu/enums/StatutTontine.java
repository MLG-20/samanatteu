package com.samanatteu.enums;

// Un enum = une liste fermée de valeurs possibles, pas une entité JPA.
// Pas de @Entity/@Table/@Getter/@Setter ici : ce n'est pas une classe "objet",
// juste un type qui limite les valeurs autorisées à celles listées ci-dessous.
// C'est l'entité qui UTILISE ce type (via un champ + @Enumerated) qui portera
// les annotations JPA, pas l'enum lui-même.
public enum StatutTontine {
    EN_ATTENTE, ACTIVE, TERMINEE, SUSPENDUE
}
