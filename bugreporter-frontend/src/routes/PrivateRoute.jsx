
//navigate face redirect automat catre alta pagina 
import { Navigate } from "react-router-dom";
import useAuth from "../hooks/useAuth";


//componenta wrapper pe care o folosesc pentru protejarea rutelor
                                       // children is orice ii pus intre tag-urie unei compoenente
export default function PrivateRoute({ children, role }) {
  const { isAuthenticated, user } = useAuth();


  if (!isAuthenticated) {
    //trimit userul la pagina de login 
    return <Navigate to="/login" replace />;
  }

  //daca ruta cere un rol si userul nu are acel rol => nu il las sa intre
  if (role && user?.role !== role) {
    return <Navigate to="/" replace />;
  }


  //daca a trecut de verficari -> afisez pagina protejata 
  return children;
}
