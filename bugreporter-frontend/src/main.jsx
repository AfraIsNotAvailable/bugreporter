import React from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter, HashRouter } from "react-router-dom";
import App from "./App";
import { AuthProvider } from "./context/AuthContext";
import "./index.css";

const IS_DEMO = import.meta.env.VITE_DEMO_MODE === "true";
const Router = IS_DEMO ? HashRouter : BrowserRouter;

async function prepare() {
  if (IS_DEMO) {
    const { worker } = await import("./mocks/browser");
    return worker.start({ onUnhandledRequest: "bypass" });
  }
}

prepare().then(() => {
  createRoot(document.getElementById("root")).render(
    <React.StrictMode>
      <Router>
        <AuthProvider>
          <App />
        </AuthProvider>
      </Router>
    </React.StrictMode>
  );
});
