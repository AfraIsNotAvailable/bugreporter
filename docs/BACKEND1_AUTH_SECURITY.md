# Backend 1 — Auth, Security & User Management

This document covers how authentication works end-to-end on the backend: JWT token creation and validation, how every request is checked for a valid token, how the security filter chain decides what is public vs protected, and how users are created, managed, and banned.

---

## Application Startup Sequence

**File:** `BugreporterApplication.java`

```java
public static void main(String[] args) {
    DockerComposeBootstrap.bootstrapIfNecessary();
    ConfigurableApplicationContext context = SpringApplication.run(BugreporterApplication.class, args);
    context.getBeansOfType(DemoData.class)
            .values()
            .forEach(DemoData::loadDemoData);
}
```

Three things happen on every startup, in order:

1. **`DockerComposeBootstrap.bootstrapIfNecessary()`** — Before Spring even starts, this checks whether the required ports (PostgreSQL on 5436, Mailpit on 1025) are reachable. If not, it runs `docker compose up -d` automatically and waits until they come up. Configuration is read from `application.properties` under the `app.startup.docker.*` keys.

2. **`SpringApplication.run(...)`** — Starts the Spring Boot context: loads beans, applies config, starts the embedded Tomcat on port 8081, runs Hibernate schema updates.

3. **`DemoData::loadDemoData`** — Seeds demo data (see Backend 3 doc).

---

## Database Tables — User Side

### `users` table (`User.java`)

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, auto-generated |
| `username` | VARCHAR | NOT NULL, UNIQUE |
| `email` | VARCHAR | NOT NULL, UNIQUE |
| `password` | VARCHAR | NOT NULL (stores BCrypt hash, never plaintext) |
| `role` | VARCHAR | NOT NULL (enum stored as string: USER, MODERATOR, ADMIN) |
| `banned` | BOOLEAN | NOT NULL, default false |
| `created_at` | TIMESTAMP | NOT NULL, set by `@PrePersist` |
| `phone_number` | VARCHAR | NULLABLE, UNIQUE |

`@PrePersist` is a JPA lifecycle callback — it runs automatically just before an entity is first inserted into the database. Here it sets `createdAt = LocalDateTime.now()`.

`@Enumerated(EnumType.STRING)` means the role is stored as the string `"USER"`, `"MODERATOR"`, or `"ADMIN"` in the database, not as a number. This makes the database readable without needing to look up enum constants.

---

## JWT Tokens

**File:** `security/JwtService.java`

A JWT (JSON Web Token) has three parts separated by dots: `header.payload.signature`. The payload is a JSON object encoded in base64. This app puts the username and role into the payload.

### Generating a Token

```java
public String generateToken(String username, Role role) {
    return Jwts.builder()
            .subject(username)
            .claim("role", role.name())
            .issuedAt(new Date(System.currentTimeMillis()))
            .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10))
            .signWith(secretKey)
            .compact();
}
```

- `.subject(username)` — stores the username in the `sub` claim. This is how the backend knows who made the request later.
- `.claim("role", role.name())` — stores the role as a custom claim, e.g. `"ADMIN"`. The frontend reads this to decide what UI to show.
- `.expiration(...)` — token expires in 10 hours (`1000ms * 60s * 60min * 10`).
- `.signWith(secretKey)` — HMAC-SHA256 signs the token using a hardcoded secret key. Any tampering with the payload invalidates the signature. **Important:** the secret key is hardcoded in the source. In a real production deployment, this must be an environment variable.

### Validating a Token

```java
public boolean isTokenValid(String token, String username) {
    final String extractedUsername = extractUsername(token);
    return (extractedUsername.equals(username) && !isTokenExpired(token));
}
```

Two conditions must both be true:
1. The username embedded in the token matches the user we found in the database.
2. The token has not expired.

If either fails, the token is rejected and the request is treated as unauthenticated.

### Extracting Claims

```java
private Claims extractAllClaims(String token) {
    return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
}
```

The JJWT library verifies the signature during parsing. If the token was tampered with or was signed by a different key, this throws an exception and the filter catches it.

---

## JWT Authentication Filter

