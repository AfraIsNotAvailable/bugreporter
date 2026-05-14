const openBug = {
  id: 1,
  title: "Login button broken",
  text: "Clicking login does nothing.",
  imageUrl: "",
  status: "OPEN",
  createdAt: "2026-05-04T12:00:00",
  authorUsername: "alice",
  tags: [],
};

const fixedBug = {
  id: 2,
  title: "Fixed old bug",
  text: "This one is fixed.",
  imageUrl: "",
  status: "FIXED",
  createdAt: "2026-05-04T11:00:00",
  authorUsername: "alice",
  tags: [],
};

const otherComment = {
  id: 10,
  text: "Other comment text",
  authorUsername: "bob",
  score: 3,
  userVote: null,
  createdAt: "2026-05-04T12:30:00",
  imageUrl: null,
};

const ownComment = {
  id: 11,
  text: "My own comment",
  authorUsername: "alice",
  score: 0,
  userVote: null,
  createdAt: "2026-05-04T12:00:00",
  imageUrl: null,
};

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

describe("comments", () => {
  it("unauthenticated: comments visible, no create form shown", () => {
    cy.intercept("GET", "**/api/bugs/1", openBug).as("getBug");
    cy.intercept("GET", "**/api/comments/bug/1", [otherComment]).as("getComments");

    cy.clearLocalStorage();
    cy.visit("/bugs/1");
    cy.wait("@getBug");
    cy.wait("@getComments");

    cy.contains("Other comment text").should("be.visible");
    cy.get("[aria-label='Comment text']").should("not.exist");
    cy.contains("Log in to post a comment.").should("be.visible");
  });

  it("login → post comment → appears in list", () => {
    const newComment = {
      id: 20,
      text: "My new comment",
      authorUsername: "alice",
      score: 0,
      userVote: null,
      createdAt: "2026-05-04T13:00:00",
      imageUrl: null,
    };

    cy.intercept("GET", "**/api/bugs/1", openBug).as("getBug");
    cy.intercept("GET", "**/api/comments/bug/1", [otherComment]).as("getComments");
    cy.intercept("POST", "**/api/comments/bug/1", newComment).as("postComment");

    loginAs("alice", "USER");
    cy.visit("/bugs/1");
    cy.wait("@getBug");
    cy.wait("@getComments");

    cy.get("[aria-label='Comment text']").type("My new comment");
    cy.contains("button", "Post Comment").click();
    cy.wait("@postComment");

    cy.contains("My new comment").should("be.visible");
  });

  it("post comment on OPEN bug → bug status flips to IN_PROGRESS", () => {
    const newComment = {
      id: 21,
      text: "Triggering status change",
      authorUsername: "alice",
      score: 0,
      userVote: null,
      createdAt: "2026-05-04T13:01:00",
      imageUrl: null,
    };
    const inProgressBug = { ...openBug, status: "IN_PROGRESS" };

    cy.intercept("GET", "**/api/bugs/1", openBug).as("getBugFirst");
    cy.intercept("GET", "**/api/comments/bug/1", []).as("getComments");
    cy.intercept("POST", "**/api/comments/bug/1", newComment).as("postComment");

    loginAs("alice", "USER");
    cy.visit("/bugs/1");
    cy.wait("@getBugFirst");
    cy.wait("@getComments");

    cy.intercept("GET", "**/api/bugs/1", inProgressBug).as("getBugSecond");

    cy.get("[aria-label='Comment text']").type("Triggering status change");
    cy.contains("button", "Post Comment").click();
    cy.wait("@postComment");
    cy.wait("@getBugSecond");

    cy.contains("IN_PROGRESS").should("be.visible");
  });

  it("cannot comment on FIXED bug → form hidden", () => {
    cy.intercept("GET", "**/api/bugs/2", fixedBug).as("getFixedBug");
    cy.intercept("GET", "**/api/comments/bug/2", []).as("getComments");

    loginAs("alice", "USER");
    cy.visit("/bugs/2");
    cy.wait("@getFixedBug");
    cy.wait("@getComments");

    cy.get("[aria-label='Comment text']").should("not.exist");
    cy.contains("Commenting is disabled on FIXED or CLOSED bugs.").should("be.visible");
  });

  it("upvote → score +1; upvote again → score back (toggle)", () => {
    cy.intercept("GET", "**/api/bugs/1", openBug).as("getBug");
    cy.intercept("GET", "**/api/comments/bug/1", [otherComment]).as("getComments");
    cy.intercept("POST", "**/api/comments/10/vote", { statusCode: 200 }).as("vote");

    loginAs("alice", "USER");
    cy.visit("/bugs/1");
    cy.wait("@getBug");
    cy.wait("@getComments");

    cy.get("[aria-label='Upvote']").click();
    cy.wait("@vote");
    cy.get("[data-testid='comment-score']").should("contain", "4");

    cy.get("[aria-label='Upvote']").click();
    cy.wait("@vote");
    cy.get("[data-testid='comment-score']").should("contain", "3");
  });

  it("downvote then upvote → score changes +2", () => {
    cy.intercept("GET", "**/api/bugs/1", openBug).as("getBug");
    cy.intercept("GET", "**/api/comments/bug/1", [otherComment]).as("getComments");
    cy.intercept("POST", "**/api/comments/10/vote", { statusCode: 200 }).as("vote");

    loginAs("alice", "USER");
    cy.visit("/bugs/1");
    cy.wait("@getBug");
    cy.wait("@getComments");

    cy.get("[aria-label='Downvote']").click();
    cy.wait("@vote");
    cy.get("[data-testid='comment-score']").should("contain", "2");

    cy.get("[aria-label='Upvote']").click();
    cy.wait("@vote");
    cy.get("[data-testid='comment-score']").should("contain", "4");
  });

  it("cannot vote own comment → vote buttons not shown", () => {
    cy.intercept("GET", "**/api/bugs/1", openBug).as("getBug");
    cy.intercept("GET", "**/api/comments/bug/1", [ownComment]).as("getComments");

    loginAs("alice", "USER");
    cy.visit("/bugs/1");
    cy.wait("@getBug");
    cy.wait("@getComments");

    cy.contains("My own comment").should("be.visible");
    cy.get("[aria-label='Upvote']").should("not.exist");
    cy.get("[aria-label='Downvote']").should("not.exist");
  });

  it("edit own comment → text updates", () => {
    cy.intercept("GET", "**/api/bugs/1", openBug).as("getBug");
    cy.intercept("GET", "**/api/comments/bug/1", [ownComment]).as("getComments");
    cy.intercept("PUT", "**/api/comments/11", { ...ownComment, text: "Updated comment text" }).as("editComment");

    loginAs("alice", "USER");
    cy.visit("/bugs/1");
    cy.wait("@getBug");
    cy.wait("@getComments");

    cy.contains("My own comment").should("be.visible");
    cy.get("[data-testid='comment-card']").within(() => {
      cy.contains("button", "Edit").click();
    });
    cy.get("[aria-label='Edit comment text']").clear().type("Updated comment text");
    cy.get("[data-testid='comment-card']").within(() => {
      cy.contains("button", "Submit").click();
    });
    cy.wait("@editComment");

    cy.contains("Updated comment text").should("be.visible");
    cy.contains("My own comment").should("not.exist");
  });

  it("delete own comment → removed from list", () => {
    cy.intercept("GET", "**/api/bugs/1", openBug).as("getBug");
    cy.intercept("GET", "**/api/comments/bug/1", [ownComment]).as("getComments");
    cy.intercept("DELETE", "**/api/comments/11", { statusCode: 204 }).as("deleteComment");

    loginAs("alice", "USER");
    cy.visit("/bugs/1");
    cy.wait("@getBug");
    cy.wait("@getComments");

    cy.contains("My own comment").should("be.visible");
    cy.get("[data-testid='comment-card']").within(() => {
      cy.contains("button", "...").click();
      cy.contains("button", "Delete").should("be.visible").click();
    });
    cy.contains("button", "Confirm").click();
    cy.wait("@deleteComment");

    cy.contains("My own comment").should("not.exist");
  });

  it("MODERATOR can edit and delete any comment", () => {
    cy.intercept("GET", "**/api/bugs/1", openBug).as("getBug");
    cy.intercept("GET", "**/api/comments/bug/1", [otherComment]).as("getComments");
    cy.intercept("PUT", "**/api/comments/10", { ...otherComment, text: "Mod edited" }).as("editComment");

    loginAs("mod", "MODERATOR");
    cy.visit("/bugs/1");
    cy.wait("@getBug");
    cy.wait("@getComments");

    cy.contains("Other comment text").should("be.visible");
    cy.contains("button", "...").click();
    cy.contains("button", "Edit").should("be.visible");
    cy.contains("button", "Delete").should("be.visible");
    cy.contains("button", "Edit").click();

    cy.get("[aria-label='Edit comment text']").clear().type("Mod edited");
    cy.contains("button", "Submit").click();
    cy.wait("@editComment");

    cy.contains("Mod edited").should("be.visible");
  });
});
