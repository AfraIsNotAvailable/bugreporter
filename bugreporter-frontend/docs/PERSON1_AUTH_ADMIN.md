# Person 1 — Foundation, Auth & Admin

This document covers the foundation of the frontend: how authentication works, how the app decides what pages a user can see, and how the admin and moderator panels are built. Everything else in the app (bugs, comments, voting) sits on top of these building blocks.

---

## Pages Owned

| Route | File | Access |
|---|---|---|
| `/login` | `src/pages/Login.jsx` | Public |
| `/register` | `src/pages/Register.jsx` | Public |
| `/admin` | `src/pages/Admin.jsx` | ADMIN only |
| `/moderator` | `src/pages/Moderator.jsx` | MODERATOR only |

The Navbar (`src/components/Navbar.jsx`) and the route guard (`src/routes/PrivateRoute.jsx`) are also Person 1's responsibility since everything depends on them.

---

## The Axios Instance

**File:** `src/api/axios.js`

Every API call in the app goes through a single shared Axios instance, not a raw `fetch` or a fresh `axios.create()` in each file. This is the single most important piece of shared infrastructure.

```js
const api = axios.create({
  baseURL: "http://localhost:8081/api",
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});
```

**What `axios.create` does:** It creates a pre-configured Axios client. Every call made through `api` automatically uses `http://localhost:8081/api` as the base URL, so callers write `/auth/login` instead of the full URL.

**What the interceptor does:** Before every request leaves the browser, the interceptor runs. It reads the JWT from `localStorage` and, if one exists, attaches it to the `Authorization` header as `Bearer <token>`. This is how protected backend routes know who is making the call. If there is no token (user is logged out), the header is simply omitted.

All three team members import and use this same instance:
```js
import api from "../api/axios";
```

---

## JWT Tokens — What They Are and How They Are Stored

A JWT (JSON Web Token) is a string made of three base64-encoded parts joined by dots:
```
header.payload.signature
```

The backend sends one back when a user logs in or registers. The payload part contains claims — in this app, the relevant ones are `sub` (the username) and `role`.

**Where it is stored:** `localStorage` under the key `"token"`. LocalStorage is persistent — it survives page refreshes and browser restarts until explicitly cleared.

**A second key `"user"` is also stored:** After the token is decoded, the extracted `{ username, role }` object is saved separately as `localStorage.setItem("user", JSON.stringify({ username, role }))`. This avoids re-decoding the JWT on every render.

**How the token is decoded** (`src/context/AuthContext.jsx`, `parseUserFromToken`):

1. Split the token string on `.` and take the middle part (the payload).
2. Undo URL-safe base64 encoding (replace `-` → `+`, `_` → `/`).
3. Add padding characters so `atob()` can decode it.
4. Decode with `atob()` and parse as JSON.
5. Extract `decoded.sub` as `username` and `decoded.role` (normalized via `normalizeRole`).

**Role normalization:** The backend may return roles prefixed with `ROLE_` (e.g., `ROLE_ADMIN`). `normalizeRole()` strips that prefix and uppercases the result, so the frontend always works with `"ADMIN"`, `"MODERATOR"`, or `"USER"`.

---

## AuthContext — Global Auth State

**File:** `src/context/AuthContext.jsx`

React's Context API lets you put data into a "box" at the top of the component tree and read it from anywhere below, without passing props through every level. `AuthContext` is that box for authentication state.

### What It Provides

```js
const value = {
  token,       // the raw JWT string (or null)
  user,        // { username, role } (or null)
  login,       // function: saves token, updates state
  logout,      // function: clears token, updates state
  isAuthenticated: Boolean(token),  // true if token exists
};
```

Any component that calls `useAuth()` gets access to all of these.

### Initial State on Page Load

Both `token` and `user` use lazy initialization — the function passed to `useState` runs once on mount:

```js
const [token, setToken] = useState(() => localStorage.getItem("token"));
const [user, setUser] = useState(() => {
  const savedUser = localStorage.getItem("user");
  if (savedUser) return JSON.parse(savedUser);
  return parseUserFromToken(localStorage.getItem("token"));
});
```

This means if a user refreshes the page, their session is restored from localStorage automatically. They do not need to log in again.

### The `login` Function

Called after a successful `/auth/login` or `/auth/register` API call:

```js
const login = (newToken) => {
  const decodedUser = parseUserFromToken(newToken);
  localStorage.setItem("token", newToken);
  if (decodedUser) {
    localStorage.setItem("user", JSON.stringify(decodedUser));
  }
  setToken(newToken);
  setUser(decodedUser);
  return decodedUser; // caller uses the role to decide where to redirect
};
```

It persists to localStorage, updates React state (which triggers a re-render in the Navbar and any other component reading auth state), and returns the decoded user so the caller can redirect based on role.

### The `logout` Function

```js
const logout = () => {
  localStorage.removeItem("token");
  localStorage.removeItem("user");
  setToken(null);
  setUser(null);
};
```

