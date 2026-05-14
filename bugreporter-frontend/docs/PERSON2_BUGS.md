# Person 2 — Bugs

This document covers the bug listing, bug detail, and all bug-related interactions: creating, editing, deleting, tagging, searching, filtering, and changing status. This is the core content of the application.

---

## Pages Owned

| Route | File | Access |
|---|---|---|
| `/bugs` | `src/pages/BugList.jsx` | Public (read) / Authenticated (create) |
| `/bugs/:id` | `src/pages/BugDetail.jsx` | Public (read) / Authenticated (edit, delete, comment) |

The `BugCard` component (`src/components/BugCard.jsx`) and the bug service (`src/services/bugService.js`) are also Person 2's work.

---

## The Bug Service

**File:** `src/services/bugService.js`

Every API call related to bugs is centralized here. All functions return the Axios promise directly so callers can `await` them and read `.data` from the response.

```js
import api from "../api/axios";

export const getBugs = () => api.get("/bugs");
export const filterBugs = (params) => api.get("/bugs/filter", { params });
export const getBug = (id) => api.get(`/bugs/${id}`);
export const createBug = (payload) => api.post("/bugs", payload);
export const updateBug = (id, payload) => api.put(`/bugs/${id}`, payload);
export const deleteBug = (id) => api.delete(`/bugs/${id}`);
export const addBugTags = (id, tags) => api.post(`/bugs/${id}/tags`, tags);
export const resolveBug = (id) => api.patch(`/bugs/${id}/resolve`);
export const updateBugStatus = (id, status) =>
  api.patch(`/bugs/${id}/status`, null, { params: { status } });
```

`filterBugs(params)` is the backbone of search and filtering. Axios serializes the `params` object into a query string automatically: `filterBugs({ search: "login" })` becomes `GET /bugs/filter?search=login`.

---

## Bug List Page

**File:** `src/pages/BugList.jsx`

This page does the most work. It manages all the filtering, the create form, and rendering the list.

### State

```js
const [bugs, setBugs] = useState([]);          // array of bug objects from API
const [search, setSearch] = useState("");      // current search input value
const [tag, setTag] = useState("");            // current tag filter input value
const [userId, setUserId] = useState("");      // current userId filter input value
const [showNewForm, setShowNewForm] = useState(false); // toggle create form visibility
const [form, setForm] = useState({ title: "", text: "", imageUrl: "" }); // create form fields
const [loading, setLoading] = useState(true); // loading indicator
const [error, setError] = useState("");       // error message to display
```

`useState` always comes in pairs: the current value and the function to update it. When the updater function is called, React re-renders the component with the new value. All inputs are *controlled* — their `value` is always in sync with state.

### Initial Data Load with `useEffect`

```js
useEffect(() => {
  loadBugs();
}, []);
```

`useEffect` with `[]` as the dependency array runs once after the component first renders. Here it kicks off the initial fetch. Without `useEffect`, the fetch would run on every re-render, causing infinite loops.

### The `loadBugs` Function

```js
const loadBugs = async (params = null) => {
  setLoading(true);
  setError("");

  try {
    const response = params ? await filterBugs(params) : await getBugs();
    setBugs(response.data);
  } catch (err) {
    setError(err.response?.data?.message || "Could not load bugs");
  } finally {
    setLoading(false);
  }
};
```

This single function handles both the "show all bugs" and "show filtered bugs" cases. If `params` is provided, it calls `filterBugs`; if null, it calls `getBugs`. The `finally` block always runs — it clears the loading state whether the request succeeded or failed.

### How Search Works

The search input is a controlled input tied to `search` state. When the user types, `setSearch` updates the state:

```jsx
<input
  aria-label="Search by title"
  value={search}
  onChange={(event) => setSearch(event.target.value)}
  placeholder="Search title"
/>
```

Nothing happens to the list yet — this is just tracking what the user typed. The actual search fires only when the form is submitted:

```js
const handleSearch = (event) => {
  event.preventDefault();  // prevent browser from refreshing the page
  loadBugs(search.trim() ? { search: search.trim() } : null);
};
```

