import { useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../api/axios";
import { useAuth } from "../context/AuthContext";

function getErrorMessage(error, fallback) {
  const data = error.response?.data;

  if (typeof data === "string") {
    return data;
  }

  return data?.message || fallback;
}

function Login() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  const navigate = useNavigate();
  const { login } = useAuth();

  const pageStyle = {
    minHeight: "100vh",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    padding: "24px",
  };

  const formStyle = {
    width: "100%",
    maxWidth: "360px",
    padding: "24px",
    border: "1px solid #ccc",
    borderRadius: "4px",
  };

  const fieldStyle = {
    marginBottom: "16px",
  };

  const inputStyle = {
    width: "100%",
    padding: "10px",
    border: "1px solid #999",
    boxSizing: "border-box",
    marginTop: "6px",
  };

  const buttonStyle = {
    padding: "10px 16px",
    backgroundColor: "#f3f3f3",
    border: "1px solid #333",
    cursor: "pointer",
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    try {
      const response = await api.post("/auth/login", {
        username,
        password,
      });

      const loggedInUser = login(response.data.token);

      if (loggedInUser?.role === "ADMIN") {
        navigate("/admin");
      } else if (loggedInUser?.role === "MODERATOR") {
        navigate("/moderator");
      } else {
        navigate("/");
      }
    } catch (err) {
      setError(getErrorMessage(err, "Login failed"));
    }
  };

  return (
    <div style={pageStyle}>
      <form onSubmit={handleSubmit} style={formStyle}>
        <h1 style={{ marginTop: 0, marginBottom: "20px" }}>Login</h1>

        <div style={fieldStyle}>
          <label>Username:</label>
          <input
            type="text"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            style={inputStyle}
          />
        </div>

        <div style={fieldStyle}>
          <label>Password:</label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            style={inputStyle}
          />
        </div>

        {error && <p style={{ marginBottom: "16px", color: "red" }}>{error}</p>}

        <button type="submit" style={buttonStyle}>
          Login
        </button>
      </form>
    </div>
  );
}

export default Login;
