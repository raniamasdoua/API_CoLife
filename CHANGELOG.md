# Changelog

## v0.4.0 - 2026-07-23

### 🚀 Features
- feat(data) : préconfigurer le compte admin, le type Autre et la validation téléphone
- feat(infra) : dockeriser l'API, externaliser la configuration et aligner le fuseau horaire
- feat(config) : externaliser les origines CORS via variable d'environnement

### 🐛 Fixes
- fix : bloquer la création d'activité en conflit avec une inscription existante
- fix(tests) : définir cors.allowed-origins pour les tests d'intégration
- fix(tests) : différer l'initialisation SQL après la création du schéma

### 🔧 Others
- test(coverage) : TU + TI, Jacoco et Sonar

---

## v0.3.0 - 2026-06-28

### 🚀 Features
- feat(auth) : migration de l'authentification vers Keycloak (OIDC, identité UUID)
- feat(Authentification) : mot de passe oublié
- feat(covoiturage) : modifier / annuler un covoiturage
- feat(covoiturage) : proposer / rejoindre un covoiturage après avoir rejoint une activité
- feat(profil) : changement de mot de passe utilisateur
- feat(admin) : statistiques
- feat(activité) : gestion admin des activités
- feat(type_activités): suppression type déjà utilisé
- feat(type_activités): implémentation CRUD types des activités
- feat(activité): désinscription d'une activité (#37)
- feat(activité): correction récupération des activités disponibles  (#36)
- feat(activite): inscription à une activité (#35)

### 🐛 Fixes
- fix(ci) : exécuter les git log dans le script de changelog et supprimer les workflows en double
- fix(ci): correction de l'historique du changelog

### 🔧 Others
- test(covoiturage) : réalignement des tests activité sur la nouvelle API et tests covoit

---

## v0.2.1 - 2026-04-18

### 🐛 Fixes
- fix generation du changelog

### 🔧 Others
- chore(release): v0.2.0

---

  ## v0.2.0 - 2026-04-12

  ### 🚀 Features
  git log v0.2.0..HEAD --pretty=format:"%s" | grep "^feat" | sed 's/^/- /' || true

  ### 🐛 Fixes
  git log v0.2.0..HEAD --pretty=format:"%s" | grep "^fix" | sed 's/^/- /' || true
  

  ### 🔧 Others
  git log v0.2.0..HEAD --pretty=format:"%s" | grep -E "^(chore|docs|refactor|test)" | sed 's/^/- /' || true

  ---

# Changelog

## v0.2.0 - 2026-04-05

### 🚀 Features
- feat(activite): suppression d'une activite
- feat(activity): modifier une activité
- feat(ci): ajout du script de génération du changelog et mise en place de la CI (#19)

### 🐛 Fixes
- fix(activite): récupérer le nombre de place restantes pour une activité
- fix(activite): création d'une activité sur un créneau déjà passé de la journée
- fix(ci) : ajout du script de génération de la prochaine version
- fix(ci): correction de la ci
- fix(ci): correction des paths pour les scripts (#25)
- fix(ci): move workflows to root directory (#24)
- fix(ci): move workflows to root directory (#23)

### 🔧 Others
- test(activite): correction du test activite

