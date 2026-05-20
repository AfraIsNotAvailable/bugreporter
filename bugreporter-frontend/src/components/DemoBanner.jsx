const IS_DEMO = import.meta.env.VITE_DEMO_MODE === "true";

export default function DemoBanner() {
  if (!IS_DEMO) return null;
  return (
    <div style={{
      position: "fixed", bottom: 0, left: 0, right: 0, zIndex: 9999,
      background: "#1e293b", color: "#f8fafc",
      padding: "10px 20px",
      display: "flex", alignItems: "center", justifyContent: "center", gap: "12px",
      fontSize: "0.85rem",
    }}>
      <span style={{ background: "#f59e0b", color: "#1e293b", fontWeight: 700, padding: "2px 8px", borderRadius: "4px", fontSize: "0.75rem" }}>
        DEMO
      </span>
      <span>
        Live portfolio demo — data resets on refresh. Log in as&nbsp;
        <strong>ana / ana</strong> (Admin),&nbsp;
        <strong>mihai / mihai</strong> (Moderator), or&nbsp;
        <strong>rares / rares</strong> (User).
      </span>
      <a
        href="https://github.com/AfraIsNotAvailable/bugreporter"
        target="_blank"
        rel="noopener noreferrer"
        style={{ color: "#93c5fd", textDecoration: "none", whiteSpace: "nowrap" }}
      >
        View source ↗
      </a>
    </div>
  );
}
