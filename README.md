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
  -d '{"nom":"Tontine des amis","montantPart":10000,"frequence":"MENSUELLE","nbCycles":10,"jourCotisation":5}'
```

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
| `POST` | `/tontine/{id}/activer` | propriétaire | `EN_ATTENTE` ou `SUSPENDUE` → `ACTIVE` |
| `POST` | `/tontine/{id}/suspendre` | propriétaire | `ACTIVE` → `SUSPENDUE` |
| `POST` | `/tontine/{id}/cloturer` | propriétaire | `ACTIVE` → `TERMINEE` (état final) |

Cycle de vie du statut :

```
EN_ATTENTE ──activer──▶ ACTIVE ──suspendre──▶ SUSPENDUE
                          │  ◀──activer (reprise)──┘
                          └──cloturer──▶ TERMINEE   (état final)
```

Le statut ne se modifie que par ces actions : un `statut` envoyé dans un `POST` ou un `PUT` est ignoré.

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

### Autres ressources (CRUD générique, sans règles métier pour l'instant)

`/cycle`, `/cotisation`, `/tirage`, `/pret`, `/echeancePret`, `/transaction`, `/invitation`,
`/importMembre`, `/notification`.

Elles demandent simplement d'être connecté : **elles seront durcies au fil des phases.**

### Codes d'erreur

Les erreurs métier renvoient un message lisible et un code HTTP cohérent :

| Code | Signification | Exemple |
|---|---|---|
| `400` | requête incomplète ou valeur invalide | nombre de parts à 0, membre sans id |
| `401` | non authentifié / token invalide ou expiré | token absent |
| `403` | authentifié mais pas autorisé | un `MEMBRE` qui active une tontine |
| `404` | ressource introuvable | tontine inexistante |
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
├── security/      JwtUtil, JwtAuthFilter, gestionnaires d'erreur 401 / 403
├── controller/    un contrôleur REST par ressource
├── service/       logique métier, regroupée par domaine
│   ├── auth/  utilisateur/  tontine/  cotisation/  pret/  onboarding/  notification/
├── repository/    interfaces Spring Data JPA
├── entity/        entités JPA (12 tables)
├── dto/           objets d'échange avec le client (jamais l'entité brute : pas de fuite de mot de passe)
├── enums/         statuts et rôles
├── exception/     exceptions métier (héritent de SamanatteuException, avec leur code HTTP)
└── handler/       GlobalExceptionHandler : transforme les exceptions en réponses JSON
```

Choix notables :

- **Les entités ne sortent jamais de l'API** : chaque réponse passe par un DTO.
- **Une exception métier = un code HTTP**, portée par la classe elle-même ; le
  `GlobalExceptionHandler` n'a pas à être modifié pour en ajouter une.
- **Le schéma est validé, pas généré** (`ddl-auto: validate`) : la base fait foi.

## Sécurité

- **JWT** : access token de 15 minutes + refresh token de 7 jours (un claim `type` empêche de
  confondre les deux). L'identité du token est le **numéro de téléphone**, car l'email est optionnel.
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
./mvnw test -Dtest='*ServiceTest,*ControllerSecurityTest'
```

- `TontineServiceTest`, `ParticipationServiceTest`, `UtilisateurServiceTest` : règles métier des
  services avec des faux repositories (Mockito) — propriété, cycle de vie du statut, doublons,
  valeurs décidées par le serveur, identité issue du token, lecture filtrée par rôle, champs
  modifiables d'un profil.
- `TontineControllerSecurityTest`, `ParticipationControllerSecurityTest` : règles d'accès HTTP de
  `SecurityConfig` (401 / 403 / 200 / 204 / 404) avec MockMvc, sans serveur ni base.

> `SamanatteuApplicationTests` (chargement complet du contexte) nécessite PostgreSQL et les variables
> d'environnement ; il n'est donc pas inclus dans la commande ci-dessus.

## Avancement

Le développement suit un planning en 8 phases.

| Phase | Contenu | État |
|---|---|---|
| 1 | Bases : projet, entités, base PostgreSQL | ✅ terminée |
| 2 | Authentification et rôles | ✅ terminée, sauf « mot de passe oublié » (nécessite l'envoi de SMS, phase 6) |
| 3 | Tontines, membres, participations | ✅ tontines et cycle de vie, participations (doublons, parts, propriété, lecture filtrée par rôle) ; reste à trancher : comment devient-on `GESTIONNAIRE` |
| 4 | Cycles et cotisations (calcul du montant dû, retards, reçu PDF) | ⏳ à venir |
| 5 | Tirage aléatoire pondéré, prêts et échéanciers | ⏳ à venir |
| 6 | Notifications e-mail et SMS, invitations, import Excel/CSV | ⏳ à venir |
| 7-8 | Tableaux de bord par rôle, finitions | ⏳ à venir |

### Limites connues

- Les ressources autres que `/tontine` et `/participation` n'ont pas encore de règles métier ni de contrôle de propriété.
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
- **« Pas d'email » s'écrit toujours `null`, jamais `""`.** L'email est optionnel et unique en base :
  si des chaînes vides étaient stockées, deux comptes sans email entreraient en conflit. L'entrée est
  normalisée (vide ou blanc → `null`) avant toute vérification.

### Bugs trouvés et corrigés

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
