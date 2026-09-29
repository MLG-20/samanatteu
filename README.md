# SamaNatteu

API REST de gestion de **tontines** (épargne rotative entre membres d'un groupe), construite avec
Spring Boot. Un gestionnaire crée une tontine, y inscrit des membres, et l'application suit les
cycles, les cotisations, les tirages et les prêts.

> **État du projet : en développement actif.** L'authentification et la gestion des tontines sont
> fonctionnelles et testées ; le reste du domaine (cotisations, tirages, prêts, notifications…) existe
> pour l'instant sous forme de CRUD générique. Voir [Avancement](#avancement).

## Sommaire

- [Rôles](#rôles)
- [Stack technique](#stack-technique)
- [Démarrage rapide](#démarrage-rapide)
- [API](#api)
- [Architecture](#architecture)
- [Sécurité](#sécurité)
- [Tests](#tests)
- [Avancement](#avancement)
- [Journal des décisions](#journal-des-décisions)

## Rôles

| Rôle | Qui | Ce qu'il peut faire |
|---|---|---|
| `ADMIN` | le propriétaire de la plateforme | gérer les comptes utilisateurs. **Ne peut ni lire ni modifier le contenu des tontines** des gestionnaires (frontière entre les données de chaque client). |
| `GESTIONNAIRE` | l'organisateur d'une tontine | créer et piloter **ses** tontines et leurs participations |
| `MEMBRE` | un participant | consulter les tontines où il participe et ses participations |

## Stack technique

- **Java 21**, **Spring Boot 4.1.1** (Maven wrapper inclus)
- Spring Web (REST), Spring Data JPA / Hibernate, Bean Validation
- Spring Security + **JWT** ([jjwt](https://github.com/jwtk/jjwt) 0.12.6), mots de passe hachés en BCrypt
- **PostgreSQL**
- Lombok
- Tests : JUnit 5, Mockito, MockMvc, Spring Security Test

## Démarrage rapide

### Prérequis

- JDK 21
- PostgreSQL (une base et un utilisateur dédiés)

### 1. Créer la base

```bash
sudo -u postgres psql <<'SQL'
CREATE USER samanatteu_user WITH PASSWORD 'choisis_un_mot_de_passe';
CREATE DATABASE samanatteu OWNER samanatteu_user;
SQL

# Créer les 12 tables (l'application valide le schéma au démarrage, elle ne le crée pas)
psql -h localhost -U samanatteu_user -d samanatteu -f db/schema.sql
```

### 2. Configurer les secrets

```bash
cp .env.example .env
```

Puis renseigne dans `.env` :

| Variable | Rôle |
|---|---|
| `DB_PASSWORD` | mot de passe de `samanatteu_user` |
| `JWT_SECRET` | secret de signature des tokens, **long et aléatoire** (par ex. `openssl rand -base64 48`) |

`.env` est ignoré par Git. Il est chargé automatiquement au démarrage
(`spring.config.import` dans `application.yaml`) tant que tu lances l'application **depuis ce
dossier**.

### 3. Lancer

```bash
./mvnw spring-boot:run
```

L'API écoute sur `http://localhost:8080`. Pour un autre port :
`./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081`.

### Premier essai

```bash
# Inscription d'un gestionnaire (l'email est optionnel, le téléphone obligatoire)
curl -X POST localhost:8080/utilisateur -H 'Content-Type: application/json' \
  -d '{"nom":"Diop","prenom":"Awa","telephone":"770000101","motDePasse":"motdepasse123","role":"GESTIONNAIRE"}'

# Connexion : renvoie un accessToken (15 min) et un refreshToken (7 jours)
curl -X POST localhost:8080/auth/login -H 'Content-Type: application/json' \
  -d '{"telephone":"770000101","motDePasse":"motdepasse123"}'

# Créer une tontine avec le token reçu
curl -X POST localhost:8080/tontine -H "Authorization: Bearer <accessToken>" \
  -H 'Content-Type: application/json' \
  -d '{"nom":"Tontine des amis","montantPart":10000,"frequence":"MOIS","intervalle":1,"nbCycles":10,"jourCotisation":5}'
```

`frequence` est une unité (`JOUR`, `SEMAINE`, `MOIS`) et `intervalle` un nombre d'unités :
`"frequence":"MOIS","intervalle":2` = un cycle tous les 2 mois.

## API

Toutes les routes sont en JSON. Les routes protégées demandent l'en-tête
`Authorization: Bearer <accessToken>`.

### Authentification et comptes

| Méthode | Route | Accès | Description |
|---|---|---|---|
| `POST` | `/auth/login` | public | connexion par **email ou téléphone** |
| `POST` | `/auth/refresh` | public | échange un refresh token contre une nouvelle paire de tokens |
| `POST` | `/utilisateur` | public | inscription (créer un `ADMIN` exige un token `ADMIN`) |
| `GET` | `/utilisateur` | `ADMIN` | liste des utilisateurs |
| `PUT` | `/utilisateur/{id}` | connecté | modifier **son propre** profil (ou tout profil si `ADMIN`) : `nom`, `prenom`, `email` uniquement |
| `DELETE` | `/utilisateur/{id}` | `ADMIN` | supprimer un utilisateur |

### Tontines (implémenté, testé)

| Méthode | Route | Accès | Description |
|---|---|---|---|
| `POST` | `/tontine` | `GESTIONNAIRE` | crée une tontine (statut forcé à `EN_ATTENTE`, propriétaire = le token) |
| `GET` | `/tontine` | `GESTIONNAIRE` | liste **ses** tontines uniquement |
| `PUT` | `/tontine/{id}` | propriétaire | modifie la tontine, **seulement tant qu'elle est `EN_ATTENTE`** |
| `DELETE` | `/tontine/{id}` | propriétaire | supprime la tontine |
| `POST` | `/tontine/{id}/activer` | propriétaire | `EN_ATTENTE` ou `SUSPENDUE` → `ACTIVE` ; la 1re activation calcule `nbCycles` |
| `POST` | `/tontine/{id}/suspendre` | propriétaire | `ACTIVE` → `SUSPENDUE` |
| `POST` | `/tontine/{id}/cloturer` | propriétaire | `ACTIVE` → `TERMINEE` (état final) |
| `POST` | `/tontine/{id}/cycles` | propriétaire | ouvre le cycle suivant (voir *Cycles et cotisations*) |

Cycle de vie du statut :

```
EN_ATTENTE ──activer──▶ ACTIVE ──suspendre──▶ SUSPENDUE
                          │  ◀──activer (reprise)──┘
                          └──cloturer──▶ TERMINEE   (état final)
```

Le statut ne se modifie que par ces actions : un `statut` envoyé dans un `POST` ou un `PUT` est ignoré.

`nbCycles` n'est pas saisi non plus : il vaut `0` à la création (« pas encore calculé ») et la
**première activation** le fixe à la somme des parts des membres actifs (une part = un gain = un
cycle). Activer une tontine sans membre est refusé (409). Une reprise (`SUSPENDUE` → `ACTIVE`) ne
le recalcule pas.

### Participations (implémenté, testé)

Une participation inscrit un membre à une tontine avec un nombre de parts.

| Méthode | Route | Accès | Description |
|---|---|---|---|
| `POST` | `/participation` | propriétaire de la tontine | inscrit un membre (`{"membre":{"id":…},"tontine":{"id":…},"nombreParts":…}`) |
| `GET` | `/participation` | `GESTIONNAIRE`, `MEMBRE` | un gestionnaire voit les participations de **ses** tontines, un membre **les siennes** |
| `PUT` | `/participation/{id}` | propriétaire de la tontine | modifie **uniquement** le nombre de parts |
| `DELETE` | `/participation/{id}` | propriétaire de la tontine | retire un membre |

Règles : un membre ne peut pas être inscrit deux fois à la même tontine (409) ; `nombreParts` est
obligatoire et **au moins 1** (400) ; inscrire, modifier ou retirer un membre n'est possible que
**tant que la tontine est `EN_ATTENTE`** (409 ensuite) ; le statut (`ACTIF`), la date d'adhésion et l'ordre d'inscription sont
décidés par le serveur ; un membre ou une tontine inexistants renvoient 404.

`GET /tontine` est aussi ouvert aux `MEMBRE`, qui voient alors les tontines où ils participent.

### Cycles et cotisations (implémenté, testé)

Un **cycle** est une période de collecte ; à son ouverture, chaque membre actif doit une
**cotisation** de `nombreParts × montantPart`.

| Méthode | Route | Accès | Description |
|---|---|---|---|
| `POST` | `/tontine/{id}/cycles` | propriétaire | ouvre le cycle suivant, **sans corps** : tout est calculé par le serveur |
| `POST` | `/cotisation/{id}/paiement` | propriétaire | enregistre un paiement (`{"montant":…,"modePaiement":"CASH\|WAVE\|ORANGE\|FREE","reference":…}`) |
| `POST` | `/cycle/{id}/cloturer` | propriétaire | clôture le cycle ; les impayés passent `EN_RETARD` |
| `GET` | `/cycle`, `/cotisation` | `GESTIONNAIRE`, `MEMBRE` | un gestionnaire voit ceux de **ses** tontines ; un membre les cycles de ses tontines et **ses propres** cotisations |
| `DELETE` | `/cycle/{id}`, `/cotisation/{id}` | propriétaire | suppression |

Règles :

- **Ouverture** : la tontine doit être `ACTIVE` (409), aucun autre cycle `EN_COURS` (409), et pas
  plus de `nbCycles` cycles (409). Le serveur fixe le numéro (dernier + 1), la date de début, la
  fin prévue (début + `intervalle` × `frequence`), le statut `EN_COURS`, crée une cotisation
  `EN_ATTENTE` par membre `ACTIF` et calcule le montant attendu (somme des montants dus).
- **Paiement** : montant > 0 et mode obligatoires (400) ; pas plus que le reste dû (400) ; une
  cotisation `COMPLET` est refusée (409). Statut obtenu : `COMPLET` si tout est payé, sinon
  `PARTIEL` — sauf une cotisation `EN_RETARD`, qui le reste jusqu'au solde. Le montant collecté du
  cycle augmente du paiement.
- **Clôture** : seul un cycle `EN_COURS` se clôture (409) ; les cotisations non `COMPLET` passent
  `EN_RETARD` et **restent payables** ; le cycle passe `CLOTURE` avec sa date de fin réelle.
- Ouverture, paiement et clôture sont **atomiques** (`@Transactional`) : tout réussit ou rien.
- Aucune route ne permet de créer ou modifier un cycle ou une cotisation à la main.

```
cotisation : EN_ATTENTE ──paiement──▶ PARTIEL ──paiement──▶ COMPLET
                 │                       │                     ▲
                 └──── clôture ──▶ EN_RETARD ──paiement (solde)─┘
```

### Tirages (implémenté, testé)

À la fin de chaque cycle, un tirage au sort désigne le membre qui reçoit **toute la cagnotte**.
Une part = un gain : un membre à deux parts (*gnari lokho*) gagne deux fois, à deux cycles
différents.

| Méthode | Route | Accès | Description |
|---|---|---|---|
| `POST` | `/cycle/{id}/tirage` | propriétaire | tire au sort le gagnant du cycle, **sans corps** |
| `POST` | `/tirage/{id}/verser` | propriétaire | enregistre une remise d'argent au gagnant (`{"montant":…}`) |
| `POST` | `/tirage/{id}/reporter` | propriétaire | met le versement en pause (arrangement entre membres) |
| `GET` | `/tirage` | `GESTIONNAIRE`, `MEMBRE` | un gestionnaire voit les tirages de **ses** tontines ; un membre **tous** les tirages des tontines où il participe (transparence) |

Règles :

- **Tirage** : seulement sur un cycle `CLOTURE` (409) et une seule fois par cycle (409). L'urne
  contient chaque membre actif à qui il reste au moins une part non gagnée (parts − tirages déjà
  gagnés), **une seule fois** : chance égale pour tous à chaque tirage. Urne vide → 409. Le hasard
  vient de `SecureRandom` (imprévisible).
- **Montants** : le gagnant a droit à la cagnotte attendue du cycle ; il reçoit tout de suite ce
  qui est en caisse. Statut `VERSE` si tout est remis, `PARTIEL` sinon, `EN_ATTENTE` si rien.
- **Compensation** : si le gagnant est lui-même `EN_RETARD` sur ce cycle, sa dette est payée par
  son gain (cotisation `COMPLET`, caisse complétée).
- **Versement** : on ne remet pas plus que l'argent en caisse pas encore remis (400). Le gestionnaire
  l'enregistre quand la remise a **réellement** lieu.
- **Report** : seulement `EN_ATTENTE` ou `PARTIEL` (409) ; un versement fait sortir de `REPORTE`.
  Le gagnant ne change jamais.
- Aucune route ne permet de créer, modifier ou supprimer un tirage à la main.

```
tirage : EN_ATTENTE ──verser──▶ PARTIEL ──verser (solde)──▶ VERSE
             │                     │                         ▲
             └──reporter──▶ REPORTE ◀──reporter──┘           │
                               └──────────verser─────────────┘
```

### Autres ressources (CRUD générique, sans règles métier pour l'instant)

`/pret`, `/echeancePret`, `/transaction`, `/invitation`,
`/importMembre`, `/notification`.

Elles demandent simplement d'être connecté : **elles seront durcies au fil des phases.**

### Codes d'erreur

Les erreurs métier renvoient un message lisible et un code HTTP cohérent :

| Code | Signification | Exemple |
|---|---|---|
| `400` | requête incomplète ou valeur invalide | nombre de parts à 0, membre sans id |
| `401` | non authentifié / token invalide ou expiré | token absent |
| `403` | authentifié mais pas autorisé | un `MEMBRE` qui active une tontine |
| `404` | ressource ou route introuvable | tontine inexistante |
| `405` | méthode HTTP non prise en charge par la route | `POST /tirage` |
| `409` | l'état actuel de la ressource bloque l'action | modifier une tontine `ACTIVE`, réactiver une tontine `TERMINEE` |

## Architecture

Architecture en couches classique :

```
Controller  ──▶  Service  ──▶  Repository  ──▶  PostgreSQL
 (HTTP)        (règles métier)   (Spring Data JPA)
```

```
src/main/java/com/samanatteu/
├── config/        SecurityConfig (règles d'accès par route et par rôle)
├── security/      JwtUtil, JwtAuthFilter, gestionnaires d'erreur 401 / 403, UtilisateurConnecte
├── controller/    un contrôleur REST par ressource
├── service/       logique métier, regroupée par domaine
│   ├── auth/  utilisateur/  tontine/  cotisation/  pret/  onboarding/  notification/
├── repository/    interfaces Spring Data JPA
├── entity/        entités JPA (12 tables)
├── dto/           objets d'échange avec le client (jamais l'entité brute : pas de fuite de mot de passe)
├── enums/         statuts et rôles
├── exception/     exceptions métier (héritent de SamanatteuException, avec leur code HTTP),
│   ├── auth/  utilisateur/  tontine/  cotisation/  pret/  onboarding/  notification/
└── handler/       GlobalExceptionHandler : transforme les exceptions en réponses JSON
```

Choix notables :

- **Les entités ne sortent jamais de l'API** : chaque réponse passe par un DTO.
- **Une exception métier = un code HTTP**, portée par la classe elle-même ; le
  `GlobalExceptionHandler` n'a pas à être modifié pour en ajouter une.
- **Le schéma est validé, pas généré** (`ddl-auto: validate`) : la base fait foi.
- **Les services ne dépendent pas de Spring Security** : ils demandent « qui est connecté ? » et
  « a-t-il tel rôle ? » à `UtilisateurConnecte` (`telephone()`, `aLeRole(RoleUtilisateur)`), seul
  endroit, avec `JwtAuthFilter`, à toucher au `SecurityContextHolder`.
- **Une règle sur une entité vit dans l'entité** : « cette tontine est-elle gérée par tel
  utilisateur ? » est `Tontine.estGereePar(telephone)`. Son usage (« sinon 403 ») est
  `UtilisateurConnecte.verifierGestionnaire(tontine)`, partagé par les services de tontines,
  participations, cycles et cotisations.

## Sécurité

- **JWT** : access token de 15 minutes + refresh token de 7 jours. Un claim `type` (`access` /
  `refresh`) empêche de confondre les deux, **dans les deux sens** : `/auth/refresh` n'accepte que
  `refresh`, et le filtre JWT n'authentifie une requête qu'avec `access` (liste blanche). L'identité du token est le **numéro de téléphone**, car l'email est optionnel.
- **Rôle dans le token** : traduit en autorité Spring (`ROLE_ADMIN`, `ROLE_GESTIONNAIRE`,
  `ROLE_MEMBRE`) par `JwtAuthFilter`.
- **Autorisation à deux niveaux** : le rôle est vérifié dans `SecurityConfig`, puis la **propriété de
  la ressource** est revérifiée dans le service (un gestionnaire ne touche pas aux tontines d'un autre).
- **Identité prise dans le token, jamais dans le JSON** : le propriétaire d'une tontine créée est
  toujours l'utilisateur connecté, quoi que le client envoie.
- **Secrets hors du code** : mot de passe de base et secret JWT viennent de variables d'environnement.

## Tests

```bash
# Tests unitaires et de sécurité (sans base de données)
./mvnw test -Dtest='*ServiceTest,*ControllerSecurityTest,JwtAuthFilterTest'
```

- `TontineServiceTest`, `ParticipationServiceTest`, `UtilisateurServiceTest`, `CycleServiceTest`,
  `CotisationServiceTest`, `TirageServiceTest` : règles métier des services avec des faux
  repositories (Mockito) — propriété, cycle de vie du statut, doublons, valeurs décidées par le
  serveur, identité issue du token, lecture filtrée par rôle, champs modifiables d'un profil, calcul
  des montants dus et attendus, paiements partiels, retards à la clôture, composition de l'urne,
  compensation, versements et reports. Le hasard du tirage est remplacé par un faux `Random` qui
  choisit une case connue et retient la taille de l'urne.
- `TontineControllerSecurityTest`, `ParticipationControllerSecurityTest`,
  `CycleControllerSecurityTest`, `CotisationControllerSecurityTest`,
  `TirageControllerSecurityTest` : règles d'accès HTTP de `SecurityConfig` (401 / 403 / 200 / 204 /
  404 / 405) et validation des corps (400) avec MockMvc, sans serveur ni base.
- `JwtAuthFilterTest` : le filtre JWT avec de **vrais** tokens signés (access accepté, refresh et
  token falsifié refusés). Les tests MockMvc simulent l'utilisateur avec `@WithMockUser` et ne
  traversent pas ce filtre.

> `SamanatteuApplicationTests` (chargement complet du contexte) nécessite PostgreSQL et les variables
> d'environnement ; il n'est donc pas inclus dans la commande ci-dessus.

**Base de test.** Les essais de l'API de bout en bout se font sur une base séparée,
`samanatteu_test`, pour ne jamais toucher aux données réelles. Le profil Spring `test`
(`application-test.yaml`) ne change que l'URL de la base :

```bash
sudo -u postgres createdb -O samanatteu_user samanatteu_test
psql -h localhost -U samanatteu_user -d samanatteu_test -f db/schema.sql
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8081 --spring.profiles.active=test"
```

## Avancement

Le développement suit un planning en 8 phases.

| Phase | Contenu | État |
|---|---|---|
| 1 | Bases : projet, entités, base PostgreSQL | ✅ terminée |
| 2 | Authentification et rôles | ✅ terminée, sauf « mot de passe oublié » (nécessite l'envoi de SMS, phase 6) |
| 3 | Tontines, membres, participations | ✅ tontines et cycle de vie, participations (doublons, parts, propriété, lecture filtrée par rôle) ; reste à trancher : comment devient-on `GESTIONNAIRE` |
| 4 | Cycles et cotisations (calcul du montant dû, retards, reçu PDF) | ✅ ouverture, paiements, clôture avec retards, lecture filtrée ; reste le reçu PDF (avec l'historique des paiements) |
| 5 | Tirage au sort, prêts et échéanciers | 🚧 tirage terminé (urne, compensation, versements, reports, `nbCycles` calculé) ; prêts et échéanciers à venir |
| 6 | Notifications e-mail et SMS, invitations, import Excel/CSV | ⏳ à venir |
| 7-8 | Tableaux de bord par rôle, finitions | ⏳ à venir |

### Limites connues

- Les ressources autres que `/tontine`, `/participation`, `/cycle`, `/cotisation` et `/tirage` n'ont pas encore de règles métier ni de contrôle de propriété.
- Un tirage ne garde que la date de son **dernier** versement ; l'historique des remises au
  gagnant relèvera, comme les paiements, de la table `transaction`.
- Un corps JSON mal formé n'a pas encore de traitement dédié dans `GlobalExceptionHandler`
  (`HttpMessageNotReadableException` n'est pas une `ErrorResponse`) : à vérifier et ramener à 400.
- Une cotisation ne garde que le mode, la référence et la date de son **dernier** paiement :
  l'historique des paiements successifs (table `transaction`) et le reçu PDF par paiement restent à
  faire.
- Un membre sorti d'une tontine déjà lancée n'a pas encore d'action dédiée (statut `SORTI`) : retirer un participant n'est possible que tant que la tontine est `EN_ATTENTE`.
- Le refresh token n'est pas révoqué après usage (pas de stockage côté serveur).
- Les champs `createdAt` / `updatedAt` ne sont pas encore alimentés.
- Comment devient-on `GESTIONNAIRE` : décidé pour un modèle par **abonnement** (voir le journal
  ci-dessous), pas encore implémenté — aujourd'hui l'inscription permet encore de choisir
  `GESTIONNAIRE` ou `MEMBRE` librement.
- Pas de migration de base (Flyway/Liquibase) : le schéma est fourni tel quel dans `db/schema.sql`.
- `POST /tontine`, `PUT /tontine/{id}` et `/participation` reçoivent encore l'entité JPA plutôt
  qu'un DTO d'entrée (protégées par une recopie champ par champ, à migrer vers des DTO).
- L'inscription enregistre encore un email vide (`""`) tel quel au lieu de `null`.

## Journal des décisions

Ce projet est aussi un exercice d'apprentissage : les décisions de conception et les bugs
significatifs sont tracés ici plutôt que perdus dans l'historique Git, pour que le raisonnement
reste lisible.

### Décisions de conception

- **L'ADMIN ne touche pas au contenu des tontines.** Dans un SaaS multi-clients, la plateforme
  (ADMIN) ne doit ni lire ni modifier les données métier de ses clients (GESTIONNAIRE). Toutes les
  routes `/tontine` et `/participation` sont donc en `hasRole("GESTIONNAIRE")`, sans échappatoire
  pour l'ADMIN — contrairement à `/utilisateur`, où l'ADMIN garde un accès complet.
- **L'identité du JWT est le téléphone, pas l'email.** L'email est devenu optionnel à l'inscription
  (téléphone obligatoire, cas d'usage principal). Utiliser l'email comme identité aurait cassé les
  comptes téléphone-only.
- **Un statut ne se modifie jamais par un `PUT` libre.** Une ressource avec un cycle de vie
  (`StatutTontine`) n'expose que des actions dédiées (`activer`/`suspendre`/`cloturer`), chacune
  avec ses transitions autorisées ; le `PUT` générique n'écrit plus le statut du tout. Voir
  [`Notes_Cours/11_Cycle_de_Vie_Statut.md`](../Notes_Cours/11_Cycle_de_Vie_Statut.md).
- **Un objet imbriqué du JSON (`{"id": …}`) n'est jamais une entité de confiance.** `tontine` et
  `membre` envoyés dans le corps d'une requête ne servent qu'à indiquer un id ; le service recharge
  toujours la vraie ressource en base avant de contrôler quoi que ce soit dessus.
- **Comment devient-on `GESTIONNAIRE` : par abonnement.** Dans un SaaS, c'est l'organisateur qui
  paie, pas les membres. Prévu en 3 étapes futures : (1) une entité `Abonnement` (formule, statut,
  échéance) avec la règle « créer une tontine exige un abonnement actif », (2) une activation
  manuelle par l'ADMIN pour développer et tester sans paiement réel, (3) un vrai paiement (Wave,
  Orange Money…), dépendant de la phase notifications. Non implémenté à ce stade.
- **Un membre qui quitte une tontine active n'est pas supprimé.** Ses cotisations passées font
  partie de l'historique financier ; l'effacer les effacerait aussi. La solution prévue est un
  troisième statut de participation, `SORTI` (déjà dans l'enum), via une action dédiée
  (`POST /participation/{id}/sortir`) qui l'exclut des futurs tirages/cotisations sans toucher à
  ses lignes passées — à construire avec les phases cotisations/tirages, une fois leur logique
  réelle en place.
- **Une requête d'écriture reçoit un DTO d'entrée, pas l'entité.** Recevoir l'entité JPA en
  `@RequestBody` laisse le client remplir n'importe lequel de ses champs (*mass assignment*) : se
  protéger en « oubliant » de recopier les champs sensibles est une liste noire, fragile. Un DTO
  d'entrée est une liste blanche : un champ absent du DTO ne peut tout simplement pas être envoyé,
  et le compilateur refuse qu'on le recopie. Appliqué à l'inscription et à la modification de
  profil (`ModificationUtilisateurDTO` : `nom`, `prenom`, `email`). Changer de rôle relèvera d'une
  action d'administration dédiée ; changer de téléphone (l'identité du JWT) d'une vérification par
  SMS.
- **Un seul point de lecture de l'utilisateur connecté.** Trois services lisaient eux-mêmes
  `SecurityContextHolder` et comparaient des chaînes `"ROLE_..."` écrites à la main (une faute de
  frappe y compile sans erreur). `UtilisateurConnecte` les remplace, avec l'enum `RoleUtilisateur`
  en paramètre. Refonte faite sans modifier les tests existants (injection d'un `@Spy` réel), ce
  qui prouve que le comportement n'a pas changé.
- **Deux `verifierProprietaire` identiques sont gardés volontairement.** La règle elle-même
  (`Tontine.estGereePar`) n'existe qu'à un endroit ; il ne reste dans les services que son usage
  (« sinon 403 »). Les fusionner rendrait `ParticipationService` dépendant de `TontineService` pour
  trois lignes. On factorisera au troisième usage (règle de trois). *Fait en phase 4* : les cycles
  en avaient besoin, le contrôle est devenu `UtilisateurConnecte.verifierGestionnaire`.
- **« Pas d'email » s'écrit toujours `null`, jamais `""`.** L'email est optionnel et unique en base :
  si des chaînes vides étaient stockées, deux comptes sans email entreraient en conflit. L'entrée est
  normalisée (vide ou blanc → `null`) avant toute vérification.
- **Clôturer un cycle avec des impayés est permis (écart volontaire au cahier des charges).** Le CDC
  se contredit : sa règle R4 interdit de clôturer tant qu'une cotisation n'est pas complète, mais il
  prévoit aussi un marquage « en retard si le cycle est fermé sans paiement complet ». Appliquer R4
  laisserait un seul retardataire bloquer une tontine de 300 membres. Choix retenu : la clôture
  passe les impayés `EN_RETARD`, qui restent payables ; un paiement partiel n'efface pas le retard,
  pour garder la trace utile aux rappels.
- **Fréquence libre : une unité et un intervalle.** Le CDC ne prévoyait que trois valeurs
  (hebdomadaire, mensuelle, trimestrielle) ; une tontine « tous les 15 jours » n'y entrait pas.
  `frequence` devient une unité (`JOUR`, `SEMAINE`, `MOIS`) et `intervalle` un nombre (≥ 1). Un
  simple nombre de jours a été écarté : 30 jours ne font pas un mois, les échéances auraient
  dérivé (`plusMonths` gère les fins de mois).
- **Cycles et cotisations naissent et changent uniquement par des actions métier.** Les `POST` et
  `PUT` génériques de `/cycle` et `/cotisation` laissaient le client écrire numéro, montants et
  statuts. Ils sont remplacés par `ouvrir`, `paiement` et `cloturer`, où le serveur calcule tout —
  même principe que le statut des tontines. Chacune de ces actions écrit plusieurs lignes (un cycle
  et N cotisations, ou une cotisation et son cycle) : elles sont `@Transactional`, pour qu'un échec
  au milieu ne laisse pas un cycle `EN_COURS` à moitié créé qui bloquerait la tontine.
- **Les montants sont des `BigDecimal` comparés avec `compareTo`.** `equals` tient compte du nombre
  de décimales (`10000` ≠ `10000.00`) et aurait laissé une cotisation soldée en `PARTIEL`. Un test
  couvre ce cas précis.
- **Le tirage se fait par part, pas par membre (écart volontaire au cahier des charges).** Le CDC
  donne au gagnant `parts_gagnant / total_parts × montant_collecte` (R2) et interdit de tirer deux
  fois le même membre (R3). Avec Awa (2 parts), Binta et Coumba (1 part), 10 000 F la part : Awa
  cotiserait 80 000 F et ne recevrait que 20 000 F, et personne ne sait où irait le reste. Le modèle
  retenu, validé auprès de personnes qui pratiquent la tontine : **une part = un gain de toute la
  cagnotte**. Awa gagne deux fois, à deux cycles différents. Critère de vérification : à la fin,
  chacun a reçu exactement ce qu'il a cotisé.
- **Chance égale à chaque tirage (écart au cahier des charges).** Le CDC prévoit qu'un membre à deux
  parts ait deux fois plus de chances. Choix de la pratique réelle : chaque membre encore en lice
  figure **une seule fois** dans l'urne ; ses parts fixent combien de fois il gagne, pas sa
  probabilité de gagner.
- **L'urne n'est pas stockée, elle est recalculée.** Tickets restants = parts − tirages déjà gagnés.
  Un compteur à part serait une seconde source de vérité, qui finirait par contredire la table
  `tirage`. Même principe pour `nbCycles`, qui se déduit des parts : il est calculé à la première
  activation au lieu d'être saisi (une valeur qui n'a qu'une seule réponse correcte ne se saisit
  pas).
- **On ne tire qu'un cycle clôturé.** Comme en pratique : on rassemble l'argent du tour, puis on
  tire.
- **Retards au moment du tirage : versement en plusieurs fois et compensation.** Le gagnant a droit
  à toute la cagnotte attendue ; il reçoit tout de suite ce qui est en caisse, le reste lui est
  remis quand les retardataires paient. Si le gagnant est lui-même en retard, sa dette est payée
  par son gain : lui demander de payer pour se faire rendre l'argent aussitôt n'aurait pas de sens.
- **Un versement s'enregistre quand il a réellement lieu.** Le reverser automatiquement au paiement
  d'un retard aurait affiché « versé » alors que l'argent était encore chez le gestionnaire. Sur une
  plateforme dont la promesse est la transparence, `montantVerse` doit dire la vérité aux membres :
  c'est une action manuelle, `verser`, plafonnée par l'argent disponible.
- **Le résultat d'un tirage est définitif.** Aucune route ne crée, ne modifie ni ne supprime un
  tirage : supprimer puis retirer permettrait de relancer le hasard jusqu'au « bon » gagnant, sans
  trace. Le statut `REPORTE` ne met en pause que le **versement** (arrangement entre membres) ; une
  option « annuler et retirer » a été écartée pour la même raison.

### Bugs trouvés et corrigés

- **Toute erreur du client devenait une « erreur inattendue » 500.** Le filet
  `@ExceptionHandler(Exception.class)` de `GlobalExceptionHandler` attrapait aussi les exceptions
  par lesquelles Spring signale une route inexistante (404) ou une méthode non prise en charge
  (405) : un client qui se trompait d'URL croyait le serveur en panne, et les logs se remplissaient
  de fausses erreurs. Trouvé en écrivant les tests qui vérifient que les anciennes routes du tirage
  n'existent plus. Corrigé en laissant passer les exceptions qui implémentent `ErrorResponse` avec
  leur propre code ; le filet ne garde que les vrais bugs.
- **Le tirage générique était une porte dérobée.** `POST`, `PUT` et `DELETE /tirage` acceptaient un
  tirage complet envoyé par le client, sans contrôle de propriété : n'importe quel utilisateur
  connecté pouvait se désigner gagnant, ou supprimer un tirage pour le relancer. Routes supprimées,
  remplacées par les actions métier ci-dessus.

- **N'importe quel utilisateur connecté pouvait écrire les cycles et cotisations de toutes les
  tontines.** `/cycle` et `/cotisation` n'avaient aucune règle dans `SecurityConfig` et aucun
  contrôle de propriété : un `MEMBRE`, ou le gestionnaire d'une autre tontine, pouvait créer,
  modifier ou supprimer des cycles et cotisations chez n'importe qui, et même déplacer une
  cotisation vers une autre tontine (`setCycle` recopié depuis le JSON). Même faille qu'en phase 3
  sur `/tontine`, restée ouverte sur les ressources encore « génériques ». Corrigé à deux niveaux
  (rôle dans `SecurityConfig`, propriété dans le service), puis les routes génériques d'écriture ont
  été supprimées.
- **Deux erreurs attrapées en revue avant tout commit, désormais couvertes par des tests.** Pendant
  la factorisation du contrôle de propriété, la vérification de `updateTontine` a été branchée sur
  la tontine **envoyée par le client** au lieu de celle lue en base : un gestionnaire aurait pu
  modifier la tontine d'un autre en écrivant son propre téléphone dans le JSON. Et le montant
  attendu d'un cycle était calculé avec `total.add(total)` au lieu de `total.add(montantDu)` —
  toujours 0, sans aucune erreur. Le second a été réintroduit volontairement pour vérifier que
  `CycleServiceTest` le détecte.

- **Un refresh token servait d'access token pendant 7 jours.** Le filtre JWT acceptait tout token
  correctement signé, sans lire son `type` : un refresh token (sans rôle, valable 7 jours) donnait
  accès à toutes les routes qui exigent seulement d'être connecté, ce qui annulait l'intérêt d'un
  access token court. `/auth/refresh` vérifiait le type, mais pas le sens inverse. Corrigé en
  marquant les access tokens `"type": "access"` et en n'authentifiant **que** ce type (liste
  blanche : un futur type de token sera refusé par défaut). Trouvé en relisant le filtre pendant le
  nettoyage des commentaires ; aucun test ne le couvrait, car les tests MockMvc ne passent pas par
  le filtre — d'où `JwtAuthFilterTest`, avec de vrais tokens.

- **Élévation de privilèges : un `MEMBRE` pouvait se rendre `ADMIN`.** `PUT /utilisateur/{id}`
  recevait l'entité `Utilisateur` et recopiait tous ses champs, dont `role` et `actif`, sur le compte
  en base. Comme la règle « propre profil » autorise chacun à modifier son compte, un membre pouvait
  envoyer `{"role": "ADMIN"}` sur son propre profil et contourner la protection de création
  d'administrateurs. Trouvé lors d'une revue de code « clean code », corrigé par un DTO d'entrée
  limité à `nom`, `prenom`, `email` (voir les décisions ci-dessus), couvert par un test.
- **500 au lieu de 409 en changeant d'email pour celui d'un autre compte.** La modification de
  profil ne vérifiait pas que le nouvel email était libre ; la contrainte d'unicité de la base
  rejetait l'enregistrement avec une erreur non contrôlée. Corrigé par la même vérification qu'à
  l'inscription, en ignorant le cas où l'utilisateur renvoie son propre email.

- **500 au lieu de 404 sur un membre ou une tontine inexistants.** `createParticipation` faisait
  confiance à l'id envoyé par le client sans vérifier qu'il existait ; la base rejetait la clé
  étrangère avec une erreur non contrôlée. Corrigé en rechargeant systématiquement la ressource
  avant de continuer.
- **`existsByEmail(null)` bloquait toute inscription téléphone-only après la première.** Une fois
  l'email devenu optionnel, la vérification de doublon d'email s'exécutait même sans email fourni ;
  un seul compte sans email pouvait exister. Corrigé en ne l'exécutant que si l'email est renseigné.
- **`/auth/refresh` renvoyait 401 avant même d'atteindre le service.** La route avait été oubliée
  dans la liste `permitAll()` de `SecurityConfig` ; `anyRequest().authenticated()` la bloquait pour
  tout appelant non connecté, cas d'usage pourtant normal d'un refresh token expiré.
- **Erreur de rôle Spring Security : 401 au lieu de 403 sur un accès insuffisant.** Le chemin
  `AccessDeniedException → sendError(403)` déclenche un forward interne vers `/error` que le filtre
  JWT ignore par défaut ; la requête atterrissait anonyme sur `anyRequest().authenticated()`.
  Corrigé avec un `AccessDeniedHandler` dédié.
- **Un `PUT` qui validait une valeur sans l'appliquer.** `updateParticipation` vérifiait le nombre
  de parts envoyé, mais oubliait de l'écrire sur l'entité avant `save()` : la requête répondait 200
  sans rien changer, sans qu'aucune erreur ne le révèle. Trouvé par un test qui asserte la valeur
  après coup, pas seulement l'absence d'exception.
- **Une transition de statut mal câblée pouvait ressusciter une tontine terminée.** Une faute de
  frappe dans les statuts autorisés d'`activerTontine` (`TERMINEE` au lieu de `SUSPENDUE`) rendait
  possible de réactiver une tontine `TERMINEE`, censée être un état final. Le test correspondant
  est volontairement passé au rouge pour confirmer qu'il détectait bien le problème, avant d'être
  corrigé.
