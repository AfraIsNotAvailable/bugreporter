import { useNavigate } from "react-router";

function BugCard({ bug }) {
  const navigate = useNavigate();

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
        {bug.authorUsername} · {new Date(bug.createdAt).toLocaleDateString()}
      </p>
      <p style={descStyle}>{bug.text}</p>
    </div>
  );
}

export default BugCard;
