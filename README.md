# FaceRecognition Backend

A Spring Boot backend providing user registration, sign-in (optional 2FA), image entry tracking and face detection (Clarifai). This README documents how to build, run and deploy the service locally and to AWS EKS, and how to run database migrations safely with Flyway.

---

## Table of contents

- [Overview](#overview)
- [Requirements](#requirements)
- [Local Development](#local-development)
  - [Quick start](#quick-start)
  - [Database (H2)](#database-h2)
  - [Configuration and secrets](#local-configuration-and-secrets)
- [AWS/Production Deployment](#awsproduction-deployment)
  - [Database migrations](#database-migrations)
  - [Docker & ECR](#docker--ecr-push-image)
  - [Deploy to Kubernetes (EKS)](#deploy-to-kubernetes-eks)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)
- [Project structure](#project-structure)

---

## Overview

- Java 21, Spring Boot 3.x
- JPA / Hibernate for data access
- H2 file-based database for local development; PostgreSQL (AWS RDS) for production
- Clarifai gRPC client for face detection
- Spring Security with CORS enabled for frontend integration
- Two deployment modes:
  - **Local**: H2 database, file-based persistence, ideal for development
  - **Production**: PostgreSQL on AWS RDS, deployed to EKS with Flyway migrations

---

## Requirements

### Local Development
- Java 21
- Maven (or use `./mvnw` wrapper)

### AWS/Production Deployment
- All local requirements plus:
- Docker (for building images)
- kubectl + kubeconfig for target EKS cluster
- AWS CLI configured (for ECR/EKS operations)

---

## Local Development

### Quick start

Run locally with the file-based H2 database using the `local` profile:

```bash
# Set Clarifai credentials (if using face detection features)
export CLARIFAI_API_PAT=your_pat_here
export CLARIFAI_USER_ID=your_user_id_here
export CLARIFAI_APP_ID=your_app_id_here

# Start the application with local profile
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

## AWS/Production Deployment

### Database Migrations

Migrations are located in `src/main/resources/db/migration` and follow Flyway naming (V1__, V2__, ...). In production you must run these migrations against your PostgreSQL RDS before (or as part of) deployment.

**Important:**
- Production profile uses PostgreSQL and `spring.jpa.hibernate.ddl-auto: none`. Hibernate will not create tables in prod.
- If you already manually created objects in the DB, tell Flyway to baseline so Flyway records the current state instead of re-applying migrations (`baseline-on-migrate=true` or `flyway baseline`).

#### Running Flyway locally (optional)

If your workstation has network access to the RDS (and the RDS SG allows your IP), you can run Flyway locally using Docker:

```bash
export RDS_HOST="<rds_host_name>"
export RDS_DB="<database_name>"
export RDS_USERNAME="<database_username>"
export RDS_PASSWORD="<your_password>"

docker run --rm -v "$(pwd)/src/main/resources/db/migration":/flyway/sql \
  -e FLYWAY_URL="jdbc:postgresql://${RDS_HOST}:5432/${RDS_DB}?sslmode=require" \
  -e FLYWAY_USER="${RDS_USERNAME}" \
  -e FLYWAY_PASSWORD="${RDS_PASSWORD}" \
  flyway/flyway:latest -baselineOnMigrate=true migrate
```

Note: use `-baselineOnMigrate=true` if the DB already contains objects you created manually
### Database (H2)

The local profile uses H2 file-based database:
- **Database file location**: `./data/facerecognition.mv.db`
- **Schema initialization**: `src/main/resources/h2/schema.sql` (executed on startup)
- **H2 Console**: accessible at `http://localhost:8080/h2-console` 
  - JDBC URL: `jdbc:h2:file:./data/facerecognition`
  - Username: `sa`
  - Password: (leave blank)
- **Hibernate DDL**: `update` mode (automatically creates/updates tables)

The database persists data between application restarts. To reset, delete the `./data/` directory.

### Local Configuration and Secrets

Local configuration is in `src/main/resources/application-local.yml` and imports `secret-clarifai.properties` for Clarifai credentials.

#### Clarifai Setup

Create or update `secret-clarifai.properties` in the project root:

```properties
clarifai.api.pat=YOUR_ACTUAL_PAT
clarifai.api.user-id=YOUR_USER_ID
clarifai.api.app-id=YOUR_APP_ID
```

**Important:** Keep `secret-clarifai.properties` gitignored and never commit real credentials.

Alternatively, export environment variables before starting:

```bash
export CLARIFAI_API_PAT=your_pat
export CLARIFAI_USER_ID=your_user_id
export CLARIFAI_APP_ID=your_app_id
```

If Clarifai credentials are not configured, the client will be disabled and face detection endpoints will return an error.

#### CORS Configuration

CORS is configured in `src/main/java/nl/cyberella/hands_on/config/CorsConfig.java` to allow:
- Origin: `http://localhost:5173` (Vite dev server)
- Methods: GET, POST, PUT, DELETE, OPTIONS
- Credentials: enabled

Add additional origins if needed for your frontend.

---

## Database & migrations

Migrations are located in `src/main/resources/db/migration` and follow Flyway naming (V1__, V2__, ...). In production you must run these migrations against your PostgreSQL RDS before (or as part of) deployment.

Local dev: the project contains H2 SQL scripts in `src/main/resources/h2/` used for local startup when running with the default profile.

Important:
- Production profile uses PostgreSQL and `spring.jpa.hibernate.ddl-auto: none`. Hibernate will not create tables in prod.
- If you already manually created objects in the DB, tell Flyway to baseline so Flyway records the current state instead of re-applying migrations (`baseline-on-migrate=true` or `flyway baseline`).

---

## Docker & ECR (push image)

Create an ECR repo (example uses profile `vanessa.admin` and `eu-central-1`):

```bash
export AWS_REGION=eu-central-1
AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
ECR_URI=${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/facerecognition-backend

# create repo if it does not exist
aws ecr create-repository --repository-name facerecognition-backend --region ${AWS_REGION} || true
```

Build and push:

```bash
./mvnw -DskipTests package
docker build -t facerecognition-backend:latest .
docker tag facerecognition-backend:latest ${ECR_URI}:latest
aws ecr get-login-password --region ${AWS_REGION} \
  | docker login --username AWS --password-stdin ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com

docker push ${ECR_URI}:latest
```

---

## Deploy to Kubernetes (EKS)

Prerequisites:
- `kubectl` configured for your EKS cluster
- ECR image pushed
- `db-secret` created in the cluster with DB credentials
- EKS nodes/pods can reach your RDS on port 5432 (RDS security group must allow the node/pod SG or VPC CIDR)

Example steps (in order):

1) Apply DB secret (you can create `db-secret.yaml` or create from literals):

```bash
kubectl apply -f db-secret.yaml
# OR create from literals (careful with quoting passwords)
kubectl create secret generic db-secret \
  --from-literal=RDS_USERNAME='<your_username>' \
  --from-literal=RDS_PASSWORD='<your_password>' \
  --from-literal=RDS_HOST='db-face-recognition....rds.amazonaws.com' \
  --from-literal=RDS_DB='facerecognition' \
  --from-literal=RDS_PORT='5432'
```

2) Run Flyway migrations (recommended in-cluster):

```bash
### Production Configuration & Secrets

Production config: `src/main/resources/application-prod.yml`. This file expects DB configuration from environment variables (or Kubernetes `Secret`).

Required env vars for production:

- `RDS_HOST`, `RDS_PORT`, `RDS_DB`, `RDS_USERNAME`, `RDS_PASSWORD`
- Clarifai: `CLARIFAI_API_PAT`, `CLARIFAI_USER_ID`, `CLARIFAsql \
  -e FLYWAY_URL="jdbc:postgresql://${RDS_HOST}:5432/${RDS_DB}?sslmode=require" \
  -e FLYWAY_USER="${RDS_USERNAME}" \
  -e FLYWAY_PASSWORD="${RDS_PASSWORD}" \
  flyway/flyway:latest -baselineOnMigrate=true migrate
```

Note: use `-baselineOnMigrate=true` if the DB already contains objects you created manually.

---

## Configuration & Secrets

- Development config: `src/main/resources/application.yml` and H2 scripts under `src/main/resources/h2/`.
- Production config: `src/main/resources/application-prod.yml`. This file expects DB configuration from environment variables (or Kubernetes `Secret`).

Required env vars for production:

- `RDS_HOST`, `RDS_PORT`, `RDS_DB`, `RDS_USERNAME`, `RDS_PASSWORD`
- Clarifai: `CLARIFAI_API_PAT`, `CLARIFAI_API_USER_ID`, `CLARIFAI_API_APP_ID` (only if using Clarifai features)

Security note: avoid checking secrets into the repo. Prefer AWS Secrets Manager + ExternalSecrets or Kubernetes Secrets with restricted RBAC.

---

## Testing

Run unit tests with Maven:

```bash
./mvnw test
```

Tests use JUnit 5 and Mockito. Unit tests mock repositories and external integrations so they run without network or DB.

---

## Troubleshooting

- "Connect timed out" to RDS: update RDS Security Group to allow traffic from EKS node security group (preferred) or from your IP when running locally.
- Flyway plugin dependency failures when running via Maven: newer Flyway versions separate DB-specific plugins; running Flyway in-cluster (Docker image) avoids Maven plugin resolution issues.
- Hibernate trying to create `testdb.*` tables: ensure your entity mappings and `application-prod.yml` point to the `public` schema and `ddl-auto` is `none` in production.

---
### Local Development

**Clarifai client not initialized**

If you see "Clarifai client not initialized or PAT not configured":
1. Verify you're running with the `local` profile: `SPRING_PROFILES_ACTIVE=local`
2. Check that `secret-clarifai.properties` exists and contains valid credentials, or export environment variables
3. Restart the application and look for "Clarifai gRPC client initialized" in the logs

**CORS errors from frontend**

If your frontend can't connect:
1. Verify the backend is running on `http://localhost:8080`
2. Check that your frontend origin is listed in `CorsConfig.java`
3. Ensure you're using the correct HTTP methods (POST for /register, /signin, etc.)

**Database schema issues**

If tables aren't created:
1. Check that `src/main/resources/h2/schema.sql` exists and is valid
2. Verify the `local` profile is active (look for "No active profile" in startup logs)
3. Delete `./data/` directory and restart to recreate the database

**Port already in use**

If port 8080 is busy

- `src/main/java/nl/cyberella/hands_on/` — application code
  - `controllers/` — REST endpoints
  - `services/` — business logic
  - `models/` — JPA entities
  - `repositories/` — data access
  - `config/` — Spring configuration (CORS, Security, etc.)
  - `dto/` — data transfer objects
  - `audit/` — audit logging
  - `utils/` — utility classes
- `src/main/resources/` — configuration and resources
  - `application-local.yml` — local profile configuration (H2)
  - `application-prod.yml` — production profile configuration (PostgreSQL)
  - `h2/schema.sql` — H2 database schema for local dev
  - `db/migration/` — Flyway SQL migrations for production
  - `logback.xml` — logging configuration
- `src/test/` — unit tests
- `k8s/` — Kubernetes manifests (ConfigMaps, Jobs, deployments) for AWS EKS
- `pom.xml` — Maven dependencies and build configuration
- `mvnw`, `mvnw.cmd` — Maven wrapper scripts
- `secret-clarifai.properties` — Clarifai credentials for local dev (gitignored)
- `data/` — H2 database files (gitignored) update RDS Security Group to allow traffic from EKS node security group (preferred) or from your IP when running locally.
- **Flyway plugin dependency failures when running via Maven**: newer Flyway versions separate DB-specific plugins; running Flyway in-cluster (Docker image) avoids Maven plugin resolution issues.
- **Hibernate trying to create `testdb.*` tables**
- `src/main/java/...` — application code (controllers, services, models, repos)
- `src/main/resources/db/migration` — Flyway SQL migrations
- `src/main/resources/h2` — H2 schema & data used in local dev
- `k8s/` — Kubernetes manifests (ConfigMaps, Jobs, deployments)
- `db-secret.yaml` — example Kubernetes secret (do not commit secrets in plaintext)
- `pom.xml`, `mvnw` — Maven build


