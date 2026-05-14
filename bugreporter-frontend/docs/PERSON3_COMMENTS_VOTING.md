# Person 3 — Comments & Voting

This document covers the comment system embedded in the bug detail page: loading and displaying comments, creating new ones, editing and deleting, voting with toggle behavior, sorting, and all the permission logic that controls what each user sees.

---

## Pages & Components Owned

| File | Role |
|---|---|
| `src/components/CommentCard.jsx` | Single comment display with all interactions |
| `src/components/CommentSort.jsx` | Sort order dropdown |
| `src/services/commentService.js` | API functions (partially used) |
| `src/hooks/useComments.js` | Comment fetch hook (read-only, used in BugDetail) |

The comment section itself lives inside `src/pages/BugDetail.jsx` (Person 2's file). Person 3 added the comment list rendering, the create form, and the CommentCard and CommentSort components that BugDetail imports.

---

## How Comments Are Loaded

**In `BugDetail.jsx`:**

```js
const [comments, setComments] = useState([]);

const loadBug = useCallback(async () => {
  const [bugRes, commentsRes] = await Promise.all([
    getBug(id),
    api.get(`/comments/bug/${id}`),
  ]);
  setBug(bugRes.data);
  setComments(commentsRes.data);
}, [id]);

useEffect(() => {
  loadBug();
}, [loadBug]);
```

Comments are loaded at the same time as the bug itself using `Promise.all`, which fires both requests simultaneously instead of sequentially. The response is an array of comment objects stored in `comments` state.

---

## Sorting Comments

**File:** `src/components/CommentSort.jsx`

```jsx
<select value={value} onChange={(e) => onChange(e.target.value)}>
  <option value="date_desc">Newest first</option>
  <option value="date_asc">Oldest first</option>
  <option value="score_desc">Highest score</option>
  <option value="score_asc">Lowest score</option>
</select>
```

The sort dropdown is a *controlled component* — its value comes from the parent (BugDetail) as a prop, and changes are sent back up via the `onChange` prop. BugDetail holds the sort order in state:

```js
const [sortOrder, setSortOrder] = useState("date_desc");
```

The actual sorting happens in BugDetail using `useMemo`:

```js
const sortedComments = useMemo(() => {
  const copy = [...comments];  // never mutate the original array
  switch (sortOrder) {
    case "date_desc": return copy.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
    case "date_asc":  return copy.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
    case "score_desc": return copy.sort((a, b) => b.score - a.score);
    case "score_asc":  return copy.sort((a, b) => a.score - b.score);
    default: return copy;
  }
}, [comments, sortOrder]);
```

`useMemo` caches the sorted array and only recomputes it when `comments` or `sortOrder` changes. This avoids re-sorting on every unrelated re-render (like typing in the comment form).

`[...comments]` creates a shallow copy before sorting because `.sort()` mutates the array in place — mutating React state directly causes subtle bugs.

**Sorting is done entirely in the browser** — the backend is not called again when the sort order changes.

The sorted array is then rendered:

```jsx
{sortedComments.map((comment) => (
  <CommentCard
    key={comment.id}
    comment={comment}
    onDelete={(deletedId) =>
      setComments((prev) => prev.filter((c) => c.id !== deletedId))
    }
  />
))}
```

Each `CommentCard` receives the full comment object and an `onDelete` callback. When a comment is deleted, the callback removes it from the local `comments` array using `.filter`, which returns a new array without the deleted comment. React re-renders and the comment disappears from the list without a page reload.

---

## Creating a Comment

**In `BugDetail.jsx`:**

```js
const [commentText, setCommentText] = useState("");
const [commentImageUrl, setCommentImageUrl] = useState("");
const [commentSubmitting, setCommentSubmitting] = useState(false);
const [commentError, setCommentError] = useState("");
```

### When the Form Is Shown

```jsx
{isAuthenticated && bug.status !== "FIXED" && bug.status !== "CLOSED" ? (
  <form onSubmit={handleCommentSubmit}>
    <textarea aria-label="Comment text" ... />
    <input aria-label="Comment image URL" ... />
    <button type="submit">Post Comment</button>
  </form>
) : (
  <p>
    {!isAuthenticated
      ? "Log in to post a comment."
      : "Commenting is disabled on FIXED or CLOSED bugs."}
  </p>
)}
```

Two conditions must both be true to show the form:
1. The user must be logged in (`isAuthenticated`).
2. The bug must not be FIXED or CLOSED.

If either condition fails, a message is shown instead. This is enforced in the UI — the backend also rejects invalid posts, but blocking it in the UI gives immediate feedback.

### The Submit Handler

```js
const handleCommentSubmit = async (event) => {
  event.preventDefault();
  setCommentError("");
  setCommentSubmitting(true);
  try {
    const payload = { text: commentText };
    if (commentImageUrl) payload.imageUrl = commentImageUrl;

    const res = await api.post(`/comments/bug/${id}`, payload);

    setComments((prev) => [res.data, ...prev]); // prepend new comment to top
    setCommentText("");         // clear form
    setCommentImageUrl("");

    const bugRes = await getBug(id); // re-fetch bug to catch status flip
    setBug(bugRes.data);
  } catch (err) {
    setCommentError(err.response?.data?.message || "Could not post comment");
  } finally {
    setCommentSubmitting(false);
  }
};
```

**Step by step:**
1. Prevent default browser form submit.
2. Clear any previous error, set submitting to true (disables the button).
3. Build the payload — `imageUrl` is only included if the user typed something.
4. `POST /api/comments/bug/{id}` — the backend creates the comment.
5. Prepend the returned comment to the top of the list with `[res.data, ...prev]`.
6. Clear the form fields.
7. Re-fetch the bug to detect any status change (see next section).

### Status Flip After Commenting

When a comment is posted on a bug that is currently `OPEN`, the backend automatically flips its status to `IN_PROGRESS`. The frontend does not know this happened until it asks — so after every successful comment post, `getBug(id)` is called and `setBug(bugRes.data)` updates the displayed status.

This is why you see `IN_PROGRESS` appear in the status badge after posting the first comment on a fresh bug.

---

## The CommentCard Component

**File:** `src/components/CommentCard.jsx`

Each comment is rendered by its own `CommentCard` instance. The component manages all its own interaction state locally.

### Props

```js
function CommentCard({ comment, onDelete }) {
```

- `comment` — the full comment object from the API (`{ id, text, authorUsername, score, userVote, createdAt, imageUrl }`)
- `onDelete` — a callback function passed from BugDetail; called after a successful delete to remove the comment from the parent's list

### Local State

```js
const [vote, setVote] = useState(comment.userVote ?? null);
const [score, setScore] = useState(comment.score);
const [isEditing, setIsEditing] = useState(false);
const [editText, setEditText] = useState(comment.text);
const [displayText, setDisplayText] = useState(comment.text);
const [showMenu, setShowMenu] = useState(false);
const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
```

| State | Purpose |
|---|---|
| `vote` | Current user's vote: `"UPVOTE"`, `"DOWNVOTE"`, or `null` |
| `score` | Current comment score (updated locally after voting) |
| `isEditing` | Whether the inline edit textarea is shown |
| `editText` | Live value in the edit textarea |
| `displayText` | What is shown when not editing (updated after edit is saved) |
| `showMenu` | Whether the "..." dropdown is open |
| `showDeleteConfirm` | Whether the delete confirmation modal is shown |

### Permission Checks

```js
const { user } = useAuth();

const isOwn = comment.authorUsername === user?.username;
const isPriviledged = user?.role === "ADMIN" || user?.role === "MODERATOR";
const canEdit = isOwn || isPriviledged;
```

`user?.username` — the `?.` prevents a crash if `user` is null (unauthenticated visitors). If null, the expression evaluates to `undefined`, which is not equal to any username, so `isOwn` is false.

`canEdit` is true if you wrote the comment OR if you have a privileged role. This gates the "..." menu button and the inline Edit button.

---

## How Own Comment Controls Work

The "..." menu button and the standalone Edit button at the bottom are both only rendered when `canEdit` is true:

```jsx
{canEdit && (
  <div style={{ position: "absolute", top: "12px", right: "12px" }}>
    <button onClick={() => setShowMenu(v => !v)}>...</button>
    {showMenu && (
      <div>
        <button onClick={() => { setIsEditing(true); setShowMenu(false); }}>Edit</button>
        {(isOwn || isPriviledged) && (
          <button onClick={() => { setShowDeleteConfirm(true); setShowMenu(false); }}>Delete</button>
        )}
      </div>
    )}
  </div>
)}
```

An unauthenticated user or a user who neither wrote the comment nor has a privileged role sees none of these controls at all — the `canEdit` check prevents them from rendering.

The standalone Edit button at the bottom of the card:

```jsx
{canEdit && (
  <div style={{ marginLeft: "auto" }}>
    {isEditing ? (
      <button onClick={handleEditSubmit}>Submit</button>
    ) : (
      <button onClick={() => setIsEditing(true)}>Edit</button>
    )}
  </div>
)}
```

When `isEditing` is false, "Edit" shows. Click it and `isEditing` becomes true, which replaces the display text with the edit textarea and replaces "Edit" with "Submit".

---

## How Voting Is Hidden on Own Comments

```jsx
<div style={bottomRowStyle}>
  {!isOwn && (
    <>
      <button aria-label="Upvote" onClick={() => handleVote("UPVOTE")}>▲</button>
      <button aria-label="Downvote" onClick={() => handleVote("DOWNVOTE")}>▼</button>
    </>
  )}
  <span data-testid="comment-score">{score}</span>
  ...
</div>
```

`{!isOwn && (...)}` — if `isOwn` is true, the entire block does not render. The vote buttons are simply not in the DOM at all. The score still shows so you can see how your own comment is doing.

The backend also rejects voting on your own comment, but hiding the buttons in the UI makes for a cleaner experience.

---

## Voting — Toggle Behavior and Score Updates

```js
const handleVote = async (type) => {
  const removing = vote === type;  // clicking the same vote = remove it
  try {
    await api.post(`/comments/${comment.id}/vote`, { voteType: type });
    if (removing) {
      setScore((s) => (type === "UPVOTE" ? s - 1 : s + 1));
      setVote(null);
    } else {
      setScore((s) => {
        let delta = type === "UPVOTE" ? 1 : -1;
        if (vote !== null) delta *= 2;  // switching from opposite vote
        return s + delta;
      });
      setVote(type);
    }
  } catch {
    // silent fail — score stays as-is
  }
};
```

### The Three Cases

**Case 1: No current vote, clicking Upvote**
- `removing` = false (no existing vote to remove)
- `vote` is null, so `delta *= 2` does NOT apply
- `delta` = +1
- Score goes up by 1, `vote` set to `"UPVOTE"`

**Case 2: Already upvoted, clicking Upvote again (toggle off)**
- `removing` = true (`vote === type` is `"UPVOTE" === "UPVOTE"`)
- Score goes down by 1 (removes the upvote)
- `vote` set to null

**Case 3: Already downvoted, clicking Upvote (switch)**
- `removing` = false
- `vote` is `"DOWNVOTE"` (not null), so `delta *= 2` applies: `delta` = +2
- Score goes up by 2 (undoing the -1 from the downvote, plus adding +1 for the new upvote)
- `vote` set to `"UPVOTE"`

The same logic applies symmetrically for downvotes.

### Score Is Updated Optimistically

The score changes immediately when the button is clicked — the `setScore` and `setVote` calls happen inside the `try` block but outside the `await`. Actually, they happen inside the try but after the `await`, so the server confirms the vote before the score updates. If the server call fails (caught by `catch {}`), the score stays unchanged. This prevents showing a wrong score if the request fails.

---

## Edit Flow

```js
const handleEditSubmit = async () => {
  try {
    await api.put(`/comments/${comment.id}`, { text: editText });
    setDisplayText(editText);  // update what's shown
    setIsEditing(false);       // exit edit mode
  } catch {
    // keep editing open on failure (user can try again)
  }
};
```

`displayText` and `editText` are separate states:
- `editText` tracks live typing in the textarea
- `displayText` is what gets shown to everyone when not editing

After a successful PUT, `displayText` is updated to match `editText`, and `isEditing` flips back to false. On failure, the edit form stays open.

---

## Delete Flow

Clicking Delete in the "..." menu sets `showDeleteConfirm = true`, which renders a fixed-position modal overlay:

```jsx
{showDeleteConfirm && (
  <div style={{ position: "fixed", inset: 0, backgroundColor: "rgba(0,0,0,0.4)", ... }}>
    <div>
      <p>Are you sure you want to delete this comment?</p>
      <button onClick={() => setShowDeleteConfirm(false)}>Cancel</button>
      <button onClick={handleDelete}>Confirm</button>
    </div>
  </div>
)}
```

`position: fixed; inset: 0` makes the overlay cover the entire viewport.

```js
const handleDelete = async () => {
  try {
    await api.delete(`/comments/${comment.id}`);
    setShowDeleteConfirm(false);
    onDelete?.(comment.id);  // notify parent to remove from list
  } catch {
    setShowDeleteConfirm(false);
  }
};
```

`onDelete?.(comment.id)` — the `?.` is *optional chaining* on a function call. If `onDelete` is undefined (e.g., the component is used without the prop), this does not crash; it just skips the call. If `onDelete` is provided (from BugDetail), it runs the filter to remove the deleted comment from the parent's state.

---

## Cypress Tests — Person 3

**File:** `cypress/e2e/comments.cy.js`

Like the bugs tests, all API calls are intercepted — no real network requests are made.

### Test Data

```js
const openBug = { id: 1, status: "OPEN", authorUsername: "alice", ... };
const fixedBug = { id: 2, status: "FIXED", authorUsername: "alice", ... };
const otherComment = { id: 10, authorUsername: "bob", score: 3, ... };
const ownComment = { id: 11, authorUsername: "alice", score: 0, ... };
```

### Auth Setup

Same `tokenFor` and `loginAs` helpers as the bugs tests. `loginAs("alice", "USER")` sets up localStorage so the app boots as alice.

### Test Flow Summary

| Test | What Is Tested | Key Assertions |
|---|---|---|
| Unauthenticated: no create form | Clears localStorage, visits bug | Comment text visible; `[aria-label="Comment text"]` not in DOM |
| Login → post comment → appears | Logs in as alice, intercepts POST, submits form | New comment text visible in list |
| Post on OPEN bug → flips to IN_PROGRESS | First GET returns OPEN, second GET (after post) returns IN_PROGRESS | "IN_PROGRESS" badge visible after submit |
| Cannot comment on FIXED bug | Logs in as alice, visits fixed bug | Form not in DOM; disabled message shown |
| Upvote → +1; upvote again → back | Intercepts POST vote, clicks ▲ twice | Score goes 3→4→3 |
| Downvote then upvote → +2 | Intercepts POST vote, clicks ▼ then ▲ | Score goes 3→2→4 |
| Cannot vote own comment | ownComment (authorUsername: alice), logged in as alice | No Upvote/Downvote buttons in DOM |
| Edit own comment | Intercepts PUT, clicks Edit in comment card, changes text | New text visible; old text gone |
| Delete own comment | Intercepts DELETE, opens "..." → Delete → Confirm | Comment text not in DOM |
| Moderator edits another's comment | loginAs mod MODERATOR, otherComment by bob | "..." menu visible; Edit and Delete buttons visible; edit saves |

### Selector Strategy

Because BugDetail also shows Edit and Delete buttons for the bug itself (when the logged-in user is the bug author), the tests scope within `[data-testid="comment-card"]` to avoid ambiguity:

```js
cy.get("[data-testid='comment-card']").within(() => {
  cy.contains("button", "Edit").click();
});
cy.get("[aria-label='Edit comment text']").clear().type("Updated comment text");
cy.get("[data-testid='comment-card']").within(() => {
  cy.contains("button", "Submit").click();
});
```

`.within()` restricts all Cypress commands inside it to look only within the matched element. Without this, `cy.contains("button", "Edit")` would match the bug's own Edit button before the comment's Edit button.

The delete confirmation modal (`position: fixed`) is rendered outside the comment card in the DOM, so the `cy.contains("button", "Confirm")` click is made outside `.within()`.

Aria-labels on key elements enable precise targeting:
- `aria-label="Upvote"` / `aria-label="Downvote"` on vote buttons
- `aria-label="Comment text"` on the create form textarea
- `aria-label="Edit comment text"` on the inline edit textarea
- `data-testid="comment-score"` on the score span
- `data-testid="comment-card"` on the card wrapper div

---

## How Person 3's Work Connects to Everyone Else

- **`BugDetail.jsx`** (Person 2) is where the comment section lives. Person 3 added state, the handler, and JSX directly into that file because the comment form needs access to `bug.status` and `id` which are already there.
- **`AuthContext`** (Person 1) — `useAuth()` is called in both BugDetail and CommentCard. BugDetail uses `isAuthenticated` to decide whether to show the create form. CommentCard uses `user.username` and `user.role` for permission checks.
- **`api` from `axios.js`** (Person 1) — imported directly in CommentCard and in BugDetail's comment submit handler. The JWT header injection happens automatically via the interceptor.
- **`getBug` from `bugService.js`** (Person 2) — called in BugDetail's `handleCommentSubmit` after a new comment is posted, to re-fetch the bug and reflect any status change.
