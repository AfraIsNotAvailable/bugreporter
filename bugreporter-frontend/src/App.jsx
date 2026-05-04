import { Routes, Route } from "react-router-dom";
import Navbar from "./components/Navbar";
import Home from "./pages/Home";
import Login from "./pages/Login";
import Register from "./pages/Register";
import Admin from "./pages/Admin";
import Moderator from "./pages/Moderator";
import PrivateRoute from "./routes/PrivateRoute";
import Bugs from "./pages/Bugs";
import BugDetail from "./pages/BugDetail";

function App() {
  return (
    <>
      <Navbar />

      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/bugs" element={<Bugs />} />
        <Route path="/bugs/:id" element={<BugDetail />} />

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
            <PrivateRoute role="MODERATOR">
              <Moderator />
            </PrivateRoute>
          }
        />
      </Routes>
    </>
  );
}

export default App;
