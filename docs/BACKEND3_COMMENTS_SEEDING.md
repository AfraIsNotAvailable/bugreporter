# Backend 3 — Comments, Voting & Data Seeding

This document covers the comment and voting system in full detail, plus the complete data seeding pipeline that populates the database with demo data on startup.

---

## Database Tables — Comment Side

### `comments` table (`Comment.java`)

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, auto-generated |
| `text` | VARCHAR(255) | NOT NULL |
| `image_url` | VARCHAR | NULLABLE |
| `created_at` | TIMESTAMP | NOT NULL, set once on insert by `@CreationTimestamp` |
| `author_id` | BIGINT | FK → `users.id`, NOT NULL |
| `bug_id` | BIGINT | FK → `bugs.id`, NOT NULL |
| `score` | INTEGER | NOT NULL, default 0 |

`score` is a denormalized counter — it is the sum of all upvotes minus all downvotes, stored directly on the comment row. It is updated every time a vote is cast, changed, or removed. This avoids recounting all votes on every read. The actual vote records are stored separately in `comment_votes`.

### `comment_votes` table (`CommentVote.java`)

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, auto-generated |
| `vote_type` | VARCHAR | NOT NULL (UPVOTE or DOWNVOTE) |
| `created_at` | TIMESTAMP | NOT NULL |
| `user_id` | BIGINT | FK → `users.id`, NOT NULL |
| `comment_id` | BIGINT | FK → `comments.id`, NOT NULL |

**Unique constraint:** `(comment_id, user_id)` — one vote record per user per comment. Enforced at the database level, not just in code. If a user votes twice on the same comment (shouldn't happen in normal flow), the database rejects it with a constraint violation.

```java
@Table(name = "comment_votes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"comment_id", "user_id"})
})
```

---

## Comment Repository

**File:** `repository/CommentRepository.java`

```java
List<Comment> findByBugIdOrderByScoreDescCreatedAtDesc(Long bugId);

@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select c from Comment c where c.id = :commentId")
Optional<Comment> findByIdForUpdate(@Param("commentId") Long commentId);
```

### `findByBugIdOrderByScoreDescCreatedAtDesc`

Spring Data derives this from the name: fetch all comments where `bug_id = ?`, order first by `score` descending (highest score first), then by `created_at` descending (newest first) as a tiebreaker. This is the default order the frontend receives comments — the frontend then sorts client-side for the display order.

### `findByIdForUpdate` — Pessimistic Locking

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select c from Comment c where c.id = :commentId")
Optional<Comment> findByIdForUpdate(@Param("commentId") Long commentId);
```

`@Lock(LockModeType.PESSIMISTIC_WRITE)` issues a `SELECT ... FOR UPDATE` statement. This locks the row in the database until the current transaction commits or rolls back. It is used exclusively in `voteComment` to prevent a race condition:

Without locking: two users vote on the same comment at the same time, both read `score = 5`, both add 1, both write `score = 6`. The correct result should be 7.

With `SELECT FOR UPDATE`: the second transaction blocks and waits until the first one finishes updating and commits. It then reads the already-updated `score = 6` and writes `score = 7`.

---

## CommentVote Repository

**File:** `repository/CommentVoteRepository.java`

```java
Optional<CommentVote> findByCommentIdAndUserId(Long commentId, Long userId);
List<CommentVote> findByUserIdAndCommentIdIn(Long userId, List<Long> commentIds);
```

`findByCommentIdAndUserId` — looks up whether a specific user has already voted on a specific comment. Used in `voteComment` to decide whether to add, change, or remove a vote.

`findByUserIdAndCommentIdIn` — fetches all votes by a given user for a list of comment IDs in a single query. This is called when loading the comment list for an authenticated user, to populate the `userVote` field on each `CommentResponse` (so the frontend knows which comments the user has already voted on).

`In` at the end of a Spring Data method name generates a `WHERE column IN (?, ?, ...)` clause. Without this, loading 50 comments would require 50 separate queries to check each vote — one batched query is much faster.

---

## Comment Service

**File:** `service/CommentService.java`

### `createComment(Long bugId, Long requestingUserId, String text, String image)`

```java
Bug bug = bugRepository.findById(bugId)
        .orElseThrow(() -> new ResourceNotFoundException("Bug not found with id: " + bugId));

if (bug.getStatus() == BugStatus.FIXED || bug.getStatus() == BugStatus.CLOSED) {
    throw new ForbiddenException("Bug-ul este rezolvat. Nu se mai pot adauga comentarii.");
}

User author = userRepository.findById(requestingUserId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + requestingUserId));