If the search box is empty, `search.trim()` is an empty string (falsy), so `loadBugs(null)` is called, which fetches all bugs. Otherwise `{ search: "whatever they typed" }` is passed, which becomes `GET /bugs/filter?search=whatever+they+typed`.

**The backend does the actual filtering.** The frontend only sends the query parameter — the API returns only the matching bugs.

### How Tag Filtering Works

Tag filtering has two entry points:

**1. Manual input + button:**
```js
const handleTagFilter = (selectedTag = tag) => {
  loadBugs(selectedTag.trim() ? { tag: selectedTag.trim() } : null);
};
```
The function has a default parameter `selectedTag = tag`. If called with no argument (from the button click), it uses the `tag` state. If called with an argument (from clicking a tag chip on a bug), it uses that argument.

**2. Clicking a tag chip on a bug card:**
```jsx
{(bug.tags || []).map((bugTag) => (
  <button
    key={bugTag}
    onClick={() => {
      setTag(bugTag);           // update the input box to show the tag
      handleTagFilter(bugTag);  // immediately filter by it
    }}
  >
    {bugTag}
  </button>
))}
```

Clicking any tag chip on any bug in the list instantly filters the list to show only bugs with that tag, and also updates the tag input field to reflect the active filter.

### How "My Bugs" Works

```js
const handleMine = () => {
  loadBugs({ mine: true });
};
```

Calls `GET /bugs/filter?mine=true`. The backend uses the JWT in the request header to identify the current user and returns only their bugs.

### How "By User" Works

```js
const handleUserFilter = (event) => {
  event.preventDefault();
  loadBugs(userId.trim() ? { userId: userId.trim() } : null);
};
```

Filters by a specific user ID: `GET /bugs/filter?userId=123`.

### Clear Button

```jsx
<button onClick={() => loadBugs()}>Clear</button>
```

Calls `loadBugs()` with no arguments (so `params` defaults to `null`), which fetches all bugs unfiltered.

### Bug List Rendering

```jsx
{!loading &&
  bugs.map((bug) => (
    <article key={bug.id} style={rowStyle}>
      <h2>
        <Link to={`/bugs/${bug.id}`}>#{bug.id} | {bug.title}</Link>
      </h2>
      <span>{bug.status}</span>
      <p>Author: {bug.authorUsername} | {formatDate(bug.createdAt)}</p>
      <div>
        {(bug.tags || []).map((bugTag) => (
          <button key={bugTag} onClick={() => handleTagFilter(bugTag)}>
            {bugTag}
          </button>
        ))}
      </div>
    </article>
  ))}
```

`bugs.map(...)` transforms the array of bug objects into an array of JSX elements. React renders all of them. The `key` prop on each element is required and must be unique within the list — React uses it internally to track which elements have changed, been added, or been removed.

`bug.tags || []` is a safety guard: if `bug.tags` is null or undefined (which can happen if the bug has no tags), the `||` fallback ensures `.map` has an empty array to work with instead of crashing.

### Report Bug Create Form

```js
const [showNewForm, setShowNewForm] = useState(false);
```

The form is hidden by default and shown by toggling `showNewForm`:

```jsx
<button onClick={() => setShowNewForm(!showNewForm)}>Report Bug</button>
{showNewForm && (
  <form onSubmit={handleCreate}>
    ...
  </form>
)}
```

`{showNewForm && <form>...}` — the `&&` operator is a common React pattern for conditional rendering. If `showNewForm` is false, nothing renders. If true, the form renders.

The form fields share a single `form` state object:

```js
const [form, setForm] = useState({ title: "", text: "", imageUrl: "" });
```

Each input updates its own field using spread syntax:

```jsx
onChange={(event) => setForm({ ...form, title: event.target.value })}
```

`{ ...form, title: event.target.value }` copies all existing fields and overrides only `title`. This keeps the other fields intact.

**On submit:**

```js
const handleCreate = async (event) => {
  event.preventDefault();
  try {
    await createBug(form);        // POST to /api/bugs
    setForm({ title: "", text: "", imageUrl: "" }); // reset form
    setShowNewForm(false);         // hide form
    await loadBugs();              // refresh list to show new bug
  } catch (err) {
    setError(err.response?.data?.message || "Could not create bug");
  }
};
```

---

## Bug Detail Page

