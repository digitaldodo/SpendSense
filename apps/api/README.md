# SpendSense API

The API is a Spring Boot 4.0.6 service using Java 21, Spring WebMVC, Spring Security, MongoDB, Validation, Actuator, and Springdoc OpenAPI.

## Structure

```text
src/main/java/com/spendsense/api/
  common/       API response envelopes
  config/       application, OpenAPI, Mongo, tracing config
  controller/   HTTP entry points
  domain/       base Mongo document support
  dto/          request and response DTOs
  exception/    global API error handling
  mapper/       DTO/domain mapping boundaries
  repository/   persistence contracts
  security/     JWT and role architecture skeleton
  service/      application services
```

## Local Run

Start MongoDB from the repository root:

```bash
docker compose up -d
```

Run the API:

```bash
./gradlew bootRun
```

On Windows PowerShell:

```powershell
.\gradlew.bat bootRun
```

Health:

```text
GET http://localhost:8080/api/v1/health
```

Swagger:

```text
http://localhost:8080/swagger-ui
```

## Layering Rules

- Controllers own HTTP shape only.
- DTOs define transport contracts.
- Services own application orchestration.
- Repositories own MongoDB access.
- Mappers isolate conversion between transport and domain.
- Security owns authentication and authorization infrastructure.
- Exceptions return normalized API errors with trace IDs.

No business schemas or financial calculations belong in Phase 1.
