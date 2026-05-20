import { http, HttpResponse } from "msw";
import { db } from "./db";

const BASE = "http://localhost:8081/api";

// ── Helpers ──────────────────────────────────────────────

function makeToken(username, role) {
  const payload = btoa(JSON.stringify({ sub: username, role }))
    .replace(/\+/g, "-").replace(/\//g, "_").replace(/=/g, "");
  return `demo.${payload}.demo`;
}

function currentUser(request) {
  const auth = request.headers.get("Authorization") ?? "";
  const token = auth.replace("Bearer ", "");
  if (!token || !token.startsWith("demo.")) return null;
  try {
    const [, b64] = token.split(".");
    const padded = b64.replace(/-/g, "+").replace(/_/g, "/").padEnd(b64.length + ((4 - b64.length % 4) % 4), "=");
    const { sub } = JSON.parse(atob(padded));
    return db.getUserByUsername(sub) ?? null;
  } catch { return null; }
}

function requireAuth(request) {
  const user = currentUser(request);
  if (!user) return HttpResponse.json({ message: "Authentication is required" }, { status: 401 });
  if (user.banned) return HttpResponse.json({ message: "Your account is banned." }, { status: 403 });
  return user;
}

function isMod(user) {
  return user.role === "MODERATOR" || user.role === "ADMIN";
}

function applyBugVote(bugId, userId, voteType) {
  const bug = db.getBugById(bugId);
  if (!bug) return null;
  const author = db.getUserById(bug.authorId);
  const existing = db.bugVotes.find(v => v.bugId === +bugId && v.userId === +userId);

  if (existing) {
    if (existing.voteType === voteType) {
      // remove
      db.bugVotes.splice(db.bugVotes.indexOf(existing), 1);
      bug.voteScore += voteType === "UPVOTE" ? -1 : 1;
      if (author) author.score += voteType === "UPVOTE" ? -2.5 : 1.5;
    } else {
      // flip
      existing.voteType = voteType;
      bug.voteScore += voteType === "UPVOTE" ? 2 : -2;
      if (author) author.score += voteType === "UPVOTE" ? 4.0 : -4.0;
    }
  } else {
    // new
    db.bugVotes.push({ bugId: +bugId, userId: +userId, voteType });
    bug.voteScore += voteType === "UPVOTE" ? 1 : -1;
    if (author) author.score += voteType === "UPVOTE" ? 2.5 : -1.5;
  }
  return bug;
}

function applyCommentVote(commentId, userId, voteType) {
  const comment = db.getCommentById(commentId);
  if (!comment) return null;
  const author  = db.getUserById(comment.authorId);
  const voter   = db.getUserById(userId);
  const existing = db.commentVotes.find(v => v.commentId === +commentId && v.userId === +userId);

  if (existing) {
    if (existing.voteType === voteType) {
      // remove
      db.commentVotes.splice(db.commentVotes.indexOf(existing), 1);
      comment.score += voteType === "UPVOTE" ? -1 : 1;
      if (author) author.score += voteType === "UPVOTE" ? -5.0 : 2.5;
      if (voter && voteType === "DOWNVOTE") voter.score += 1.5;
    } else {
      // flip
      existing.voteType = voteType;
      comment.score += voteType === "UPVOTE" ? 2 : -2;
      if (author) author.score += voteType === "UPVOTE" ? 7.5 : -7.5;
      if (voter) voter.score += voteType === "UPVOTE" ? 1.5 : -1.5;
    }
  } else {
    // new
    db.commentVotes.push({ commentId: +commentId, userId: +userId, voteType });
    comment.score += voteType === "UPVOTE" ? 1 : -1;
    if (author) author.score += voteType === "UPVOTE" ? 5.0 : -2.5;
    if (voter && voteType === "DOWNVOTE") voter.score -= 1.5;
  }
  return comment;
}

// ── Auth ─────────────────────────────────────────────────

const authHandlers = [
  http.post(`${BASE}/auth/login`, async ({ request }) => {
    const { username, password } = await request.json();
    const user = db.getUserByUsername(username);
    if (!user) return HttpResponse.json({ message: "User not found" }, { status: 400 });
    if (user.banned) return HttpResponse.json({ message: "User is banned" }, { status: 403 });
    if (user.password !== password) return HttpResponse.json({ message: "Invalid password" }, { status: 400 });
    return HttpResponse.json({ token: makeToken(user.username, user.role) });
  }),

  http.post(`${BASE}/auth/register`, async ({ request }) => {
    const { username, email, password, phoneNumber } = await request.json();
    if (db.users.find(u => u.username === username))
      return HttpResponse.json({ message: "Username already taken" }, { status: 400 });
    if (db.users.find(u => u.email === email))
      return HttpResponse.json({ message: "Email already in use" }, { status: 400 });
    const newUser = {
      id: db.nextId(), username, email, password,
      role: "USER", banned: false,
      createdAt: new Date().toISOString(),
      phoneNumber: phoneNumber ?? null,
      score: 0.0,
    };
    db.users.push(newUser);
    return HttpResponse.json({ token: makeToken(username, "USER") });
  }),
];

// ── Bugs ─────────────────────────────────────────────────

const bugHandlers = [
  http.get(`${BASE}/bugs`, ({ request }) => {
    const user = currentUser(request);
    const userId = user?.id ?? null;
    const sorted = [...db.bugs].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
    return HttpResponse.json(sorted.map(b => db.bugResponse(b, userId)));
  }),

  http.get(`${BASE}/bugs/filter`, ({ request }) => {
    const user = currentUser(request);
    const userId = user?.id ?? null;
    const url = new URL(request.url);
    const search = url.searchParams.get("search");
    const tag    = url.searchParams.get("tag");
    const mine   = url.searchParams.get("mine");
    const byUser = url.searchParams.get("userId");

    let results = [...db.bugs];
    if (mine === "true" && user) {
      results = results.filter(b => b.authorId === user.id);
    } else if (tag) {
      const tagRec = db.getTagByName(tag);
      results = tagRec ? results.filter(b => b.tagIds.includes(tagRec.id)) : [];
    } else if (search) {
      results = results.filter(b => b.title.toLowerCase().includes(search.toLowerCase()));
    } else if (byUser) {
      results = results.filter(b => b.authorId === +byUser);
    }
    results.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
    return HttpResponse.json(results.map(b => db.bugResponse(b, userId)));
  }),

  http.get(`${BASE}/bugs/:id`, ({ params, request }) => {
    const user = currentUser(request);
    const bug = db.getBugById(params.id);
    if (!bug) return HttpResponse.json({ message: "Bug not found" }, { status: 404 });
    return HttpResponse.json(db.bugResponse(bug, user?.id ?? null));
  }),

  http.post(`${BASE}/bugs`, async ({ request }) => {
    const user = requireAuth(request);
    if (user instanceof HttpResponse) return user;
    const { title, text, imageUrl } = await request.json();
    const bug = {
      id: db.nextId(), title, text,
      imageUrl: imageUrl ?? null,
      status: "OPEN",
      createdAt: new Date().toISOString(),
      authorId: user.id,
      voteScore: 0,
      tagIds: [],
    };
    db.bugs.push(bug);
    return HttpResponse.json(db.bugResponse(bug, user.id), { status: 201 });
  }),

  http.put(`${BASE}/bugs/:id`, async ({ params, request }) => {
    const user = requireAuth(request);
    if (user instanceof HttpResponse) return user;
    const bug = db.getBugById(params.id);
    if (!bug) return HttpResponse.json({ message: "Bug not found" }, { status: 404 });
    if (bug.authorId !== user.id && !isMod(user))
      return HttpResponse.json({ message: "Forbidden" }, { status: 403 });
    const { title, text, imageUrl } = await request.json();
    bug.title = title;
    bug.text  = text;
    if (imageUrl != null) bug.imageUrl = imageUrl;
    return HttpResponse.json(db.bugResponse(bug, user.id));
  }),

  http.delete(`${BASE}/bugs/:id`, ({ params, request }) => {
    const user = requireAuth(request);
    if (user instanceof HttpResponse) return user;
    const idx = db.bugs.findIndex(b => b.id === +params.id);
    if (idx === -1) return HttpResponse.json({ message: "Bug not found" }, { status: 404 });
    const bug = db.bugs[idx];
    if (bug.authorId !== user.id && !isMod(user))
      return HttpResponse.json({ message: "Forbidden" }, { status: 403 });
    db.bugs.splice(idx, 1);
    // cascade delete comments
    db.comments.splice(0, db.comments.length, ...db.comments.filter(c => c.bugId !== +params.id));
    return new HttpResponse(null, { status: 204 });
  }),

  http.post(`${BASE}/bugs/:id/tags`, async ({ params, request }) => {
    const user = requireAuth(request);
    if (user instanceof HttpResponse) return user;
    const bug = db.getBugById(params.id);
    if (!bug) return HttpResponse.json({ message: "Bug not found" }, { status: 404 });
    const tagNames = await request.json();
    tagNames.forEach(name => {
      const trimmed = name.trim();
      let tag = db.getTagByName(trimmed);
      if (!tag) {
        tag = { id: db.nextId(), name: trimmed };
        db.tags.push(tag);
      }
      if (!bug.tagIds.includes(tag.id)) bug.tagIds.push(tag.id);
    });
    return HttpResponse.json(db.bugResponse(bug, user.id));
  }),

  http.patch(`${BASE}/bugs/:id/resolve`, ({ params, request }) => {
    const user = requireAuth(request);
    if (user instanceof HttpResponse) return user;
    const bug = db.getBugById(params.id);
    if (!bug) return HttpResponse.json({ message: "Bug not found" }, { status: 404 });
    if (bug.authorId !== user.id)
      return HttpResponse.json({ message: "Only the author can resolve this bug" }, { status: 403 });
    bug.status = "FIXED";
    return HttpResponse.json(db.bugResponse(bug, user.id));
  }),

  http.patch(`${BASE}/bugs/:id/status`, ({ params, request }) => {
    const user = requireAuth(request);
    if (user instanceof HttpResponse) return user;
    if (!isMod(user)) return HttpResponse.json({ message: "Forbidden" }, { status: 403 });
    const bug = db.getBugById(params.id);
    if (!bug) return HttpResponse.json({ message: "Bug not found" }, { status: 404 });
    const url = new URL(request.url);
    bug.status = url.searchParams.get("status") ?? bug.status;
    return HttpResponse.json(db.bugResponse(bug, user.id));
  }),

  http.post(`${BASE}/bugs/:id/vote`, async ({ params, request }) => {
    const user = requireAuth(request);
    if (user instanceof HttpResponse) return user;
    const bug = db.getBugById(params.id);
    if (!bug) return HttpResponse.json({ message: "Bug not found" }, { status: 404 });
    if (bug.authorId === user.id)
      return HttpResponse.json({ message: "Cannot vote on your own bug" }, { status: 403 });
    const { voteType } = await request.json();
    applyBugVote(+params.id, user.id, voteType.toUpperCase());
    return HttpResponse.json(db.bugResponse(bug, user.id));
  }),
];

// ── Comments ─────────────────────────────────────────────

const commentHandlers = [
  http.get(`${BASE}/comments/bug/:bugId`, ({ params, request }) => {
    const user = currentUser(request);
    const userId = user?.id ?? null;
    const bugId = +params.bugId;
    if (!db.getBugById(bugId))
      return HttpResponse.json({ message: "Bug not found" }, { status: 404 });
    const list = db.comments
      .filter(c => c.bugId === bugId)
      .sort((a, b) => b.score - a.score || new Date(b.createdAt) - new Date(a.createdAt));
    return HttpResponse.json(list.map(c => db.commentResponse(c, userId)));
  }),

  http.post(`${BASE}/comments/bug/:bugId`, async ({ params, request }) => {
    const user = requireAuth(request);
    if (user instanceof HttpResponse) return user;
    const bugId = +params.bugId;
    const bug = db.getBugById(bugId);
    if (!bug) return HttpResponse.json({ message: "Bug not found" }, { status: 404 });
    if (bug.status === "FIXED" || bug.status === "CLOSED")
      return HttpResponse.json({ message: "Bug is resolved. No more comments." }, { status: 403 });
    const { text, imageUrl } = await request.json();
    if (bug.status === "OPEN") bug.status = "IN_PROGRESS";
    const comment = {
      id: db.nextId(), text,
      imageUrl: imageUrl ?? null,
      createdAt: new Date().toISOString(),
      authorId: user.id,
      bugId,
      score: 0,
    };
    db.comments.push(comment);
    return HttpResponse.json(db.commentResponse(comment, user.id), { status: 201 });
  }),

  http.put(`${BASE}/comments/:id`, async ({ params, request }) => {
    const user = requireAuth(request);
    if (user instanceof HttpResponse) return user;
    const comment = db.getCommentById(params.id);
    if (!comment) return HttpResponse.json({ message: "Comment not found" }, { status: 404 });
    if (comment.authorId !== user.id && !isMod(user))
      return HttpResponse.json({ message: "Forbidden" }, { status: 403 });
    const { text, imageUrl } = await request.json();
    comment.text = text;
    if (imageUrl != null) comment.imageUrl = imageUrl;
    return HttpResponse.json(db.commentResponse(comment, user.id));
  }),

  http.delete(`${BASE}/comments/:id`, ({ params, request }) => {
    const user = requireAuth(request);
    if (user instanceof HttpResponse) return user;
    const idx = db.comments.findIndex(c => c.id === +params.id);
    if (idx === -1) return HttpResponse.json({ message: "Comment not found" }, { status: 404 });
    const comment = db.comments[idx];
    if (comment.authorId !== user.id && !isMod(user))
      return HttpResponse.json({ message: "Forbidden" }, { status: 403 });
    db.comments.splice(idx, 1);
    return new HttpResponse(null, { status: 204 });
  }),

  http.post(`${BASE}/comments/:id/vote`, async ({ params, request }) => {
    const user = requireAuth(request);
    if (user instanceof HttpResponse) return user;
    const comment = db.getCommentById(params.id);
    if (!comment) return HttpResponse.json({ message: "Comment not found" }, { status: 404 });
    if (comment.authorId === user.id)
      return HttpResponse.json({ message: "Cannot vote on your own comment" }, { status: 403 });
    const { voteType } = await request.json();
    applyCommentVote(+params.id, user.id, voteType.toUpperCase());
    return HttpResponse.json(db.commentResponse(comment, user.id));
  }),
];

// ── Admin ─────────────────────────────────────────────────

const adminHandlers = [
  http.get(`${BASE}/admin/users`, ({ request }) => {
    const user = requireAuth(request);
    if (user instanceof HttpResponse) return user;
    if (!isMod(user)) return HttpResponse.json({ message: "Forbidden" }, { status: 403 });
    return HttpResponse.json(db.users.map(u => db.userResponse(u)));
  }),

  http.get(`${BASE}/admin/bugs`, ({ request }) => {
    const user = requireAuth(request);
    if (user instanceof HttpResponse) return user;
    if (user.role !== "ADMIN") return HttpResponse.json({ message: "Forbidden" }, { status: 403 });
    return HttpResponse.json(db.bugs.map(b => db.bugResponse(b, user.id)));
  }),

  http.get(`${BASE}/admin/comments`, ({ request }) => {
    const user = requireAuth(request);
    if (user instanceof HttpResponse) return user;
    if (user.role !== "ADMIN") return HttpResponse.json({ message: "Forbidden" }, { status: 403 });
    return HttpResponse.json(db.comments.map(c => db.commentResponse(c, user.id)));
  }),

  http.put(`${BASE}/admin/users/:id/ban`, ({ params, request }) => {
    const user = requireAuth(request);
    if (user instanceof HttpResponse) return user;
    if (!isMod(user)) return HttpResponse.json({ message: "Forbidden" }, { status: 403 });
    const target = db.getUserById(params.id);
    if (!target) return HttpResponse.json({ message: "User not found" }, { status: 404 });
    if (target.role === "ADMIN" || target.role === "MODERATOR")
      return HttpResponse.json({ message: "Cannot ban moderators or admins" }, { status: 403 });
    const url = new URL(request.url);
    target.banned = url.searchParams.get("banned") === "true";
    return HttpResponse.json(db.userResponse(target));
  }),

  http.put(`${BASE}/admin/users/:id/role`, ({ params, request }) => {
    const user = requireAuth(request);
    if (user instanceof HttpResponse) return user;
    if (user.role !== "ADMIN") return HttpResponse.json({ message: "Forbidden" }, { status: 403 });
    const target = db.getUserById(params.id);
    if (!target) return HttpResponse.json({ message: "User not found" }, { status: 404 });
    const url = new URL(request.url);
    target.role = url.searchParams.get("role") ?? target.role;
    return HttpResponse.json(db.userResponse(target));
  }),
];

export const handlers = [
  ...authHandlers,
  ...bugHandlers,
  ...commentHandlers,
  ...adminHandlers,
];
