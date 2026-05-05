import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import api from "../api/axios";
import {
  addBugTags,
  deleteBug,
  getBug,
  resolveBug,
  updateBug,
  updateBugStatus,
} from "../services/bugService";
import { useAuth } from "../context/AuthContext";
import CommentCard from "../components/CommentCard";
import CommentSort from "../components/CommentSort";

const statuses = ["OPEN", "IN_PROGRESS", "FIXED", "CLOSED"];

function formatDate(value) {
  if (!value) {
    return "No date";
  }
  return new Date(value).toLocaleString();
}

function BugDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { isAuthenticated, user } = useAuth();

  const [bug, setBug] = useState(null);
  const [tagInput, setTagInput] = useState("");
  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState({ title: "", text: "", imageUrl: "" });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [comments, setComments] = useState([]);
  const [sortOrder, setSortOrder] = useState("date_desc");

  const pageStyle = {
    padding: "24px",
    maxWidth: "720px",
    margin: "0 auto",
  };

  const buttonStyle = {
    padding: "8px 14px",
    border: "1px solid #333",
    backgroundColor: "#f3f3f3",
    color: "#000",
    cursor: "pointer",
    textDecoration: "none",
  };

  const inputStyle = {
    padding: "10px",
    border: "1px solid #999",
    width: "100%",
    boxSizing: "border-box",
  };

  const isModerator = user?.role === "MODERATOR" || user?.role === "ADMIN";
  const isAuthor = Boolean(user?.username && bug?.authorUsername === user.username);
  const canEdit = isAuthenticated && isAuthor;
  const canDelete = isAuthenticated && (isAuthor || isModerator);
  const canResolve = isAuthor && bug?.status !== "FIXED" && bug?.status !== "CLOSED";

  const sortedComments = useMemo(() => {
    const copy = [...comments];
    switch (sortOrder) {
      case "date_desc":
        return copy.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
      case "date_asc":
        return copy.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
      case "score_desc":
        return copy.sort((a, b) => b.score - a.score);
      case "score_asc":
        return copy.sort((a, b) => a.score - b.score);
      default:
        return copy;
    }
  }, [comments, sortOrder]);

  const loadBug = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [bugRes, commentsRes] = await Promise.all([
        getBug(id),
        api.get(`/comments/bug/${id}`),
      ]);
      setBug(bugRes.data);
      setForm({
        title: bugRes.data.title || "",
        text: bugRes.data.text || "",
        imageUrl: bugRes.data.imageUrl || "",
      });
      setComments(commentsRes.data);
    } catch (err) {
      setError(err.response?.data?.message || "Failed to load bug details");
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    loadBug();
  }, [loadBug]);

  const handleUpdate = async (event) => {
    event.preventDefault();
    setError("");
    try {
      const response = await updateBug(id, form);
      setBug(response.data);
      setEditing(false);
    } catch (err) {
      setError(err.response?.data?.message || "Could not update bug");
    }
  };

  const handleDelete = async () => {
    setError("");
    try {
      await deleteBug(id);
      navigate("/bugs");
    } catch (err) {
      setError(err.response?.data?.message || "Could not delete bug");
    }
  };

  const handleTags = async (event) => {
    event.preventDefault();
    const tags = tagInput
      .split(",")
      .map((tag) => tag.trim())
      .filter(Boolean);
    if (tags.length === 0) return;
    try {
      const response = await addBugTags(id, tags);
      setBug(response.data);
      setTagInput("");
    } catch (err) {
      setError(err.response?.data?.message || "Could not add tags");
    }
  };

  const handleResolve = async () => {
    setError("");
    try {
      const response = await resolveBug(id);
      setBug(response.data);
    } catch (err) {
      setError(err.response?.data?.message || "Could not resolve bug");
    }
  };

  const handleStatus = async (event) => {
    setError("");
    try {
      const response = await updateBugStatus(id, event.target.value);
      setBug(response.data);
    } catch (err) {
      setError(err.response?.data?.message || "Could not update status");
    }
  };

  if (loading) return <div style={{ padding: "24px" }}>Loading...</div>;
  if (error && !bug) return <div style={{ padding: "24px", color: "red" }}>{error}</div>;
  if (!bug) return <div style={{ padding: "24px" }}>Bug not found.</div>;

  return (
    <div style={pageStyle}>
      <Link to="/bugs" style={buttonStyle}>Back to bugs</Link>

      <div style={{ border: "1px solid #ddd", padding: "24px", marginTop: "16px" }}>
        {editing ? (
          <form onSubmit={handleUpdate}>
            <div style={{ marginBottom: "16px" }}>
              <label>Title:</label>
              <input
                required
                style={{ ...inputStyle, marginTop: "6px" }}
                value={form.title}
                onChange={(event) => setForm({ ...form, title: event.target.value })}
              />
            </div>
            <div style={{ marginBottom: "16px" }}>
              <label>Description:</label>
              <textarea
                required
                style={{ ...inputStyle, marginTop: "6px", minHeight: "140px" }}
                value={form.text}
                onChange={(event) => setForm({ ...form, text: event.target.value })}
              />
            </div>
            <div style={{ marginBottom: "16px" }}>
              <label>Image URL:</label>
              <input
                style={{ ...inputStyle, marginTop: "6px" }}
                value={form.imageUrl}
                onChange={(event) => setForm({ ...form, imageUrl: event.target.value })}
              />
            </div>
            <div style={{ display: "flex", gap: "8px" }}>
              <button type="submit" style={buttonStyle}>Save</button>
              <button type="button" style={buttonStyle} onClick={() => setEditing(false)}>
                Cancel
              </button>
            </div>
          </form>
        ) : (
          <>
            <div style={{ display: "flex", justifyContent: "space-between", gap: "12px" }}>
              <h1 style={{ marginTop: 0 }}>{bug.title}</h1>
              <span style={{ border: "1px solid #999", padding: "4px 10px", height: "fit-content" }}>
                {bug.status}
              </span>
            </div>
            <p style={{ color: "#666", fontSize: "13px" }}>
              {bug.authorUsername} · {formatDate(bug.createdAt)}
            </p>
            {bug.imageUrl && (
              <p>Image: <a href={bug.imageUrl} style={{ color: "#000" }}>{bug.imageUrl}</a></p>
            )}
            <p style={{ whiteSpace: "pre-wrap" }}>{bug.text}</p>
          </>
        )}

        <div style={{ display: "flex", gap: "6px", flexWrap: "wrap", marginTop: "12px" }}>
          {(bug.tags || []).map((tag) => (
            <span key={tag} style={{ border: "1px solid #ccc", padding: "4px 8px" }}>
              {tag}
            </span>
          ))}
        </div>

        {isAuthenticated && (
          <form onSubmit={handleTags} style={{ display: "flex", gap: "8px", marginTop: "16px" }}>
            <input
              style={{ ...inputStyle, width: "auto", flex: 1 }}
              value={tagInput}
              onChange={(event) => setTagInput(event.target.value)}
              placeholder="Add tags, comma separated"
            />
            <button type="submit" style={buttonStyle}>Add Tags</button>
          </form>
        )}

        <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", marginTop: "16px" }}>
          {canEdit && !editing && (
            <button type="button" style={buttonStyle} onClick={() => setEditing(true)}>
              Edit
            </button>
          )}
          {canDelete && (
            <button type="button" style={buttonStyle} onClick={handleDelete}>
              Delete
            </button>
          )}
          {canResolve && (
            <button type="button" style={buttonStyle} onClick={handleResolve}>
              Mark as Resolved
            </button>
          )}
          {isModerator && (
            <select value={bug.status} onChange={handleStatus} style={buttonStyle}>
              {statuses.map((status) => (
                <option key={status} value={status}>{status}</option>
              ))}
            </select>
          )}
        </div>

        {error && <p style={{ marginTop: "16px", color: "red" }}>{error}</p>}
      </div>

      <hr style={{ margin: "24px 0", borderColor: "#ddd" }} />

      <h2>Comments</h2>
      <CommentSort value={sortOrder} onChange={setSortOrder} />
      {sortedComments.length === 0 && <p>No comments yet.</p>}
      {sortedComments.map((comment) => (
        <CommentCard
          key={comment.id}
          comment={comment}
          onDelete={(deletedId) =>
            setComments((prev) => prev.filter((c) => c.id !== deletedId))
          }
        />
      ))}
    </div>
  );
}

export default BugDetail;
