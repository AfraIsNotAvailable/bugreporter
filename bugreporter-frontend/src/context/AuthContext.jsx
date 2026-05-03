import { createContext, useContext, useState } from "react";

export const AuthContext = createContext(null);

function normalizeRole(role) {
  if (typeof role !== "string" || role.trim() === "") {
    return null;
  }

  return role.toUpperCase().replace(/^ROLE_/, "");
}

function parseUserFromToken(token) {
  if (!token) {
    return null;
  }

  try {
    const [, payload] = token.split(".");

    if (!payload) {
      return null;
    }

    const normalizedPayload = payload.replace(/-/g, "+").replace(/_/g, "/");
    const paddedPayload = normalizedPayload.padEnd(
      normalizedPayload.length + ((4 - (normalizedPayload.length % 4)) % 4),
      "="
    );
    const decodedPayload = atob(paddedPayload);
    const decoded = JSON.parse(decodedPayload);
    const role = normalizeRole(decoded.role);

    if (!role) {
      return null;
    }

    return {
      username: decoded.sub || null,
      role,
    };
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem("token"));
  const [user, setUser] = useState(() => {
    try {
      const savedUser = localStorage.getItem("user");

      if (savedUser) {
        return JSON.parse(savedUser);
      }

      return parseUserFromToken(localStorage.getItem("token"));
    } catch {
      localStorage.removeItem("user");
      return parseUserFromToken(localStorage.getItem("token"));
    }
  });

  const login = (newToken) => {
    const decodedUser = parseUserFromToken(newToken);

    localStorage.setItem("token", newToken);

    if (decodedUser) {
      localStorage.setItem("user", JSON.stringify(decodedUser));
    } else {
      localStorage.removeItem("user");
    }

    setToken(newToken);
    setUser(decodedUser);

    return decodedUser;
  };

  const logout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
    setToken(null);
    setUser(null);
  };

  const value = {
    token,
    user,
    login,
    logout,
    isAuthenticated: Boolean(token),
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used inside an AuthProvider");
  }

  return context;
}
