# TODO — Spec Parity Checklist

## 1. internalExecutionId (Required — cannot be deleted)

- [ ] **Backend** `BugreporterApplication.java`: add `static final String internalExecutionId = UUID.randomUUID().toString()` and print it once at startup via `System.out.println` or logger before `SpringApplication.run()`

---

## 2. Bug Voting (Feature 3 — entirely missing)

### Backend

- [ ] Create `BugVote` entity: `id`, `voteType (VoteType)`, `user (ManyToOne User)`, `bug (ManyToOne Bug)`, unique constraint `(bug_id, user_id)`
- [ ] Add denormalized `voteScore` field to `Bug` entity (default 0), same pattern as `Comment.score`
- [ ] Create `BugVoteRepository`: method `findByBugAndUser(Bug, User)`, `findAllByBugIdAndUserId(Long, Long)`
- [ ] Add `voteBug(Long bugId, VoteRequest, String username)` method to `BugService`:
  - No self-vote (throw `ForbiddenException` if voter == author)
  - One vote per user — toggle if same type, flip if opposite
  - Update `bug.voteScore` with pessimistic lock (same pattern as `CommentService.voteComment()`)
- [ ] Add `POST /api/bugs/{id}/vote` endpoint to `BugController` (authenticated)
- [ ] Update `BugResponse` to include `voteScore` and `userVote` (nullable)

### Frontend

- [ ] Add upvote/downvote buttons to `BugCard.jsx` and `BugDetail.jsx`
- [ ] Display `voteScore` on bug cards and detail page (green/red/gray like comments)
- [ ] Disable vote buttons when `bug.authorUsername === currentUser.username`
- [ ] Call `POST /api/bugs/{id}/vote` on click, update local state (toggle / flip logic same as `CommentCard.jsx`)

---

## 3. User Score (Bonus Feature 1 — entirely missing)

### Backend

- [ ] Add `score` field (`double`, default 0.0) to `User` entity
- [ ] Add `score` field to `UserResponse` DTO
- [ ] In `BugService.voteBug()` — update author's score on each bug vote:
  - New UPVOTE on bug: author `+2.5`
  - New DOWNVOTE on bug: author `-1.5`
  - Remove UPVOTE: author `-2.5`
  - Remove DOWNVOTE: author `+1.5`
  - Flip UPVOTE→DOWNVOTE: author `-4.0` (reverse +2.5, apply -1.5)
  - Flip DOWNVOTE→UPVOTE: author `+4.0`
- [ ] In `CommentService.voteComment()` — update comment author's score AND voter's score:
  - New UPVOTE on comment: comment author `+5.0`
  - New DOWNVOTE on comment: comment author `-2.5`, **voter `-1.5`**
  - Remove UPVOTE: comment author `-5.0`
  - Remove DOWNVOTE: comment author `+2.5`, **voter `+1.5`**
  - Flip UPVOTE→DOWNVOTE: comment author `-7.5`, voter `-1.5`
  - Flip DOWNVOTE→UPVOTE: comment author `+7.5`, voter `+1.5`

### Frontend

- [ ] Display user score next to author username on `BugCard.jsx` (e.g. `alice [12.5]`)
- [ ] Display user score next to author username on `CommentCard.jsx`
- [ ] Score color: green if > 0, red if < 0, gray if 0
- [ ] Include `authorScore` in `BugResponse` and `CommentResponse` (backend update needed)

> **Note:** `BugResponse` and `CommentResponse` need an `authorScore` field pulled from the author `User` entity at serialization time — add to `fromEntity()` static factories.

---

## 4. Moderator Can Edit Any Bug (Bonus Feature 2 — partial)

### Backend

- [ ] `BugService.updateBug()`: add moderator/admin bypass — same pattern as `deleteBug()` line 100. Currently only author can edit (line ~66).

### Frontend

- [ ] `BugDetail.jsx`: show Edit button when `currentUser.role === 'MODERATOR'` or `'ADMIN'` (currently author-only)

---

## 5. Cypress Tests (from `frontend-tasks.md` — incomplete)

- [ ] `auth.cy.js`: "Login with banned user → ban error shown" test
- [ ] `auth.cy.js`: "Admin ban user → user appears as banned in table" test
- [ ] `auth.cy.js`: "Admin change role → role updates in table" test

---

## Priority Order

1. `internalExecutionId` — 5 min fix, required, cannot skip
2. Moderator edit bug — 15 min fix backend + frontend
3. Bug voting — ~2–3h (new entity, service, endpoints, frontend)
4. User score — ~2–3h (depends on bug voting being done first)
5. Cypress tests — ~1h
