import { useNavigate } from "react-router";
import { useAuth } from "../context/AuthContext";

function BugCard({ bug }) {
  const navigate = useNavigate();
  const { user } = useAuth();

  const score = bug.voteScore ?? 0;
  const scoreColor = score > 0 ? "#2a7a2a" : score < 0 ? "#b00" : "#666";

  const cardStyle = {
    padding: "16px",
    border: "1px solid #ccc",
    borderRadius: "0px",
    cursor: "pointer",
    marginBottom: "16px",
  };

  const titleStyle = {
    margin: "0 0 4px 0",
  };

  const metaStyle = {
    fontSize: "13px",
    color: "#666",
    marginBottom: "8px",
  };

  const descStyle = {
    margin: "0",
  };

  return (
    <div style={cardStyle} onClick={() => navigate(`/bugs/${bug.id}`)}>
      <h2 style={titleStyle}>
        <i style={{ color: "gray", fontWeight: "bold", fontSize: "12px" }}>
          #{bug.id}
        </i>
        {" | "}
        {bug.title}
      </h2>
      <p style={metaStyle}>
        {bug.authorUsername}
        {bug.authorScore !== undefined && (
          <span style={{ color: (bug.authorScore ?? 0) > 0 ? "#2a7a2a" : (bug.authorScore ?? 0) < 0 ? "#b00" : "#666" }}>
            {" "}[{(bug.authorScore ?? 0) > 0 ? `+${bug.authorScore}` : bug.authorScore ?? 0}]
          </span>
        )}
        {" · "}{new Date(bug.createdAt).toLocaleDateString()}
        {" · "}
        <span style={{ color: scoreColor, fontWeight: "bold" }}>
          {score > 0 ? `+${score}` : score}
        </span>
      </p>
      <p style={descStyle}>{bug.text}</p>
    </div>
  );
}

export default BugCard;
