import React from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter, HashRouter } from "react-router-dom";
import App from "./App";
import { AuthProvider } from "./context/AuthContext";
import "./index.css";

const IS_DEMO = import.meta.env.VITE_DEMO_MODE === "true";
const Router = IS_DEMO ? HashRouter : BrowserRouter;

function mount() {
  createRoot(document.getElementById("root")).render(
    <React.StrictMode>
      <Router>
        <AuthProvider>
          <App />
        </AuthProvider>
      </Router>
    </React.StrictMode>
  );
}

async function prepare() {
  if (IS_DEMO) {
    const { worker } = await import("./mocks/browser");
    await worker.start({
      onUnhandledRequest: "bypass",
      serviceWorker: {
        url: `${import.meta.env.BASE_URL}mockServiceWorker.js`,
      },
    });
  }
}

prepare().then(mount).catch((e) => {
  console.error("[MSW] Failed to start service worker, mounting anyway:", e);
  mount();
});
