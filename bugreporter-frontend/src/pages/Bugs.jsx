import { useEffect, useState } from "react";
import api from "../api/axios";
import BugCard from "../components/BugCard";

function Bugs() {
  const [bugs, setBugs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const pageStyle = {
    padding: "24px",
    maxWidth: "720px",
    margin: "0 auto",
  };

  useEffect(() => {
    api
      .get("/bugs")
      .then((res) => {
        console.log("Fetched bugs:", res.data[0]);
        setBugs(res.data);
      })
      .catch(() => setError("Failed to load bugs"))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div style={{ padding: "24px" }}>Loading...</div>;
  if (error)
    return <div style={{ padding: "24px", color: "red" }}>{error}</div>;

  return (
    <div style={pageStyle}>
      <h1 style={{ marginTop: "0", marginBottom: "16px", fontSize: "16px", fontStyle: "italic" }}>Bugs</h1>
      {bugs.length === 0 && <p>No bugs reported yet.</p>}
      {bugs.map((bug) => (
        <BugCard key={bug.id} bug={bug} />
      ))}
    </div>
  );
}

export default Bugs;