Clears both localStorage keys and nulls out the React state. The Navbar re-renders immediately showing Login/Register links.

### How to Read Auth State in Any Component

```js
import { useAuth } from "../context/AuthContext";

function MyComponent() {
  const { user, isAuthenticated, login, logout } = useAuth();
  // ...
}
```

---

## Login Page

**File:** `src/pages/Login.jsx`

### State

```js
const [username, setUsername] = useState("");
const [password, setPassword] = useState("");
const [error, setError] = useState("");
```

Three pieces of state: two for form inputs and one for error messages. Each input is a *controlled input* — its value comes from state, and every keystroke fires an `onChange` that updates state with `setUsername`/`setPassword`.

### Submit Flow

```js
const handleSubmit = async (e) => {
  e.preventDefault();          // stop browser default form submit
  setError("");                // clear any previous error

  try {
    const response = await api.post("/auth/login", { username, password });
    const loggedInUser = login(response.data.token); // save token, decode user

    if (loggedInUser?.role === "ADMIN") navigate("/admin");
    else if (loggedInUser?.role === "MODERATOR") navigate("/moderator");
    else navigate("/");
  } catch (err) {
    setError(getErrorMessage(err, "Login failed")); // show error
  }
};
```

`async/await` is used here because the API call takes time (network round-trip). `await` pauses execution inside the function until the response arrives without blocking the rest of the browser. The `try/catch` block handles any error the server returns (wrong password, banned user, network error).

**Post-login redirect:** Admins go to `/admin`, moderators to `/moderator`, regular users to `/`. This is the role-based redirect on login.

---

## Register Page

**File:** `src/pages/Register.jsx`

Same pattern as Login. Collects `username`, `email`, `password`, and `phoneNumber`. On success, the backend creates the account and returns a token immediately, so the user is logged in right after registering (no separate login step needed). Always redirects to `/` regardless of role.

---

## Private Routes

**File:** `src/routes/PrivateRoute.jsx`

```jsx
export default function PrivateRoute({ children, role, roles }) {
  const { isAuthenticated, user } = useAuth();

  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (role && user?.role !== role) return <Navigate to="/" replace />;
  if (roles && !roles.includes(user?.role)) return <Navigate to="/" replace />;

  return children;
}
```

This is a *wrapper component*. It wraps a page component and conditionally renders it or redirects.

**Three checks in order:**

1. **Not logged in at all** → redirect to `/login`.
2. **Logged in but wrong role (single role check)** → redirect to `/`.
3. **Logged in but not in allowed roles list (multi-role check)** → redirect to `/`.
4. **Passes all checks** → render the page.

**How it is used in App.jsx:**

```jsx
<Route
  path="/admin"
  element={
    <PrivateRoute role="ADMIN">
      <Admin />
    </PrivateRoute>
  }
/>
<Route
  path="/moderator"
  element={
    <PrivateRoute role="MODERATOR">
      <Moderator />
    </PrivateRoute>
  }
/>
```

Public routes (Home, BugList, BugDetail, Login, Register) are plain `<Route>` elements with no wrapper.

### Public vs Private Routes Summary

| Route | Who Can Access |
|---|---|
| `/` | Everyone |
| `/bugs` | Everyone |
| `/bugs/:id` | Everyone |
| `/login` | Everyone (redirect away if already logged in would be a future improvement) |
| `/register` | Everyone |
| `/admin` | ADMIN only |
| `/moderator` | MODERATOR only |

---

## The Navbar

**File:** `src/components/Navbar.jsx`

The Navbar renders conditionally based on auth state:

```jsx
const { isAuthenticated, logout, user } = useAuth();
const isAdmin = user?.role === "ADMIN";
const isModerator = user?.role === "MODERATOR";
```

| Condition | What Shows |
|---|---|
| Not authenticated | Bugs, Home, Login, Register links |
| Authenticated (USER) | Bugs, Home, Logout button |
| Authenticated (MODERATOR) | Bugs, Home, Moderator link, Logout button |
| Authenticated (ADMIN) | Bugs, Home, Admin link, Logout button |

The Navbar is rendered outside `<Routes>` in App.jsx, so it persists across all page navigations.

---

## ADMIN vs MODERATOR — What's Different

Both roles grant elevated privileges, but they are not equal:

| Capability | USER | MODERATOR | ADMIN |
|---|---|---|---|
| View bug list | Yes | Yes | Yes |
| Report / edit own bugs | Yes | Yes | Yes |
| Edit any bug | No | No | No |
| Delete any bug | No | Yes | Yes |
| Change bug status | No | Yes | Yes |
| Edit/delete any comment | No | Yes | Yes |
| View users table | No | Yes (no role change) | Yes |
| Ban / unban users | No | No | Yes |
| Change user roles | No | No | Yes |
| Access `/admin` | No | No | Yes |
| Access `/moderator` | No | Yes | No |

**Key distinction:** Moderators manage content (bugs, comments, bug statuses). Admins manage users (ban, change roles) and have a broader view of all content. They each have their own dedicated page — admins see `/admin`, moderators see `/moderator`.