if (bug.getStatus() == BugStatus.OPEN) {
    bug.setStatus(BugStatus.IN_PROGRESS);
    bugRepository.save(bug);
}

Comment comment = Comment.builder()
        .text(text)
        .imageUrl(image)
        .author(author)
        .bug(bug)
        .build();

return commentRepository.save(comment);
```

Steps:
1. Verify the bug exists.
2. Reject if bug is FIXED or CLOSED.
3. Verify the user exists.
4. If bug is OPEN, flip it to IN_PROGRESS and save.
5. Build and save the comment.

The status flip is why the frontend re-fetches the bug after posting a comment — the response from `POST /api/comments/bug/{bugId}` contains the comment, not the updated bug.

### `getCommentsByBugId(Long bugId, Long userId)`

```java
List<Comment> comments = commentRepository.findByBugIdOrderByScoreDescCreatedAtDesc(bugId);

if (userId == null || comments.isEmpty()) {
    return comments.stream().map(CommentResponse::fromEntity).toList();
}

List<Long> commentIds = comments.stream().map(Comment::getId).toList();
Map<Long, VoteType> voteMap = commentVoteRepository
        .findByUserIdAndCommentIdIn(userId, commentIds)
        .stream()
        .collect(Collectors.toMap(
                v -> v.getComment().getId(),
                CommentVote::getVoteType
        ));

return comments.stream()
        .map(c -> CommentResponse.fromEntity(c, voteMap.get(c.getId())))
        .toList();
```

Two code paths:
- **Unauthenticated (`userId == null`):** Map each comment to a `CommentResponse` with `userVote = null`.
- **Authenticated:** Fetch all votes by this user for these comments in one query. Build a `Map<commentId, VoteType>`. Map each comment to a `CommentResponse` passing the vote type from the map (null if not voted).

`Collectors.toMap(keyMapper, valueMapper)` converts a stream of `CommentVote` objects into a `Map<Long, VoteType>` where the key is the comment ID. `voteMap.get(c.getId())` returns null for unvoted comments, which becomes `userVote = null` in the response.

### `updateComment(Long commentId, Long requestingUserId, Role requestingUserRole, String newText, String newImage)`

```java
boolean isAuthor = comment.getAuthor().getId().equals(requestingUserId);
boolean isModerator = requestingUserRole == Role.MODERATOR || requestingUserRole == Role.ADMIN;
if (!isAuthor && !isModerator) {
    throw new ForbiddenException("User is not the author of the comment");
}

comment.setText(newText);
if (newImage != null) {
    comment.setImageUrl(newImage);
}
return commentRepository.save(comment);
```

Author or privileged role can edit. `newImage == null` check preserves the existing image if the frontend sends null for that field.

### `deleteComment(Long commentId, Long requestingUserId, Role requestingUserRole)`

Same permission check as update. Calls `commentRepository.delete(comment)`. Because `CommentVote` has a foreign key to `Comment`, Hibernate handles cascade deletion of votes when a comment is deleted (or the database cascades it depending on the constraint definition).

### `voteComment(Long commentId, Long requestingUserId, VoteType voteType)`

```java
Comment comment = commentRepository.findByIdForUpdate(commentId)  // pessimistic lock
        .orElseThrow(...);

if (comment.getAuthor().getId().equals(requestingUserId)) {
    throw new ForbiddenException("Users cannot vote on their own comments");
}

Optional<CommentVote> existingVoteOpt =
        commentVoteRepository.findByCommentIdAndUserId(commentId, requestingUserId);

