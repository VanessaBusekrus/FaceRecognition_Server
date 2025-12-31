# FaceRecognition Backend

A Spring Boot backend providing user registration, sign-in (optional 2FA), image entry tracking and face detection (Clarifai). This README documents how to build, run and test the service locally.

---

## Table of contents

- Overview
- Requirements
- Quick start
- Database
- Configuration and secrets
- Testing
- Troubleshooting
- Project structure

---

## Overview

- Java 21, Spring Boot 3.x
- JPA / Hibernate for data access
- H2 file-based database for local development
- Clarifai gRPC client for face detection
- Spring Security with CORS enabled for frontend integration

---

## Requirements

- Java 21
- Maven (or use `./mvnw` wrapper)

---

## Quick start

Run locally with the file-based H2 database using the `local` profile:

```bash
# Set Clarifai credentials (if using face detection features)
export CLARIFAI_API_PAT=your_pat_here
export CLARIFAI_USER_ID=your_user_id_here
export CLARIFAI_APP_ID=your_app_id_here

# Start the application with local profile
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`.

Build a runnable JAR:

```bash
./mvnw clean package
java -jar target/hands_on-0.0.1.jar
```

---

## Database

The local profile uses H2 file-based database:
- Database file location: `./data/facerecognition.mv.db`
- Schema initialization: `src/main/resources/h2/schema.sql` (executed on startup)
- H2 Console: accessible at `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:file:./data/facerecognition`)
- Hibernate DDL: `update` mode (automatically creates/updates tables)

The database persists data between application restarts. To reset, delete the `./data/` directory.

---

## Configuration and secrets

Local configuration is in `src/main/resources/application-local.yml` and imports `secret-clarifai.properties` for Clarifai credentials.

### Clarifai Setup

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

### CORS Configuration

CORS is configured in `src/main/java/nl/cyberella/hands_on/config/CorsConfig.java` to allow:
- Origin: `http://localhost:5173` (Vite dev server)
- Methods: GET, POST, PUT, DELETE, OPTIONS
- Credentials: enabled

Add additional origins if needed for your frontend.

---

## Testing

Run unit tests with Maven:

```bash
./mvnw test
```

Tests use JUnit 5 and Mockito. Unit tests mock repositories and external integrations so they run without network or database dependencies.

---

## Troubleshooting

### Clarifai client not initialized

If you see "Clarifai client not initialized or PAT not configured":
1. Verify you're running with the `local` profile: `SPRING_PROFILES_ACTIVE=local`
2. Check that `secret-clarifai.properties` exists and contains valid credentials, or export environment variables
3. Restart the application and look for "Clarifai gRPC client initialized" in the logs

### CORS errors from frontend

If your frontend can't connect:
1. Verify the backend is running on `http://localhost:8080`
2. Check that your frontend origin is listed in `CorsConfig.java`
3. Ensure you're using the correct HTTP methods (POST for /register, /signin, etc.)

### Database schema issues

If tables aren't created:
1. Check that `src/main/resources/h2/schema.sql` exists and is valid
2. Verify the `local` profile is active (look for "No active profile" in startup logs)
3. Delete `./data/` directory and restart to recreate the database

### Port already in use

If port 8080 is busy, set a different port:
```bash
SERVER_PORT=8000 SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

---

## Project structure

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
  - `application-local.yml` — local profile configuration
  - `h2/schema.sql` — H2 database schema
  - `logback.xml` — logging configuration
- `src/test/` — unit tests
- `pom.xml` — Maven dependencies and build configuration
- `mvnw`, `mvnw.cmd` — Maven wrapper scripts
- `secret-clarifai.properties` — Clarifai credentials (gitignored)
- `data/` — H2 database files (gitignored)


