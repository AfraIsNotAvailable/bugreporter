// In-memory database for demo mode. Seeded from the same placeholder data the backend uses.
// State resets on page refresh — intentional for a portfolio demo.

const seedUsers = [
  { id: 1, username: "ana",   email: "ana@example.com",   password: "ana",   role: "ADMIN",     banned: false, createdAt: "2026-03-01T09:00:00", phoneNumber: null, score: 142.0 },
  { id: 2, username: "mihai", email: "mihai@example.com", password: "mihai", role: "MODERATOR", banned: false, createdAt: "2026-03-02T10:30:00", phoneNumber: null, score: 87.5  },
  { id: 3, username: "elena", email: "elena@example.com", password: "elena", role: "USER",      banned: false, createdAt: "2026-03-03T11:15:00", phoneNumber: null, score: 63.0  },
  { id: 4, username: "rares", email: "rares@example.com", password: "rares", role: "USER",      banned: false, createdAt: "2026-03-04T08:45:00", phoneNumber: null, score: 31.5  },
  { id: 5, username: "ioana", email: "ioana@example.com", password: "ioana", role: "USER",      banned: false, createdAt: "2026-03-05T14:20:00", phoneNumber: null, score: 55.0  },
];

const seedTags = [
  { id: 1, name: "auth" },
  { id: 2, name: "ui" },
  { id: 3, name: "backend" },
  { id: 4, name: "performance" },
  { id: 5, name: "email" },
  { id: 6, name: "mobile" },
  { id: 7, name: "security" },
  { id: 8, name: "database" },
];

const seedBugs = [
  { id: 101, title: "Login button unresponsive on Safari",         text: "Clicking the login button on Safari 17 does nothing. No network request is fired and no console error is logged. Works fine on Chrome and Firefox.", imageUrl: null, status: "OPEN",        createdAt: "2026-03-10T09:15:00", authorId: 3, voteScore: 7,  tagIds: [1, 2] },
  { id: 102, title: "Dashboard charts flicker on refresh",         text: "When refreshing the dashboard page, the bar charts briefly render with 0 values before populating. Likely a race condition between the initial render and the data fetch.", imageUrl: null, status: "IN_PROGRESS", createdAt: "2026-03-12T14:42:00", authorId: 4, voteScore: 12, tagIds: [2, 4] },
  { id: 103, title: "Password reset email not delivered",          text: "Triggering a password reset from /auth/forgot-password returns 200 but no email arrives. Mailpit shows no outgoing message. Reproduced with multiple accounts.", imageUrl: null, status: "OPEN",        createdAt: "2026-03-14T08:05:00", authorId: 5, voteScore: 5,  tagIds: [1, 5] },
  { id: 104, title: "Comment vote count goes negative past -1",    text: "Downvoting a comment repeatedly decreases the score beyond -1 instead of being idempotent. Should be capped per-user at ±1 via toggle/swap semantics.", imageUrl: null, status: "FIXED",       createdAt: "2026-03-16T17:20:00", authorId: 3, voteScore: 18, tagIds: [3] },
  { id: 105, title: "Bug status stuck on IN_PROGRESS after resolve", text: "Calling PATCH /api/bugs/{id}/resolve returns 200 but the status in the response body still shows IN_PROGRESS. A subsequent GET shows the correct FIXED state, so it looks like a stale entity returned from the service layer.", imageUrl: null, status: "IN_PROGRESS", createdAt: "2026-03-18T11:55:00", authorId: 4, voteScore: 9,  tagIds: [3, 8] },
  { id: 106, title: "CORS preflight fails for PATCH requests",     text: "Browser blocks PATCH requests from localhost:5173 with a preflight error. GET/POST work. Suspect the allowed methods list in SecurityConfig is missing PATCH.", imageUrl: null, status: "CLOSED",      createdAt: "2026-03-20T10:10:00", authorId: 2, voteScore: 4,  tagIds: [3, 7] },
  { id: 107, title: "Tag filter ignores case",                     text: "Filtering bugs by tag 'Backend' returns no results, while 'backend' works. Filter should be case-insensitive or tags should be normalized on insert.", imageUrl: null, status: "OPEN",        createdAt: "2026-03-22T13:30:00", authorId: 5, voteScore: 3,  tagIds: [3, 2] },
  { id: 108, title: "JWT token not refreshed after role change",   text: "When an admin promotes a user to MODERATOR, the user must log out and back in for @PreAuthorize checks to recognize the new role. Token should be invalidated or role claims refreshed.", imageUrl: null, status: "IN_PROGRESS", createdAt: "2026-03-25T16:00:00", authorId: 1, voteScore: 21, tagIds: [1, 7] },
];

