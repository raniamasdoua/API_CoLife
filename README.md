# API_CoLife# API CoLife

Backend de l'application CoLife (gestion d’activités en entreprise).

## 🛠 Stack technique

- Java 21
- Spring Boot
- Spring Data JPA
- Spring Security
- H2 (environnement de développement)
- Maven

---

## Lancer le projet en local

### Prérequis

- Java 21
- Maven (ou Maven Wrapper)
- Git

### Étapes

1. Cloner le projet

```bash
git clone <url-du-repo>
cd api
mvn clean install
mvn spring-boot:run
http://localhost:8080

## 🐘 Base PostgreSQL (Dev)

Lancer la base :

docker-compose up -d

Variables d’environnement :

DB_USERNAME=colife_user
DB_PASSWORD=colife_password