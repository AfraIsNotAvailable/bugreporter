import { useEffect, useState } from "react";
import api from "../api/axios";
import AdminUsersTable from "../components/AdminUsersTable";

const pageStyle = {
  padding: "24px",
  maxWidth: "1040px",
  margin: "0 auto",
};

const sectionStyle = {
  marginBottom: "28px",
};

const tableStyle = {
  width: "100%",
  borderCollapse: "collapse",
};

const thStyle = {
  padding: "10px",
  border: "1px solid #ccc",
  backgroundColor: "#f3f3f3",
  textAlign: "left",
};

const tdStyle = {
  padding: "10px",
  border: "1px solid #ccc",
  verticalAlign: "top",
};

function getErrorMessage(error, fallback) {
  const data = error.response?.data;

  if (typeof data === "string") {
    return data;
  }

  return data?.message || fallback;
}

function formatDate(date) {
  if (!date) {
    return "-";
  }

  return new Date(date).toLocaleDateString();
}

function Admin() {
  const [bugs, setBugs] = useState([]);
  const [comments, setComments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    Promise.all([api.get("/admin/bugs"), api.get("/admin/comments")])
      .then(([bugsRes, commentsRes]) => {
        setBugs(bugsRes.data);
        setComments(commentsRes.data);
      })
      .catch((err) =>
        setError(getErrorMessage(err, "Failed to load admin data")),
      )
      .finally(() => setLoading(false));
  }, []);

  return (
    <div style={pageStyle}>
      <h1 style={{ marginTop: 0, marginBottom: "20px" }}>Admin Panel</h1>

      <section style={sectionStyle}>
        <h2>Users</h2>
        <AdminUsersTable canChangeRoles />
      </section>

      <section style={sectionStyle}>
        <h2>Bugs / Posts</h2>
        {loading && <p>Loading bugs...</p>}
        {error && <p style={{ color: "red" }}>{error}</p>}
        {!loading && !error && (
          <div style={{ overflowX: "auto" }}>
            <table style={tableStyle}>
              <thead>
                <tr>
                  <th style={thStyle}>ID</th>
                  <th style={thStyle}>Title</th>
                  <th style={thStyle}>Description</th>
                  <th style={thStyle}>Status</th>
                  <th style={thStyle}>Author</th>
                  <th style={thStyle}>Created</th>
                </tr>
              </thead>
              <tbody>
                {bugs.length === 0 && (
                  <tr>
                    <td style={tdStyle} colSpan="5">
                      No bugs found.
                    </td>
                  </tr>
                )}
                {bugs.map((bug) => (
                  <tr key={bug.id}>
                    <td style={tdStyle}>{bug.id}</td>
                    <td style={tdStyle}>{bug.title}</td>
                    <td style={tdStyle}>{bug.text}</td>
                    <td style={tdStyle}>{bug.status}</td>
                    <td style={tdStyle}>{bug.authorUsername || "-"}</td>
                    <td style={tdStyle}>{formatDate(bug.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section style={sectionStyle}>
        <h2>Comments</h2>
        {loading && <p>Loading comments...</p>}
        {error && <p style={{ color: "red" }}>{error}</p>}
        {!loading && !error && (
          <div style={{ overflowX: "auto" }}>
            <table style={tableStyle}>
              <thead>
                <tr>
                  <th style={thStyle}>ID</th>
                  <th style={thStyle}>Text</th>
                  <th style={thStyle}>Author</th>
                  <th style={thStyle}>Bug / Post ID</th>
                  <th style={thStyle}>Created</th>
                </tr>
              </thead>
              <tbody>
                {comments.length === 0 && (
                  <tr>
                    <td style={tdStyle} colSpan="5">
                      No comments found.
                    </td>
                  </tr>
                )}
                {comments.map((comment) => (
                  <tr key={comment.id}>
                    <td style={tdStyle}>{comment.id}</td>
                    <td style={tdStyle}>{comment.text}</td>
                    <td style={tdStyle}>{comment.authorUsername || "-"}</td>
                    <td style={tdStyle}>{comment.bugId}</td>
                    <td style={tdStyle}>{formatDate(comment.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}

export default Admin;
