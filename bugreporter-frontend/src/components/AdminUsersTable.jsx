import { useEffect, useState } from "react";
import api from "../api/axios";

const roles = ["USER", "MODERATOR", "ADMIN"];

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
  verticalAlign: "middle",
};

const buttonStyle = {
  padding: "8px 12px",
  border: "1px solid #333",
  backgroundColor: "#f3f3f3",
  cursor: "pointer",
};

const selectStyle = {
  padding: "8px",
  border: "1px solid #999",
  backgroundColor: "#fff",
};

function getErrorMessage(error, fallback) {
  const data = error.response?.data;

  if (typeof data === "string") {
    return data;
  }

  return data?.message || fallback;
}

//transforma datele venite intr-o forma mai usor de citit
function formatDate(date) {
  if (!date) {
    return "-";
  }

  return new Date(date).toLocaleDateString();
}

function AdminUsersTable({ canChangeRoles = false }) {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [actionError, setActionError] = useState("");
  const [updatingUserId, setUpdatingUserId] = useState(null);

  useEffect(() => {
    api
      .get("/admin/users")
      .then((res) => setUsers(res.data))
      .catch((err) =>
        setError(getErrorMessage(err, "Failed to load users")),
      )
      .finally(() => setLoading(false));
  }, []);

  const updateUserInState = (userId, changes) => {
    setUsers((currentUsers) =>
      currentUsers.map((user) =>
        //daca id-ul cautat returnez o copie actualizata, daca nu ramane neschimbat
        user.id === userId ? { ...user, ...changes } : user,
      ),
    );
  };

  const handleBanToggle = async (user) => {
    setActionError("");
    setUpdatingUserId(user.id);

    try {
      const banned = !user.banned;
      const response = await api.put(`/admin/users/${user.id}/ban`, null, {
        params: { banned },
      });

      updateUserInState(
        user.id,
        response.data && typeof response.data === "object"
          ? response.data
          : { banned },
      );
    } catch (err) {
      setActionError(getErrorMessage(err, "Failed to update ban status"));
    } finally {
      setUpdatingUserId(null);
    }
  };

  const handleRoleChange = async (user, role) => {
    setActionError("");
    setUpdatingUserId(user.id);

    try {
      const response = await api.put(`/admin/users/${user.id}/role`, null, {
        params: { role },
      });

      updateUserInState(
        user.id,
        response.data && typeof response.data === "object"
          ? response.data
          : { role },
      );
    } catch (err) {
      setActionError(getErrorMessage(err, "Failed to update role"));
    } finally {
      setUpdatingUserId(null);
    }
  };

  if (loading) {
    return <p>Loading users...</p>;
  }

  if (error) {
    return <p style={{ color: "red" }}>{error}</p>;
  }

  return (
    <>
      {actionError && (
        <p style={{ marginTop: 0, color: "red" }}>{actionError}</p>
      )}

      <div style={{ overflowX: "auto" }}>
        <table style={tableStyle}>
          <thead>
            <tr>
              <th style={thStyle}>ID</th>
              <th style={thStyle}>Username</th>
              <th style={thStyle}>Email</th>
              <th style={thStyle}>Role</th>
              <th style={thStyle}>Banned</th>
              <th style={thStyle}>Created</th>
              <th style={thStyle}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {users.length === 0 && (
              <tr>
                <td style={tdStyle} colSpan="7">
                  No users found.
                </td>
              </tr>
            )}

            {users.map((user) => (
              <tr key={user.id}>
                <td style={tdStyle}>{user.id}</td>
                <td style={tdStyle}>{user.username}</td>
                <td style={tdStyle}>{user.email}</td>
                <td style={tdStyle}>
                  {canChangeRoles ? (
                    <select
                      value={user.role}
                      onChange={(e) => handleRoleChange(user, e.target.value)}
                      disabled={updatingUserId === user.id}
                      style={selectStyle}
                    >
                      {roles.map((role) => (
                        <option key={role} value={role}>
                          {role}
                        </option>
                      ))}
                    </select>
                  ) : (
                    user.role
                  )}
                </td>
                <td style={tdStyle}>{user.banned ? "Yes" : "No"}</td>
                <td style={tdStyle}>{formatDate(user.createdAt)}</td>
                <td style={tdStyle}>
                  <button
                    type="button"
                    onClick={() => handleBanToggle(user)}
                    disabled={updatingUserId === user.id}
                    style={buttonStyle}
                  >
                    {user.banned ? "Unban" : "Ban"}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}

export default AdminUsersTable;
