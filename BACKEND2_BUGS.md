# Backend 2 — Bugs

This document covers everything related to bugs: the database table, the entity, all service methods, every controller endpoint, the repository queries behind search and filtering, how tags work, and how status transitions are enforced.

---

## Database Tables — Bug Side

### `bugs` table (`Bug.java`)

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, auto-generated |
| `title` | VARCHAR(255) | NOT NULL |
| `text` | VARCHAR(5000) | NOT NULL |
| `image_url` | VARCHAR | NULLABLE |
| `status` | VARCHAR | NOT NULL (enum: OPEN, IN_PROGRESS, FIXED, CLOSED) |
| `created_at` | TIMESTAMP | NOT NULL, set once by Hibernate on insert |
| `author_id` | BIGINT | FK → `users.id`, NOT NULL |
| `vote_score` | INTEGER | NOT NULL, default 0 |

`@CreationTimestamp` on `createdAt` means Hibernate automatically sets this field to the current time when the row is first inserted. Unlike `@PrePersist`, this is managed by Hibernate's event system rather than JPA lifecycle callbacks. `updatable = false` ensures Hibernate never overwrites it on update.

### `tags` table (`Tag.java`)

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, auto-generated |
| `name` | VARCHAR | NOT NULL, UNIQUE |

Tags are shared across bugs — `"auth"` is one row in the `tags` table, linked to many bugs.

### `bug_tags` join table

| Column | Type |
|---|---|
| `bug_id` | BIGINT (FK → `bugs.id`) |
| `tag_id` | BIGINT (FK → `tags.id`) |

This is a many-to-many relationship: one bug can have many tags, and one tag can appear on many bugs. JPA manages this table automatically through the `@ManyToMany` annotation on `Bug.tags`.

### `bug_votes` table (`BugVote.java`)

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, auto-generated |
| `vote_type` | VARCHAR | NOT NULL (UPVOTE or DOWNVOTE) |
| `created_at` | TIMESTAMP | NOT NULL |
| `user_id` | BIGINT | FK → `users.id`, NOT NULL |
| `bug_id` | BIGINT | FK → `bugs.id`, NOT NULL |

**Unique constraint:** `(bug_id, user_id)` — one vote per user per bug.

```java
@Table(name = "bug_votes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"bug_id", "user_id"})
})
```

---

## Bug Entity

**File:** `entity/Bug.java`

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "author_id", nullable = false)
private User author;

@ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
@JoinTable(
    name = "bug_tags",
    joinColumns = @JoinColumn(name = "bug_id"),
    inverseJoinColumns = @JoinColumn(name = "tag_id")
)
private Set<Tag> tags = new HashSet<>();
```

**`FetchType.LAZY`:** The author and tags are not fetched from the database when the bug is loaded — only when you actually access them. This avoids N+1 query problems in list endpoints. The downside: you must be inside a `@Transactional` context when you access lazy fields, or you get a `LazyInitializationException`.

**`cascade = {CascadeType.PERSIST, CascadeType.MERGE}`:** When a bug is saved, any new `Tag` objects attached to it are also saved. This lets the service create new tags and add them to a bug in one operation.

**`@Enumerated(EnumType.STRING)`** on `status`: the `BugStatus` enum value is stored as its string name (`"OPEN"`, `"IN_PROGRESS"`, etc.) rather than an integer ordinal. This makes the database readable and resilient to enum reordering.

The `voteScore` field is a denormalized counter — same pattern as `Comment.score`. Updated on every vote cast, changed, or removed via `BugService.voteBug`. Default is 0, uses `@Builder.Default`.

---

## Bug Status Lifecycle

```
OPEN → IN_PROGRESS → FIXED
                   → CLOSED
