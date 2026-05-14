import { useState } from "react";
import { useAuth } from "../context/AuthContext";
import api from "../api/axios";

function CommentCard({ comment, onDelete }) {
  const { user } = useAuth();
  const [vote, setVote] = useState(comment.userVote ?? null); // 'up' | 'down' | null
  const [score, setScore] = useState(comment.score);
  const [isEditing, setIsEditing] = useState(false);
  const [editText, setEditText] = useState(comment.text);
  const [displayText, setDisplayText] = useState(comment.text);
  const [showMenu, setShowMenu] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  const isOwn = comment.authorUsername === user?.username;
  // console.log(
  //   "authorUsername:",
  //   comment.authorUsername,
  //   "| user:",
  //   user?.username,
  //   "| isOwn:",
  //   isOwn,
  // );
  // console.log("full user object:", user);
  const isPriviledged = user?.role === "ADMIN" || user?.role === "MODERATOR";
  const canEdit = isOwn || isPriviledged;

  const handleDelete = async () => {
    try {
      await api.delete(`/comments/${comment.id}`);
      setShowDeleteConfirm(false);
      onDelete?.(comment.id);
    } catch {
      setShowDeleteConfirm(false);
    }
  };

  const handleEditSubmit = async () => {
    try {
      await api.put(`/comments/${comment.id}`, {
        text: editText,
      });
      setDisplayText(editText);
      setIsEditing(false);
    } catch {
      // keep editing open on failure
    }
  };

  const handleVote = async (type) => {
    const removing = vote === type; // if clicking same vote, we want to remove it
    try {
      await api.post(`/comments/${comment.id}/vote`, {
        voteType: type,
      });
      if (removing) {
        setScore((s) => (type === "UPVOTE" ? s - 1 : s + 1));
        setVote(null);
      } else {
        setScore((s) => {
          let delta = type === "UPVOTE" ? 1 : -1;
          if (vote !== null) {
            delta *= 2; // switching sides
          }
          return s + delta;
        });
        setVote(type);
      }
    } catch {
      // silent fail
    }
  };

  const arrowButtonStyle = (active) => ({
    padding: "4px 8px",
    // border: "1px solid #333",
    backgroundColor: active ? "#333" : "#f3f3f3",
    color: active ? "#fff" : "#000",
    cursor: "pointer",
  });

  const cardStyle = {
    padding: "16px",
    border: "1px solid #ddd",
    // borderRadius: "4px",
    marginBottom: "12px",
    position: "relative",
  };

  const metaStyle = {
    fontSize: "13px",
    color: "#666",
    marginBottom: "10px",
  };

  const imageStyle = {
    maxWidth: "100%",
    marginBottom: "10px",
    border: "1px solid #ccc",
  };

  const bottomRowStyle = {
    display: "flex",
    alignItems: "center",
    gap: "8px",
    marginTop: "12px",
  };

  const editStyle = {
    width: "100%",
    padding: "8px",
    border: "1px solid #999",
    boxSizing: "border-box",
    minHeight: "80px",
  };

  const buttonStyle = {
    padding: "6px 12px",
    border: "1px solid #333",
    backgroundColor: "#f3f3f3",
    // color: "#000",
    cursor: "pointer",
  };

  return (
    <div data-testid="comment-card" style={cardStyle}>
      <p style={metaStyle}>
        {comment.authorUsername ?? `User #${comment.authorId}`} ·{" "}
        {new Date(comment.createdAt).toLocaleDateString()}
      </p>

      {canEdit && (
        <div style={{ position: "absolute", top: "12px", right: "12px" }}>
          <button
            style={{ ...buttonStyle, padding: "2px 8px" }}
            onClick={() => setShowMenu((v) => !v)}
          >
            ...
          </button>
          {showMenu && (
            <div
              style={{
                position: "absolute",
                top: "100%",
                right: "0",
                backgroundColor: "#fff",
                border: "1px solid #ccc",
                zIndex: 10,
                minWidth: "100px",
              }}
            >
              <button
                style={{
                  display: "block",
                  width: "100%",
                  padding: "8px 12px",
                  border: "none",
                  backgroundColor: "#fff",
                  cursor: "pointer",
                  textAlign: "left",
                }}
                onClick={() => {
                  setIsEditing(true);
                  setShowMenu(false);
                }}
              >
                Edit
              </button>
              {(isOwn || isPriviledged) && (
                <button
                  style={{
                    display: "block",
                    width: "100%",
                    padding: "8px 12px",
                    border: "none",
                    backgroundColor: "#fff",
                    cursor: "pointer",
                    textAlign: "left",
                  }}
                  onClick={() => {
                    setShowDeleteConfirm(true);
                    setShowMenu(false);
                  }}
                >
                  Delete
                </button>
              )}
            </div>
          )}
        </div>
      )}

      {comment.imageUrl && (
        <img src={comment.imageUrl} alt="attachment" style={imageStyle} />
      )}

      {isEditing ? (
        <textarea
          aria-label="Edit comment text"
          value={editText}
          onChange={(e) => setEditText(e.target.value)}
          style={editStyle}
        />
      ) : (
        <p style={{ margin: "0" }}>{displayText}</p>
      )}

      <div style={bottomRowStyle}>
        {!isOwn && (
          <>
            <button
              aria-label="Upvote"
              style={arrowButtonStyle(vote === "UPVOTE")}
              onClick={() => handleVote("UPVOTE")}
            >
              ▲
            </button>
            <button
              aria-label="Downvote"
              style={arrowButtonStyle(vote === "DOWNVOTE")}
              onClick={() => handleVote("DOWNVOTE")}
            >
              ▼
            </button>
          </>
        )}
        <span data-testid="comment-score">{score}</span>
        {canEdit && (
          <div style={{ marginLeft: "auto" }}>
            {isEditing ? (
              <button style={buttonStyle} onClick={handleEditSubmit}>
                Submit
              </button>
            ) : (
              <button style={buttonStyle} onClick={() => setIsEditing(true)}>
                Edit
              </button>
            )}
          </div>
        )}
      </div>
      {showDeleteConfirm && (
        <div
          style={{
            position: "fixed",
            inset: 0,
            backgroundColor: "rgba(0,0,0,0.4)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            zIndex: 100,
          }}
        >
          <div
            style={{
              backgroundColor: "#fff",
              padding: "24px",
              border: "1px solid #ccc",
              borderRadius: "4px",
              maxWidth: "360px",
              width: "100%",
            }}
          >
            <p style={{ marginTop: 0, marginBottom: "16px" }}>
              Are you sure you want to delete this comment?
            </p>
            <div style={{ display: "flex", gap: "12px" }}>
              <button
                style={buttonStyle}
                onClick={() => setShowDeleteConfirm(false)}
              >
                Cancel
              </button>
              <button
                style={{ ...buttonStyle, borderColor: "red", color: "red" }}
                onClick={handleDelete}
              >
                Confirm
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default CommentCard;
