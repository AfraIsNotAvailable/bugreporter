import { Routes, Route } from "react-router-dom";
import Navbar from "./components/Navbar";

import BugList from "./pages/BugList";
import BugDetail from "./pages/BugDetail";
import Login from "./pages/Login";
import Register from "./pages/Register";
import Admin from "./pages/Admin";
import Moderator from "./pages/Moderator";
import PrivateRoute from "./routes/PrivateRoute";

function App() {
  return (
    <>
      <Navbar />

      <Routes>
        <Route path="/" element={<BugList />} />
        <Route path="/bugs" element={<BugList />} />
        <Route path="/bugs/:id" element={<BugDetail />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />

        <Route
          path="/admin"
          element={
            <PrivateRoute role="ADMIN">
              <Admin />
            </PrivateRoute>
          }
        />
        <Route
          path="/moderator"
          element={
            <PrivateRoute roles={["ADMIN", "MODERATOR"]}>
              <Moderator />
            </PrivateRoute>
          }
        />
      </Routes>
    </>
  );
}

export default App;