if (existingVoteOpt.isPresent()) {
    CommentVote existingVote = existingVoteOpt.get();
    if (existingVote.getVoteType() == voteType) {
        // Same vote again → remove it (toggle off)
        commentVoteRepository.delete(existingVote);
        comment.setScore(comment.getScore() + (voteType == VoteType.UPVOTE ? -1 : 1));
    } else {
        // Opposite vote → flip it
        existingVote.setVoteType(voteType);
        commentVoteRepository.save(existingVote);
        comment.setScore(comment.getScore() + (voteType == VoteType.UPVOTE ? 2 : -2));
    }
} else {
    // No existing vote → create one
    CommentVote newVote = CommentVote.builder()
            .comment(comment)
            .user(user)
            .voteType(voteType)
            .build();
    commentVoteRepository.save(newVote);
    comment.setScore(comment.getScore() + (voteType == VoteType.UPVOTE ? 1 : -1));
}
return commentRepository.save(comment);
```

**Three cases:**

| Situation | Action | Comment score delta | Author score delta | Voter score delta |
|---|---|---|---|---|
| New UPVOTE | Create `CommentVote` | +1 | +5.0 | — |
| New DOWNVOTE | Create `CommentVote` | -1 | -2.5 | -1.5 |
| Remove UPVOTE (same vote) | Delete `CommentVote` | -1 | -5.0 | — |
| Remove DOWNVOTE (same vote) | Delete `CommentVote` | +1 | +2.5 | +1.5 |
| Flip to UPVOTE | Update `CommentVote.voteType` | +2 | +7.5 | +1.5 |
| Flip to DOWNVOTE | Update `CommentVote.voteType` | -2 | -7.5 | -1.5 |

Switching votes is ±2 because the previous vote also needs to be undone: removing a downvote is +1, adding an upvote is another +1, total +2.

In addition to updating the comment's `score`, `voteComment` now updates the reputation scores of the involved users. The comment author gains/loses points based on how their content is received. The voter loses 1.5 points when downvoting another user's comment (discourages spam downvoting). Both the author and voter `User` entities are saved at the end of the transaction.

---

## Comment Controller

**File:** `controller/CommentController.java`

### Endpoints

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/comments/bug/{bugId}` | Required | Create comment |
| GET | `/api/comments/bug/{bugId}` | Optional | Get comments for bug |
| PUT | `/api/comments/{commentId}` | Required | Update comment |
| DELETE | `/api/comments/{commentId}` | Required | Delete comment |
| POST | `/api/comments/{commentId}/vote` | Required | Vote on comment |

GET is public (no auth required, per `SecurityConfig`). If a valid token is present, the endpoint also returns the user's vote state per comment. Without a token, all `userVote` fields are null.

### Vote Type Parsing

```java
String voteType = payload.getVoteType();  // raw string from JSON
VoteType parsedVoteType;
try {
    parsedVoteType = VoteType.valueOf(voteType.toUpperCase(Locale.ROOT));
} catch (IllegalArgumentException ex) {
    throw new InvalidVoteTypeException("Invalid voteType '" + voteType + "'...");
}
```

`VoteType.valueOf(String)` is Java's built-in enum parsing — it converts a string to an enum constant. The `toUpperCase(Locale.ROOT)` ensures that `"upvote"`, `"Upvote"`, and `"UPVOTE"` all work. `Locale.ROOT` is locale-neutral — using the default locale could produce unexpected results for Turkish locales where uppercase of `"i"` is `"İ"` not `"I"`.

---

## DTOs — Comment Side

### `CommentRequest` (incoming)

```java
public class CommentRequest {
    @NotBlank(message = "Comment text cannot be blank")
    @Size(max = 2000, message = "Comment text cannot exceed 2000 characters")
    private String text;

    @URL(message = "Image URL must be a valid URL")
    private String imageUrl;
}
```

`@URL` from Hibernate Validator checks that `imageUrl`, if present, is a syntactically valid URL. Since the field is nullable, validation only fires when a non-null value is provided.

### `VoteRequest` (incoming)

```java
public class VoteRequest {
    @NotBlank(message = "voteType is required. Allowed values: UPVOTE, DOWNVOTE.")
    private String voteType;
}
```

`voteType` is received as a string (not the enum directly) to allow case-insensitive parsing and better error messages for invalid values.

### `CommentResponse` (outgoing)

