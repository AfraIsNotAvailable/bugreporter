import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { createBug, filterBugs, getBugs } from "../services/bugService";
import { useAuth } from "../context/AuthContext";

const pageStyle = {
  maxWidth: "980px",
  margin: "0 auto",
  padding: "24px",
};

const rowStyle = {
  border: "1px solid #ddd",
  borderRadius: "4px",
  padding: "16px",
  marginBottom: "12px",
  backgroundColor: "#fff",
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
  minWidth: "180px",
};

function formatDate(value) {
  if (!value) {
    return "No date";
  }

  return new Date(value).toLocaleString();
}

function BugList() {
  const { isAuthenticated } = useAuth();
  const [bugs, setBugs] = useState([]);
  const [search, setSearch] = useState("");
  const [tag, setTag] = useState("");
  const [userId, setUserId] = useState("");
  const [showNewForm, setShowNewForm] = useState(false);
  const [form, setForm] = useState({ title: "", text: "", imageUrl: "" });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadBugs = async (params = null) => {
    setLoading(true);
    setError("");

    try {
      const response = params ? await filterBugs(params) : await getBugs();
      setBugs(response.data);
    } catch (err) {
      setError(err.response?.data?.message || "Could not load bugs");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadBugs();
  }, []);

  const handleSearch = (event) => {
    event.preventDefault();
    loadBugs(search.trim() ? { search: search.trim() } : null);
  };

  const handleTagFilter = (selectedTag = tag) => {
    loadBugs(selectedTag.trim() ? { tag: selectedTag.trim() } : null);
  };

  const handleMine = () => {
    loadBugs({ mine: true });
  };

  const handleUserFilter = (event) => {
    event.preventDefault();
    loadBugs(userId.trim() ? { userId: userId.trim() } : null);
  };

  const handleCreate = async (event) => {
    event.preventDefault();
    setError("");

    try {
      await createBug(form);
      setForm({ title: "", text: "", imageUrl: "" });
      setShowNewForm(false);
      await loadBugs();
    } catch (err) {
      setError(err.response?.data?.message || "Could not create bug");
    }
  };

  return (
    <main style={pageStyle}>
      <div style={{ display: "flex", justifyContent: "space-between", gap: "12px" }}>
        <h1 style={{ marginTop: 0 }}>Bugs</h1>
        {isAuthenticated && (
          <button type="button" style={buttonStyle} onClick={() => setShowNewForm(!showNewForm)}>
            Report Bug
          </button>
        )}
      </div>

      <div style={{ display: "flex", gap: "12px", flexWrap: "wrap", marginBottom: "16px" }}>
        <form onSubmit={handleSearch} style={{ display: "flex", gap: "8px" }}>
          <input
            aria-label="Search by title"
            style={inputStyle}
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Search title"
          />
          <button type="submit" style={buttonStyle}>Search</button>
        </form>

        <div style={{ display: "flex", gap: "8px" }}>
          <input
            aria-label="Filter by tag"
            style={inputStyle}
            value={tag}
            onChange={(event) => setTag(event.target.value)}
            placeholder="Filter tag"
          />
          <button type="button" style={buttonStyle} onClick={() => handleTagFilter()}>
            Filter Tag
          </button>
        </div>

        <form onSubmit={handleUserFilter} style={{ display: "flex", gap: "8px" }}>
          <input
            aria-label="Filter by user id"
            style={inputStyle}
            value={userId}
            onChange={(event) => setUserId(event.target.value)}
            placeholder="User id"
          />
          <button type="submit" style={buttonStyle}>By User</button>
        </form>

        {isAuthenticated && (
          <button type="button" style={buttonStyle} onClick={handleMine}>
            My Bugs
          </button>
        )}
        <button type="button" style={buttonStyle} onClick={() => loadBugs()}>
          Clear
        </button>
      </div>

      {showNewForm && (
        <form onSubmit={handleCreate} style={{ ...rowStyle, marginBottom: "16px" }}>
          <h2 style={{ marginTop: 0 }}>Report Bug</h2>
          <input
            aria-label="Bug title"
            required
            style={{ ...inputStyle, width: "100%", boxSizing: "border-box", marginBottom: "8px" }}
            value={form.title}
            onChange={(event) => setForm({ ...form, title: event.target.value })}
            placeholder="Title"
          />
          <textarea
            aria-label="Bug description"
            required
            style={{ ...inputStyle, width: "100%", minHeight: "100px", boxSizing: "border-box", marginBottom: "8px" }}
            value={form.text}
            onChange={(event) => setForm({ ...form, text: event.target.value })}
            placeholder="Description"
          />
          <input
            aria-label="Bug image url"
            style={{ ...inputStyle, width: "100%", boxSizing: "border-box", marginBottom: "8px" }}
            value={form.imageUrl}
            onChange={(event) => setForm({ ...form, imageUrl: event.target.value })}
            placeholder="Image URL"
          />
          <button type="submit" style={buttonStyle}>Create</button>
        </form>
      )}

      {error && <p style={{ color: "red" }}>{error}</p>}
      {loading && <p>Loading bugs...</p>}
      {!loading && bugs.length === 0 && <p>No bugs found.</p>}

      {!loading &&
        bugs.map((bug) => (
          <article key={bug.id} style={rowStyle}>
            <div style={{ display: "flex", justifyContent: "space-between", gap: "12px" }}>
              <h2 style={{ margin: "0 0 8px" }}>
                <Link to={`/bugs/${bug.id}`} style={{ color: "#000" }}>
                  {bug.title}
                </Link>
              </h2>
              <span style={{ border: "1px solid #777", padding: "4px 8px", height: "fit-content" }}>
                {bug.status}
              </span>
            </div>
            <p style={{ margin: "0 0 8px" }}>
              Author: {bug.authorUsername || "Unknown"} | {formatDate(bug.createdAt)}
            </p>
            <div style={{ display: "flex", gap: "6px", flexWrap: "wrap" }}>
              {(bug.tags || []).map((bugTag) => (
                <button
                  type="button"
                  key={bugTag}
                  style={{ ...buttonStyle, padding: "4px 8px" }}
                  onClick={() => {
                    setTag(bugTag);
                    handleTagFilter(bugTag);
                  }}
                >
                  {bugTag}
                </button>
              ))}
            </div>
          </article>
        ))}
    </main>
  );
}

export default BugList;