**File:** `src/pages/BugDetail.jsx`

This page loads a single bug and its comments. It is also where Person 3's comment section is embedded.

### Route Parameter

The route `/bugs/:id` means `:id` is a dynamic segment. React Router makes it available via:

```js
const { id } = useParams();
```

So visiting `/bugs/42` gives `id = "42"`.

### Navigation

```js
const navigate = useNavigate();
```

`useNavigate` returns a function that programmatically moves the user to a different page. Used after deleting a bug: `navigate("/bugs")`.

### Data Loading

```js
const loadBug = useCallback(async () => {
  setLoading(true);
  try {
    const [bugRes, commentsRes] = await Promise.all([
      getBug(id),
      api.get(`/comments/bug/${id}`),
    ]);
    setBug(bugRes.data);
    setComments(commentsRes.data);
  } catch (err) {
    setError(err.response?.data?.message || "Failed to load bug details");
  } finally {
    setLoading(false);
  }
}, [id]);

useEffect(() => {
  loadBug();
}, [loadBug]);
```

`useCallback` is used here so that `loadBug` only gets a new function reference when `id` changes. This matters because `loadBug` is listed in the `useEffect` dependency array — if `loadBug` changed on every render, the effect would loop infinitely.

`Promise.all` fetches the bug and its comments simultaneously. The result is an array destructured into `[bugRes, commentsRes]`.

### Permission Checks

```js
const isModerator = user?.role === "MODERATOR" || user?.role === "ADMIN";
const isAuthor = Boolean(user?.username && bug?.authorUsername === user.username);
const canEdit = isAuthenticated && isAuthor;
const canDelete = isAuthenticated && (isAuthor || isModerator);
const canResolve = isAuthor && bug?.status !== "FIXED" && bug?.status !== "CLOSED";
```

These are plain boolean values computed during render. The `?.` operator is *optional chaining* — it short-circuits to `undefined` if the left side is null or undefined, preventing crashes when the user is not logged in.

| Variable | True When |
|---|---|
| `isModerator` | User has role MODERATOR or ADMIN |
| `isAuthor` | Logged-in username matches bug's `authorUsername` |
| `canEdit` | Authenticated AND author |
| `canDelete` | Authenticated AND (author OR moderator) |
| `canResolve` | Author AND bug is not FIXED or CLOSED |

These booleans gate the rendering of controls:

```jsx
{canEdit && !editing && (
  <button onClick={() => setEditing(true)}>Edit</button>
)}
{canDelete && (
  <button onClick={handleDelete}>Delete</button>
)}
{canResolve && (
  <button onClick={handleResolve}>Mark as Resolved</button>
)}
{isModerator && (
  <select aria-label="Bug status" value={bug.status} onChange={handleStatus}>
    {statuses.map((status) => (
      <option key={status} value={status}>{status}</option>
    ))}
  </select>
)}
```

### Edit Flow

```js
const [editing, setEditing] = useState(false);
const [form, setForm] = useState({ title: "", text: "", imageUrl: "" });
```

When the user clicks "Edit", `setEditing(true)` replaces the display view with an editable form. `form` is pre-populated when the bug loads:

```js
setForm({
  title: bugRes.data.title || "",
  text: bugRes.data.text || "",
  imageUrl: bugRes.data.imageUrl || "",
});
```

On submit, `updateBug(id, form)` sends `PUT /api/bugs/{id}` with the updated fields. The response contains the updated bug, which replaces `bug` state: `setBug(response.data)`.

### Delete Flow

```js
const handleDelete = async () => {
  await deleteBug(id);
  navigate("/bugs");  // send user back to the list
};
```

After deletion, the backend returns 204 No Content. The frontend navigates away because there is no bug to display.

### Mark as Resolved

```js
const handleResolve = async () => {
  const response = await resolveBug(id);
  setBug(response.data);  // update bug state with new status (FIXED)
};
```

Calls `PATCH /api/bugs/{id}/resolve`. The backend changes status to FIXED and returns the updated bug.

### Moderator Status Dropdown

```js
const handleStatus = async (event) => {
  const response = await updateBugStatus(id, event.target.value);
  setBug(response.data);
};
```