```

| Status | When Set |
|---|---|
| `OPEN` | On bug creation (always) |
| `IN_PROGRESS` | Automatically when the first comment is posted |
| `FIXED` | When the author clicks "Mark as Resolved" |
| `CLOSED` | Only via moderator status dropdown |

Bugs with status `FIXED` or `CLOSED` do not accept new comments (enforced in `CommentService.createComment`).

---

## Bug Repository

**File:** `repository/BugRepository.java`

```java
public interface BugRepository extends JpaRepository<Bug, Long> {
    List<Bug> findAllByTags_NameIgnoreCaseOrderByCreatedAtDesc(String tagName);
    List<Bug> findAllByTitleContainingIgnoreCaseOrderByCreatedAtDesc(String title);
    List<Bug> findAllByAuthor_IdOrderByCreatedAtDesc(Long authorId);
    List<Bug> findAllByOrderByCreatedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Bug b where b.id = :bugId")
    Optional<Bug> findByIdForUpdate(@Param("bugId") Long bugId);
}
```

Spring Data JPA generates SQL for these methods by parsing their names. No SQL is written manually.

| Method | Generated SQL (conceptual) |
|---|---|
| `findAllByTags_NameIgnoreCaseOrderByCreatedAtDesc(tag)` | `SELECT b FROM bugs b JOIN bug_tags bt JOIN tags t WHERE UPPER(t.name) = UPPER(?) ORDER BY b.created_at DESC` |
| `findAllByTitleContainingIgnoreCaseOrderByCreatedAtDesc(title)` | `SELECT b FROM bugs b WHERE UPPER(b.title) LIKE UPPER('%?%') ORDER BY b.created_at DESC` |
| `findAllByAuthor_IdOrderByCreatedAtDesc(authorId)` | `SELECT b FROM bugs b WHERE b.author_id = ? ORDER BY b.created_at DESC` |
| `findAllByOrderByCreatedAtDesc()` | `SELECT b FROM bugs b ORDER BY b.created_at DESC` |
| `findByIdForUpdate(bugId)` | `SELECT b FROM bugs b WHERE b.id = ? FOR UPDATE` |

The `_` in method names like `Tags_Name` and `Author_Id` denotes navigation across a relationship — `Tags_Name` means "through the `tags` collection, access the `name` field".

`IgnoreCase` makes the comparison case-insensitive on the database side, so searching for `"auth"` and `"AUTH"` return the same results.

`ContainingIgnoreCase` generates a `LIKE '%?%'` query — the search term can appear anywhere in the title.

---

## Bug Service

**File:** `service/BugService.java`

Every method is annotated with `@Transactional` or `@Transactional(readOnly = true)`. Transactional methods run inside a database transaction — if an exception is thrown, all database changes within the method are rolled back. `readOnly = true` is a hint to the database to optimize for reads and prevents accidental writes.

### `createBug(BugRequest dto, Long authorId)`

Called by `POST /api/bugs`. Always sets status to `OPEN`. The builder pattern:

```java
Bug bug = Bug.builder()
        .title(dto.getTitle())
        .text(dto.getText())
        .imageUrl(dto.getImageUrl())
        .status(BugStatus.OPEN)
        .author(author)
        .build();
