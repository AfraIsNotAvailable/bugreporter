//asta ii componenta din React Riuter care face redirect
import { Navigate } from "react-router-dom";
//import hook-ul custom din AuthContext
import { useAuth } from "../context/AuthContext";

//asta imi zice pe ce pagini au voie sa intre anumiti useri 
//children -> pagina pe care vreau sa o protejez
export default function PrivateRoute({ children, role, roles }) {
  const { isAuthenticated, user } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (role && user?.role !== role) {
    return <Navigate to="/" replace />;
  }

  if (roles && !roles.includes(user?.role)) {
    return <Navigate to="/" replace />;
  }

  return children;
}
