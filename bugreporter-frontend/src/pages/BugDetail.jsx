import { useCallback, useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import {
  addBugTags,
  deleteBug,
  getBug,
  resolveBug,
  updateBug,
  updateBugStatus,
} from "../services/bugService";
import { useAuth } from "../context/AuthContext";

const statuses = ["OPEN", "IN_PROGRESS", "FIXED", "CLOSED"];

const pageStyle = {
  maxWidth: "860px",
  margin: "0 auto",
  padding: "24px",
};

const buttonStyle = {
  padding: "8px 12px",
  border: "1px solid #333",
  backgroundColor: "#f3f3f3",
  color: "#000",
  cursor: "pointer",
  textDecoration: "none",
};

const inputStyle = {
  padding: "8px",
  border: "1px solid #999",
  width: "100%",
  boxSizing: "border-box",
};

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

  const isModerator = user?.role === "MODERATOR" || user?.role === "ADMIN";
  const isAuthor = Boolean(user?.username && bug?.authorUsername === user.username);
  const canEdit = isAuthenticated && isAuthor;
  const canDelete = isAuthenticated && (isAuthor || isModerator);
  const canResolve = isAuthor && bug?.status !== "FIXED" && bug?.status !== "CLOSED";

  const loadBug = useCallback(async () => {
    setLoading(true);
    setError("");

    try {
      const response = await getBug(id);
      setBug(response.data);
      setForm({
        title: response.data.title || "",
        text: response.data.text || "",
        imageUrl: response.data.imageUrl || "",
      });
    } catch (err) {
      setError(err.response?.data?.message || "Could not load bug");
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
      navigate("/");
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

    if (tags.length === 0) {
      return;
    }

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

  if (loading) {
    return <main style={pageStyle}>Loading bug...</main>;
  }

  if (error && !bug) {
    return <main style={pageStyle}><p style={{ color: "red" }}>{error}</p></main>;
  }

  if (!bug) {
    return <main style={pageStyle}>Bug not found.</main>;
  }

  return (
    <main style={pageStyle}>
      <Link to="/" style={buttonStyle}>Back to bugs</Link>

      <section style={{ border: "1px solid #ddd", borderRadius: "4px", padding: "18px", marginTop: "16px" }}>
        {editing ? (
          <form onSubmit={handleUpdate}>
            <input
              aria-label="Edit bug title"
              required
              style={{ ...inputStyle, marginBottom: "8px" }}
              value={form.title}
              onChange={(event) => setForm({ ...form, title: event.target.value })}
            />
            <textarea
              aria-label="Edit bug description"
              required
              style={{ ...inputStyle, minHeight: "140px", marginBottom: "8px" }}
              value={form.text}
              onChange={(event) => setForm({ ...form, text: event.target.value })}
            />
            <input
              aria-label="Edit bug image url"
              style={{ ...inputStyle, marginBottom: "8px" }}
              value={form.imageUrl}
              onChange={(event) => setForm({ ...form, imageUrl: event.target.value })}
            />
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
              <span style={{ border: "1px solid #777", padding: "4px 8px", height: "fit-content" }}>
                {bug.status}
              </span>
            </div>
            <p>Author: {bug.authorUsername || "Unknown"}</p>
            <p>Date: {formatDate(bug.createdAt)}</p>
            {bug.imageUrl && (
              <p>
                Image: <a href={bug.imageUrl}>{bug.imageUrl}</a>
              </p>
            )}
            <p style={{ whiteSpace: "pre-wrap" }}>{bug.text}</p>
          </>
        )}

        <div style={{ display: "flex", gap: "6px", flexWrap: "wrap", marginTop: "12px" }}>
          {(bug.tags || []).map((tag) => (
            <span key={tag} style={{ border: "1px solid #999", padding: "4px 8px" }}>
              {tag}
            </span>
          ))}
        </div>

        {isAuthenticated && (
          <form onSubmit={handleTags} style={{ display: "flex", gap: "8px", marginTop: "16px" }}>
            <input
              aria-label="Add tags"
              style={inputStyle}
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
            <select aria-label="Bug status" value={bug.status} onChange={handleStatus} style={buttonStyle}>
              {statuses.map((status) => (
                <option key={status} value={status}>{status}</option>
              ))}
            </select>
          )}
        </div>

        {error && <p style={{ color: "red" }}>{error}</p>}
      </section>

      <section style={{ border: "1px dashed #aaa", padding: "16px", marginTop: "18px" }}>
        <h2 style={{ marginTop: 0 }}>Comments</h2>
        <p>Comments section placeholder.</p>
      </section>
    </main>
  );
}

export default BugDetail;
