# FaceRecognition Backend

A Spring Boot backend providing user registration, sign-in (optional 2FA), image entry tracking and face detection (Clarifai). This README documents how to build, run and deploy the service locally and to AWS EKS, and how to run database migrations safely with Flyway.

---

## Table of contents

- Overview
- Requirements
- Quick start (development)
- Database & migrations
- Docker / ECR (push image)
- Deploy to Kubernetes (EKS)
- Running Flyway migrations (in-cluster recommended)
- Configuration and secrets
- Testing
- Troubleshooting
- Project structure

---

## Overview

- Java 21, Spring Boot 3.x
- JPA / Hibernate for data access
- Flyway for schema migrations (migrations live in `src/main/resources/db/migration`)
- Clarifai gRPC client for face detection
- H2 used for local development; PostgreSQL (AWS RDS) targeted in production

Important production behavior:
- `spring.jpa.hibernate.ddl-auto` is disabled for `prod`. Schema management must be performed with Flyway.

---

## Requirements

- Java 21
- Docker (for building/testing images)
- Maven (or use `./mvnw` wrapper)
- kubectl + kubeconfig for target cluster (EKS)
- AWS CLI configured (for ECR/EKS operations)

---

## Quick start (development)

Run locally with the embedded H2 database:

```bash
# use the Maven wrapper from repo root
./mvnw spring-boot:run
```

Build a runnable JAR:

```bash
./mvnw clean package
java -jar target/hands_on-0.0.1-SNAPSHOT.jar
```

The app exposes REST endpoints (see `src/main/java/.../controllers` for full details). There is a `HealthController` available for basic health checks.

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
AWS_ACCOUNT_ID=$(aws sts get-caller-identity --profile vanessa.admin --query Account --output text)
ECR_URI=${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/facerecognition-backend

# create repo if it does not exist
aws ecr create-repository --repository-name facerecognition-backend --region ${AWS_REGION} --profile vanessa.admin || true
```

Build and push:

```bash
./mvnw -DskipTests package
docker build -t facerecognition-backend:latest .
docker tag facerecognition-backend:latest ${ECR_URI}:latest
aws ecr get-login-password --region ${AWS_REGION} --profile vanessa.admin \
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
  --from-literal=RDS_USERNAME='Vanessa' \
  --from-literal=RDS_PASSWORD='your_password' \
  --from-literal=RDS_HOST='db-face-recognition....rds.amazonaws.com' \
  --from-literal=RDS_DB='facerecognition' \
  --from-literal=RDS_PORT='5432'
```

2) Run Flyway migrations (recommended in-cluster):

```bash
kubectl apply -f k8s/flyway-configmap.yaml
kubectl apply -f k8s/flyway-job.yaml
kubectl logs -l job-name=flyway-migrate -f --tail=200
```

Wait for the Job to complete and verify `flyway_schema_history` in the DB.

3) Deploy or update the backend deployment to use the new image:

```bash
# example: update image on existing deployment
kubectl set image deployment/facerecognition-backend \
  facerecognition-backend=${ECR_URI}:latest
kubectl rollout status deployment/facerecognition-backend
kubectl logs -f deployment/facerecognition-backend
```

If you don't have a deployment manifest yet, create one in `k8s/backend-deployment.yaml` and `k8s/backend-service.yaml` and `kubectl apply -f k8s/`.

---

## Running Flyway locally (optional)

If your workstation has network access to the RDS (and the RDS SG allows your IP), you can run Flyway locally using Docker:

```bash
export RDS_HOST="db-face-recognition.cpm0qiasuwb7.eu-central-1.rds.amazonaws.com"
export RDS_DB="facerecognition"
export RDS_USERNAME="Vanessa"
export RDS_PASSWORD="<your_password>"

docker run --rm -v "$(pwd)/src/main/resources/db/migration":/flyway/sql \
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

## Project structure (high level)

- `src/main/java/...` — application code (controllers, services, models, repos)
- `src/main/resources/db/migration` — Flyway SQL migrations
- `src/main/resources/h2` — H2 schema & data used in local dev
- `k8s/` — Kubernetes manifests (ConfigMaps, Jobs, deployments)
- `db-secret.yaml` — example Kubernetes secret (do not commit secrets in plaintext)
- `pom.xml`, `mvnw` — Maven build

---

If you want, I can:
- generate a single shell script that builds, pushes to ECR and deploys the app to EKS, or
- apply the `k8s/` manifests to your current cluster (I will verify `kubectl config current-context` first).

Pick one and I will prepare the exact commands or run them for you.

# Hands-On Spring Application

This is a Spring Boot application that provides user registration, sign-in (with optional 2FA), image entry tracking, and face detection using the Clarifai gRPC API.