return bugRepository.save(bug);
```

`Bug.builder()` is generated by Lombok's `@Builder` annotation. It creates a builder object that lets you set each field by name before calling `.build()` to construct the immutable object. This is an alternative to calling `new Bug()` and then calling setters one by one.

### `getBugById(Long id)`

```java
return bugRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Bug not found with id: " + id));
```

`findById` returns an `Optional<Bug>`. `.orElseThrow` either unwraps the value if present or throws the exception. This pattern ensures a 404 response rather than a NullPointerException.

### `updateBug(Long id, BugRequest request, Long userId, Role role)`

```java
boolean isAuthor = bug.getAuthor().getId().equals(userId);
boolean canEdit = isAuthor || role == Role.MODERATOR || role == Role.ADMIN;
if (!canEdit) {
    throw new ForbiddenException("You are not allowed to edit this bug");
}
```

The author, moderator, or admin can edit bugs. The `role` parameter is checked against `Role.MODERATOR` and `Role.ADMIN`. The role is passed in from the controller (extracted from the JWT via `resolveAuthenticatedUser`).

### `deleteBug(Long id, Long requestingUserId, Role requestingUserRole)`

```java
boolean isAuthor = bug.getAuthor().getId().equals(requestingUserId);
boolean canDeleteAnyBug = requestingUserRole == Role.MODERATOR || requestingUserRole == Role.ADMIN;
if (!isAuthor && !canDeleteAnyBug) {
    throw new ForbiddenException("...");
}
bugRepository.delete(bug);
```

Author OR moderator OR admin can delete. `==` is used for enum comparison, not `.equals()`, because enum values are singletons in Java — there is only one `Role.MODERATOR` object, so reference equality is safe.

### `updateBugStatus(Long bugId, BugStatus newStatus)`

No permission check here — the permission is enforced at the controller level with `@PreAuthorize("hasRole('MODERATOR')")`. The service simply trusts that only authorized users reach it.

### `addTagsToBug(Long bugId, List<String> tagNames)`

```java
Set<Tag> tagEntities = tagNames.stream()
        .map(name -> tagRepository.findByNameIgnoreCase(name.trim())
                .orElseGet(() -> tagRepository.save(Tag.builder().name(name.trim()).build())))
        .collect(Collectors.toSet());

