# Manuel de mise à jour — CoLife API

Ce document décrit le processus à suivre pour faire évoluer l'application.

## Conventions de commits

Le projet utilise la convention **Conventional Commits** :

```
type(scope) : description courte
```

| Type | Usage |
|---|---|
| `feat` | Nouvelle fonctionnalité |
| `fix` | Correction de bug |
| `refactor` | Refactoring sans changement de comportement |
| `test` | Ajout ou modification de tests |
| `ci` | Modification des pipelines CI/CD |
| `chore` | Tâches de maintenance |
| `docs` | Mise à jour de la documentation |

Le type de commit détermine l'incrément de version (SemVer) lors de la release.

## Stratégie de branches

| Branche | Rôle | Pipeline déclenché |
|---|---|---|
| `main` | Code stable en production | Release (tag + GitHub Release) |
| `develop` | Intégration continue | Snapshot (artefact versionné) |
| `feat/KAN-XX-*` | Fonctionnalité en cours | Aucun |

Les branches de fonctionnalité sont créées depuis le tableau Kanban Jira. Le nom de la branche reprend l'identifiant du ticket.

## Processus de contribution

1. Créer une branche depuis `develop` (via Jira ou manuellement)
2. Développer la fonctionnalité avec des commits conventionnels
3. Vérifier localement : `mvn clean verify`
4. Vérifier la qualité : `mvn sonar:sonar` — le Quality Gate doit être vert
5. Ouvrir une Pull Request vers `develop`
6. Après validation, merger dans `develop` (déclenche le pipeline Snapshot)
7. Lorsque `develop` est stable, ouvrir une PR vers `main` (déclenche la Release)

## Gestion des versions

Les versions suivent le **versioning sémantique** (`MAJOR.MINOR.PATCH`).  
La version est calculée automatiquement par `scripts/next-version.sh` à partir de l'historique des commits.  
Le `CHANGELOG.md` est généré automatiquement à chaque release.

## Qualité

- Couverture de code ≥ 80 % sur le nouveau code (Quality Gate SonarQube)
- Duplication ≤ 3 %
- Zéro nouvelle faille de sécurité

Tout code ne satisfaisant pas le Quality Gate ne doit pas être mergé dans `develop`.
