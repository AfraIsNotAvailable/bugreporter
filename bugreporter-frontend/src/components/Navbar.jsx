//asta mi componenta din React Router care ma lasa sa fac navigare intre pagini
import { Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

//asta o sa fie afisata deasupra rutelor
function Navbar() {
  const { isAuthenticated, logout, user } = useAuth();

  const navStyle = {
    //pune elementele pe aceeasi linie
    display: "flex",
    //pune spatiu intre link-uri
    gap: "12px",
    padding: "16px 24px",
    borderBottom: "1px solid #ddd",
    alignItems: "center",
  };

  const linkStyle = {
    display: "inline-block",
    padding: "8px 14px",
    border: "1px solid #333",
    backgroundColor: "#f3f3f3",
    color: "#000",
    textDecoration: "none",
    cursor: "pointer",
  };

  const buttonStyle = {
    padding: "8px 14px",
    border: "1px solid #333",
    backgroundColor: "#f3f3f3",
    cursor: "pointer",
  };

  //verific rolul userului
  const isAdmin = user?.role === "ADMIN";
  const isModerator = user?.role === "MODERATOR";

  return (
    //asta ca sa returneze o bara de navigare 
    <nav style={navStyle}>
      <Link to="/bugs" style={linkStyle}>
        Bugs
      </Link>
      
      <Link to="/" style={linkStyle}>
        Home
      </Link>

      {!isAuthenticated && (
        <>
          <Link to="/login" style={linkStyle}>
            Login
          </Link>
          <Link to="/register" style={linkStyle}>
            Register
          </Link>
        </>
      )}

      {isAuthenticated && isAdmin && (
        <Link to="/admin" style={linkStyle}>
          Admin
        </Link>
      )}

      {isAuthenticated && isModerator && (
        <Link to="/moderator" style={linkStyle}>
          Moderator
        </Link>
      )}

      {isAuthenticated && (
        <button type="button" onClick={logout} style={buttonStyle}>
          Logout
        </button>
      )}
    </nav>
  );
}

export default Navbar;
