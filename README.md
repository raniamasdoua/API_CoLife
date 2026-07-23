# CoLife — API Backend

API REST de l'application CoLife (gestion d'activités et covoiturage en entreprise).

## Stack technique

| Couche | Technologie |
|---|---|
| Langage | Java 21 |
| Framework | Spring Boot 3.5 |
| Persistance | Spring Data JPA / PostgreSQL |
| Sécurité | Spring Security, Keycloak (OIDC/OAuth2) |
| Tests | JUnit 5, Spring Boot Test, H2 (tests) |
| Couverture | JaCoCo 0.8 |
| Qualité | SonarQube |
| Build | Maven 3.9 |
| Conteneurisation | Docker, Docker Compose |

---

## Manuel de déploiement

### Prérequis

- Docker et Docker Compose

### 1. Variables d'environnement

```bash
cp .env.exemple .env
```

| Variable | Rôle |
|---|---|
| `DB_NAME`, `DB_USER`, `DB_PASSWORD` | Credentials PostgreSQL |
| `POSTGRES_HOST_PORT` | Port exposé sur la machine hôte (changer si conflit) |
| `KEYCLOAK_ISSUER_URI` | URI public du realm (validé dans les JWT) |
| `KEYCLOAK_JWK_SET_URI` | URI interne pour télécharger les clés publiques Keycloak |
| `KEYCLOAK_ADMIN`, `KEYCLOAK_ADMIN_PASSWORD` | Compte admin de la console Keycloak |
| `CORS_ALLOWED_ORIGINS` | Origines frontend autorisées |

### 2. Lancer l'application

```bash
docker compose up -d
```

Démarre PostgreSQL, l'API Spring Boot, Keycloak, Mailpit et SonarQube.  
Au premier démarrage, Keycloak importe automatiquement le realm `colife` depuis `keycloak/realm-export.json` — rôles, client OIDC et compte administrateur (`admin@entreprise.com`) inclus.

| Service | URL |
|---|---|
| API | http://localhost:8080 |
| Swagger | http://localhost:8080/swagger-ui/index.html |
| Keycloak | http://localhost:8081 |
| Mailpit | http://localhost:8025 |
| SonarQube | http://localhost:9000 |

### 3. Build de production (Docker)

```bash
docker build -t colife-api .
docker run -p 8080:8080 --env-file .env colife-api
```

---

## Manuel de test et qualité

### Exécuter les tests

```bash
mvn clean verify
```

Génère le rapport de couverture JaCoCo dans `target/site/jacoco/index.html`.

### Analyse SonarQube

```bash
mvn sonar:sonar
```

Résultats disponibles sur `http://localhost:9000`.

---

## Structure du projet

```
src/
├── main/java/com/colife/api/
│   ├── activity/          # Gestion des activités
│   ├── activityType/      # Types d'activité
│   ├── carpool/           # Covoiturage
│   ├── subscription/      # Inscriptions
│   ├── user/              # Utilisateurs
│   └── shared/            # Config, sécurité, exceptions
└── test/
    └── java/com/colife/api/
```

L'architecture suit le pattern **hexagonal** : chaque domaine est découpé en couches `domain`, `application`, `infrastructure` et `presentation`.