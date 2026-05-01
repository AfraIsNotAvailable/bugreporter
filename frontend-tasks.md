# Frontend Tasks

React + React Router + Axios. Person 1 sets up shared foundation first — others build on top.

---

## Person 1 — Foundation, Auth, Admin

### Setup (do first, others depend on it)
- Axios instance with `Authorization: Bearer <token>` header injected from stored JWT
- `AuthContext` — stores `{ user, token, login, logout, isAuthenticated }`, persists JWT in `localStorage`
- React Router setup — public routes, protected routes (`<PrivateRoute role="...">` wrapper)
- Navbar — shows login/register if logged out; username + logout if logged in; admin link if ADMIN role

### Auth pages
- `/login` — form → `POST /api/auth/login` → save token → redirect
- `/register` — form (username, email, password) → `POST /api/auth/register` → save token → redirect

### Admin panel (`/admin`, ADMIN only)
- View all users: `GET /api/admin/users` — table with username, email, role, banned status
- Ban/unban user: `PUT /api/admin/users/{id}/ban` (toggle `banned`)
- Change role: `PUT /api/admin/users/{id}/role` — dropdown (USER / MODERATOR / ADMIN)
- View all bugs: `GET /api/admin/bugs`
- View all comments: `GET /api/admin/comments`

### Cypress
- Register new user → lands on home
- Login with valid credentials → JWT stored
- Login with wrong password → error shown
- Login with banned user → error shown
- Admin: ban user → user appears as banned
- Admin: change role → role updates in table
- Unauthenticated → `/admin` redirects to login

---

## Person 2 — Bugs

### Bug list page (`/bugs` or `/`, public)
- `GET /api/bugs` — show title, status badge, author, tags, date
- Filter by tag: `GET /api/bugs/filter?tag=xxx` — tag chips/input
- Search by title: `GET /api/bugs/filter?search=xxx` — search box
- Filter "my bugs": `GET /api/bugs/filter?mine=true` (auth required), or by userId: `?userId=xxx`
- Link each bug → bug detail page

### Bug detail page (`/bugs/:id`, public)
- `GET /api/bugs/{id}` — show all fields, status, tags, author
- Placeholder slot for comments section (Person 3 plugs in here)

### Bug CRUD (authenticated)
- "Report Bug" button → modal or `/bugs/new` — `POST /api/bugs`
- Edit button (own bug or MODERATOR) → `PUT /api/bugs/{id}`
- Delete button (own bug or MODERATOR) → `DELETE /api/bugs/{id}`
- Add tags: input on bug detail → `POST /api/bugs/{id}/tags`

### Status controls
- "Mark as Resolved" button (author only, not on FIXED/CLOSED) → `PATCH /api/bugs/{id}/resolve`
- Status dropdown (MODERATOR only) → `PATCH /api/bugs/{id}/status` — options: OPEN, IN_PROGRESS, FIXED, CLOSED

### Cypress
- Unauthenticated: bug list loads, bug detail loads
- Search/filter by tag and title → results update
- Login → create bug → appears in list
- Edit own bug → changes saved
- Delete own bug → removed from list
- Author clicks "Mark Resolved" → status changes to FIXED
- Moderator can change status via dropdown
- Regular user cannot see moderator controls

---

## Person 3 — Comments + Voting

### Comments section component (embedded in Person 2's bug detail — one clean import)
- `GET /api/comments/bug/{bugId}` — list, ordered by score desc
- Show: text, imageUrl if present, author, score, up/downvote buttons, edit/delete if own or MODERATOR/ADMIN

### Create comment (authenticated, bug not FIXED/CLOSED)
- Form below list → `POST /api/comments/bug/{bugId}` — text + optional imageUrl
- After submit: bug status may flip to IN_PROGRESS automatically (reflect in UI)

### Edit/delete comment
- Edit inline or modal → `PUT /api/comments/{id}`
- Delete → `DELETE /api/comments/{id}`
- Only show controls if: own comment OR MODERATOR/ADMIN role

### Voting
- Upvote / Downvote buttons per comment → `POST /api/comments/{id}/vote` with `{ voteType: "UPVOTE" | "DOWNVOTE" }`
- Show current score
- Toggle behavior: same vote again removes it; opposite vote flips it
- Disable vote buttons on own comments (backend also rejects)

### Cypress
- Unauthenticated: comments list visible, no create form shown
- Login → post comment → appears in list
- Post comment on OPEN bug → bug status flips to IN_PROGRESS
- Cannot comment on FIXED bug → form hidden or error
- Upvote comment → score +1; upvote again → score back to original (toggle)
- Downvote then upvote → score changes +2
- Cannot vote own comment → buttons disabled
- Edit own comment → text updates
- Delete own comment → removed from list
- MODERATOR can edit/delete any comment

---

## Styling Phase (after all above works)

All three people apply styling to their own pages/components:

- Static color palette — pick 3–4 colors up front, everyone uses same tokens (CSS vars or Tailwind config)
- Status badges: distinct colors per status (OPEN = blue, IN_PROGRESS = yellow, FIXED = green, CLOSED = gray)
- Score display: green if positive, red if negative, gray if zero
- Button states: visible `:hover` and `:active` — no animations, just color shift
- Form validation: red border + error text on invalid fields
- Disabled states: visually clear (grayed out) for vote-own-comment, comment-on-closed-bug
- Mobile-responsive layout — basic flex/grid, no JS required
- Navbar: active route highlighted
- No gradients, no animations, no purple/magenta

---

## Integration Points (project owner connects these)

1. Person 3's `<CommentsSection bugId={id} bugStatus={status} />` → drops into Person 2's bug detail page
2. Person 1's `AuthContext` → Person 2 and 3 both consume for auth state and role checks
3. Person 1's Axios instance → Person 2 and 3 import and use (no one creates their own)