The `<select>` `onChange` handler fires immediately when a new option is selected. `event.target.value` is the selected status string. Calls `PATCH /api/bugs/{id}/status?status=CLOSED` (or whichever status was chosen).

### Tags

```js
const handleTags = async (event) => {
  event.preventDefault();
  const tags = tagInput
    .split(",")
    .map((tag) => tag.trim())
    .filter(Boolean);  // remove empty strings
  const response = await addBugTags(id, tags);
  setBug(response.data);
};
```

Tags are entered as a comma-separated string (e.g. `"auth, ui, login"`). The code splits on comma, trims whitespace from each, removes empty strings, and sends the array to `POST /api/bugs/{id}/tags`. The backend returns the updated bug with the new tags added.

---

## Cypress Tests — Person 2

**File:** `cypress/e2e/bugs.cy.js`

These tests use **intercepted (mocked) API calls** — they never hit the real backend. This makes tests fast and deterministic.

### Test Data

```js
const bugOne = {
  id: 1, title: "Login button broken", status: "OPEN",
  authorUsername: "alice", tags: ["auth", "ui"], ...
};
const bugTwo = {
  id: 2, title: "Dashboard chart fails", status: "IN_PROGRESS",
  authorUsername: "bob", tags: ["dashboard"], ...
};
```

### `tokenFor` and `loginAs` Helpers

```js
function tokenFor(username, role) {
  const payload = btoa(JSON.stringify({ sub: username, role })).replace(/=/g, "");
  return `header.${payload}.signature`;
}

function loginAs(username = "alice", role = "USER") {
  cy.visit("/", {
    onBeforeLoad(win) {
      win.localStorage.setItem("token", tokenFor(username, role));
      win.localStorage.setItem("user", JSON.stringify({ username, role }));
    },
  });
}
```

`tokenFor` crafts a fake JWT that AuthContext can actually decode (the payload is real base64-encoded JSON with `sub` and `role`). `loginAs` visits the app and sets both localStorage keys before the page loads, so the app boots up as if the user is already logged in.

### `beforeEach` Setup

```js
beforeEach(() => {
  cy.intercept("GET", "**/api/bugs", [bugOne, bugTwo]).as("getBugs");
  cy.intercept("GET", "**/api/bugs/1", bugOne).as("getBug");
  cy.intercept("GET", "**/api/comments/bug/1", []).as("getComments");
});
```

`cy.intercept` catches matching network requests and responds with the provided data instead of hitting the real server. `**` matches any host. `.as("getBugs")` names the intercept so tests can `cy.wait("@getBugs")` to pause until the response arrives.

### Test Flow Summary

| Test | Setup | Assertion |
|---|---|---|
| Bug list + detail loads unauthenticated | No auth, intercepts GET bugs | Sees bug title, status, author, tag, navigates to detail, sees comment placeholder |
| Search + filter by tag | Extra intercepts for filter URLs, type and click buttons | List updates to show only matching bug |
| Create bug | Intercept POST bugs, login as alice, fill form | New bug appears in list |
| Edit and delete own bug | loginAs alice, intercept PUT and DELETE | Edited title visible; deleted bug not in list |
| Author marks resolved | loginAs alice, intercept PATCH resolve | FIXED status shown |
| Moderator changes status | loginAs mod MODERATOR, intercept PATCH status | CLOSED status shown |
| Regular user cannot see moderator controls | loginAs charlie USER | No status select in DOM |
| Moderator can delete other's bug, not edit | loginAs mod MODERATOR | No Edit button; Delete works |
| Non-owner cannot edit or delete | loginAs charlie USER | Neither Edit nor Delete button in DOM |

---

## How Person 2's Work Connects to Everyone Else

- **`BugDetail.jsx`** contains the comment section, which Person 3 built into it. The `bug.status` is passed implicitly (it is already in `bug` state) to control whether the comment form shows.
- **`bugService.js`** is also used in BugDetail which Person 3's comment form reads (calls `getBug` after posting a comment to refresh the status).
- **`AuthContext`** (Person 1) is consumed by BugList and BugDetail to determine what controls to render.
- **`api` from `axios.js`** (Person 1) is imported by `bugService.js` and used directly in BugDetail for comment fetching.