const seedComments = [
  { id: 5,  text: "I checked the logs, and it looks like a timeout issue with the database connection.", imageUrl: null, createdAt: "2026-03-17T09:45:10", authorId: 2, bugId: 101, score: 8  },
  { id: 6,  text: "Can we increase the connection pool size as a temporary fix?",                         imageUrl: null, createdAt: "2026-03-17T09:50:00", authorId: 4, bugId: 101, score: 3  },
  { id: 7,  text: "The UI alignment is completely broken on mobile portrait mode. See attached.",         imageUrl: null, createdAt: "2026-03-12T11:05:22", authorId: 5, bugId: 103, score: 15 },
  { id: 8,  text: "I'm unable to reproduce this on iOS Safari. Is it Android specific?",                 imageUrl: null, createdAt: "2026-03-12T11:30:15", authorId: 1, bugId: 103, score: 0  },
  { id: 9,  text: "Yes, only happening on Android Chrome version 122+.",                                  imageUrl: null, createdAt: "2026-03-12T12:00:45", authorId: 5, bugId: 103, score: 4  },
  { id: 10, text: "Clearing the cache resolves it temporarily, but it comes back after a reload.",        imageUrl: null, createdAt: "2026-03-13T08:15:00", authorId: 3, bugId: 104, score: 2  },
  { id: 11, text: "I've opened a PR with a potential fix: #4052. Please review.",                        imageUrl: null, createdAt: "2026-03-14T14:22:10", authorId: 2, bugId: 104, score: 25 },
  { id: 12, text: "PR looks good, but it breaks the unit tests in the auth module.",                      imageUrl: null, createdAt: "2026-03-14T15:10:05", authorId: 1, bugId: 104, score: 7  },
  { id: 13, text: "Ah, good catch. I will update the tests and push again.",                              imageUrl: null, createdAt: "2026-03-14T15:15:30", authorId: 2, bugId: 104, score: 1  },
  { id: 14, text: "Users are reporting data loss when clicking 'Save' rapidly. Critical issue.",          imageUrl: null, createdAt: "2026-03-16T18:00:00", authorId: 4, bugId: 105, score: 45 },
  { id: 15, text: "We need to disable the button immediately after the first click.",                     imageUrl: null, createdAt: "2026-03-16T18:10:12", authorId: 2, bugId: 105, score: 30 },
  { id: 16, text: "Here is the network payload during the double-click event.",                           imageUrl: null, createdAt: "2026-03-16T18:25:00", authorId: 3, bugId: 105, score: 12 },
  { id: 17, text: "Rolling back the latest deployment to version 2.4.1 to mitigate this.",               imageUrl: null, createdAt: "2026-03-16T18:40:00", authorId: 1, bugId: 105, score: 50 },
  { id: 18, text: "Not sure why this is marked as low priority, it's blocking our entire QA flow.",      imageUrl: null, createdAt: "2026-03-15T09:00:00", authorId: 5, bugId: 102, score: 18 },
  { id: 19, text: "Upgraded severity to HIGH.",                                                           imageUrl: null, createdAt: "2026-03-15T09:15:30", authorId: 1, bugId: 102, score: 5  },
  { id: 20, text: "Thanks! I'll assign someone from the frontend team today.",                            imageUrl: null, createdAt: "2026-03-15T09:20:00", authorId: 4, bugId: 102, score: 2  },
  { id: 21, text: "Does anyone have the exact steps to reproduce? The description is a bit vague.",       imageUrl: null, createdAt: "2026-03-16T10:05:00", authorId: 3, bugId: 102, score: 1  },
  { id: 22, text: "Step 1: Login. Step 2: Go to Dashboard. Step 3: Click 'Export PDF'. Result: 500.",    imageUrl: null, createdAt: "2026-03-16T10:12:45", authorId: 5, bugId: 102, score: 22 },
  { id: 23, text: "Confirmed. Getting a 'Missing library' exception in the worker logs.",                 imageUrl: null, createdAt: "2026-03-16T10:30:10", authorId: 2, bugId: 102, score: 14 },
  { id: 24, text: "Looks like the dependency wasn't included in the Dockerfile update yesterday.",        imageUrl: null, createdAt: "2026-03-16T10:45:00", authorId: 1, bugId: 102, score: 10 },
];

function deepClone(x) {
  return JSON.parse(JSON.stringify(x));
}

export function createDb() {
  const users    = deepClone(seedUsers);
  const tags     = deepClone(seedTags);
  const bugs     = deepClone(seedBugs);
  const comments = deepClone(seedComments);
  const bugVotes     = [];  // { userId, bugId, voteType }
  const commentVotes = [];  // { userId, commentId, voteType }
  let nextId = 200;

  return {
    users,
    tags,
    bugs,
    comments,
    bugVotes,
    commentVotes,
    nextId() { return ++nextId; },

    getUserById(id)           { return users.find(u => u.id === +id); },
    getUserByUsername(name)   { return users.find(u => u.username === name); },
    getBugById(id)            { return bugs.find(b => b.id === +id); },
    getCommentById(id)        { return comments.find(c => c.id === +id); },
    getTagByName(name)        { return tags.find(t => t.name.toLowerCase() === name.toLowerCase()); },

    bugResponse(bug, userId) {
      const author = this.getUserById(bug.authorId);
      const tagNames = (bug.tagIds || []).map(tid => tags.find(t => t.id === tid)?.name).filter(Boolean);
      const userVoteRec = userId ? bugVotes.find(v => v.bugId === bug.id && v.userId === +userId) : null;
      return {
        id: bug.id,
        title: bug.title,
        text: bug.text,
        imageUrl: bug.imageUrl,
        status: bug.status,
        createdAt: bug.createdAt,
        authorUsername: author?.username ?? "unknown",
        authorScore: author?.score ?? 0,
        tags: tagNames,
        voteScore: bug.voteScore,
        userVote: userVoteRec?.voteType ?? null,
      };
    },

    commentResponse(comment, userId) {
      const author = this.getUserById(comment.authorId);
      const userVoteRec = userId ? commentVotes.find(v => v.commentId === comment.id && v.userId === +userId) : null;
      return {
        id: comment.id,
        text: comment.text,
        imageUrl: comment.imageUrl,
        bugId: comment.bugId,
        authorId: comment.authorId,
        authorUsername: author?.username ?? "unknown",
        authorScore: author?.score ?? 0,
        createdAt: comment.createdAt,
        score: comment.score,
        userVote: userVoteRec?.voteType ?? null,
      };
    },

    userResponse(user) {
      return {
        id: user.id,
        username: user.username,
        email: user.email,
        role: user.role,
        banned: user.banned,
        createdAt: user.createdAt,
        phoneNumber: user.phoneNumber,
        score: user.score,
      };
    },
  };
}

// Singleton — same instance across all handlers in this session
export const db = createDb();