**File:** `security/JwtAuthenticationFilter.java`

This is the core of how every protected request is authenticated. It runs for every HTTP request that is not a public auth endpoint.

```java
public class JwtAuthenticationFilter extends OncePerRequestFilter {
```

`OncePerRequestFilter` guarantees the filter runs exactly once per request, even if the servlet chain forwards internally.

### shouldNotFilter

```java
protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getServletPath();
    return path != null && path.startsWith("/api/auth/");
}
```

Skips the filter entirely for `/api/auth/login` and `/api/auth/register`. These are the endpoints that create tokens — they cannot require a token to access.

### doFilterInternal — Step by Step

```java
final String authHeader = request.getHeader("Authorization");
if (authHeader == null || !authHeader.startsWith("Bearer ")) {
    filterChain.doFilter(request, response);
    return;
}
```

**Step 1:** Look for the `Authorization` header. If it doesn't exist or doesn't start with `Bearer `, pass the request through without authenticating. Public GET endpoints (bug list, bug detail, comments) will still work — the security config permits them. Protected endpoints will return 401 when the controller/security layer checks for authentication.

```java
jwt = authHeader.substring(7);  // strip "Bearer "
username = jwtService.extractUsername(jwt);
```

**Step 2:** Extract the username from the token. If the token is malformed, `extractUsername` throws and the filter logs it.

```java
User user = userRepository.findByUsername(username).orElse(null);
if (user == null) { filterChain.doFilter(...); return; }
```

**Step 3:** Look up the user in the database. This confirms the user still exists (they could have been deleted since the token was issued).

```java
if (user.isBanned()) {
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.getWriter().write("{\"message\": \"Your account is banned.\"}");
    return;
}
```

**Step 4:** If the user is banned, immediately return 403 and stop the request. The response is written directly — no controller is called.

```java
if (jwtService.isTokenValid(jwt, user.getUsername())) {
    UsernamePasswordAuthenticationToken authToken =
        new UsernamePasswordAuthenticationToken(
            user.getUsername(),
            null,
            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    SecurityContextHolder.getContext().setAuthentication(authToken);
}
```

**Step 5:** If the token is valid, create an `Authentication` object and put it in the `SecurityContextHolder`. This is how Spring Security knows the current user for the rest of the request lifecycle. The `ROLE_` prefix is required by Spring Security's role-checking system — `@PreAuthorize("hasRole('ADMIN')")` internally checks for authority `"ROLE_ADMIN"`.

---

## Security Configuration

**File:** `config/SecurityConfig.java`

### Filter Chain

```java
http
    .cors(cors -> cors.configurationSource(corsConfigurationSource()))
    .csrf(csrf -> csrf.disable())
    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/auth/**").permitAll()
        .requestMatchers("/api/comments/bug/**").permitAll()
        .requestMatchers("/api/users").permitAll()
        .requestMatchers(HttpMethod.GET, "/api/bugs", "/api/bugs/", "/api/bugs/*", "/api/bugs/filter").permitAll()
        .requestMatchers("/api/bugs/**").authenticated()
        .anyRequest().authenticated()
    )
    .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
```

**CSRF disabled:** Cross-Site Request Forgery protection is disabled because the API is stateless — it uses JWT headers, not cookies, so CSRF attacks don't apply.

**STATELESS session:** Spring will never create an HTTP session. Every request must carry its own JWT — the server holds no session state between requests.

**`permitAll` vs `authenticated`:** Rules are evaluated top to bottom. First match wins. All `/api/auth/**` routes are public. Bug GET requests are public. Everything else requires a valid token in the `SecurityContextHolder`.

**CORS:** Only `localhost:5173` and `localhost:5174` are allowed to make cross-origin requests. The browser enforces this via preflight OPTIONS requests.

### `@EnableMethodSecurity`

This annotation enables `@PreAuthorize` annotations on individual controller methods. Without it, role checks on methods would be silently ignored.

### Password Encoder

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

BCrypt is a slow hashing algorithm designed for passwords. It adds a random salt per password (preventing rainbow table attacks) and has a configurable cost factor. Passwords are never stored in plaintext — only the BCrypt hash is persisted in the `users.password` column.

