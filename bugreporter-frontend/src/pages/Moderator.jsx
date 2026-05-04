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

const selectStyle = {
  padding: "8px",
  border: "1px solid #999",
  backgroundColor: "#fff",
};

const statuses = ["OPEN", "IN_PROGRESS", "FIXED", "CLOSED"];

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

function Moderator() {
  const [bugs, setBugs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [actionError, setActionError] = useState("");
  const [updatingBugId, setUpdatingBugId] = useState(null);

  useEffect(() => {
    api
      .get("/bugs")
      .then((res) => setBugs(res.data))
      .catch((err) => setError(getErrorMessage(err, "Failed to load bugs")))
      .finally(() => setLoading(false));
  }, []);

  const updateBugStatus = async (bug, status) => {
    setActionError("");
    setUpdatingBugId(bug.id);

    try {
      const response = await api.patch(`/bugs/${bug.id}/status`, null, {
        params: { status },
      });

      setBugs((currentBugs) =>
        currentBugs.map((currentBug) =>
          currentBug.id === bug.id ? response.data : currentBug,
        ),
      );
    } catch (err) {
      setActionError(getErrorMessage(err, "Failed to update bug status"));
    } finally {
      setUpdatingBugId(null);
    }
  };

  if (loading) {
    return <div style={{ padding: "24px" }}>Loading bugs...</div>;
  }

  if (error) {
    return <div style={{ padding: "24px", color: "red" }}>{error}</div>;
  }

  return (
    <div style={pageStyle}>
      <h1 style={{ marginTop: 0, marginBottom: "20px" }}>Moderator Panel</h1>

      <section style={sectionStyle}>
        <h2>Users</h2>
        <AdminUsersTable />
      </section>

      <section style={sectionStyle}>
        <h2>Bug Status</h2>

        {actionError && <p style={{ color: "red" }}>{actionError}</p>}

        <div style={{ overflowX: "auto" }}>
          <table style={tableStyle}>
            <thead>
              <tr>
                <th style={thStyle}>ID</th>
                <th style={thStyle}>Title</th>
                <th style={thStyle}>Description</th>
                <th style={thStyle}>Author</th>
                <th style={thStyle}>Created</th>
                <th style={thStyle}>Status</th>
              </tr>
            </thead>
            <tbody>
              {bugs.length === 0 && (
                <tr>
                  <td style={tdStyle} colSpan="6">
                    No bugs found.
                  </td>
                </tr>
              )}

              {bugs.map((bug) => (
                <tr key={bug.id}>
                  <td style={tdStyle}>{bug.id}</td>
                  <td style={tdStyle}>{bug.title}</td>
                  <td style={tdStyle}>{bug.text}</td>
                  <td style={tdStyle}>{bug.authorUsername || "-"}</td>
                  <td style={tdStyle}>{formatDate(bug.createdAt)}</td>
                  <td style={tdStyle}>
                    <select
                      value={bug.status}
                      onChange={(e) => updateBugStatus(bug, e.target.value)}
                      disabled={updatingBugId === bug.id}
                      style={selectStyle}
                    >
                      {statuses.map((status) => (
                        <option key={status} value={status}>
                          {status}
                        </option>
                      ))}
                    </select>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}

export default Moderator;
