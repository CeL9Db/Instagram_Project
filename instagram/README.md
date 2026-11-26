# Serveur Instagram

Serveur Java / Spring Boot qui communique avec le client JavaFX par socket TCP (port 8080).
Les données sont stockées dans PostgreSQL.

## Lancer avec Docker

Il faut avoir Docker Desktop.

```bash
cd instagram
docker compose up --build
```

Ça lance 2 conteneurs :
- `postgres` : la base de données (port 5432)
- `instagram-server` : le serveur (port 8080)

Ensuite on lance le client JavaFX normalement, il se connecte sur `localhost:8080`.

Pour tout arrêter :

```bash
docker compose down
```

(`docker compose down -v` supprime aussi les données de la base)

## Lancer sans Docker

Il faut Java 17 et un PostgreSQL qui tourne en local avec une base `instagram` (utilisateur `postgres`, mot de passe `1234`).

```bash
cd instagram
./mvnw spring-boot:run
```

## Tests

```bash
cd instagram
./mvnw test
```