---

## Auth Service

**File:** `service/AuthService.java`

### Login

```java
public String authenticate(LoginRequest request) {
    User user = userRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));

    if (user.isBanned()) {
        throw new RuntimeException("User is banned");
    }

    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
        throw new RuntimeException("Invalid password");
    }

    return jwtService.generateToken(user.getUsername(), user.getRole());
}
```

Three checks in order:
1. User must exist by username.
2. User must not be banned (double check — the filter also checks, but login is a public endpoint that bypasses the filter).
3. The submitted password must match the BCrypt hash via `passwordEncoder.matches()`.

On success, returns a signed JWT string. The `RuntimeException` messages are intentionally generic for security — you don't want to tell an attacker whether the username exists.

### Register

```java
public String register(RegisterRequest request) {
    if (userRepository.existsByUsername(request.getUsername())) {
        throw new IllegalArgumentException("Username is already taken");
    }
    if (userRepository.existsByEmail(request.getEmail())) {
        throw new IllegalArgumentException("Email is already in use");
    }
    if (request.getPhoneNumber() != null &&
            userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
        throw new IllegalArgumentException("Phone number is already in use");
    }

    User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .role(Role.USER)
            .banned(false)
            .phoneNumber(request.getPhoneNumber())
            .build();
    userRepository.save(user);

    return jwtService.generateToken(user.getUsername(), user.getRole());
}
```

All new users always get `Role.USER`. No registration endpoint can create admins or moderators — those roles are assigned by an existing admin through `PUT /api/admin/users/{id}/role`.

---

## Auth Controller

**File:** `controller/AuthController.java`

| Method | Endpoint | Request Body | Response |
|---|---|---|---|
| POST | `/api/auth/login` | `LoginRequest` | `AuthResponse` (JWT string) |
| POST | `/api/auth/register` | `RegisterRequest` | `AuthResponse` (JWT string) |

Both are public (no JWT required). Both return the same `AuthResponse` structure:

```java
public class AuthResponse {
    private String token;  // the raw JWT string
}
```

The frontend stores this token in `localStorage` under the key `"token"`.

---

## DTOs — Auth Side

### Request DTOs

**`LoginRequest`:** `{ username, password }` — just two strings, no validation annotations.

**`RegisterRequest`:** `{ username, email, password, phoneNumber }` — phoneNumber is optional (can be null).

### Response DTOs

**`AuthResponse`:** `{ token }` — a single string. The frontend decodes the base64 payload to extract `username` and `role`.

---

## User Repository

**File:** `repository/UserRepository.java`

Extends `JpaRepository<User, Long>`. Spring Data generates the SQL for all standard CRUD operations automatically. Custom methods:

```java
Optional<User> findByUsername(String username);
Optional<User> findByEmail(String email);
boolean existsByUsername(String username);
boolean existsByEmail(String email);
boolean existsByPhoneNumber(String phoneNumber);
```

Spring Data derives queries from method names. `findByUsername(String username)` generates `SELECT * FROM users WHERE username = ?`. `existsByUsername` generates an existence check query that returns a boolean — more efficient than fetching the full entity when you only need to know if it exists.

`Optional<User>` is Java's null-safe wrapper. It forces callers to explicitly handle the case where the user is not found, using `.orElseThrow(...)` or `.orElse(null)`.

---

## User Service and UserController

**Files:** `service/UserServiceImpl.java`, `controller/UserController.java`

### UserController Endpoints

| Method | Endpoint | Auth Required | Description |
|---|---|---|---|
| POST | `/api/users` | No | Create user (bypasses auth — mostly used for testing) |
| GET | `/api/users` | No | List all users |
| GET | `/api/users/{id}` | No | Get user by ID |
| PUT | `/api/users/{id}` | No | Update user |
| DELETE | `/api/users/{id}` | No | Delete user |
| PUT | `/api/users/{id}/ban` | ADMIN or MODERATOR | Ban user |
| PUT | `/api/users/{id}/unban` | ADMIN or MODERATOR | Unban user |