```java
@Builder @Data
public class CommentResponse {
    private Long id;
    private String text;
    private String imageUrl;
    private Long bugId;
    private Long authorId;
    private String authorUsername;
    private LocalDateTime createdAt;
    private Integer score;
    private String userVote;  // null, "UPVOTE", or "DOWNVOTE"
    private Double authorScore;
}
```

Two static factory overloads:

```java
public static CommentResponse fromEntity(Comment comment) {
    return fromEntity(comment, null);  // no vote info
}

public static CommentResponse fromEntity(Comment comment, VoteType userVote) {
    return CommentResponse.builder()
            ...
            .userVote(userVote != null ? userVote.name() : null)
            .build();
}
```

The second overload is called from `getCommentsByBugId` with the user's actual vote. `userVote.name()` converts the enum to its string name (`"UPVOTE"` or `"DOWNVOTE"`). The frontend reads this to pre-highlight the active vote button. `authorScore` is pulled from `comment.getAuthor().getScore()` at serialization time — the frontend uses it to display the author's reputation next to their name.

---

## Data Seeding

**Files:** `config/DemoData.java`, `src/main/resources/data/PlaceholderData*.json`

The seeding system runs at startup under the `dev` Spring profile. It loads three JSON files and inserts their data into the database if it doesn't already exist.

### Profile Guard

```java
@Component
@Profile("dev")
public class DemoData {
```

`@Profile("dev")` means this bean only exists when the `dev` profile is active. The `application.properties` has `spring.profiles.active=dev`, so it runs on every normal startup. The `Reset DB` run configuration activates `dev,reset` — the `reset` profile sets `ddl-auto=create` which drops and recreates all tables, after which seeding runs fresh.

### JSON File Structure

**`PlaceholderDataUsers.json`** — array of user objects:
```json
{
  "id": 1,
  "username": "ana",
  "email": "ana@example.com",
  "password": "ana",
  "role": "ADMIN",
  "banned": false,
  "createdAt": "2026-03-01T09:00:00"
}
```

The `id` in JSON is the *source ID* used to build relationships between JSON objects. It is NOT necessarily the ID that ends up in the database (the database auto-generates IDs). The seeding code maintains a mapping from JSON source IDs to actual persisted IDs.

**`PlaceholderDataBugs.json`** — bug objects referencing user IDs by the source ID:
```json
{
  "id": 101,
  "title": "Login button unresponsive on Safari",
  "status": "OPEN",
  "author": { "id": 3 }
}
```

**`PlaceholderDataComments.json`** — comment objects referencing both bug and user source IDs:
```json
{
  "id": 5,
  "text": "I checked the logs...",
  "score": 8,
  "author": { "id": 2 },
  "bug": { "id": 101 }
}
```

### The loadDemoData Pipeline

```java
@Transactional
public void loadDemoData() {
    List<Comment> demoComments = getDemoCommentsFromJson();
    List<Bug> demoBugs = getDemoBugsFromJson();
    Map<Long, Long> authorIdMapping = seedUsers(demoComments, demoBugs);
    Map<Long, Long> bugIdMapping = seedBugs(demoBugs, demoComments, authorIdMapping);
    seedComments(demoComments, authorIdMapping, bugIdMapping);
}
```

The entire operation runs in one transaction — if anything fails, all inserts roll back together.

### Step 1: `seedUsers`

Collects all user IDs referenced anywhere in comments and bugs. Checks if each user already exists (by ID or username). For users with plaintext passwords in the JSON, calls `AuthService.register()` to properly BCrypt-hash them. Returns a `Map<sourceId, persistedId>`.

```java
String plaintext = jsonPlaintextPasswords.get(sourceId);
if (plaintext != null) {
    saved = registerDemoUser(template, plaintext);
} else {
    saved = userRepository.save(template);  // already-hashed password
}
```

Users registered via `AuthService.register()` can log in with their JSON password at `/api/auth/login`. After registering, `registerDemoUser` also patches the role and `banned` flag (since `register` always creates `Role.USER`).

### Step 2: `seedBugs`

Iterates the bug list, resolves author IDs through `authorIdMapping`, checks if the bug already exists (by ID), and saves it. Builds `Map<sourceBugId, persistedBugId>`.

