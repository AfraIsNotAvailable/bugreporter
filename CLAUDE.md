# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Is

BugReporter is a full-stack bug reporting platform built as a UTCN Software Design group project. It has a Spring Boot 4 REST API backend (port 8081) and a React 19 + Vite frontend (port 5173), backed by PostgreSQL 17 and Mailpit (local email) both run via Docker Compose.

## Commands

### Backend

```bash
docker compose up -d          # Start PostgreSQL (5436) + Mailpit (1025, UI on 8025)
./mvnw spring-boot:run        # Run the API (auto-starts Docker if ports unreachable)
./mvnw test                   # Run all tests
./mvnw test -Dtest=BugControllerTest  # Run a single test class
./mvnw clean package          # Build JAR
```

### Frontend

```bash
cd bugreporter-frontend
npm run dev                   # Vite dev server on 5173
npm run build                 # Production build
npm run lint                  # ESLint
npm run storybook             # Storybook on 6006
```

## Architecture

### Backend Layers

Follows strict layered architecture: Controller → Service → Repository → Entity.

```
com.group11.bugreporter/
├── config/          # SecurityConfig (JWT/CORS), DemoData (seeds DB on startup), DockerComposeBootstrap
├── controller/      # REST endpoints — thin, delegate to services
├── service/         # Business logic — all domain rules live here
├── repository/      # Spring Data JPA interfaces
├── entity/          # JPA entities with Lombok
├── dto/             # request/ and response/ DTOs; response DTOs use static fromEntity() factory
├── exception/       # GlobalExceptionHandler + custom exceptions (ResourceNotFound, Forbidden, etc.)
└── security/        # JwtService, JwtAuthenticationFilter
```

**Startup sequence**: `BugreporterApplication` → `DockerComposeBootstrap` (starts containers if needed) → Spring context → `DemoData.loadDemoData()` (seeds from JSON files in `src/main/resources/data/` if tables are empty).

### Key Domain Rules

- **Bug status auto-transitions**: `OPEN` → `IN_PROGRESS` when first comment is added. Moderators can manually set status via `PATCH /api/bugs/{id}/status`. Authors call `PATCH /api/bugs/{id}/resolve`.
- **Comment voting**: toggle (same vote removes it), swap (opposite vote = ±2 score). Uses pessimistic write lock to prevent race conditions.
- **Roles**: `USER`, `MODERATOR`, `ADMIN`. Authorization is method-level via `@PreAuthorize`. Admin/moderator routes are under `/api/admin/**`.
- **Public endpoints**: `GET /api/bugs`, `GET /api/comments/bug/{bugId}`, all `/api/auth/**`. Everything else requires a JWT in `Authorization: Bearer <token>`.

### Entity Conventions

```java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "...")
```

- Use `@CreationTimestamp` for audit fields, `FetchType.LAZY` for `@ManyToOne`
- Default field values go in `@Builder.Default`
- Enums: `BugStatus` (OPEN, IN_PROGRESS, FIXED, CLOSED), `Role` (USER, MODERATOR, ADMIN), `VoteType` (UPVOTE, DOWNVOTE)

### Response DTOs

All response DTOs have a static factory:

```java
public static BugResponse fromEntity(Bug bug) { ... }
```

Never expose entity objects directly from controllers.

### Frontend

```
bugreporter-frontend/src/
├── services/    # Axios calls (api.js sets baseURL to http://localhost:8081)
├── hooks/       # Custom React hooks for state management
├── components/  # Reusable UI components
├── pages/       # Route-level components
└── stories/     # Storybook stories
```

Frontend uses React Router 7, Tailwind CSS 4, and Axios. CORS is configured for `http://localhost:5173`.

### Testing

- **Controller tests**: `@ExtendWith(MockitoExtension.class)`, mock services, test request validation and auth
- **Service tests**: use real repositories with `@Transactional`
- **Integration smoke test**: `BugreporterApplicationTests` — just checks context loads

Test files mirror source: `src/test/java/com/group11/bugreporter/{layer}/`.
