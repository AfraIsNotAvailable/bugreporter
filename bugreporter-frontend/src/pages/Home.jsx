import { Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

function Home() {
  const { isAuthenticated, user } = useAuth();

  const containerStyle = {
    minHeight: "100vh",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    padding: "24px",
  };

  const contentStyle = {
    textAlign: "center",
    display: "flex",
    flexDirection: "column",
    gap: "16px",
  };

  const buttonStyle = {
    display: "inline-block",
    padding: "10px 18px",
    margin: "0 8px",
    border: "1px solid #333",
    backgroundColor: "#f3f3f3",
    color: "#000",
    textDecoration: "none",
    cursor: "pointer",
  };

  const isAdmin = user?.role === "ADMIN";
  const isModerator = user?.role === "MODERATOR" || user?.role === "ADMIN";

  return (
    <div style={containerStyle}>
      <div style={contentStyle}>
        <h1>Welcome to Bug Reporter</h1>

        {isAuthenticated ? (
          <p>You are logged in.</p>
        ) : (
          <p>Please login or register.</p>
        )}

        {!isAuthenticated && (
          <div>
            <Link to="/login" style={buttonStyle}>
              Go to Login
            </Link>
            <Link to="/register" style={buttonStyle}>
              Go to Register
            </Link>
          </div>
        )}

        {isAuthenticated && (
          <div>
            {isModerator && (
              <Link to="/moderator" style={buttonStyle}>
                Go to Moderator Panel
              </Link>
            )}
            {isAdmin && (
              <Link to="/admin" style={buttonStyle}>
                Go to Admin Panel
              </Link>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

export default Home;
