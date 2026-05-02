import { createContext, useState } from "react";


//aici frontend-ul tine info despre user-ul logat 

//context global -> orice componenta poate accesa datele direct
export const AuthContext = createContext(null);


//provider-ul               // toate componentele din interior 
export function AuthProvider({ children }) {

                            //memoriee interna in React pentru token si user
  const [token, setToken] = useState(() => localStorage.getItem("token"));

  // iau userul din browser si il fac string daca nu ii returnez null
  const [user, setUser] = useState(() => {
    try {
      const savedUser = localStorage.getItem("user");
      return savedUser ? JSON.parse(savedUser) : null;
    } catch {
      localStorage.removeItem("user");
      return null;
    }
  });


  //asta se apeleaza cand userul se logheaza 
  const login = (token, user) => {

    //salvez token in browser 
    localStorage.setItem("token", token);
    //salvez user                 //localStorage accepta doar string-uri 
    localStorage.setItem("user", JSON.stringify(user));


    //acctualizez state-ul React 
    setToken(token);
    setUser(user);
  };


  //asta se face cand userul face logout 
  const logout = () => {
    //sterg datele din browser 
    localStorage.removeItem("token");
    localStorage.removeItem("user");

    //resetez state-ul React
    setToken(null);
    setUser(null);
  };


  //verific daca userul e logat 
                          //conversie la boolean 
  const isAuthenticated = !!token;

  //cu asta dau acces gobal la date -> orice componenta poate folosi valorile 
  return (
    <AuthContext.Provider
      value={{ user, token, login, logout, isAuthenticated }}
    >
      {children}
    </AuthContext.Provider>
  );
}


