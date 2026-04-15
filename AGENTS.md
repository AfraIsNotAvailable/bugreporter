# AGENTS.md

## Project snapshot
- This repository is a Spring Boot 4.0.3 backend. The full Comment vertical slice is implemented (`CommentController`, `CommentService`, `CommentRepository`, `Comment` entity, `CommentRequest`/`CommentResponse` DTOs, `GlobalExceptionHandler`, `ResourceNotFoundException`, `ForbiddenException`, `InvalidVoteTypeException`). The comment voting system is also implemented: `CommentVote` entity, `VoteType` enum, `CommentVoteRepository`, `VoteRequest` DTO, and `POST /api/comments/{commentId}/vote` endpoint. User entity (`User.java`) is no longer a stub — it has `username`, `email`, `password` (BCrypt hash), `role` (`Role` enum), `banned`, and `createdAt` (via `@PrePersist`). Auth is now implemented with `AuthController`/`AuthService`, request DTOs (`LoginRequest`, `RegisterRequest`), `AuthResponse`, and JWT components (`JwtService`, `JwtAuthenticationFilter`). `AdminController` also exists for `/api/admin/**` endpoints. Bug is still a skeleton stub with only `id`. Tag has no entity, repository, or any code yet (it appears only in `folder-structure.txt` as a planned addition).
- `entity/enums/Role.java` defines `USER`, `MODERATOR`, `ADMIN` — used by `User.role`.
- A React/Vite frontend lives in `bugreporter-frontend/` (Vite dev server on `http://localhost:5173`); it is separate from the Spring Boot app and consumes the REST API.
- Treat `folder-structure.txt` as the architecture contract: `controller -> service -> repository -> entity`, with request/response DTOs under `dto/` and centralized HTTP error handling under `exception/`.
- Keep new code under `src/main/java/com/group11/bugreporter`; the package names in `folder-structure.txt` are the intended long-term layout even though most directories are still sparse.

## Runtime assumptions you need to know first
- Local development expects Docker services from `docker-compose.yml`: PostgreSQL 17 on `localhost:5436` (container port `5432`) and Mailpit SMTP on `localhost:1025` with UI on `http://localhost:8025`.
- `src/main/resources/application.properties` points directly at those local ports (`spring.datasource.url=jdbc:postgresql://localhost:5436/bugreporter_db`). The backend HTTP port is `8081` (`server.port=8081`).
- `spring.jpa.hibernate.ddl-auto=update` is enabled. Any new JPA entity/table mapping will mutate the local schema on startup; be careful when renaming fields or tables.
- Mail is configured for local capture only (`spring.mail.host=localhost`, port `1025`), so email-related work should be checked in Mailpit rather than expecting real delivery.
- `config/DockerComposeBootstrap.java` runs at startup (controlled by `app.startup.docker.enabled=true` in `application.properties`). It socket-probes `app.startup.docker.required-ports` (5436, 1025) and calls `docker compose up -d` automatically if either port is unreachable. This means `./mvnw test` will attempt to start Docker Compose by itself when the services are down, rather than failing immediately.
- `BugreporterApplication` calls `DemoData.loadDemoData()` at startup, which seeds users/bugs/comments from `src/main/resources/data/PlaceholderData*.json` when relevant tables are empty.

## Verified commands
- Start dependencies first:
  - `docker compose up -d`
- Run tests:
  - `./mvnw test`
- Run the app locally:
  - `./mvnw spring-boot:run`
- Stop local services:
  - `docker compose down`
- Non-obvious gotcha: `DockerComposeBootstrap` tries to auto-start Docker services before the context loads, but it requires Docker to be installed and the `docker-compose.yml` to be discoverable from `user.dir`. If Docker is not installed or the compose file is absent, the context-load will still fail.

