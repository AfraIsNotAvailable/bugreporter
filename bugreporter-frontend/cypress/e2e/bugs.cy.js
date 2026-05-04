const bugOne = {
  id: 1,
  title: "Login button broken",
  text: "Clicking login does nothing.",
  imageUrl: "",
  status: "OPEN",
  createdAt: "2026-05-04T12:00:00",
  authorUsername: "alice",
  tags: ["auth", "ui"],
};

const bugTwo = {
  id: 2,
  title: "Dashboard chart fails",
  text: "The dashboard chart does not render.",
  imageUrl: "",
  status: "IN_PROGRESS",
  createdAt: "2026-05-04T13:00:00",
  authorUsername: "bob",
  tags: ["dashboard"],
};

function tokenFor(username, role) {
  const payload = btoa(JSON.stringify({ sub: username, role })).replace(/=/g, "");
  return `header.${payload}.signature`;
}

function loginAs(username = "alice", role = "USER") {
  cy.window().then((win) => {
    win.localStorage.setItem("token", tokenFor(username, role));
    win.localStorage.setItem("user", JSON.stringify({ username, role }));
  });
}

describe("bugs", () => {
  beforeEach(() => {
    cy.intercept("GET", "**/api/bugs", [bugOne, bugTwo]).as("getBugs");
    cy.intercept("GET", "**/api/bugs/1", bugOne).as("getBug");
  });

  it("loads the bug list and bug detail while unauthenticated", () => {
    cy.visit("/");
    cy.wait("@getBugs");
    cy.contains("Login button broken");
    cy.contains("OPEN");
    cy.contains("alice");
    cy.contains("auth");

    cy.contains("Login button broken").click();
    cy.wait("@getBug");
    cy.contains("Clicking login does nothing.");
    cy.contains("Comments section placeholder.");
  });

  it("updates results when searching by title and filtering by tag", () => {
    cy.intercept("GET", "**/api/bugs/filter?search=Login*", [bugOne]).as("searchBugs");
    cy.intercept("GET", "**/api/bugs/filter?tag=auth*", [bugOne]).as("tagBugs");

    cy.visit("/bugs");
    cy.wait("@getBugs");

    cy.get("[aria-label='Search by title']").type("Login");
    cy.contains("button", "Search").click();
    cy.wait("@searchBugs");
    cy.contains("Login button broken");
    cy.contains("Dashboard chart fails").should("not.exist");

    cy.get("[aria-label='Filter by tag']").clear().type("auth");
    cy.contains("button", "Filter Tag").click();
    cy.wait("@tagBugs");
    cy.contains("Login button broken");
  });

  it("logs in, creates a bug, and shows it in the list", () => {
    const createdBug = {
      ...bugOne,
      id: 3,
      title: "New profile bug",
      text: "Profile save fails.",
      tags: [],
    };

    cy.intercept("POST", "**/api/auth/login", {
      token: tokenFor("alice", "USER"),
    }).as("login");
    cy.intercept("POST", "**/api/bugs", createdBug).as("createBug");
    cy.intercept("GET", "**/api/bugs", [createdBug, bugOne]).as("getBugsAfterCreate");

    cy.visit("/login");
    cy.get("input[type='text']").type("alice");
    cy.get("input[type='password']").type("password");
    cy.contains("button", "Login").click();
    cy.wait("@login");

    cy.contains("button", "Report Bug").click();
    cy.get("[aria-label='Bug title']").type("New profile bug");
    cy.get("[aria-label='Bug description']").type("Profile save fails.");
    cy.contains("button", "Create").click();
    cy.wait("@createBug");
    cy.wait("@getBugsAfterCreate");
    cy.contains("New profile bug");
  });

  it("edits and deletes the signed-in user's bug", () => {
    loginAs("alice", "USER");
    cy.intercept("PUT", "**/api/bugs/1", {
      ...bugOne,
      title: "Login button fixed title",
    }).as("editBug");
    cy.intercept("DELETE", "**/api/bugs/1", { statusCode: 204 }).as("deleteBug");
    cy.intercept("GET", "**/api/bugs", [bugTwo]).as("getAfterDelete");

    cy.visit("/bugs/1");
    cy.wait("@getBug");
    cy.contains("button", "Edit").click();
    cy.get("[aria-label='Edit bug title']").clear().type("Login button fixed title");
    cy.contains("button", "Save").click();
    cy.wait("@editBug");
    cy.contains("Login button fixed title");

    cy.contains("button", "Delete").click();
    cy.wait("@deleteBug");
    cy.wait("@getAfterDelete");
    cy.contains("Login button fixed title").should("not.exist");
  });

  it("lets the author mark a bug as resolved", () => {
    loginAs("alice", "USER");
    cy.intercept("PATCH", "**/api/bugs/1/resolve", {
      ...bugOne,
      status: "FIXED",
    }).as("resolveBug");

    cy.visit("/bugs/1");
    cy.wait("@getBug");
    cy.contains("button", "Mark as Resolved").click();
    cy.wait("@resolveBug");
    cy.contains("FIXED");
  });

  it("lets a moderator change status from the dropdown", () => {
    loginAs("mod", "MODERATOR");
    cy.intercept("PATCH", "**/api/bugs/1/status?status=CLOSED", {
      ...bugOne,
      status: "CLOSED",
    }).as("changeStatus");

    cy.visit("/bugs/1");
    cy.wait("@getBug");
    cy.get("[aria-label='Bug status']").select("CLOSED");
    cy.wait("@changeStatus");
    cy.contains("CLOSED");
  });

  it("does not show moderator controls to a regular user", () => {
    loginAs("charlie", "USER");
    cy.visit("/bugs/1");
    cy.wait("@getBug");
    cy.get("[aria-label='Bug status']").should("not.exist");
  });

  it("lets moderator or admin delete another user's bug but not edit it", () => {
    loginAs("mod", "MODERATOR");
    cy.intercept("DELETE", "**/api/bugs/1", { statusCode: 204 }).as("deleteBug");
    cy.intercept("GET", "**/api/bugs", [bugTwo]).as("getAfterDelete");

    cy.visit("/bugs/1");
    cy.wait("@getBug");
    cy.contains("button", "Edit").should("not.exist");
    cy.contains("button", "Delete").click();
    cy.wait("@deleteBug");
    cy.wait("@getAfterDelete");
  });

  it("does not let a regular non-owner edit or delete another user's bug", () => {
    loginAs("charlie", "USER");
    cy.visit("/bugs/1");
    cy.wait("@getBug");
    cy.contains("button", "Edit").should("not.exist");
    cy.contains("button", "Delete").should("not.exist");
  });
});
