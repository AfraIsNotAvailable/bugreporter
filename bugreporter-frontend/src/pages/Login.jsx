//pentru valori care se schimba in componenta gen username, password, error
import { useState } from "react";
//pentru redirectionare de cod
import { useNavigate } from "react-router-dom";
//asta ii instanta de axios configurata de mine 
import api from "../api/axios";
//hooku custom de autentificare
import { useAuth } from "../context/AuthContext";


//extrage mesajul de eroare din raspunsul backend-ului
function getErrorMessage(error, fallback) {
  const data = error.response?.data;

  if (typeof data === "string") {
    return data || fallback;
  }

  return data?.message || fallback;
}

function isBannedAccountError(message) {
  if (typeof message !== "string") {
    return false;
  }

  const normalizedMessage = message.toLowerCase();

  return (
    normalizedMessage.includes("banned") ||
    normalizedMessage.includes("blocked")
  );
}

async function isEnteredUserBanned(username) {
  if (!username.trim()) {
    return false;
  }

  try {
    const response = await api.get("/users");
    const users = Array.isArray(response.data) ? response.data : [];

    const enteredUsername = username.trim();
    const matchingUser = users.find((user) => user.username === enteredUsername);

    return matchingUser?.banned === true;
  } catch {
    return false;
  }
}

function Login() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [showBannedModal, setShowBannedModal] = useState(false);

  const navigate = useNavigate();
  //acot functia de login din useAuth
  const { login } = useAuth();

  //centrez formularul pe pagina
  const pageStyle = {
    minHeight: "100vh",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    padding: "24px",
  };

  //controleaza cutia formularului
  const formStyle = {
    width: "100%",
    maxWidth: "360px",
    padding: "24px",
    border: "1px solid #ccc",
    borderRadius: "4px",
  };

  //pune spatiu intre campuri
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

  const modalOverlayStyle = {
    position: "fixed",
    inset: 0,
    backgroundColor: "rgba(0, 0, 0, 0.45)",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    padding: "24px",
  };

  const modalStyle = {
    width: "100%",
    maxWidth: "320px",
    padding: "24px",
    border: "1px solid #ccc",
    backgroundColor: "#fff",
    textAlign: "center",
  };

  //functia care ruleaza cand userul apasa butonul de login
  const handleSubmit = async (e) => {
    //ca sa previn refresh-ul la pagina cand e trimis
    e.preventDefault();
    setError("");
    setShowBannedModal(false);

    try {
      //request la backend
      const response = await api.post("/auth/login", {
        username,
        password,
      });

      //scot token-ul din raspuns
      const loggedInUser = login(response.data.token);

      //redirectioneaza dupa rol
      if (loggedInUser?.role === "ADMIN") {
        navigate("/admin");
      } else if (loggedInUser?.role === "MODERATOR") {
        navigate("/moderator");
      } else {
        navigate("/");
      }
    } catch (err) {
      const message = getErrorMessage(err, "Login failed");

      if (isBannedAccountError(message)) {
        setShowBannedModal(true);
        return;
      }

      if (err.response?.status === 403) {
        const userIsBanned = await isEnteredUserBanned(username);

        if (userIsBanned) {
          setShowBannedModal(true);
          return;
        }
      }

      setError(message);
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

      {showBannedModal && (
        <div style={modalOverlayStyle} role="dialog" aria-modal="true">
          <div style={modalStyle}>
            <p style={{ marginTop: 0, marginBottom: "20px" }}>
              This user got banned
            </p>
            <button
              type="button"
              onClick={() => setShowBannedModal(false)}
              style={buttonStyle}
            >
              OK
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

export default Login;