---

## Admin Panel

**File:** `src/pages/Admin.jsx`

### Data Loading with `useEffect`

```js
useEffect(() => {
  Promise.all([api.get("/admin/bugs"), api.get("/admin/comments")])
    .then(([bugsRes, commentsRes]) => {
      setBugs(bugsRes.data);
      setComments(commentsRes.data);
    })
    .catch((err) => setError(getErrorMessage(err, "Failed to load admin data")))
    .finally(() => setLoading(false));
}, []);
```

`useEffect` with an empty dependency array `[]` runs once when the component first mounts (appears on screen). `Promise.all` fires both API calls simultaneously rather than one after the other, halving the wait time. `.then` handles success for both, `.catch` handles any error, `.finally` clears the loading state regardless of outcome.

### Three Sections

1. **Users** — rendered by `<AdminUsersTable canChangeRoles />`. The `canChangeRoles` prop enables the role dropdown (Moderator page uses the same component but without this prop, so the dropdown is hidden).

2. **Bugs** — maps over the `bugs` array:
   ```jsx
   {bugs.map((bug) => (
     <tr key={bug.id}>
       <td>{bug.id}</td>
       <td>{bug.title}</td>
       ...
     </tr>
   ))}
   ```
   `key={bug.id}` is required by React when rendering lists — it lets React efficiently update only changed rows.

3. **Comments** — same pattern, maps over `comments`.

---

## AdminUsersTable Component

**File:** `src/components/AdminUsersTable.jsx`

Reused by both Admin and Moderator pages. Accepts one prop:
- `canChangeRoles` (boolean, default false): whether to show the role dropdown

Fetches users via `GET /api/admin/users` inside its own `useEffect`. Manages its own state: `users`, `loading`, `error`, `actionError`.

**Ban/Unban:** Calls `PUT /api/admin/users/{id}/ban`. The response returns the updated user object. The component updates only that user in the array using `.map`:
```js
setUsers((prev) => prev.map((u) => (u.id === id ? res.data : u)));
```
This is a common React pattern: you never mutate the array directly; you return a new array with the one changed element.

**Role change:** Calls `PUT /api/admin/users/{id}/role` with the new role. Same `.map` pattern to update local state.

---

## Moderator Panel

**File:** `src/pages/Moderator.jsx`

Similar layout to Admin. Uses `<AdminUsersTable />` without `canChangeRoles`, so moderators can see users but cannot change their roles.

Adds a bugs table where each row has a status `<select>` dropdown. When changed, it calls `PATCH /api/bugs/{id}/status?status=VALUE` immediately (no submit button). The updated bug replaces itself in the local `bugs` array.

---

## Cypress Tests — Person 1

**File:** `cypress/e2e/auth.cy.js`

The tests use real network calls (no mocking) since they test the full auth flow end-to-end.

### Test Setup

A unique username is generated per test run using `Date.now()`:
```js
const username = `testuser${Date.now()}`;
```
This prevents tests from conflicting with each other if run multiple times.

### Test Flow

| Test | What Happens |
|---|---|
| Register new user | Visits `/register`, fills all fields, clicks Register, asserts URL is `/` and welcome text is visible |
| Login with valid credentials | Visits `/login`, submits form, asserts redirect to `/` and `localStorage.token` is not null |
| Login with wrong password | Submits wrong password, waits for API response, asserts status 400/401/403 and URL still contains `/login` |
| Unauthenticated → `/admin` redirects | Clears localStorage, visits `/admin`, asserts redirect to `/login` |
| Admin: ban user | Logs in as admin (`ana`/`ana`), finds the test user's row, clicks Ban, waits for the API call to confirm `banned: true`, reloads, asserts "Yes" in banned column and "Unban" button |
| Login with banned user | Clears localStorage, tries to log in as the now-banned user, asserts 4xx response and URL stays on `/login` |
| Admin: change role | Logs in as admin, finds user's row, changes the role select to "MODERATOR", asserts the select now shows MODERATOR |

### The `loginAsAdmin` Helper

```js
const loginAsAdmin = () => {
  cy.visit("/login");
  cy.contains("label", "Username").next("input").type(adminUsername);
  cy.contains("label", "Password").next("input").type(adminPassword);
  cy.contains("button", "Login").click();
  cy.url().should("include", "/admin");
};
```

Reused across tests that need admin access. Cypress commands are chainable and auto-retry until the assertion passes or times out.

---

## How Person 1's Work Connects to Everyone Else

- **`src/api/axios.js`** — Person 2 and Person 3 import this for every API call. Without it, no data would load.
- **`AuthContext`** — Person 2 reads `isAuthenticated`, `user.role`, and `user.username` to decide what controls to show on bugs. Person 3 reads the same to decide who can vote, edit, or post comments.
- **`PrivateRoute`** — Protects `/admin` and `/moderator` from unauthorized access. Transparent to Persons 2 and 3.
- **`Navbar`** — Visible on every page. Shows the Admin/Moderator links only to the right roles.
