import { BrowserRouter, Routes, Route } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import PrivateRoute from "./routes/PrivateRoute";
import Navbar from "./components/Navbar";

// pagini simple temporare
function Home() {
  return (
    <main className="p-6">
      <h1 className="text-2xl font-bold">BugReporter</h1>
      <p className="mt-2">Home page</p>
    </main>
  );
}

function Login() {
  return <h1 className="p-6 text-xl">Login page</h1>;
}

function Register() {
  return <h1 className="p-6 text-xl">Register page</h1>;
}

function Admin() {
  return <h1 className="p-6 text-xl">Admin page</h1>;
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Navbar />

        <Routes>
          <Route path="/" element={<Home />} />
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
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