bug.getTags().addAll(tagEntities);
return bugRepository.save(bug);
```

**Stream pipeline:** `.stream()` converts the list to a stream of elements. `.map(...)` transforms each tag name string into a `Tag` entity. `.collect(Collectors.toSet())` gathers the results back into a `Set`.

**Find-or-create pattern:** `findByNameIgnoreCase(name)` looks for an existing tag. `.orElseGet(...)` runs the supplier only if the Optional is empty — creating and saving a new `Tag` entity. This prevents duplicate tags in the database.

Tags are added with `addAll` and the bug is saved, which cascades to the join table.

### `resolveBug(Long bugId, Long requestingUserId)`

```java
if (!bug.getAuthor().getId().equals(requestingUserId)) {
    throw new ForbiddenException("...");
}
bug.setStatus(BugStatus.FIXED);
return bugRepository.save(bug);
```

Only the author can mark their own bug as FIXED. No moderator override here — moderators use the status dropdown instead.

### `voteBug(Long bugId, Long requestingUserId, VoteType voteType)`

Handles upvote/downvote on bugs. Uses a pessimistic write lock (`findByIdForUpdate`) to prevent concurrent vote race conditions.

- Self-vote is rejected with `ForbiddenException`.
- Three cases (same as `voteComment`):

| Situation | Action | Bug score delta | Author score delta |
|---|---|---|---|
| New UPVOTE | Create `BugVote` | +1 | +2.5 |
| New DOWNVOTE | Create `BugVote` | -1 | -1.5 |
| Remove UPVOTE (same vote) | Delete `BugVote` | -1 | -2.5 |
| Remove DOWNVOTE (same vote) | Delete `BugVote` | +1 | +1.5 |
| Flip to UPVOTE | Update `BugVote.voteType` | +2 | +4.0 |
| Flip to DOWNVOTE | Update `BugVote.voteType` | -2 | -4.0 |

Both the bug and the author `User` entity are saved at the end of the transaction.

---

## Bug Controller

**File:** `controller/BugController.java`

### Endpoints

| Method | Endpoint | Auth | `@PreAuthorize` | Description |
|---|---|---|---|---|
| POST | `/api/bugs` | Required | `isAuthenticated()` | Create bug |
| GET | `/api/bugs` | None (optional auth for `userVote`) | None | List all bugs |
| GET | `/api/bugs/{id}` | None (optional auth for `userVote`) | None | Get bug by ID |
| PUT | `/api/bugs/{id}` | Required | `isAuthenticated()` | Update bug |
| PATCH | `/api/bugs/{id}/status` | Required | `hasRole('MODERATOR')` | Change status (moderator) |
| DELETE | `/api/bugs/{id}` | Required | `isAuthenticated()` | Delete bug |
| POST | `/api/bugs/{id}/tags` | Required | `isAuthenticated()` | Add tags |
| GET | `/api/bugs/filter` | None/Required | Checked in code for `mine` | Filter/search |
| PATCH | `/api/bugs/{id}/resolve` | Required | `isAuthenticated()` | Mark as resolved (author only) |
| POST | `/api/bugs/{id}/vote` | Required | `isAuthenticated()` | Vote on bug (upvote/downvote) |

### `resolveAuthenticatedUser` Helper

```java
private User resolveAuthenticatedUser(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
        throw new ForbiddenException("Authentication is required");
    }
    Object principal = authentication.getPrincipal();
    String username;
    if (principal instanceof User userPrincipal) {
        username = userPrincipal.getUsername();
    } else if (principal instanceof UserDetails userDetails) {
        username = userDetails.getUsername();
    } else {
        username = authentication.getName();
    }
    return userRepository.findByUsername(username)
            .orElseThrow(() -> new ForbiddenException("Authenticated user not found: " + username));
}
```

`Authentication` is Spring Security's interface for the currently logged-in user — populated by the JWT filter. The `principal` (the "who") can be different types depending on how authentication was set up. This helper normalizes all cases to extract the username, then fetches the full `User` entity from the database. The full entity is needed to access the user's `id` and `role` for permission checks.

`instanceof User userPrincipal` is Java 16+ pattern matching — it combines the `instanceof` check and cast into one line.

### Filter Endpoint

```java
@GetMapping("/filter")
public ResponseEntity<List<BugResponse>> filterBugs(
        @RequestParam(required = false) String tag,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) Long userId,
        @RequestParam(required = false) boolean mine,
        Authentication auth
) {
    List<Bug> results;
    if (mine) {
        User user = resolveAuthenticatedUser(auth);
        results = bugService.getBugsByAuthor(user.getId());
    } else if (tag != null) {
        results = bugService.getBugsByTag(tag);
    } else if (search != null) {
        results = bugService.searchByTitle(search);
    } else if (userId != null) {
        results = bugService.getBugsByAuthor(userId);
    } else {
        results = bugService.getAllBugs();
    }
    return ResponseEntity.ok(results.stream().map(BugResponse::fromEntity).toList());
}
```

A single endpoint handles all filter modes. Priority order: `mine` > `tag` > `search` > `userId` > all bugs. Only `mine=true` requires authentication — all other modes work without a token.

`@RequestParam(required = false)` means Spring does not throw an error if that parameter is absent from the URL. The value will be `null` (or `false` for boolean) if not provided.

---

## DTOs — Bug Side

### `BugRequest` (incoming)

```java
public class BugRequest {
    @NotBlank(message = "Title cannot be blank")
    @Size(max = 255, message = "Title is too long")
    private String title;

    @NotBlank(message = "Description cannot be blank")
    @Size(max = 5000, message = "Description is too long")
    private String text;

