# Changelog

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

