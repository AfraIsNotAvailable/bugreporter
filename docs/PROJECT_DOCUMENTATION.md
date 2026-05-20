# BugReporter — Full Project Documentation

---

## Table of Contents

1. [Project Description](#1-project-description)
2. [Use Cases](#2-use-cases)
3. [Tech Stack](#3-tech-stack)
4. [Project Structure](#4-project-structure)
5. [Database Schema](#5-database-schema)
6. [API Reference](#6-api-reference)
7. [Backend — Auth & Security](#7-backend--auth--security)
8. [Backend — Bugs](#8-backend--bugs)
9. [Backend — Comments & Voting](#9-backend--comments--voting)
10. [Backend — Data Seeding](#10-backend--data-seeding)
11. [Frontend — Foundation & Routing](#11-frontend--foundation--routing)
12. [Frontend — Bugs](#12-frontend--bugs)
13. [Frontend — Comments & Voting](#13-frontend--comments--voting)
14. [Styling](#14-styling)
15. [Testing](#15-testing)
16. [Running the Project](#16-running-the-project)
17. [Team Responsibilities](#17-team-responsibilities)

---

## 1. Project Description

BugReporter is a full-stack web application for tracking and managing bug reports within a development team. Users can report bugs, discuss them via comments, vote on comments to surface the most helpful ones, and track the lifecycle of each bug from open to resolved.

The platform has three user roles with increasing privileges: regular users who report and comment on bugs, moderators who manage content and user bans, and administrators who have full control including user role management. The system enforces these roles at both the frontend UI layer and the backend API layer independently.

The backend is a REST API built with Spring Boot. The frontend is a React single-page application that consumes that API. A separate notification microservice handles email and SMS delivery when users are banned.

---

## 2. Use Cases

### Regular User
- Register an account with username, email, password, and optional phone number
- Log in and receive a JWT session token
- Browse all bug reports without logging in
- Search bugs by title or filter by tag
- Report a new bug (title, description, optional image URL)
- Add tags to any bug
- Edit or delete their own bug reports
- Mark their own bug as resolved (sets status to FIXED)
- Post comments on open or in-progress bugs
- Edit and delete their own comments
- Upvote or downvote other users' comments (toggle behavior)
- View their own bugs via "My Bugs" filter

### Moderator
- All regular user capabilities
- Delete any bug or comment regardless of authorship
- Change the status of any bug (OPEN / IN_PROGRESS / FIXED / CLOSED) via dropdown
- Edit any comment
- View the users table (cannot change roles)
- Ban or unban regular users

### Admin
- All moderator capabilities
- Change any user's role (USER / MODERATOR / ADMIN)
- View all bugs and comments in the admin panel
- Cannot be banned

---

## 3. Tech Stack

### Backend

| Layer | Technology | Version | Purpose |
|---|---|---|---|
| Language | Java | 21 | Primary backend language |
| Framework | Spring Boot | 4.0.3 | Application container, auto-configuration |
| Web | Spring MVC | (via Boot) | REST controllers, request mapping, response serialization |
| Security | Spring Security | (via Boot) | Filter chain, method-level authorization, CORS |
| Persistence | Spring Data JPA | (via Boot) | Repository abstraction over Hibernate |
| ORM | Hibernate | (via JPA) | Entity-to-table mapping, query generation, transactions |
| Database | PostgreSQL | 17 | Relational database, runs in Docker |
| Auth | JJWT | 0.13.0 | JWT token generation and validation |
| Passwords | BCrypt | (via Spring) | Password hashing |
| Code generation | Lombok | (via Boot) | `@Getter`, `@Setter`, `@Builder`, `@RequiredArgsConstructor`, `@Slf4j` |
| JSON | Jackson | (via Boot) | Serialization / deserialization, JavaTimeModule for dates |
| Email testing | Mailpit | v1.27 | Local SMTP server for capturing outgoing emails |
| Build | Maven | 3.x | Dependency management, build lifecycle |
| Containers | Docker Compose | — | PostgreSQL + Mailpit services |

#### Spring MVC
All REST endpoints are defined in `@RestController` classes. Spring MVC handles HTTP routing via `@GetMapping`, `@PostMapping`, `@PutMapping`, `@PatchMapping`, `@DeleteMapping`. Request bodies are deserialized from JSON via `@RequestBody`. Path variables come from `@PathVariable`. Query parameters come from `@RequestParam`. `ResponseEntity<T>` wraps responses with explicit HTTP status codes. `@Valid` on request body parameters triggers Jakarta Bean Validation before the method body runs.

#### Spring Security
The security layer is fully stateless — no HTTP sessions. A custom `JwtAuthenticationFilter` (extending `OncePerRequestFilter`) intercepts every request, extracts the `Authorization: Bearer <token>` header, validates the JWT, and loads the user into the `SecurityContextHolder`. The `SecurityFilterChain` bean defines which routes are public and which require authentication. Method-level guards use `@PreAuthorize("hasRole('ADMIN')")` etc., enabled by `@EnableMethodSecurity`. CORS is configured to allow the frontend origins.

#### Spring Data JPA / Hibernate
Repositories extend `JpaRepository<Entity, Id>`, which provides standard CRUD methods for free. Custom queries are derived from method names (e.g., `findAllByTitleContainingIgnoreCaseOrderByCreatedAtDesc`) — Spring Data parses the name and generates the SQL automatically. `@Transactional` wraps service methods in database transactions. `@Transactional(readOnly = true)` is used for read-only methods as a performance hint. `FetchType.LAZY` on relationships avoids N+1 query problems by loading associations only when accessed. `@CreationTimestamp` sets timestamp fields automatically on insert.

#### Lombok
Used throughout the backend to eliminate boilerplate. `@Getter`/`@Setter` generate accessor methods. `@Builder` generates a builder class for constructing entities without long constructor calls. `@RequiredArgsConstructor` generates a constructor for all `final` fields — this is how Spring injects dependencies without needing `@Autowired`. `@Slf4j` injects a `log` field backed by SLF4J.

#### JJWT
Used in `JwtService` to generate signed JWT tokens containing the username (`sub` claim) and role (`role` claim). Tokens expire after 10 hours. Validation verifies the HMAC-SHA256 signature and checks expiry. `Keys.hmacShaKeyFor(bytes)` creates the signing key from the hardcoded secret string.

---

### Frontend

| Layer | Technology | Version | Purpose |
|---|---|---|---|
| Language | JavaScript (JSX) | ES2022+ | Primary frontend language |
| UI Framework | React | 19.2.4 | Component-based UI, virtual DOM |
| Routing | React Router | 7.14.2 | Client-side navigation, URL params |
| Build Tool | Vite | 8.0.0 | Dev server, HMR, production bundling |
| HTTP Client | Axios | 1.13.6 | API calls with interceptors |
| Styling | Inline styles | — | Per-component style objects |
| E2E Testing | Cypress | 15.15.0 | Browser-based end-to-end tests |
| Linting | ESLint | 9.39.4 | Code quality |

#### React
The UI is built entirely from function components using React hooks. No class components are used. Key hooks:
- `useState` — local component state (form fields, loading flags, error messages, data arrays)
- `useEffect` — side effects (data fetching on mount, re-fetching when dependencies change)
- `useCallback` — memoizing functions to stabilize `useEffect` dependency arrays
- `useMemo` — memoizing derived values (sorted comment list)
- `useContext` / `useAuth` — reading shared auth state from `AuthContext`
- `useParams` — reading URL path parameters (bug ID from `/bugs/:id`)
- `useNavigate` — programmatic navigation after mutations

#### React Router
Defines the client-side route table in `App.jsx`. Public routes render their pages directly. Protected routes are wrapped in `<PrivateRoute role="...">` which reads from `AuthContext` and redirects to `/login` if unauthorized. The `<Navbar>` is rendered outside `<Routes>` so it persists across all pages.

#### Vite
The dev server runs on port 5173 (or 5174 if that port is taken) with Hot Module Replacement — edits appear in the browser without a full page reload. Production builds are output to `dist/`. `import.meta.env.VITE_*` is used for environment variables in config files.

#### Axios
A single shared instance in `src/api/axios.js` is configured with `baseURL: "http://localhost:8081/api"`. A request interceptor reads the JWT from `localStorage` and injects it as `Authorization: Bearer <token>` on every outgoing request. All team members import this same instance — no one creates their own.

#### ESLint
Configured in `eslint.config.js`. Catches unused variables, undefined references, and React hook rule violations during development.

---

### Notification Microservice

| Technology | Version | Purpose |
|---|---|---|
| Java | 25 | Language |
| Spring Boot | 4.0.6 | Application container |
| Twilio SDK | 9.13.0 | SMS delivery |
| JavaMail | (via Boot) | Email delivery |
| Gradle | — | Build |

Runs on port 8082. Called by the main backend's `UserServiceImpl.banUser()` via `RestTemplate`. Receives `{ username, email, phoneNumber }` and sends ban notification via both email (Mailpit in dev) and SMS (Twilio).

---

## 4. Project Structure

```
bugreporter/
├── src/main/java/com/group11/bugreporter/
│   ├── BugreporterApplication.java        # Entry point, startup sequence
│   ├── config/
│   │   ├── DemoData.java                  # JSON seeding on startup
│   │   ├── DockerComposeBootstrap.java    # Auto-starts Docker if needed
│   │   ├── JacksonConfig.java             # ObjectMapper with JavaTimeModule
│   │   └── SecurityConfig.java            # Filter chain, CORS, BCrypt bean
│   ├── controller/
│   │   ├── AuthController.java            # POST /api/auth/login|register
│   │   ├── BugController.java             # /api/bugs/**
│   │   ├── CommentController.java         # /api/comments/**
│   │   ├── UserController.java            # /api/users/**
│   │   └── AdminController.java           # /api/admin/**
│   ├── service/
│   │   ├── AuthService.java               # Login, register logic
│   │   ├── BugService.java                # Bug CRUD, tags, status
│   │   ├── CommentService.java            # Comment CRUD, voting
│   │   ├── UserService.java               # Interface
│   │   └── UserServiceImpl.java           # User CRUD, ban, notification call
│   ├── repository/
│   │   ├── UserRepository.java
│   │   ├── BugRepository.java             # Custom filter/search queries
│   │   ├── CommentRepository.java         # Score-ordered query, lock query
│   │   ├── CommentVoteRepository.java     # Vote lookup, batch vote fetch
│   │   └── TagRepository.java             # Find-or-create by name
│   ├── entity/
│   │   ├── User.java                      # @PrePersist for createdAt
│   │   ├── Bug.java                       # @ManyToMany tags, @ManyToOne author
│   │   ├── Comment.java                   # denormalized score field
│   │   ├── CommentVote.java               # unique(comment_id, user_id)
│   │   ├── Tag.java
│   │   └── enums/
│   │       ├── Role.java                  # USER, MODERATOR, ADMIN
│   │       ├── BugStatus.java             # OPEN, IN_PROGRESS, FIXED, CLOSED
│   │       └── VoteType.java              # UPVOTE, DOWNVOTE
│   ├── dto/
│   │   ├── request/
│   │   │   ├── LoginRequest.java
│   │   │   ├── RegisterRequest.java
│   │   │   ├── BugRequest.java            # @NotBlank, @Size validation
│   │   │   ├── CommentRequest.java        # @NotBlank, @Size, @URL
│   │   │   ├── UserRequest.java
│   │   │   └── VoteRequest.java
│   │   └── response/
│   │       ├── AuthResponse.java          # { token }
│   │       ├── BugResponse.java           # fromEntity() static factory
│   │       ├── CommentResponse.java       # fromEntity(comment, voteType)
│   │       └── UserResponse.java          # no password field
│   ├── security/
│   │   ├── JwtService.java                # Token generation, validation
│   │   └── JwtAuthenticationFilter.java   # Per-request JWT check
│   └── exception/
│       ├── GlobalExceptionHandler.java    # @RestControllerAdvice
│       ├── ResourceNotFoundException.java # → 404
│       ├── ForbiddenException.java        # → 403
│       └── InvalidVoteTypeException.java  # → 400
├── src/main/resources/
│   ├── application.properties
│   ├── application-reset.properties       # ddl-auto=create for Reset DB config
│   └── data/
│       ├── PlaceholderDataUsers.json
│       ├── PlaceholderDataBugs.json
│       └── PlaceholderDataComments.json
│
├── bugreporter-frontend/
│   ├── src/
│   │   ├── api/axios.js                   # Shared Axios instance
│   │   ├── context/AuthContext.jsx        # JWT decode, login/logout, persistence
│   │   ├── routes/PrivateRoute.jsx        # Auth + role guard
│   │   ├── App.jsx                        # Route table
│   │   ├── pages/
│   │   │   ├── Home.jsx
│   │   │   ├── Login.jsx
│   │   │   ├── Register.jsx
│   │   │   ├── Admin.jsx
│   │   │   ├── Moderator.jsx
│   │   │   ├── BugList.jsx                # List, search, filter, create
│   │   │   └── BugDetail.jsx              # Detail, edit, delete, comment form
│   │   ├── components/
│   │   │   ├── Navbar.jsx
│   │   │   ├── BugCard.jsx
│   │   │   ├── CommentCard.jsx            # Vote, edit, delete per comment
│   │   │   ├── CommentSort.jsx
│   │   │   └── AdminUsersTable.jsx        # Reused by Admin and Moderator
│   │   └── services/
│   │       ├── bugService.js              # Bug API functions
│   │       └── commentService.js          # Comment API functions (partial use)
│   ├── cypress/
│   │   └── e2e/
│   │       ├── auth.cy.js
│   │       ├── bugs.cy.js
│   │       └── comments.cy.js
│   ├── cypress.config.js
│   ├── vite.config.js
│   └── package.json
│
├── notification-service/                  # Separate Gradle microservice
├── docker-compose.yml                     # PostgreSQL + Mailpit
└── .idea/runConfigurations/
    └── BugreporterApplication__Reset_DB_.xml
```

---

## 5. Database Schema

### Tables and Relationships

```
users
├── id (PK)
├── username (UNIQUE)
├── email (UNIQUE)
├── password (BCrypt hash)
├── role (USER | MODERATOR | ADMIN)
├── banned
├── created_at
├── phone_number (UNIQUE, nullable)
└── score (default 0.0)

bugs
├── id (PK)
├── title
├── text
├── image_url (nullable)
├── status (OPEN | IN_PROGRESS | FIXED | CLOSED)
├── created_at
├── author_id (FK → users.id)
└── vote_score (default 0)

tags
├── id (PK)
└── name (UNIQUE)

bug_tags  [join table, many-to-many]
├── bug_id (FK → bugs.id)
└── tag_id (FK → tags.id)

comments
├── id (PK)
├── text
├── image_url (nullable)
├── created_at
├── score (denormalized vote sum, default 0)
├── author_id (FK → users.id)
└── bug_id (FK → bugs.id)

comment_votes
├── id (PK)
├── vote_type (UPVOTE | DOWNVOTE)
├── created_at
├── user_id (FK → users.id)
└── comment_id (FK → comments.id)
UNIQUE (comment_id, user_id)

bug_votes
├── id (PK)
├── vote_type (UPVOTE | DOWNVOTE)
├── created_at
├── user_id (FK → users.id)
└── bug_id (FK → bugs.id)
UNIQUE (bug_id, user_id)
```

### Key Design Decisions

**Denormalized score on `comments`:** Instead of counting votes on every read, the `score` column is kept in sync: +1/-1 on new vote, ±2 on vote switch, reversed on vote removal. A pessimistic write lock (`SELECT FOR UPDATE`) during vote operations prevents race conditions when two users vote simultaneously.

**Tags as a shared table:** Tags are not duplicated per bug. `"auth"` exists once in `tags` and is linked to all bugs that have it via `bug_tags`. This makes tag-based filtering efficient and keeps tag names consistent.

**Enum columns stored as strings (`EnumType.STRING`):** All enum values (`role`, `status`, `vote_type`) are stored as their string names, not integer ordinals. This makes the database readable without code and prevents breakage if enum order changes.

---

## 6. API Reference

All endpoints are prefixed with `/api`. The frontend's Axios instance uses `http://localhost:8081/api` as the base URL.

### Authentication — `/api/auth`
> All endpoints public (no JWT required)

| Method | Path | Request Body | Response | Description |
|---|---|---|---|---|
| POST | `/auth/login` | `{ username, password }` | `{ token }` | Validate credentials, return JWT |
| POST | `/auth/register` | `{ username, email, password, phoneNumber? }` | `{ token }` | Create account, return JWT |

### Users — `/api/users`
> Mostly unguarded (used for internal/testing purposes; admin operations use `/api/admin`)

| Method | Path | Auth | Response | Description |
|---|---|---|---|---|
| POST | `/users` | No | `UserResponse` | Create user |
| GET | `/users` | No | `UserResponse[]` | List all users |
| GET | `/users/{id}` | No | `UserResponse` | Get by ID |
| PUT | `/users/{id}` | No | `UserResponse` | Update user |
| DELETE | `/users/{id}` | No | — | Delete user |
| PUT | `/users/{id}/ban` | ADMIN or MODERATOR | `UserResponse` | Ban user |
| PUT | `/users/{id}/unban` | ADMIN or MODERATOR | `UserResponse` | Unban user |

### Admin — `/api/admin`
> All require JWT. Role checked via `@PreAuthorize`.

| Method | Path | Role Required | Response | Description |
|---|---|---|---|---|
| GET | `/admin/users` | ADMIN or MODERATOR | `UserResponse[]` | All users |
| GET | `/admin/bugs` | ADMIN | `BugResponse[]` | All bugs |
| GET | `/admin/comments` | ADMIN | `CommentResponse[]` | All comments |
| PUT | `/admin/users/{id}/ban?banned=true\|false` | ADMIN or MODERATOR | `UserResponse` | Toggle ban |
| PUT | `/admin/users/{id}/role?role=ADMIN\|MODERATOR\|USER` | ADMIN | `UserResponse` | Change role |

### Bugs — `/api/bugs`
> GET endpoints are public. Write endpoints require JWT.

| Method | Path | Auth | Response | Description |
|---|---|---|---|---|
| GET | `/bugs` | No | `BugResponse[]` | All bugs, newest first |
| GET | `/bugs/{id}` | No | `BugResponse` | Single bug |
| GET | `/bugs/filter?search=...` | No | `BugResponse[]` | Search by title (case-insensitive, substring) |
| GET | `/bugs/filter?tag=...` | No | `BugResponse[]` | Filter by tag name (case-insensitive) |
| GET | `/bugs/filter?userId=...` | No | `BugResponse[]` | Bugs by user ID |
| GET | `/bugs/filter?mine=true` | Yes | `BugResponse[]` | Current user's bugs |
| POST | `/bugs` | Yes | `BugResponse` (201) | Create bug (status always OPEN) |
| PUT | `/bugs/{id}` | Yes (author only) | `BugResponse` | Update title/text/imageUrl |
| DELETE | `/bugs/{id}` | Yes (author or mod) | 204 No Content | Delete bug |
| POST | `/bugs/{id}/tags` | Yes | `BugResponse` | Add tags (raw `["tag1","tag2"]` body) |
| PATCH | `/bugs/{id}/resolve` | Yes (author only) | `BugResponse` | Set status to FIXED |
| PATCH | `/bugs/{id}/status?status=...` | MODERATOR | `BugResponse` | Set any status |
| POST | `/bugs/{id}/vote` | Yes | `BugResponse` | Vote on bug; `{ voteType: "UPVOTE"\|"DOWNVOTE" }` |

### Comments — `/api/comments`
> GET is public. All write operations require JWT.

| Method | Path | Auth | Response | Description |
|---|---|---|---|---|
| GET | `/comments/bug/{bugId}` | Optional | `CommentResponse[]` | All comments for bug; includes `userVote` if authenticated |
| POST | `/comments/bug/{bugId}` | Yes | `CommentResponse` (201) | Create comment; flips bug OPEN→IN_PROGRESS |
| PUT | `/comments/{commentId}` | Yes (author or mod) | `CommentResponse` | Update text/imageUrl |
| DELETE | `/comments/{commentId}` | Yes (author or mod) | 204 No Content | Delete comment |
| POST | `/comments/{commentId}/vote` | Yes | `CommentResponse` | Vote; `{ voteType: "UPVOTE"\|"DOWNVOTE" }` |

### Response Shapes

**`AuthResponse`**
```json
{ "token": "eyJ..." }
```

**`BugResponse`**
```json
{
  "id": 1,
  "title": "Login button broken",
  "text": "Clicking login does nothing.",
  "imageUrl": null,
  "status": "OPEN",
  "createdAt": "2026-05-01T12:00:00",
  "authorUsername": "alice",
  "tags": ["auth", "ui"],
  "voteScore": 3,
  "userVote": "UPVOTE",
  "authorScore": 12.5
}
```

**`CommentResponse`**
```json
{
  "id": 10,
  "text": "I found the issue.",
  "imageUrl": null,
  "bugId": 1,
  "authorId": 3,
  "authorUsername": "alice",
  "createdAt": "2026-05-02T09:30:00",
  "score": 5,
  "userVote": "UPVOTE",
  "authorScore": 47.5
}
```

**`UserResponse`**
```json
{
  "id": 1,
  "username": "ana",
  "email": "ana@example.com",
  "role": "ADMIN",
  "banned": false,
  "createdAt": "2026-03-01T09:00:00",
  "phoneNumber": "07xxxxxxxx",
  "score": 0.0
}
```

---

## 7. Backend — Auth & Security

### Startup Sequence

On every `main()` call:
1. `DockerComposeBootstrap.bootstrapIfNecessary()` — probes ports 5436 (PostgreSQL) and 1025 (Mailpit). If either is closed, finds the nearest `docker-compose.yml` walking up from the working directory and runs `docker compose up -d`. Waits up to 60 seconds for ports to open.
2. `SpringApplication.run(...)` — starts Spring context, applies Hibernate `ddl-auto=update` (creates or updates schema to match entities without deleting data).
3. `DemoData::loadDemoData` — seeds demo users, bugs, and comments from JSON (see Section 10).

### JWT Token Lifecycle

**Generation** (`JwtService.generateToken`):
- Payload contains `sub` (username) and `role` claim
- Signed with HMAC-SHA256 using a fixed secret key
- Expires in 10 hours

**Storage** (frontend):
- `localStorage["token"]` — raw JWT string
- `localStorage["user"]` — decoded `{ username, role }` JSON to avoid re-parsing on every render

**Validation** (per request, `JwtAuthenticationFilter`):
1. Read `Authorization: Bearer <token>` header
2. Skip `/api/auth/**` entirely
3. Extract `username` from token payload
4. Look up user in database (confirms account still exists)
5. Reject with 403 if user is banned
6. Validate signature and expiry
7. On success: place `UsernamePasswordAuthenticationToken` in `SecurityContextHolder` with `ROLE_<role>` authority

**Frontend decoding** (`AuthContext.parseUserFromToken`):
- Split on `.`, take middle segment
- Normalize URL-safe base64 (replace `-`→`+`, `_`→`/`, add padding)
- `atob()` decode, `JSON.parse()`, extract `sub` and `role`
- Normalize role: strip `ROLE_` prefix, uppercase

### Security Filter Chain

```
STATELESS session → no cookies, no CSRF
↓
CORS check (origins: localhost:5173, localhost:5174)
↓
JwtAuthenticationFilter (for all non-auth routes)
  ├── no token → pass through (public endpoints work, protected ones → 401)
  └── valid token → populate SecurityContextHolder
↓
Spring Security authorization rules
  ├── /api/auth/**          → permitAll
  ├── /api/comments/bug/**  → permitAll (GET read)
  ├── /api/users            → permitAll
  ├── GET /api/bugs/**      → permitAll
  ├── /api/bugs/**          → authenticated
  └── everything else       → authenticated
↓
@PreAuthorize on methods (fine-grained role checks)
  └── e.g. hasRole('ADMIN') checks for ROLE_ADMIN authority in SecurityContext
```

### Password Storage

BCrypt is used for all password hashing. BCrypt generates a random salt per hash, so the same password produces different hashes on every call. `passwordEncoder.matches(rawPassword, storedHash)` is the only correct way to verify — never compare raw strings. The hash is stored in `users.password`. It is never included in any response DTO.

### Banned User Enforcement

Enforced in two places:
1. `AuthService.authenticate` — login is rejected before a token is issued
2. `JwtAuthenticationFilter` — if a user is banned after login but their token has not expired, every subsequent request returns 403. This means banning takes effect immediately without needing to wait for token expiry.

### Role Hierarchy (Permission Summary)

| Action | USER | MODERATOR | ADMIN |
|---|---|---|---|
| Read bugs/comments | ✓ | ✓ | ✓ |
| Create bug/comment | ✓ | ✓ | ✓ |
| Edit own bug | ✓ | ✓ | ✓ |
| Delete own bug/comment | ✓ | ✓ | ✓ |
| Delete any bug/comment | ✗ | ✓ | ✓ |
| Edit any comment | ✗ | ✓ | ✓ |
| Change bug status | ✗ | ✓ | ✓ |
| View users table | ✗ | ✓ | ✓ |
| Ban / unban | ✗ | ✓ (non-admin/mod only) | ✓ |
| Change roles | ✗ | ✗ | ✓ |
| View all bugs/comments (admin panel) | ✗ | ✗ | ✓ |

---

## 8. Backend — Bugs

### Bug Status Lifecycle

```
CREATE → OPEN
         │
         │ (first comment posted)
         ↓
      IN_PROGRESS
         │
         ├─ author clicks Resolve → FIXED
         └─ moderator dropdown   → CLOSED (or any other status)
```

Bugs with status `FIXED` or `CLOSED` reject new comments. The status flip from OPEN to IN_PROGRESS happens automatically in `CommentService.createComment` — the frontend reflects this by re-fetching the bug after a successful comment post.

### Repository Query Derivation

Spring Data JPA generates SQL from method names. The rules:
- `findAllBy` → `SELECT * FROM`
- `Tags_Name` → `JOIN bug_tags, tags WHERE tags.name`
- `TitleContaining` → `WHERE title LIKE '%?%'`
- `IgnoreCase` → case-insensitive comparison
- `Author_Id` → `WHERE author_id = ?`
- `OrderByCreatedAtDesc` → `ORDER BY created_at DESC`

| Repository Method | Query |
|---|---|
| `findAllByOrderByCreatedAtDesc()` | All bugs, newest first |
| `findAllByTitleContainingIgnoreCaseOrderByCreatedAtDesc(title)` | Substring title search |
| `findAllByTags_NameIgnoreCaseOrderByCreatedAtDesc(tag)` | Filter by tag name |
| `findAllByAuthor_IdOrderByCreatedAtDesc(authorId)` | Filter by author |

### Tag Find-or-Create

When tags are added to a bug, each tag name is looked up in the `tags` table. If found, the existing record is reused. If not found, a new `Tag` is created and saved. This prevents duplicate tag rows and makes tags case-insensitively shared across all bugs.

```java
tagRepository.findByNameIgnoreCase(name.trim())
    .orElseGet(() -> tagRepository.save(Tag.builder().name(name.trim()).build()))
```

### Permission Enforcement (Service Layer)

- **Edit bug:** only the author. Role is checked in service, not just controller.
- **Delete bug:** author OR moderator OR admin.
- **Resolve bug:** only the author. Moderators use the status endpoint instead.
- **Change status:** moderator only, enforced by `@PreAuthorize("hasRole('MODERATOR')")` on the controller method. No service-level check — the controller guard is sufficient.

### BugResponse — fromEntity

The `fromEntity` static method is the single point of conversion from the internal `Bug` entity to the public API shape. It accesses lazy fields (`author.getUsername()`, `tags`) inside the transaction boundary so Hibernate can load them. The entity's `BugStatus` enum is converted to its string name. Tags are extracted as a `Set<String>` of names. If `tags` is null (possible on a partially initialized entity), an empty set is returned instead of crashing.

---

## 9. Backend — Comments & Voting

### Comment Score — Denormalization

The `score` column on `comments` is the sum of all upvotes minus all downvotes. It is kept in sync manually in `CommentService.voteComment` rather than computed by aggregating `comment_votes` on every read. This trades write complexity for read efficiency — a single column read vs. a GROUP BY query.

### Pessimistic Locking on Votes

`CommentRepository.findByIdForUpdate` issues `SELECT ... FOR UPDATE` before any vote operation. This prevents lost updates when two requests vote on the same comment at the same time. The second request blocks until the first transaction commits, then reads the already-updated score.

### Vote Logic — Three Cases

| Previous Vote | New Vote | Action | Score Delta |
|---|---|---|---|
| None | UPVOTE | Create new `CommentVote` | +1 |
| None | DOWNVOTE | Create new `CommentVote` | -1 |
| UPVOTE | UPVOTE | Delete `CommentVote` (toggle off) | -1 |
| DOWNVOTE | DOWNVOTE | Delete `CommentVote` (toggle off) | +1 |
| DOWNVOTE | UPVOTE | Update `CommentVote.voteType` | +2 |
| UPVOTE | DOWNVOTE | Update `CommentVote.voteType` | -2 |

Switching votes is ±2 because the old vote must be undone (+1 or -1) and the new vote applied (+1 or -1), totaling two units of change.

### User Vote State in Response

When an authenticated user fetches comments, the backend fetches all of that user's votes for those comments in a single query (`findByUserIdAndCommentIdIn`). A `Map<commentId, VoteType>` is built. Each `CommentResponse` is then populated with the user's vote type for that comment, or null if they have not voted. This lets the frontend highlight the active vote button without making separate requests.

### Comment Permissions

| Action | Who Can |
|---|---|
| Create comment | Authenticated user, bug not FIXED/CLOSED |
| Edit comment | Author OR MODERATOR/ADMIN |
| Delete comment | Author OR MODERATOR/ADMIN |
| Vote on comment | Authenticated user who is NOT the comment's author |

All permission checks are enforced in the service layer after the controller resolves the current user.

---

## 10. Backend — Data Seeding

### Profile-Gated Seeding

`DemoData` is annotated `@Profile("dev")`. It only activates when the `dev` Spring profile is active (`spring.profiles.active=dev` in `application.properties`). The `Reset DB` run configuration adds the `reset` profile, which sets `ddl-auto=create` (drops and recreates all tables) before seeding runs fresh.

### JSON Files

Three files in `src/main/resources/data/`:

**`PlaceholderDataUsers.json`** — user objects with `id`, `username`, `email`, `password` (plaintext), `role`, `banned`, `createdAt`. The `id` values are source IDs used to link bugs and comments to users — they are not forced into the database.

**`PlaceholderDataBugs.json`** — bug objects with `id`, `title`, `text`, `status`, `createdAt`, and `author: { id: N }` referencing a user source ID.

**`PlaceholderDataComments.json`** — comment objects with `id`, `text`, `score`, `createdAt`, `author: { id: N }`, and `bug: { id: N }`.

### Seeding Pipeline

```
loadDemoData()
│
├─ 1. seedUsers(comments, bugs)
│      For each desired user:
│        if already exists by ID → skip, record mapping
│        if already exists by username → skip, record mapping
│        if has plaintext password → AuthService.register() → BCrypt hash it
│        else → save with pre-hashed password
│      Returns Map<sourceId → persistedId>
│
├─ 2. seedBugs(bugs, comments, userIdMapping)
│      For each desired bug:
│        if already exists by ID → skip, record mapping
│        resolve authorId through userIdMapping
│        bugRepository.save(normalizedBug)
│      Returns Map<sourceBugId → persistedBugId>
│
└─ 3. seedComments(comments, userIdMapping, bugIdMapping)
       if comments table not empty → skip entirely
       for each comment:
         resolve authorId and bugId through mappings
         skip if either cannot be resolved
         force id = null (let DB generate fresh ID)
         commentRepository.saveAll(validComments)
```

### Idempotency

Users and bugs are skipped if they already exist (by ID or username). Comments are skipped entirely if the table is non-empty. Running `BugreporterApplication` multiple times does not duplicate data. Running `BugreporterApplication (Reset DB)` drops all tables first, so a full fresh seed always runs.

---

## 11. Frontend — Foundation & Routing

### AuthContext

`src/context/AuthContext.jsx` wraps the entire app via `<AuthProvider>`. Any component can call `useAuth()` to get:
- `token` — raw JWT string or null
- `user` — `{ username, role }` or null
- `isAuthenticated` — boolean (`Boolean(token)`)
- `login(token)` — saves to localStorage, decodes user, updates state, returns decoded user
- `logout()` — clears localStorage, nulls state

Both `token` and `user` are initialized lazily from `localStorage` so sessions persist across browser refreshes.

### Private Routes

`src/routes/PrivateRoute.jsx` wraps protected pages:

```jsx
<PrivateRoute role="ADMIN">
  <Admin />
</PrivateRoute>
```

Checks in order:
1. Not authenticated → redirect to `/login`
2. Wrong role → redirect to `/`
3. Passes → render children

### Route Table (`App.jsx`)

| Path | Component | Guard |
|---|---|---|
| `/` | `Home` | None |
| `/login` | `Login` | None |
| `/register` | `Register` | None |
| `/bugs` | `BugList` | None |
| `/bugs/:id` | `BugDetail` | None |
| `/admin` | `Admin` | ADMIN only |
| `/moderator` | `Moderator` | MODERATOR only |

### Navbar Conditional Rendering

The `Navbar` reads `isAuthenticated`, `user.role`. Links shown:
- Always: Bugs, Home
- Unauthenticated: Login, Register
- ADMIN: Admin link
- MODERATOR: Moderator link
- Authenticated: Logout button

### Login Flow

1. User submits form → `POST /api/auth/login`
2. Response contains JWT
3. `login(token)` called → token saved, user decoded
4. Navigate based on role: ADMIN → `/admin`, MODERATOR → `/moderator`, USER → `/`

### Register Flow

Same as login but calls `POST /api/auth/register`. Always redirects to `/` regardless of role (new accounts are always USER).

---

## 12. Frontend — Bugs

### BugList Page (`/bugs`)

**State:** `bugs[]`, `search`, `tag`, `userId`, `showNewForm`, `form`, `loading`, `error`

**`loadBugs(params)`:** Central fetch function. Calls `filterBugs(params)` if params given, else `getBugs()`. Called on mount via `useEffect(loadBugs, [])` and after every filter/create action.

**Search:** Controlled input tied to `search` state. On form submit: `loadBugs({ search: "..." })` → `GET /bugs/filter?search=...`. Backend does `LIKE '%?%'` case-insensitively.

**Tag filter:** Two entry points:
- Manual input + "Filter Tag" button → `loadBugs({ tag: inputValue })`
- Clicking a tag chip on a bug card → `setTag(bugTag)` then `loadBugs({ tag: bugTag })` immediately

**My Bugs:** `loadBugs({ mine: true })` → backend resolves current user from JWT.

**Create form:** Hidden by default, toggled by "Report Bug" button (authenticated only). Form state uses spread pattern to update individual fields: `setForm({ ...form, title: e.target.value })`. On submit: `createBug(form)` → reload list.

**List rendering:** `bugs.map(bug => <article key={bug.id}>...)`. `key` is required by React to efficiently diff list changes. `bug.tags || []` guards against null tags.

### BugDetail Page (`/bugs/:id`)

**Data loading:** `useCallback` wraps `loadBug` so it only changes when `id` changes. `useEffect([loadBug])` runs it on mount and on bug ID change. `Promise.all([getBug(id), api.get('/comments/bug/id')])` loads bug and comments simultaneously.

**Permission booleans:**
```
isModerator = role === "MODERATOR" || "ADMIN"
isAuthor    = username === bug.authorUsername
canEdit     = authenticated && isAuthor
canDelete   = authenticated && (isAuthor || isModerator)
canResolve  = isAuthor && status !== FIXED && status !== CLOSED
```

**Edit flow:** `editing` state toggles between display view and form. On save: `updateBug(id, form)` → `setBug(response.data)`.

**Moderator status dropdown:** `<select>` onChange fires immediately → `updateBugStatus(id, value)` → `setBug(response.data)`.

**Comment create form:** Shown when `isAuthenticated && status !== FIXED && status !== CLOSED`. After submit: prepend new comment to list, re-fetch bug (catches status flip).

### Bug Service (`services/bugService.js`)

All functions return Axios promises. Callers `await` them and read `.data`.

```js
getBugs()                     // GET /bugs
filterBugs(params)            // GET /bugs/filter?key=value
getBug(id)                    // GET /bugs/{id}
createBug(payload)            // POST /bugs
updateBug(id, payload)        // PUT /bugs/{id}
deleteBug(id)                 // DELETE /bugs/{id}
addBugTags(id, tags)          // POST /bugs/{id}/tags  (raw array body)
resolveBug(id)                // PATCH /bugs/{id}/resolve
updateBugStatus(id, status)   // PATCH /bugs/{id}/status?status=VALUE
```

---

## 13. Frontend — Comments & Voting

### Comment Sorting

Sorting is client-side only. `useMemo` recomputes the sorted array when `comments` or `sortOrder` changes. Four options: newest first, oldest first, highest score, lowest score. The backend returns comments ordered by score desc then date desc — the frontend overrides this with its own sort.

### CommentCard Component

Each comment renders its own `CommentCard`. All interaction state is local to the card:

| State | Purpose |
|---|---|
| `vote` | Current user's vote: `"UPVOTE"`, `"DOWNVOTE"`, or null |
| `score` | Current score (updated optimistically on vote) |
| `isEditing` | Whether inline edit textarea is shown |
| `editText` | Live value in edit textarea |
| `displayText` | What is shown when not editing |
| `showMenu` | Whether "..." dropdown is open |
| `showDeleteConfirm` | Whether delete confirmation modal is shown |

### Vote Buttons — Own Comment Check

```jsx
{!isOwn && (
  <>
    <button aria-label="Upvote" onClick={() => handleVote("UPVOTE")}>▲</button>
    <button aria-label="Downvote" onClick={() => handleVote("DOWNVOTE")}>▼</button>
  </>
)}
```

Vote buttons are not rendered at all for the comment's author. The score span always shows.

### Vote Toggle Logic (Frontend)

The `handleVote` function updates score optimistically (before the server confirms):

```
removing = (vote === type)  // clicking same vote twice
if removing:
  score += type === UPVOTE ? -1 : +1
  vote = null
else if vote !== null:      // switching sides
  score += type === UPVOTE ? +2 : -2
  vote = type
else:                       // first vote
  score += type === UPVOTE ? +1 : -1
  vote = type
```

### Edit Controls — Own Comment vs Privileged

```
canEdit = isOwn || isPriviledged
```

`canEdit` gates the "..." dropdown button and the standalone Edit button. The "..." dropdown contains both Edit and Delete in a floating menu. The standalone Edit button at the bottom is always visible when `canEdit` is true (toggles to Submit when editing).

### Delete Confirmation Modal

`position: fixed; inset: 0` overlay covers the whole viewport. Cancel sets `showDeleteConfirm = false`. Confirm calls `DELETE /api/comments/{id}` → calls `onDelete(id)` prop → parent filters the comment out of the list.

### Comment Create — Status Flip

After posting a comment, `handleCommentSubmit` re-fetches the bug:
```js
const res = await api.post(`/comments/bug/${id}`, payload);
setComments(prev => [res.data, ...prev]);
setCommentText(""); setCommentImageUrl("");
const bugRes = await getBug(id);
setBug(bugRes.data);
```

The second `getBug` call is necessary because the backend flips bug status from OPEN to IN_PROGRESS as a side effect of comment creation. The comment response does not include the updated bug.

---

## 14. Styling

All styling is done with **inline style objects** directly in component JSX. No CSS files, no external CSS framework for component styles.

Style objects are defined as `const` at the top of each component or function body:

```jsx
const buttonStyle = {
  padding: "8px 14px",
  border: "1px solid #333",
  backgroundColor: "#f3f3f3",
  color: "#000",
  cursor: "pointer",
};
```

Applied via the `style` prop: `<button style={buttonStyle}>`. For variations, spread syntax is used: `<button style={{ ...buttonStyle, color: "red" }}>`.

**TailwindCSS** is installed (`v4.2.1`) but not actively used for component styles — it was included in the initial project setup. The `vite.config.js` and PostCSS config reference it.

**Styling conventions applied:**
- Neutral color palette: `#f3f3f3` backgrounds, `#333` borders, `#000` text, `#ddd` dividers
- Status badges rendered as bordered `<span>` elements
- Error messages in red, metadata text in `#666`
- `position: absolute` for dropdown menus, `position: fixed` for modals
- Responsive layout via `flexWrap: "wrap"` on filter bars

---

## 15. Testing

### Backend Tests

Located in `src/test/java/com/group11/bugreporter/`. Three packages:
- `config/` — security and config tests
- `controller/` — controller endpoint tests
- `service/` — service logic tests

Run with:
```bash
./mvnw test
./mvnw test -Dtest=ClassName
./mvnw test -Dtest=ClassName#methodName
```

### Frontend E2E Tests (Cypress)

Three spec files in `cypress/e2e/`. All API calls are intercepted with `cy.intercept()` — no real backend required.

**`auth.cy.js`** — uses real network calls. Tests register, login, wrong password, banned user, admin ban, admin role change, unauthenticated redirect.

**`bugs.cy.js`** — fully mocked. Tests bug list load, search, tag filter, create, edit, delete, resolve, moderator status change, permission visibility.

**`comments.cy.js`** — fully mocked. Tests unauthenticated view, post comment, status flip, FIXED bug block, upvote toggle, downvote-then-upvote, own comment vote buttons, edit, delete, moderator access.

**Selector strategy:** `aria-label` attributes on interactive elements (`aria-label="Upvote"`, `aria-label="Comment text"`, `aria-label="Edit comment text"`), `data-testid` on containers (`data-testid="comment-card"`, `data-testid="comment-score"`). `.within()` scopes selectors when multiple identical elements exist on the page.

**`tokenFor` / `loginAs` helpers** (bugs.cy.js, comments.cy.js): craft a fake-but-decodable JWT and set both `localStorage["token"]` and `localStorage["user"]` before the page loads, bypassing the real login flow.

Run commands:
```bash
cd bugreporter-frontend
npx cypress run                                      # all specs headless
npx cypress run --spec=cypress/e2e/comments.cy.js   # single spec
npx cypress open                                     # interactive UI
```

---

## 16. Running the Project

### Prerequisites
- Java 21
- Node.js 18+
- Docker + Docker Compose

### Backend

```bash
# Start database and mail server (or let the app do it automatically)
docker-compose up -d

# Run with existing data
./mvnw spring-boot:run

# Or use IntelliJ run configs:
#   "BugreporterApplication"           → normal start
#   "BugreporterApplication (Reset DB)"→ drops all tables, re-seeds
```

Backend runs on `http://localhost:8081`.

### Frontend

```bash
cd bugreporter-frontend
npm install
npm run dev        # dev server at http://localhost:5173
npm run build      # production build to dist/
npm run lint       # ESLint
```

### Notification Microservice

```bash
cd notification-service
gradle bootRun     # runs on http://localhost:8082
```

### Demo Credentials (after seeding)

| Username | Password | Role |
|---|---|---|
| `ana` | `ana` | ADMIN |
| `mihai` | `mihai` | MODERATOR |
| `rares` | `rares` | USER |
| `ioana` | (same as username) | USER |

---

## 17. Team Responsibilities

### Afrasinei Serban — Comments (Frontend & Backend) + Backend Security

**Backend:**
- `security/JwtService.java` — JWT generation, validation, claim extraction
- `security/JwtAuthenticationFilter.java` — per-request token validation, banned user blocking, `SecurityContextHolder` population
- `config/SecurityConfig.java` — filter chain configuration, CORS, BCrypt bean, `@EnableMethodSecurity`
- `service/CommentService.java` — all comment and vote business logic: create (with status flip), read (with user vote population), update, delete, vote with pessimistic locking
- `controller/CommentController.java` — all comment endpoints, vote type parsing
- `entity/Comment.java`, `entity/CommentVote.java`, `entity/enums/VoteType.java`
- `repository/CommentRepository.java` (pessimistic lock query), `repository/CommentVoteRepository.java`
- `dto/request/CommentRequest.java`, `dto/request/VoteRequest.java`, `dto/response/CommentResponse.java`

**Frontend:**
- `src/components/CommentCard.jsx` — vote buttons, vote toggle logic, edit inline, delete modal, permission checks (own vs privileged), aria-labels, data-testid attributes
- `src/components/CommentSort.jsx` — sort order dropdown
- `src/pages/BugDetail.jsx` (comments section) — comment state, `useMemo` sort, `onDelete` callback, create form with conditional rendering, status flip re-fetch
- `cypress/e2e/comments.cy.js` — all 10 comment/vote Cypress tests

---

### Berar Alexandra — Auth & Admin (Frontend & Backend) + Frontend Setup

**Backend:**
- `service/AuthService.java` — login (banned check, BCrypt verify), register (uniqueness checks, hash, token)
- `controller/AuthController.java` — login and register endpoints
- `controller/AdminController.java` — user list, bug list, comment list, ban toggle, role change
- `service/UserService.java` (interface), `service/UserServiceImpl.java` — user CRUD, ban/unban (with notification service call via RestTemplate)
- `controller/UserController.java`
- `entity/User.java`, `entity/enums/Role.java`
- `repository/UserRepository.java`
- `dto/request/LoginRequest.java`, `dto/request/RegisterRequest.java`, `dto/request/UserRequest.java`
- `dto/response/AuthResponse.java`, `dto/response/UserResponse.java`
- `exception/GlobalExceptionHandler.java`, `exception/ResourceNotFoundException.java`, `exception/ForbiddenException.java`, `exception/InvalidVoteTypeException.java`
- `config/DemoData.java` — seeding pipeline
- `config/DockerComposeBootstrap.java` — auto-start Docker on startup
- `config/JacksonConfig.java`
- `BugreporterApplication.java`

**Frontend:**
- `src/context/AuthContext.jsx` — JWT decode, login/logout, `localStorage` persistence, `useAuth` hook
- `src/routes/PrivateRoute.jsx` — auth + role guard
- `src/api/axios.js` — shared Axios instance with request interceptor
- `src/App.jsx` — route table
- `src/components/Navbar.jsx` — conditional links by auth state and role
- `src/pages/Login.jsx` — login form, role-based redirect
- `src/pages/Register.jsx` — registration form
- `src/pages/Admin.jsx` — admin panel (users, bugs, comments tables)
- `src/pages/Moderator.jsx` — moderator panel
- `src/components/AdminUsersTable.jsx` — reusable users table with ban and role change
- `cypress/e2e/auth.cy.js` — auth Cypress tests
- **Vite project setup** — `vite.config.js`, `package.json`, initial `eslint.config.js`
- **Cypress configuration** — `cypress.config.js`, initial test infrastructure
- **Initial styling reference** — base style tokens (color palette, button, input, page layout styles) adopted by all three team members

---

### Pavel Catalin — Bugs (Frontend & Backend)

**Backend:**
- `service/BugService.java` — create, read, update, delete, tag management (find-or-create), search, filter, resolve, status change
- `controller/BugController.java` — all bug endpoints, filter logic, `resolveAuthenticatedUser` helper
- `entity/Bug.java`, `entity/Tag.java`, `entity/enums/BugStatus.java`
- `repository/BugRepository.java` — all four derived query methods (search, tag filter, author filter, all ordered)
- `repository/TagRepository.java`
- `dto/request/BugRequest.java`
- `dto/response/BugResponse.java` — `fromEntity` static factory

**Frontend:**
- `src/pages/BugList.jsx` — full listing page: search, tag filter, my bugs, user filter, create form, bug list with clickable tag chips
- `src/pages/BugDetail.jsx` (bug section) — detail display, edit form, delete, resolve, moderator status dropdown, tag addition, `useCallback`/`useEffect` data loading
- `src/components/BugCard.jsx` — individual bug display card
- `src/services/bugService.js` — all bug API functions
- `cypress/e2e/bugs.cy.js` — all bug Cypress tests
