function CommentSort({ value, onChange }) {
  const selectStyle = {
    padding: "6px 10px",
    border: "1px solid #999",
    backgroundColor: "#f3f3f3",
    cursor: "pointer",
    marginBottom: "16px",
  };

  return (
    <select
      style={selectStyle}
      value={value}
      onChange={(e) => onChange(e.target.value)}
    >
      <option value="date_desc">Newest first</option>
      <option value="date_asc">Oldest first</option>
      <option value="score_desc">Highest score</option>
      <option value="score_asc">Lowest score</option>
    </select>
  );
}

export default CommentSort;
