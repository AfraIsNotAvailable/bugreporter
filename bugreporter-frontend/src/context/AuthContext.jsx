import { createContext, useContext, useState } from "react";

//tine local datele despre userul logat 

//creez contextul 
export const AuthContext = createContext(null);


// curata rolul userului
function normalizeRole(role) {
  if (typeof role !== "string" || role.trim() === "") {
    return null;
  }

  return role.toUpperCase().replace(/^ROLE_/, "");
}

//parseaza token-ul JWT pentru a extrage informatii despre user
//JWT are forma header.payload.signature si eu am nevoie de payload
function parseUserFromToken(token) {
  if (!token) {
    return null;
  }

  try {

    //scot payload (partea din mijloc) din taken
    const [, payload] = token.split(".");

    if (!payload) {
      return null;
    }

    // JWT e codificat in Base64URL si mie imi trebuie in Base64 (asta fac aici la normalizare)
    const normalizedPayload = payload.replace(/-/g, "+").replace(/_/g, "/");
    //Base 64 are nevoie de padding uneori
    const paddedPayload = normalizedPayload.padEnd(
      normalizedPayload.length + ((4 - (normalizedPayload.length % 4)) % 4),
      "="
    );
    //decodez payload-ul
    const decodedPayload = atob(paddedPayload);
    // il transform in obiect javascript
    const decoded = JSON.parse(decodedPayload);
    //normalizez rolul
    const role = normalizeRole(decoded.role);

    //returnez un obiect cu username si rol
    return {
      username: decoded.sub || null,
      role,
    };
  } catch {
    return null;
  }
}

//AuthProvider e componenta care inveleste aplicatia si ofera acces la autentificare peste tot 

export function AuthProvider({ children }) {
  //salvez starea token
  //daca user e logat si da refresh la pagina el ramane logat pentru ca token-ul e salvat in browser
  const [token, setToken] = useState(() => localStorage.getItem("token"));
  //creez starea pentru user, care e initializata din token sau din localStorage
  const [user, setUser] = useState(() => {
    try {
      const savedUser = localStorage.getItem("user");

      if (savedUser) {
        return JSON.parse(savedUser);
      }

      return parseUserFromToken(localStorage.getItem("token"));
    } catch {
      //daca userul salvat este corupt sau nu e JSON valid il sterg si incerc sa il reconstruiesc din token
      localStorage.removeItem("user");
      return parseUserFromToken(localStorage.getItem("token"));
    }
  });

  //functia de login 
  //e apelata dupa ce backend-ul returneaza token-ul la login
  const login = (newToken) => {
    //decodez userul
    //aflu username-ul si rolul
    const decodedUser = parseUserFromToken(newToken);

    //salvez token-ul in browser
    //daca se da refresh la pagina userul ramane logat
    localStorage.setItem("token", newToken);

    //daca token-ul a putu fi decodat atunci salvez si userul
    //daca nu atunci sterg userul 
    if (decodedUser) {
      localStorage.setItem("user", JSON.stringify(decodedUser));
    } else {
      localStorage.removeItem("user");
    }


    //actualizez starea React
    //asta face ca aplicatia sa se actualizeze imediat 
    //o sa se schimbe navbar-ul si apar butoanele pentru admin si moderator unde rolul userului permite
    setToken(newToken);
    setUser(decodedUser);

    //returnez userul decodat ca sa decida pagina de login unde sa redirectioneze userul
    return decodedUser;
  };

  //functia care imi face logout
  //sterg token, user si golesc starea React 
  const logout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
    setToken(null);
    setUser(null);
  };

  //asta e ce pun la dispozitie in toata aplicatia 
  //adica orice componenta poate folosi ce e in value
  const value = {
    token,
    user,
    login,
    logout,
    isAuthenticated: Boolean(token),
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

//hook-ul pentru useAuth
//ca sa nu mai trebuiasca sa scriu useContext(AuthContext) de fiecare data cand vreau sa accesez autentificarea si sa pot scrie numai useAuth
export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used inside an AuthProvider");
  }

  return context;
}
