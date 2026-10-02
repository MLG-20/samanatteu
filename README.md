# SamaNatteu

API REST de gestion de **tontines** (épargne rotative entre membres d'un groupe), construite avec
Spring Boot. Un gestionnaire crée une tontine, y inscrit des membres, et l'application suit les
cycles, les cotisations, les tirages et les prêts.

> **État du projet : en développement actif.** Authentification, tontines, participations,
> cycles et cotisations, tirages, caisse de prêts, prêts, journal financier, invitations et
> notifications (envoi simulé) sont fonctionnels et testés ; import et tableaux de bord restent à faire. Voir
> [Avancement](#avancement).

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
- **PostgreSQL**, schéma versionné avec **Flyway** (12.4)
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
```

Les tables sont créées au premier démarrage par Flyway, à partir des migrations de
`src/main/resources/db/migration/` (`V1__schema_initial.sql`, puis `V2__…`). Hibernate ne fait
ensuite que vérifier que le schéma correspond aux entités (`ddl-auto: validate`).

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
  -d '{"nom":"Tontine des amis","montantPart":10000,"montantCaissePret":500,"frequence":"MOIS","intervalle":1,"jourCotisation":5}'
```

`frequence` est une unité (`JOUR`, `SEMAINE`, `MOIS`) et `intervalle` un nombre d'unités :
`"frequence":"MOIS","intervalle":2` = un cycle tous les 2 mois. `montantCaissePret` est la somme
fixe que chaque membre verse à la caisse de prêts à chaque cycle (`0` = pas de caisse).

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
| `POST` | `/tontine/{id}/prets` | propriétaire | accorde un prêt sur la caisse de prêts (voir *Caisse de prêts et prêts*) |

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
**cotisation** de `nombreParts × montantPart`, plus le montant fixe de la caisse de prêts
(`montantCaissePret`, le même pour tous, quel que soit le nombre de parts).

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
- **Paiement** : montant > 0 et mode obligatoires (400) ; pas plus que le reste dû, part et caisse
  comprises (400) ; une cotisation `COMPLET` est refusée (409). Un seul paiement est **réparti par
  le serveur, la part d'abord** : le surplus va à la caisse de prêts. Statut obtenu : `COMPLET`
  quand la part **et** la caisse sont payées, sinon `PARTIEL` — sauf une cotisation `EN_RETARD`,
  qui le reste jusqu'au solde. Le montant collecté du cycle (la cagnotte du tirage) n'augmente que
  de la part ; la caisse va dans `soldeCaissePret` de la tontine.
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
- **Compensation** : si le gagnant est lui-même `EN_RETARD` sur ce cycle, sa dette **de part** est
  payée par son gain (cagnotte complétée). La caisse de prêts n'est jamais compensée : s'il la doit
  encore, la cotisation reste `EN_RETARD` jusqu'à ce qu'il la paie.
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

### Caisse de prêts et prêts (implémenté, testé)

Chaque tontine peut avoir une **caisse de prêts**, séparée de la cagnotte des tirages : elle est
alimentée par le montant fixe versé à chaque cycle (`montantCaissePret`), puis par les
remboursements, intérêts compris. La gestionnaire y prête de l'argent aux membres.

| Méthode | Route | Accès | Description |
|---|---|---|---|
| `POST` | `/tontine/{id}/prets` | propriétaire | accorde un prêt (`{"membreId":…,"montant":…,"nbEcheances":…,"dateDebutRemboursement":"AAAA-MM-JJ","tauxInteret":…}` ou `"montantInteret":…` à la place du taux) |
| `POST` | `/pret/{id}/remboursement` | propriétaire | enregistre un remboursement (`{"montant":…}`) |
| `GET` | `/pret`, `/echeancePret` | `GESTIONNAIRE`, `MEMBRE` | un gestionnaire voit ceux de **ses** tontines ; un membre **uniquement les siens** |

Règles :

- **Accorder** : tontine `ACTIVE` (409) ; le membre doit participer à cette tontine avec le statut
  `ACTIF` (400) ; un seul prêt en cours (`ACTIF` ou `EN_RETARD`) par membre et par tontine (409) ;
  pas plus que le solde de la caisse (409). Le serveur débite la caisse et génère l'échéancier.
- **Intérêt** : fixé par la gestionnaire, au choix **en pourcentage** (`tauxInteret`) ou **en
  francs** (`montantInteret`), pas les deux (400), ou aucun (prêt sans intérêt). Un taux est
  converti une seule fois : `montant × taux / 100`, arrondi au franc.
- **Échéancier** : `(montant + intérêt) / nbEcheances`, arrondi au franc inférieur, la dernière
  échéance absorbant le reste pour que le total soit exact (110 000 en 3 : 36 666, 36 666,
  36 668). La 1re échéance tombe à `dateDebutRemboursement`, les suivantes au rythme de la tontine
  (`frequence` × `intervalle`), toujours calculées depuis la date de début (pas de dérive en fin
  de mois).
- **Rembourser** : pas plus que le reste dû (400) ; un prêt `REMBOURSE` est refusé (409). Le
  montant est réparti sur les échéances **les plus anciennes d'abord** (on peut payer plusieurs
  échéances, ou en avance) et retourne entièrement dans la caisse. Tout payé : `REMBOURSE`.
- **Retards** : une tâche planifiée (`@Scheduled`, chaque nuit à minuit) passe `EN_RETARD` les
  échéances dépassées non payées, et leur prêt avec. Quand le membre a rattrapé toutes ses
  échéances en retard, le prêt redevient `ACTIF`.
- Aucune route ne permet de créer, modifier ou supprimer un prêt ou une échéance à la main.

```
prêt : ACTIF ──échéance dépassée (minuit)──▶ EN_RETARD
         │  ◀──retards rattrapés──────────────────┘
         └──tout remboursé──▶ REMBOURSE   (état final, aussi depuis EN_RETARD)
```

### Journal financier (implémenté, testé)

Chaque mouvement d'argent laisse **une ligne** dans la table `transaction`, comme un relevé
bancaire. Une ligne n'est **jamais modifiée ni supprimée**, et aucune route ne permet d'en écrire
une : elle est la conséquence d'une action métier, écrite par le serveur dans la même transaction
que cette action.

| Action | Ligne écrite | Sens (vu de la tontine) |
|---|---|---|
| paiement d'une cotisation | `COTISATION`, montant **total** versé (part + caisse) | `ENTRANT` |
| tirage d'un gagnant en retard | `COTISATION` de sa dette réglée par son gain, sans mode de paiement | `ENTRANT` |
| tirage : remise immédiate au gagnant | `GAIN` (si la cagnotte n'est pas vide) | `SORTANT` |
| versement du reste au gagnant | `GAIN` du montant remis maintenant | `SORTANT` |
| prêt accordé | `PRET`, le **capital** seul | `SORTANT` |
| remboursement | `REMBOURSEMENT`, tout ce qui entre (capital + part d'intérêt) | `ENTRANT` |

| Méthode | Route | Accès | Description |
|---|---|---|---|
| `GET` | `/transaction` | `GESTIONNAIRE`, `MEMBRE` | un gestionnaire voit le journal de **ses** tontines ; un membre **uniquement ses propres lignes** ; du plus récent au plus ancien |

Chaque ligne porte : membre, tontine, type, sens, montant (toujours positif, c'est le sens qui
donne la direction), mode de paiement et référence externe (Wave, Orange Money…) quand ils sont
connus, `referenceId` (id de la cotisation, du tirage ou du prêt concerné), description, date.

### Invitations (implémenté, testé)

Deux façons de faire entrer des membres dans une tontine **encore `EN_ATTENTE`**, sans que la
gestionnaire saisisse chaque compte :

| | Lien de **groupe** | Invitation **individuelle** |
|---|---|---|
| Pour qui | tout le groupe WhatsApp de la tontine | une personne qui n'est pas dans le groupe |
| Utilisations | plusieurs personnes | une seule fois |
| Pré-remplissage | aucun | prénom, nom, téléphone, nombre de parts |
| Parts à l'arrivée | 1 (la gestionnaire ajuste ensuite) | celles fixées dans l'invitation |
| Validité | 7 jours ; un seul lien actif par tontine | 7 jours ; réservée au téléphone invité |

| Méthode | Route | Accès | Description |
|---|---|---|---|
| `POST` | `/tontine/{id}/lien-groupe` | `GESTIONNAIRE` propriétaire | génère le lien de groupe ; l'ancien passe `ANNULEE` |
| `POST` | `/tontine/{id}/invitations` | `GESTIONNAIRE` propriétaire | crée une invitation individuelle (téléphone obligatoire) |
| `GET` | `/invitation` | `GESTIONNAIRE` | ses invitations, des plus récentes aux plus anciennes |
| `GET` | `/invitation/{token}` | **public** | nom de la tontine et pré-remplissage, ou la raison du refus |
| `POST` | `/invitation/{token}/rejoindre` | `MEMBRE` | crée la participation (`204`) |

Parcours d'un membre : il ouvre le lien → l'écran appelle `GET /invitation/{token}` et affiche
tout de suite « ce lien a expiré » ou le formulaire → il crée son compte (ou se connecte s'il en a
déjà un) → l'écran appelle `/rejoindre`. Pour lui, c'est **un seul écran et un seul bouton**.

Refus possibles, avec un message pensé pour des utilisateurs peu habitués aux applications :
lien inconnu (`404`), remplacé (`410`), expiré (`410`), déjà utilisé (`409`), tontine déjà
démarrée (`409`), invitation individuelle utilisée par un autre téléphone (`403`), déjà membre
(`409`).

```
EN_ATTENTE ──rejoindre (individuelle)──▶ ACCEPTE
     │
     └──nouveau lien de groupe──▶ ANNULEE
(expiration : jugée sur expireAt, quel que soit le statut)
```

### Notifications (implémenté, testé — envoi simulé)

Les membres sont prévenus **par SMS toujours, et par email en plus** s'ils en ont un
(CDC v1.1 §4.10). Aucune route ne permet d'envoyer une notification : comme le journal
financier, elles sont la **conséquence** d'une action métier.

| Événement | Déclenché par | Exemple de SMS |
|---|---|---|
| Bienvenue | inscription par la gestionnaire, ou arrivée par un lien d'invitation | `Natt des femmes : bienvenue Awa Diop ! Vous avez 2 part(s) de 10000 F.` |
| Paiement confirmé | `POST /cotisation/{id}/paiement` | `Natt des femmes : Awa Diop, paiement de 15000 F reçu (cycle 1). Reste à payer : 5000 F.` |
| Résultat du tirage | `POST /cycle/{id}/tirage` (au gagnant) | `Natt des femmes : félicitations Awa Diop ! Vous avez gagné le tirage du cycle 1 : 30000 F. Déjà remis : 30000 F.` |
| Rappel de cotisation | tâche planifiée, chaque jour à 9h | 3 jours avant la fin prévue du cycle, pour les cotisations pas encore soldées |
| Rappel d'échéance | tâche planifiée, chaque jour à 9h | 3 jours avant une échéance de prêt non payée |

| Méthode | Route | Accès | Description |
|---|---|---|---|
| `GET` | `/notification` | `GESTIONNAIRE`, `MEMBRE` | **ses propres** notifications, des plus récentes aux plus anciennes |

Chaque envoi laisse une ligne en base (canal `SMS` ou `EMAIL`, statut `ENVOYE` ou `ECHEC`) :
preuve en cas de litige (« je n'ai jamais reçu le rappel »), et envois en échec relançables plus
tard.

**Envoi simulé.** Il n'y a pas encore de compte chez un fournisseur de SMS : les messages sont
écrits dans la console (`SMS (simulé) à 771000001 : …`). Brancher Orange SMS API ou Twilio
consistera à écrire une classe de plus, sans toucher aux services (voir
[Architecture](#architecture)).

### Autres ressources (CRUD générique, sans règles métier pour l'instant)

`/importMembre`.

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
| `410` | la ressource a existé mais n'est plus utilisable | lien d'invitation expiré ou remplacé |

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
├── repository/    interfaces Spring Data JPA, regroupées par domaine
│   ├── utilisateur/  tontine/  cotisation/  pret/  onboarding/  notification/
├── entity/        entités JPA (12 tables), regroupées par domaine
│   ├── utilisateur/  tontine/  cotisation/  pret/  onboarding/  notification/
├── dto/           objets d'échange avec le client (jamais l'entité brute : pas de fuite de mot de passe)
│   ├── auth/  utilisateur/  tontine/  cotisation/  pret/  onboarding/  notification/
├── enums/         statuts et rôles, regroupés par domaine (ModePaiement, commun, à la racine)
│   ├── utilisateur/  tontine/  cotisation/  pret/  onboarding/  notification/
├── exception/     exceptions métier (héritent de SamanatteuException, avec leur code HTTP),
│   ├── auth/  utilisateur/  tontine/  cotisation/  pret/  onboarding/  notification/
└── handler/       GlobalExceptionHandler : transforme les exceptions en réponses JSON
```

Choix notables :

- **Un même découpage par domaine partout** : `service/`, `dto/`, `entity/`, `repository/`, `enums/`
  et `exception/` ont les mêmes sous-dossiers (`tontine/` regroupe tontines, participations et
  cycles ; `cotisation/` les cotisations et tirages ; `pret/` les prêts, échéances et
  transactions). Tout ce qui concerne un domaine se trouve au même endroit dans chaque couche.
- **Les entités ne sortent jamais de l'API** : chaque réponse passe par un DTO.
- **Une exception métier = un code HTTP**, portée par la classe elle-même ; le
  `GlobalExceptionHandler` n'a pas à être modifié pour en ajouter une.
- **Le schéma est validé, pas généré** (`ddl-auto: validate`) : la base fait foi.
- **Les services ne dépendent pas de Spring Security** : ils demandent « qui est connecté ? » et
  « a-t-il tel rôle ? » à `UtilisateurConnecte` (`telephone()`, `aLeRole(RoleUtilisateur)`), seul
  endroit, avec `JwtAuthFilter`, à toucher au `SecurityContextHolder`.
- **Les services dépendent d'interfaces, pas de fournisseurs.** `NotificationService` reçoit un
  `EnvoyeurSms` et un `EnvoyeurEmail` (interfaces). Aujourd'hui, Spring injecte
  `EnvoyeurSmsConsole` / `EnvoyeurEmailConsole`, qui écrivent dans la console ; demain, une classe
  `EnvoyeurSmsOrange implements EnvoyeurSms` appellera la vraie API, et aucun service ne changera.
  Les tests en profitent : ils remplacent les envoyeurs par des faux pour simuler une panne.
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

### Des liens d'invitation impossibles à deviner

Un lien d'invitation, c'est une **clé** : quiconque connaît son token peut rejoindre la tontine.
Avant ce chantier, c'était le client qui choisissait le token (`POST /invitation` recevait
l'entité brute) ; la base contenait des tokens comme `abc123`, et deux invitations partageaient le
même (`xyz789`). Un programme qui essaie toutes les combinaisons de 6 lettres et chiffres en fait
le tour en quelques minutes.

Le token est désormais fabriqué par le serveur, et seulement par lui :

```java
private final SecureRandom hasard = new SecureRandom();

private String genererToken() {
    byte[] octets = new byte[32];                 // 256 bits de hasard
    hasard.nextBytes(octets);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(octets);
}
```

- **`SecureRandom`, pas `Random`.** `Random` est un générateur déterministe : quelques valeurs
  observées suffisent à prédire les suivantes. `SecureRandom` puise dans l'entropie du système
  et n'est pas prévisible ; c'est le même choix que pour le tirage au sort.
- **256 bits.** 2²⁵⁶ tokens possibles, soit plus que le nombre d'atomes estimé de l'univers
  observable : les essayer un à un n'a aucune chance d'aboutir, même à des milliards d'essais par
  seconde.
- **Base64 « URL ».** Les octets bruts ne s'écrivent pas dans une adresse ; la variante URL de
  Base64 n'utilise que `A-Z a-z 0-9 - _` (ni `+` ni `/`, qui ont un sens dans une URL), et sans le
  remplissage `=`. Résultat : 43 caractères, collables tels quels dans WhatsApp.
- **La base garantit l'unicité.** La migration `V3` impose `token NOT NULL UNIQUE`. La contrainte
  sert deux fois : elle rejette tout doublon, même venu d'un bug, et l'index qu'elle crée rend
  instantanée la recherche par token à chaque clic sur un lien.
- **Une clé n'est jamais montrée à qui ne la possède pas.** La liste des invitations est réservée à
  la gestionnaire et filtrée sur ses tontines ; la consultation publique d'un lien renvoie un DTO
  minimal (`LienInvitationDTO` : ni id, ni token, ni statut interne) ; les messages d'erreur ne
  répètent pas le token.
- **Une clé peut être changée.** Si le lien de groupe circule hors du groupe, la gestionnaire en
  génère un nouveau : l'ancien est immédiatement refusé. Une invitation individuelle ne sert qu'au
  téléphone invité, une seule fois. Et tout lien expire au bout de 7 jours, ou dès que la tontine
  démarre.

## Tests

```bash
# Tests unitaires et de sécurité (sans base de données)
./mvnw test -Dtest='*ServiceTest,*ControllerSecurityTest,JwtAuthFilterTest'
```

- `TontineServiceTest`, `ParticipationServiceTest`, `UtilisateurServiceTest`, `CycleServiceTest`,
  `CotisationServiceTest`, `TirageServiceTest`, `PretServiceTest`, `EcheancePretServiceTest`,
  `TransactionServiceTest`, `InvitationServiceTest`, `NotificationServiceTest` :
  règles métier des services avec des faux repositories (Mockito) — propriété, cycle de vie du
  statut, doublons, valeurs décidées par le serveur, identité issue du token, lecture filtrée par
  rôle, champs modifiables d'un profil, calcul des montants dus et attendus, paiements partiels et
  répartition part / caisse, retards à la clôture, composition de l'urne, compensation, versements
  et reports, intérêts, échéanciers (arrondis, dates de fin de mois), remboursements et retards de
  prêts, lignes du journal financier (sens déduit du type, montant, id de référence, aucune ligne
  si l'action est refusée ou si le montant vaut 0), invitations (token de 43 caractères, ancien lien
  annulé et non supprimé, expiration jugée sur la date, lien transféré refusé, usage unique),
  notifications (déclenchées par chaque action et jamais si elle est refusée ; SMS seul ou SMS +
  email, un envoi raté n'interrompt rien ; rappels J-3, reste dû et date, montants sans centimes). Le hasard du tirage est remplacé par un faux `Random` qui
  choisit une case connue et retient la taille de l'urne.
- `TontineControllerSecurityTest`, `ParticipationControllerSecurityTest`,
  `CycleControllerSecurityTest`, `CotisationControllerSecurityTest`,
  `TirageControllerSecurityTest`, `PretControllerSecurityTest`,
  `EcheancePretControllerSecurityTest`, `TransactionControllerSecurityTest`,
  `InvitationControllerSecurityTest`, `NotificationControllerSecurityTest` : règles d'accès HTTP de `SecurityConfig` (401 / 403 / 200 / 204 /
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
# Flyway crée les tables au premier démarrage sur la base vide
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8081 --spring.profiles.active=test"
```

## Avancement

Le développement suit un planning en 8 phases.

| Phase | Contenu | État |
|---|---|---|
| 1 | Bases : projet, entités, base PostgreSQL | ✅ terminée |
| 2 | Authentification et rôles | ✅ terminée, sauf « mot de passe oublié » (nécessite l'envoi de SMS, phase 6) |
| 3 | Tontines, membres, participations | ✅ tontines et cycle de vie, participations (doublons, parts, propriété, lecture filtrée par rôle) ; reste à trancher : comment devient-on `GESTIONNAIRE` |
| 4 | Cycles et cotisations (calcul du montant dû, retards, reçu PDF) | ✅ ouverture, paiements, clôture avec retards, lecture filtrée, historique des paiements (journal financier) ; reste le reçu PDF |
| 5 | Tirage au sort, prêts et échéanciers | ✅ tirage (urne, compensation, versements, reports, `nbCycles` calculé), caisse de prêts, prêts, échéanciers, remboursements et retards |
| 6 | Notifications e-mail et SMS, invitations, import Excel/CSV | 🚧 invitations (lien de groupe, invitation individuelle), notifications (SMS + email, rappels J-3, envoi simulé) ; reste l'import, le branchement d'un vrai fournisseur et « mot de passe oublié » |
| 7-8 | Tableaux de bord par rôle, finitions | ⏳ à venir |

### Limites connues

- `/importMembre` n'a pas encore de règles métier ni de contrôle de propriété.
- Les SMS et emails sont **simulés** (écrits dans la console) : aucun fournisseur (Orange SMS API,
  Twilio, serveur SMTP) n'est encore branché. Les emails sont en texte brut (pas de modèle HTML
  Thymeleaf).
- Les invitations ne sont pas **envoyées** par l'application : la gestionnaire copie le lien dans
  WhatsApp. Un SMS d'invitation individuelle demanderait de tracer un envoi vers un numéro qui n'a
  pas encore de compte (`notification.destinataire` est obligatoire) : écarté pour l'instant.
- Les notifications en `ECHEC` ne sont pas encore relancées automatiquement.
- Les rappels J-3 supposent que l'application tourne à 9h : un jour où elle est arrêtée à cette
  heure, les rappels de ce jour-là ne partent pas (pas de rattrapage).
- Une invitation individuelle ne peut pas être annulée avant ses 7 jours ; le statut `EXPIRE` de
  l'enum n'est jamais écrit (l'expiration est jugée sur la date).
- `rejoindre` duplique la création d'une participation de `ParticipationService` (deux usages :
  factorisation repoussée au troisième).
- Le journal ne connaît pas le **mode de paiement** des gains, des prêts et des remboursements :
  `tirerAuSort` ne reçoit pas de corps et `VersementDTO` / `DemandePretDTO` n'ont que le montant.
  Seuls les paiements de cotisation ont leur mode et leur référence.
- Les **pénalités** (type `PENALITE` prévu dans le journal) n'ont pas encore d'action métier : à
  concevoir (certaines tontines en appliquent, d'autres non).
- Ce que devient la caisse de prêts à la fin d'une tontine (partage entre membres ?) n'est pas
  encore décidé ; un prêt ne peut pas être annulé (statut `ANNULE` du CDC non retenu pour l'instant).
- Les relations JPA sont chargées en `EAGER` (défaut de `@ManyToOne`) sans `JOIN FETCH` : les listes
  font des requêtes N+1. Négligeable à l'échelle d'une tontine, à optimiser dans une étape dédiée.
- Un corps JSON mal formé n'a pas encore de traitement dédié dans `GlobalExceptionHandler`
  (`HttpMessageNotReadableException` n'est pas une `ErrorResponse`) : à vérifier et ramener à 400.
- Le reçu PDF par paiement reste à faire ; il pourra s'appuyer sur la ligne du journal de chaque
  paiement.
- Un membre sorti d'une tontine déjà lancée n'a pas encore d'action dédiée (statut `SORTI`) : retirer un participant n'est possible que tant que la tontine est `EN_ATTENTE`.
- Le refresh token n'est pas révoqué après usage (pas de stockage côté serveur).
- Les champs `createdAt` / `updatedAt` ne sont pas encore alimentés.
- Comment devient-on `GESTIONNAIRE` : décidé pour un modèle par **abonnement** (voir le journal
  ci-dessous), pas encore implémenté — aujourd'hui l'inscription permet encore de choisir
  `GESTIONNAIRE` ou `MEMBRE` librement.
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

- **La caisse de prêts est séparée de la cagnotte des tirages.** Le CDC ne dit pas d'où vient
  l'argent prêté. Pratique retenue : une petite somme fixe par membre et par cycle (même montant
  pour tous, sans lien avec les parts), versée dans une caisse propre à la tontine. Jamais mélangée
  à `montantCollecte`, sinon le gagnant du tirage repartirait avec l'argent des prêts.
- **Un seul paiement, la part d'abord.** En pratique, le membre donne tout ensemble. Le serveur
  répartit : la part se remplit d'abord, le surplus va à la caisse. Payer la part le matin et la
  caisse le soir fonctionne sans option, puisqu'une part remplie laisse tout le reste à la caisse.
  Une cotisation n'est `COMPLET` qu'avec les deux, sinon les 500 F de caisse ne pourraient plus être
  versés (paiement sur `COMPLET` refusé).
- **L'application ne fixe pas l'intérêt.** C'est un accord entre la gestionnaire et le membre.
  Deux saisies sont proposées, pour être utilisable par tous : en pourcentage ou directement en
  francs (écart au CDC, qui ne prévoyait qu'un taux). Un taux est converti une fois en intérêt
  simple, arrondi au franc : aucune méthode bancaire cachée dans le calcul.
- **Le franc CFA n'a pas de centimes.** Les échéances sont arrondies au franc inférieur et la
  dernière absorbe le reste : le membre rembourse exactement le total, ni 1 F de plus ni de moins.
- **Les échéances suivent le rythme de la tontine.** Le membre rembourse quand il cotise, au même
  rendez-vous. La gestionnaire ne choisit que la date de la première échéance ; une date passée est
  permise pour saisir un prêt déjà en cours avant d'adopter l'application.
- **Un seul prêt en cours par membre, dans chaque tontine.** Un membre en retard qui réemprunte,
  c'est ce qui fait perdre l'argent du groupe. Vérifié par tontine et non globalement : chaque
  tontine a sa caisse, et regarder les prêts d'une autre ferait fuiter des informations entre
  gestionnaires.
- **Un membre ne voit que ses propres prêts.** Contrairement aux tirages, publics dans le groupe
  (transparence), une dette est une information privée.
- **Un remboursement porte sur le prêt, pas sur une échéance.** Le membre apporte « ce qu'il a » ;
  le serveur remplit les échéances les plus anciennes d'abord. Rembourser en avance ou plusieurs
  échéances d'un coup fonctionne naturellement.
- **Les retards sont détectés chaque nuit par une tâche planifiée.** Une action manuelle « vérifier
  les retards » dépendait de la mémoire de la gestionnaire. `@Scheduled` lance la vérification à
  minuit, sans utilisateur connecté (donc sans contrôle de propriété, et jamais exposée par une
  route). Le même mécanisme servira aux rappels J-3 de la phase notifications.
- **Le schéma de la base est versionné avec Flyway.** Jusqu'à la phase 5, chaque nouvelle colonne
  était ajoutée à la main (`ALTER TABLE`) dans les deux bases, puis recopiée dans un
  `db/schema.sql` : rien ne garantissait que les bases et le fichier restaient identiques. Chaque
  changement est désormais une migration numérotée (`V1`, `V2`…), commitée avec le code qui en a
  besoin et appliquée automatiquement au démarrage ; une migration déjà appliquée n'est jamais
  modifiée (Flyway vérifie sa somme de contrôle), on en écrit une nouvelle. `V1` reprend le schéma
  existant ; les bases déjà remplies ont été marquées « version 1 » sans la rejouer
  (`baseline-on-migrate`), et `V1` a été vérifiée à part sur un schéma vide. L'en-tête produit par
  `pg_dump` a été retiré : des commandes propres à `psql` (`\restrict`), et surtout un
  `set_config('search_path', '', false)` qui aurait vidé le chemin de recherche des tables pour
  toute la connexion, alors que le pool réutilise ses connexions pour Hibernate.
- **Un journal financier complet, une ligne par mouvement d'argent.** Une cotisation, un tirage ou
  une échéance ne gardaient que les détails de leur **dernier** paiement : un membre qui payait en
  deux fois (CASH puis Wave) perdait la trace du premier. La table `transaction` reçoit désormais
  une ligne pour chaque mouvement (cotisation, gain, prêt, remboursement), jamais modifiée ni
  supprimée. C'est la base de l'historique d'un membre (US-M01) et du futur reçu PDF.
- **Le journal est une conséquence, pas une ressource qu'on écrit.** Il n'y a aucun
  `POST /transaction` : chaque ligne est écrite par le serveur pendant l'action qui déplace
  l'argent (paiement, tirage, prêt…), dans la même `@Transactional`. Une route d'écriture
  permettrait d'inscrire « Awa a payé 10 000 » sans que sa cotisation ne change : le journal ne
  prouverait plus rien. Le CRUD générique existant a été supprimé. Le jour où les pénalités
  arriveront, ce sera une action métier (avec ses règles) qui écrira dans le journal.
- **Un seul point d'écriture, et le sens déduit du type.** `TransactionService.journaliser` est la
  seule méthode qui crée une ligne. L'appelant donne le type (`COTISATION`, `GAIN`…), jamais le sens :
  un `switch` sans `default` le déduit (`ENTRANT` / `SORTANT`, vu de la caisse de la tontine), et le
  compilateur refusera tout nouveau type tant qu'on ne lui aura pas donné un sens. Décidé après
  qu'une copie du bloc d'écriture ait oublié `type` et `sens`, ce qui aurait annulé tous les tirages
  avec compensation : six copies auraient fini par diverger.
- **Le montant est toujours positif ; c'est le sens qui donne la direction.** Imposé aussi par la
  base (`CHECK (montant > 0)`, migration `V2`), en plus de `NOT NULL` sur le type, le sens et le
  montant. Une ancienne ligne de test mélangeait les deux idées (`-5000` avec un sens `ENTREE`).
  `type` et `sens` sont des enums stockées par leur nom (`EnumType.STRING`), plus des chaînes libres.
- **Un paiement réel = une ligne**, même quand le serveur le partage entre la part et la caisse de
  prêts : le journal raconte ce que le membre a tendu (et ce que dira son reçu) ; le partage
  interne reste visible sur la cotisation.
- **Une dette réglée par un gain s'écrit en deux lignes.** Quand le gagnant d'un tirage était
  lui-même en retard, sa dette est effacée par son gain : on écrit la `COTISATION` réglée (sans
  mode de paiement, avec une description) **et** le `GAIN` complet, comme le ferait un comptable,
  plutôt que la seule différence remise en main. Ainsi, pour un cycle, la somme des lignes
  `COTISATION` égale `montantCollecte` et celle des `GAIN` égale `montantVerse`, et l'historique du
  membre montre que sa cotisation est payée.
- **Une ligne `PRET` porte le capital, pas le total dû.** Ce jour-là, seul le capital sort de la
  caisse ; l'intérêt n'existe pas encore et entrera avec les remboursements. Le journal et le solde
  de la caisse racontent toujours la même histoire.
- **Un membre ne voit que ses propres lignes du journal.** Cohérent avec les prêts (une dette est
  privée) ; voir les mouvements des autres ferait apparaître leurs prêts.
- **`ModePaiementCotisation` renommé `ModePaiement`** et rangé à la racine de `enums/` : un gain ou
  un prêt se règle aussi en CASH ou par Wave. Sans effet sur la base, qui stocke le nom des valeurs.

- **Deux sortes d'invitations, que le CDC confondait.** Le cahier des charges demandait à la fois
  un « lien unique par tontine, partageable sur WhatsApp » et un « token à usage unique » avec
  pré-remplissage du nom. Les deux sont incompatibles : un lien posté dans un groupe et consommé
  au premier clic bloquerait tous les autres membres. On garde donc les deux, séparés par un
  `TypeInvitation` : `GROUPE` (multi-usage, pour le groupe WhatsApp, très pratique) et
  `INDIVIDUELLE` (usage unique, pré-remplie, pour une personne hors du groupe). Un enum plutôt qu'un
  booléen : la valeur dit ce qu'elle est, et un troisième type reste possible.
- **Un seul lien de groupe actif par tontine.** Si un lien fuit hors du groupe, la gestionnaire en
  génère un nouveau et l'ancien cesse de fonctionner ; il n'y a jamais de doute sur « le bon lien ».
  L'ancien n'est pas supprimé mais passe `ANNULEE` : la personne qui clique dessus lit « ce lien a
  été remplacé » au lieu d'un « introuvable » incompréhensible, et l'historique est conservé.
  L'annulation et la création forment une seule `@Transactional`, pour ne jamais laisser la tontine
  sans lien valide.
- **Lien de groupe : 1 part par défaut.** Laisser chaque membre choisir ses parts aurait permis à
  n'importe qui de modifier l'équilibre de la tontine ; la gestionnaire ajuste ensuite pour ceux qui
  prennent 2 parts (gnari lokho). Une invitation individuelle, elle, fixe les parts dès le départ.
- **Rejoindre en deux appels côté API, en un écran côté utilisateur.** La crainte était le public :
  beaucoup de membres sont peu habitués aux applications. Mais ce que la personne voit dépend de
  l'écran, pas de l'API : un seul formulaire, un seul bouton, et l'écran enchaîne inscription puis
  `/rejoindre`. Côté serveur, on réutilise l'inscription existante et déjà testée, et un membre qui
  a déjà un compte (dans une autre tontine) se connecte simplement. Une route **publique** de
  consultation dit dès le clic si le lien est utilisable, avant que la personne ne remplisse quoi
  que ce soit.
- **Les messages d'erreur des invitations sont écrits pour les membres.** Ils sont affichés tels
  quels : « Ce lien a expiré. Demandez un nouveau lien à votre gestionnaire. » plutôt que
  « L'invitation Xk9mP2… a expiré. ».
- **Une invitation ne fait pas entrer dans une tontine déjà démarrée.** Même règle que pour les
  participations (le tirage serait faussé) : la génération et l'utilisation d'un lien sont
  refusées dès que la tontine n'est plus `EN_ATTENTE`, même si le lien n'a pas encore expiré.
- **L'expiration est jugée sur la date, pas sur un statut.** Rien ne passe les invitations en
  `EXPIRE` ; s'en remettre au statut aurait rendu un lien éternel. `expireAt` est calculé à partir
  de `createdAt` (+7 jours exactement).

- **SMS toujours, email en plus, derrière des interfaces.** Pas encore de compte chez un
  fournisseur de SMS : plutôt qu'attendre, les services parlent à `EnvoyeurSms` /
  `EnvoyeurEmail`, et une implémentation « console » tient lieu d'envoi. Le jour du vrai
  fournisseur, on ajoute une classe ; les services, eux, ne changent pas.
- **Un SMS raté n'annule jamais un paiement.** Les notifications partent pendant l'action métier,
  qui est souvent `@Transactional` : si l'erreur du fournisseur remontait, le paiement d'Awa serait
  annulé (rollback) pour un simple SMS. L'envoi est donc dans un `try/catch` : l'échec est noté
  (`ECHEC`), l'action continue. Un `try` par canal : un SMS raté n'empêche pas l'email.
- **Chaque envoi est tracé, une ligne par canal.** Le même événement envoyé par SMS et par email
  donne deux lignes, chacune avec son statut. `type` et `canal` sont des enums (migration `V4`),
  plus une chaîne libre ; la base impose `NOT NULL` sur destinataire, type, canal, message, statut
  et date. `titre` reste facultatif : seul l'email a un objet.
- **Les messages parlent au membre.** Ils commencent par le **nom de la tontine** (un membre peut
  en avoir plusieurs, et « Natt des femmes » lui parle plus que le nom de l'application), puis son
  prénom et son nom. Ils restent sous 160 caractères (au-delà, un SMS est facturé double). Les
  montants s'affichent sans centimes (`10000`, pas `10000.00` : il n'y a pas de centimes en FCFA)
  et les dates en `jj/mm/aaaa`.
- **Un message utilisé deux fois est écrit une fois.** La bienvenue part à l'inscription par la
  gestionnaire **et** à l'arrivée par un lien : le texte vit dans
  `NotificationService.notifierBienvenue`, pas dans les deux services.
- **Rappels J-3 : une fois, à une heure raisonnable.** Tâches `@Scheduled` à 9h (pas minuit : un
  SMS réveille). On cherche les échéances dont la date est **exactement** dans 3 jours (une
  égalité, pas « avant ») : chacun reçoit un seul rappel, pas un par jour. Le montant rappelé est
  le **reste** dû (une partie a pu être payée), part et caisse de prêts comprises pour une
  cotisation.
- **Chacun ne lit que ses notifications.** Elles contiennent des montants et des gains : même
  règle que le journal financier. L'ADMIN n'y a pas accès (frontière SaaS). Le `DELETE` du CRUD
  générique a été supprimé : une preuve d'envoi ne s'efface pas.

### Bugs trouvés et corrigés

- **Les notifications de toute la plateforme étaient lisibles par n'importe qui.**
  `GET /notification` renvoyait les messages de tous les membres (téléphones, montants payés,
  gains) à tout utilisateur connecté, et le CRUD générique permettait de fabriquer une
  notification « envoyée » ou d'en effacer une. Lecture filtrée sur le connecté, règle ajoutée,
  écritures supprimées.
- **Montants affichés « 10000.00 F » dans les SMS.** Les montants lus en base gardent leurs deux
  décimales ; `toPlainString()` les affichait telles quelles. Invisible dans les tests unitaires
  (montants construits sans décimales), repéré au test réel sur la base de test, corrigé avec
  `stripTrailingZeros()` et verrouillé par un test.
- **Deux erreurs attrapées en revue avant tout commit.** Une dépendance circulaire
  (`NotificationService` recevait `ParticipationService`, qui reçoit `NotificationService` :
  l'application n'aurait pas démarré) et une implémentation d'envoi sans `@Component` (Spring
  n'aurait trouvé aucun `EnvoyeurEmail`). Le service recevait aussi d'abord la classe console
  plutôt que l'interface, ce qui aurait annulé tout l'intérêt de l'interface.

- **Les invitations étaient des clés en libre-service.** `/invitation` n'avait aucune règle dans
  `SecurityConfig` et exposait un CRUD générique : n'importe quel utilisateur connecté pouvait
  fabriquer une invitation avec le token, le statut et la date d'expiration de son choix, la
  « réutiliser » en la repassant `EN_ATTENTE`, ou lister **toutes** les invitations de la plateforme,
  c'est-à-dire les clés d'entrée de toutes les tontines. La base contenait d'ailleurs deux
  invitations au même token. CRUD supprimé, token généré par le serveur (voir
  [Sécurité](#des-liens-dinvitation-impossibles-à-deviner)), unicité imposée par la base, liste
  filtrée par gestionnaire.

- **N'importe quel utilisateur connecté pouvait falsifier le journal financier.** `/transaction`
  n'avait aucune règle dans `SecurityConfig` et exposait un CRUD générique : un `MEMBRE` pouvait
  inventer une ligne, en modifier une ou effacer celles d'une tontine qui n'était même pas la
  sienne, et `GET /transaction` renvoyait les mouvements de toutes les tontines de la plateforme.
  Même faille que sur `/cycle`, `/tirage` et `/pret`. Routes d'écriture supprimées, lecture filtrée
  par rôle, règle ajoutée (l'ADMIN reçoit 403).
- **La contrainte de la base a révélé deux cas où un tirage aurait échoué.** Un gagnant peut être
  « en retard » uniquement sur la caisse de prêts, sa part étant payée : la ligne de compensation
  aurait valu 0. De même, un tirage dont la cagnotte est vide ne remet rien au gagnant. Avec
  `CHECK (montant > 0)`, ces lignes à 0 auraient fait annuler tout le tirage (la base refuse, la
  transaction entière recule) ; sans la contrainte, elles auraient été écrites en silence. Repéré à
  la conception, avant tout commit : ces lignes ne sont écrites que si le montant est positif, et
  deux tests le vérifient.
- **Trois erreurs attrapées en revue avant tout commit, désormais couvertes par des tests.** Une
  copie du bloc d'écriture oubliait `type` et `sens` (refusés par `NOT NULL` : tous les tirages
  avec compensation auraient échoué) ; le versement du reste au gagnant était journalisé comme une
  `COTISATION`, donc comme de l'argent **entrant** ; et la ligne d'un prêt pointait vers l'id de la
  **tontine** au lieu de celui du prêt, une erreur qu'aucun compilateur ne voit (deux `Long`). Les
  tests vérifient le type, le sens, le montant et l'id de référence de chaque ligne ; sept bugs ont
  été réintroduits volontairement pour vérifier qu'un test les détecte.

- **N'importe quel utilisateur connecté pouvait s'accorder un prêt ou effacer une dette.** `/pret`
  et `/echeancePret` n'avaient aucune règle dans `SecurityConfig` et exposaient un CRUD générique :
  un `MEMBRE` pouvait créer un prêt sans passer par la caisse, se marquer « payé » par un `PUT`
  sur une échéance, ou supprimer son prêt. Même faille que sur `/cycle` et `/tirage`. Routes
  d'écriture supprimées, remplacées par `accorder` et `rembourser`, et règles ajoutées.
- **Quatre erreurs attrapées en revue avant tout commit, désormais couvertes par des tests.** Le
  contrôle du solde comparait le prêt à `montantCaissePret` (les 500 F par cycle, la règle) au
  lieu de `soldeCaissePret` (l'argent disponible, l'état) ; le débit de la caisse réécrivait le
  solde à sa propre valeur (`subtract` oublié), ce qui aurait permis de prêter dix fois le même
  argent ; la boucle de l'échéancier s'arrêtait à `i < nb` (la dernière échéance n'était jamais
  créée, et aucune pour un prêt en une fois) ; et le solde de la caisse était recopié depuis le
  JSON du `PUT /tontine`. Chacune a été réintroduite volontairement pour vérifier qu'un test la
  détecte.

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