For bugs without an explicit author in JSON (fallback bugs generated for comment orphans), the first available user from the mapping is used as author.

### Step 3: `seedComments`

Only runs if the `comments` table is empty — a coarse guard that prevents duplicate seeding:

```java
if (commentRepository.count() > 0) {
    log.info("Skipping demo comment seeding because comments already exist.");
    return;
}
```

For each comment, resolves `authorId` and `bugId` through the two mappings. Skips comments where either cannot be resolved. Forces `comment.setId(null)` so the database generates a fresh ID instead of using the JSON source ID.

```java
comment.setId(null);
comment.setAuthor(userRepository.getReferenceById(mappedAuthorId));
comment.setBug(bugRepository.getReferenceById(mappedBugId));
validComments.add(comment);
```

`getReferenceById` returns a Hibernate proxy — a lightweight placeholder that holds just the ID without hitting the database. This is sufficient for setting a foreign key relationship without loading the full entity.

Finally, `commentRepository.saveAll(validComments)` inserts all valid comments in one batch.

### JSON Deserialization

```java
private <T> List<T> readListFromJson(String path, Class<T> itemType) {
    ClassPathResource resource = new ClassPathResource(path);
    try (var inputStream = resource.getInputStream()) {
        return objectMapper.readValue(
                inputStream,
                objectMapper.getTypeFactory().constructCollectionType(List.class, itemType)
        );
    } catch (Exception e) {
        log.warn("Failed to read demo data from '{}': {}", path, e.getMessage());
        return List.of();
    }
}
```

`ClassPathResource` loads a file from `src/main/resources/` using the classpath — works both during development and in a packaged JAR. `ObjectMapper` is Jackson's JSON parser. `constructCollectionType(List.class, itemType)` tells Jackson to deserialize the JSON array as `List<User>` (or `List<Bug>` etc.).

If the file is missing or malformed, the method logs a warning and returns an empty list — seeding is skipped rather than crashing the whole startup.

### Jackson Configuration

**File:** `config/JacksonConfig.java`

```java
@Bean
@ConditionalOnMissingBean(ObjectMapper.class)
public ObjectMapper objectMapper() {
    return JsonMapper.builder()
            .findAndAddModules()
            .build();
}
```

`findAndAddModules()` auto-registers all Jackson modules on the classpath, including `JavaTimeModule` which handles `LocalDateTime` serialization/deserialization. Without this, Jackson cannot parse the `"createdAt": "2026-03-01T09:00:00"` fields in the JSON files.

`@ConditionalOnMissingBean` means this bean is only created if Spring Boot has not already auto-configured an `ObjectMapper`. This prevents duplicate beans.

---

## How the Frontend Connects to Comment Endpoints

| Frontend Action | HTTP Call | Backend Method |
|---|---|---|
| Load comments (unauth) | `GET /api/comments/bug/{id}` (no header) | `CommentController.getCommentsByBugId` → `CommentService.getCommentsByBugId(bugId, null)` → all `userVote = null` |
| Load comments (auth) | `GET /api/comments/bug/{id}` (with JWT) | same → `CommentService.getCommentsByBugId(bugId, userId)` → votes populated |
| Post comment | `POST /api/comments/bug/{id}` | `CommentController.createComment` → `CommentService.createComment` → status flip if OPEN |
| Edit comment | `PUT /api/comments/{id}` | `CommentController.updateComment` → `CommentService.updateComment` |
| Delete comment | `DELETE /api/comments/{id}` | `CommentController.deleteComment` → `CommentService.deleteComment` |
| Upvote | `POST /api/comments/{id}/vote` with `{ voteType: "UPVOTE" }` | `CommentController.voteComment` → `CommentService.voteComment` → pessimistic lock → score update |
| Downvote | `POST /api/comments/{id}/vote` with `{ voteType: "DOWNVOTE" }` | same |
| Toggle off | same endpoint, same vote type | `voteComment` detects same vote → deletes `CommentVote`, reverses score |
| Admin view all comments | `GET /api/admin/comments` | `AdminController.getAllComments` → direct `commentRepository.findAll()` |
