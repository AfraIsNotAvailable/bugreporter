import { Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

function Navbar() {
  const { isAuthenticated, logout, user } = useAuth();

  const navStyle = {
    display: "flex",
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

  const isAdmin = user?.role === "ADMIN";
  const isModerator = user?.role === "MODERATOR";

  return (
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