## Code patterns already implied by this repo
- Because the current test is `@SpringBootTest`, new configuration affects tests immediately; security, JPA, mail, and any future Feign clients all load unless you isolate them explicitly.
- `DockerComposeBootstrapTest` shows the pattern for infrastructure unit tests: plain JUnit with no Spring context, constructed directly with lambda mocks for `CommandExecutor` and `PortProbe` interfaces.
- `config/SecurityConfig.java` now exists. It disables CSRF, sets sessions to `STATELESS`, enables BCrypt via a `PasswordEncoder` bean, configures CORS for `http://localhost:5173`, and uses `@EnableMethodSecurity`. `POST /api/auth/**` and `GET /api/comments/bug/**` are `permitAll()`; all other endpoints require authentication. JWT auth is enforced with `JwtAuthenticationFilter` in the filter chain. The generated default password at startup no longer appears — `SecurityConfig` controls auth.
- `@PreAuthorize` (method-level security) is active. Current example: class-level `@PreAuthorize("hasRole('ADMIN')")` in `AdminController` for `/api/admin/**`. Add similar annotations when implementing role-based access on new endpoints.
- `spring-cloud-starter-openfeign` is present in `pom.xml`, and `README.md` explicitly mentions an independent notification microservice for ban emails/SMS. Any external-service integration should fit that direction instead of hard-coding remote calls in controllers.
- `src/main/resources/static/` and `templates/` exist but are empty; the frontend is the React/Vite app under `bugreporter-frontend/`.
- Comment mutating operations now resolve the authenticated user from `Authentication`/`SecurityContext` in `CommentController` (`resolveAuthenticatedUser`), then pass `requestingUserId`/`requestingUserRole` into `CommentService` for ownership/role checks.
- `CommentRepository` uses `@Lock(LockModeType.PESSIMISTIC_WRITE)` on `findByIdForUpdate` to prevent race conditions during concurrent votes. Apply the same pattern on any entity whose numeric counters/scores are updated concurrently.
- Vote toggle semantics (in `CommentService.voteComment`): casting the same vote again removes it (score ±1 reversal); casting the opposite vote swaps it (score ±2 swing). Guard with the pessimistic-write lock query.

## When adding features, follow the repo's intended seams
- For a new domain feature, add the whole vertical slice: entity + repository + service + controller + DTOs + exceptions, matching the package map in `folder-structure.txt`.
- Keep business rules in `service/`, not in controllers. The implemented example is `CommentService` for ownership checks (`ForbiddenException`) and existence checks (`ResourceNotFoundException`); follow the same pattern for `BugService` and `UserService`.
- DTOs use the `dto/request/` and `dto/response/` sub-package layout from `folder-structure.txt`. The `dto/request/` split **has been adopted** — `CommentRequest` is at `dto/request/CommentRequest.java`, `VoteRequest` is at `dto/request/VoteRequest.java`; place new request DTOs there. `dto/response/` now has `CommentResponse.java`; controllers return response DTOs via a `static CommentResponse.fromEntity(Comment)` factory method — follow this pattern for new response DTOs instead of returning raw entities.
- Reuse the existing `exception/GlobalExceptionHandler.java` for HTTP errors: it already handles `MethodArgumentNotValidException` → 400, `ResourceNotFoundException` → 404, `ForbiddenException` → 403, and `InvalidVoteTypeException` → 400.
- Entity conventions established by `Comment.java`: use Lombok `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`; use `@CreationTimestamp` for audit timestamps; declare all `@ManyToOne` with `FetchType.LAZY`; initialize default numeric fields with `@Builder.Default` (e.g., `@Builder.Default private Integer score = 0`).
- Service conventions established by `CommentService.java`: annotate every public method with `@Transactional`; use `@Transactional(readOnly = true)` for read-only methods; use `@RequiredArgsConstructor` for dependency injection; never expose raw entities — return the saved entity only when the caller needs it.
- DTO validation conventions from `CommentRequest.java`: use Bean Validation annotations (`@NotBlank`, `@Size`, `@URL` from `hibernate-validator`) directly on DTO fields; mark controller parameters with `@Valid` to trigger them. DTOs use Lombok `@Data` (not the entity's `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder` stack).
- `exception/ForbiddenException.java` is an established exception alongside `ResourceNotFoundException.java` and `InvalidVoteTypeException.java` (not listed in `folder-structure.txt` but already in use — add them to any new vertical that needs 403 or custom-400 responses).

## Collaboration conventions already documented
- `README.md` describes a feature-branch workflow: branches are named by feature area (examples: `sisteme-de-baza`, `motor-raportare`, `interactiuni-sociale`), then merged through PR review.
- The README TODO list is the best source of feature scope; it also explains why dependencies like Security, Mail, PostgreSQL, and OpenFeign are already in the build before their concrete classes exist.
