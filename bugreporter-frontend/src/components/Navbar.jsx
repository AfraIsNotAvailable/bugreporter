import { Link, useNavigate } from "react-router-dom";
import useAuth from "../hooks/useAuth";


export default function Navbar() {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  return (
    <nav className="flex gap-4 p-4 border-b">
      <Link to="/">Home</Link>

      {!isAuthenticated ? (
        <>
          <Link to="/login">Login</Link>
          <Link to="/register">Register</Link>
        </>
      ) : (
        <>
          <span>Hello, {user?.username || user?.email || "User"}</span>

          {user?.role === "ADMIN" && <Link to="/admin">Admin</Link>}

          <button onClick={handleLogout}>Logout</button>
        </>
      )}
    </nav>
  );
}
