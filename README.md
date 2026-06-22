# Powerbank Sharing Platform

A microservices-based powerbank sharing system built with Spring Boot 3, gRPC, Kafka, PostgreSQL, Keycloak, and Kong.

## Modules

| Module | Description | Port |
|---|---|---|
| `user-service` | User registration, profiles, auth integration | 8081 |
| `station-service` | Station inventory and powerbank slot management | 8082 |
| `rental-service` | Rental lifecycle orchestration | 8083 |
| `payment-service` | Billing and payment processing | 8084 |
| `proto` | Shared Protobuf/gRPC definitions | — |
| `common` | Shared DTOs, exceptions, utilities | — |

## Prerequisites

- Java 17+
- Maven 3.9+
- Docker & Docker Compose

## Quick Start

### 1. Start infrastructure

```bash
cp .env .env.local        # optional: override credentials locally
docker compose up -d
```

Wait for all services to be healthy:

```bash
docker compose ps
```

### 2. Verify infrastructure

| Service | URL |
|---|---|
| Keycloak admin console | http://localhost:8080 |
| Kong proxy | http://localhost:8000 |
| Kong admin API | http://localhost:8001 |
| PostgreSQL | localhost:5432 |
| Kafka | localhost:29092 |

### 3. Build all modules

```bash
mvn clean install -DskipTests
```

### 4. Run a service

```bash
cd user-service
mvn spring-boot:run
```

Or run the fat JAR:

```bash
java -jar user-service/target/user-service-1.0.0-SNAPSHOT.jar
```

## Development

### Build single module

```bash
mvn -pl user-service -am clean package
```

### Run tests

```bash
mvn test
```

### Regenerate Protobuf sources

```bash
mvn -pl proto generate-sources
```

## Infrastructure teardown

```bash
docker compose down -v   # -v removes volumes (wipes databases)
```

## Environment variables

See [.env](.env) for all configurable values. Never commit secrets — override in `.env.local` or your CI secrets store.