    private String imageUrl;  // no validation — optional, can be null
}
```

`@Valid` on the controller parameter triggers Jakarta Bean Validation. If any `@NotBlank` or `@Size` constraint fails, Spring throws `MethodArgumentNotValidException`, which the `GlobalExceptionHandler` catches and returns as a 400 with field-level error messages.

`@NotBlank` rejects null, empty strings, and strings with only whitespace. `@Size(max=...)` enforces a maximum length.

### `BugResponse` (outgoing)

```java
@Data
@Builder
public class BugResponse {
    private Long id;
    private String title;
    private String text;
    private String imageUrl;
    private String status;         // stored as enum, exposed as string
    private LocalDateTime createdAt;
    private String authorUsername;
    private Set<String> tags;      // tag names only, not tag IDs
    private Integer voteScore;     // sum of upvotes minus downvotes
    private String userVote;       // null, "UPVOTE", or "DOWNVOTE" (null if unauthenticated)
    private Double authorScore;    // author's reputation score at serialization time
}
```

The static factory method:

```java
public static BugResponse fromEntity(Bug bug) {
    return BugResponse.builder()
            .id(bug.getId())
            .title(bug.getTitle())
            .text(bug.getText())
            .imageUrl(bug.getImageUrl())
            .status(bug.getStatus().name())
            .createdAt(bug.getCreatedAt())
            .authorUsername(bug.getAuthor().getUsername())
            .tags(bug.getTags() != null ? bug.getTags().stream()
                    .map(tag -> tag.getName())
                    .collect(java.util.stream.Collectors.toSet()) : Collections.emptySet())
            .build();
}
```

`fromEntity` is called in every controller method that returns a bug. It is the only place where the internal `Bug` entity is converted to the public API shape. Key decisions:
- `status` is exposed as a string (`bug.getStatus().name()`) rather than the enum — easier for the frontend to consume.
- `authorUsername` is exposed, not `authorId` — the frontend needs the name for display.
- `tags` becomes a `Set<String>` of tag names — the frontend doesn't need tag IDs.
- If `tags` is null (can happen on lazily-loaded entities), return an empty set instead of crashing.
- `voteScore` mirrors the denormalized score on the `Bug` entity. `userVote` is populated by the controller from `BugVoteRepository` lookups. `authorScore` is pulled from `bug.getAuthor().getScore()`.

`bug.getAuthor().getUsername()` — accessing a lazy `@ManyToOne` field. This works because `fromEntity` is called inside a `@Transactional` context (either the service method or the default proxy on the repository).

---

## Tag Repository

**File:** `repository/TagRepository.java`

```java
Optional<Tag> findByNameIgnoreCase(String name);
```

Used in `BugService.addTagsToBug` for the find-or-create pattern. Looks up a tag by name case-insensitively. If it returns empty, a new tag is created.

---

## How the Frontend Connects to These Endpoints

| Frontend Action | HTTP Call | Backend Method |
|---|---|---|
| Load bug list | `GET /api/bugs` | `BugController.listAll` → `BugService.getAllBugs` → `BugRepository.findAllByOrderByCreatedAtDesc` |
| Search by title | `GET /api/bugs/filter?search=login` | `BugController.filterBugs` → `BugService.searchByTitle` → `BugRepository.findAllByTitleContainingIgnoreCaseOrderByCreatedAtDesc` |
| Filter by tag | `GET /api/bugs/filter?tag=auth` | `BugController.filterBugs` → `BugService.getBugsByTag` → `BugRepository.findAllByTags_NameIgnoreCaseOrderByCreatedAtDesc` |
| My bugs | `GET /api/bugs/filter?mine=true` | `BugController.filterBugs` (resolves user) → `BugService.getBugsByAuthor` |
| Open bug detail | `GET /api/bugs/{id}` | `BugController.getBugById` → `BugService.getBugById` |
| Create bug | `POST /api/bugs` | `BugController.reportBug` → `BugService.createBug` |
| Edit bug | `PUT /api/bugs/{id}` | `BugController.updateBug` → `BugService.updateBug` |
| Delete bug | `DELETE /api/bugs/{id}` | `BugController.deleteBug` → `BugService.deleteBug` |
| Add tags | `POST /api/bugs/{id}/tags` | `BugController.addTags` → `BugService.addTagsToBug` |
| Mark resolved | `PATCH /api/bugs/{id}/resolve` | `BugController.resolveBug` → `BugService.resolveBug` |
| Mod change status | `PATCH /api/bugs/{id}/status?status=CLOSED` | `BugController.updateStatus` → `BugService.updateBugStatus` |
| Vote on bug | `POST /api/bugs/{id}/vote` with `{ voteType: "UPVOTE" }` | `BugController.voteBug` → `BugService.voteBug` → pessimistic lock → score update |
