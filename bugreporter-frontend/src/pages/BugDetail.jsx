import { useEffect, useState, useMemo } from "react";
import { useParams } from "react-router";
import api from "../api/axios";

import CommentCard from "../components/CommentCard";
import CommentSort from "../components/CommentSort";

function BugDetail() {
  const { id } = useParams();
  const [bug, setBug] = useState(null);
  const [comments, setComments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [sortOrder, setSortOrder] = useState("date_desc");

  const pageStyle = {
    padding: "24px",
    maxWidth: "720px",
    margin: "0 auto",
  };

  const sortedComments = useMemo(() => {
    const copy = [...comments];

    switch (sortOrder) {
      case "date_desc":
        return copy.sort(
          (a, b) => new Date(b.createdAt) - new Date(a.createdAt),
        );
      case "date_asc":
        return copy.sort(
          (a, b) => new Date(a.createdAt) - new Date(b.createdAt),
        );
      case "score_desc":
        return copy.sort((a, b) => b.score - a.score);
      case "score_asc":
        return copy.sort((a, b) => a.score - b.score);
      default:
        return copy;
    }
  }, [comments, sortOrder]);

  useEffect(() => {
    console.log("bug id from url:", id);

    Promise.all([api.get(`/bugs/${id}`), api.get(`/comments/bug/${id}`)])
      .then(([bugRes, commentsRes]) => {
        console.log("comment's raw response:", commentsRes);
        setBug(bugRes.data);
        setComments(commentsRes.data);
      })
      .catch((err) => {
        console.log("fetch error: ", err);
        setError("Failed to load bug details");
      })
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) return <div style={{ padding: "24px" }}>Loading...</div>;
  if (error)
    return <div style={{ padding: "24px", color: "red" }}>{error}</div>;

  return (
    <div style={pageStyle}>
      <h1 style={{ marginTop: "0" }}>{bug?.title}</h1>
      <p style={{ color: "#666", fontSize: "13px" }}>
        {bug.authorUsername} · {new Date(bug.createdAt).toLocaleDateString()}
      </p>
      <p>{bug.text}</p>

      <hr style={{ margin: "24px 0", borderColor: "#ddd" }} />

      <h2>Comments</h2>
      <CommentSort value={sortOrder} onChange={setSortOrder} />
      {sortedComments.length === 0 && <p>No comments yet.</p>}
      {sortedComments.map((comment) => (
        <CommentCard
          key={comment.id}
          comment={comment}
          onDelete={(id) =>
            setComments((prev) => prev.filter((c) => c.id !== id))
          }
        />
      ))}
    </div>
  );
}

export default BugDetail;
