describe("Auth tests", () => {
  const username = `testuser${Date.now()}`;
  const email = `${username}@test.com`;
  const password = "Password123";
  const phoneNumber = `07${Date.now().toString().slice(-8)}`;

  const adminUsername = "ana"; 
  const adminPassword = "ana"; 

  const loginAsAdmin = () => {
    cy.visit("/login");

    cy.contains("label", "Username").next("input").type(adminUsername);
    cy.contains("label", "Password").next("input").type(adminPassword);

    cy.contains("button", "Login").click();
    cy.url().should("include", "/admin");
  };

  it("Register new user -> lands on home", () => {
    cy.visit("/register");

    cy.contains("label", "Username").next("input").type(username);
    cy.contains("label", "Email").next("input").type(email);
    cy.contains("label", "Password").next("input").type(password);
    cy.contains("label", "Phone Number").next("input").type(phoneNumber);

    cy.contains("button", "Register").click();

    cy.url().should("eq", Cypress.config().baseUrl + "/");
    cy.contains("Welcome to Bug Reporter").should("be.visible");
  });

  it("Login with valid credentials -> JWT stored", () => {
    cy.visit("/login");

    cy.contains("label", "Username").next("input").type(username);
    cy.contains("label", "Password").next("input").type(password);

    cy.contains("button", "Login").click();

    cy.url().should("eq", Cypress.config().baseUrl + "/");
    cy.window().then((win) => {
      expect(win.localStorage.getItem("token")).to.not.be.null;
    });
  });

  it("Login with wrong password -> error shown", () => {
  cy.intercept("POST", "**/api/auth/login").as("loginRequest");

  cy.visit("/login");

  cy.contains("label", "Username").next("input").type(username);
  cy.contains("label", "Password").next("input").type("wrongPassword123");

  cy.contains("button", "Login").click();

  cy.wait("@loginRequest")
    .its("response.statusCode")
    .should("be.oneOf", [400, 401, 403]);

  cy.url().should("include", "/login");
});

  it("Unauthenticated -> /admin redirects to login", () => {
    cy.clearLocalStorage();

    cy.visit("/admin");

    cy.url().should("include", "/login");
    cy.contains("Login").should("be.visible");
  });

  it("Admin: ban user -> user appears as banned", () => {
  loginAsAdmin();

  cy.intercept("PUT", "**/api/admin/users/*/ban*").as("banUser");
  cy.intercept("GET", "**/api/admin/users").as("getUsers");

  cy.contains("td", username)
    .parent("tr")
    .within(() => {
      cy.contains("button", "Ban").click();
    });

  cy.wait("@banUser")
    .its("response.body.banned")
    .should("eq", true);

  cy.reload();

  cy.wait("@getUsers");

  cy.contains("td", username, { timeout: 10000 })
    .parent("tr")
    .within(() => {
      cy.contains("td", "Yes", { timeout: 10000 }).should("be.visible");
      cy.contains("button", "Unban").should("be.visible");
    });
});

it("Login with banned user -> error shown", () => {
  cy.intercept("POST", "**/api/auth/login").as("loginRequest");

  cy.clearLocalStorage();
  cy.visit("/login");

  cy.contains("label", "Username").next("input").type(username);
  cy.contains("label", "Password").next("input").type(password);

  cy.contains("button", "Login").click();

  cy.wait("@loginRequest")
    .its("response.statusCode")
    .should("be.oneOf", [400, 401, 403]);

  cy.url().should("include", "/login");
});

  it("Admin: change role -> role updates in table", () => {
    loginAsAdmin();

    cy.contains("td", username)
      .parent("tr")
      .within(() => {
        cy.get("select").select("MODERATOR");
        cy.get("select").should("have.value", "MODERATOR");
      });
  });
});