> **Note:** Most `/api/users` endpoints have no auth guard at the `SecurityConfig` level, which means anyone with a network connection can call them. The admin panel uses `/api/admin/users` instead, which is properly guarded.

### UserServiceImpl — Key Methods

**`createUser`:** Checks username/email/phone uniqueness, hashes the password with BCrypt, sets role to USER, saves.

**`banUser`:** Checks user is not ADMIN or MODERATOR (those cannot be banned), sets `banned = true`, saves, then calls the notification microservice:

```java
RestTemplate restTemplate = new RestTemplate();
Map<String, String> body = new HashMap<>();
body.put("username", user.getUsername());
body.put("email", user.getEmail());
body.put("phoneNumber", user.getPhoneNumber());
restTemplate.postForObject(
    "http://localhost:8082/api/notifications/ban",
    body,
    Void.class
);
```

`RestTemplate` is Spring's HTTP client for making calls to other services. If the notification service is down, the exception is caught and logged — the ban still goes through. The notification microservice (on port 8082) sends email/SMS to the banned user.

**`mapToResponse`:** Converts a `User` entity to a `UserResponse` DTO. Called before returning from every user-related method to avoid exposing the password hash.

---

## Admin Controller

**File:** `controller/AdminController.java`

All routes are under `/api/admin`. Guards use `@PreAuthorize` from Spring Security's method security.

| Method | Endpoint | `@PreAuthorize` | Description |
|---|---|---|---|
| GET | `/api/admin/users` | ADMIN or MODERATOR | All users list |
| GET | `/api/admin/bugs` | ADMIN only | All bugs |
| GET | `/api/admin/comments` | ADMIN only | All comments |
| PUT | `/api/admin/users/{id}/ban` | ADMIN or MODERATOR | Toggle ban status |
| PUT | `/api/admin/users/{id}/role` | ADMIN only | Change user role |

The ban endpoint accepts a `?banned=true` or `?banned=false` query parameter and delegates to `userService.banUser` or `userService.unbanUser` accordingly.

The role endpoint directly updates the `User` entity and saves:

```java
user.setRole(role);
return ResponseEntity.ok(mapToResponse(userRepository.save(user)));
```

`role` is bound from a `?role=ADMIN` query parameter — Spring automatically converts the string to the `Role` enum.

---

## UserResponse DTO

```java
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private Role role;
    private boolean banned;
    private LocalDateTime createdAt;
    private String phoneNumber;
}
```

The `password` field from the `User` entity is intentionally absent. The DTO is the contract between backend and frontend — by not including the field, there is no way for a password hash to accidentally leak in an API response.

---

## Exception Handling

**File:** `exception/GlobalExceptionHandler.java`

`@RestControllerAdvice` is a Spring mechanism that intercepts exceptions thrown anywhere in the controller layer and converts them into HTTP responses. Without it, Spring would return a generic 500 error for everything.

| Exception | HTTP Status | Response Body |
|---|---|---|
| `MethodArgumentNotValidException` | 400 | Map of `{ fieldName: errorMessage }` from `@Valid` annotations |
| `ResourceNotFoundException` | 404 | Error message string |
| `ForbiddenException` | 403 | Error message string |
| `InvalidVoteTypeException` | 400 | Error message string |
| `IllegalArgumentException` | 400 | Error message string |

`ResourceNotFoundException` and `ForbiddenException` are custom exceptions. They extend `RuntimeException` so they can be thrown anywhere without checked exception handling. `@ResponseStatus` on the exception class provides a fallback status if the exception escapes the handler — here it is redundant since the handler explicitly sets the status.

---

## Seeding & Bootstrap — Overview

The full seeding documentation is in **Backend 3**, but from the auth side: `DemoData` uses `AuthService.register()` to create seed users, which means all seed users with a plaintext password in the JSON go through the proper BCrypt hashing pipeline. Seed users can log in with their JSON passwords at `/api/auth/login`.

Default seed credentials from `PlaceholderDataUsers.json`:
- `ana` / `ana` → ADMIN
- `mihai` / `mihai` → MODERATOR
- Other users → USER role, password matches their username
