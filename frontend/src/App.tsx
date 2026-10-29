import { useEffect, useState } from "react";
import type { FormEvent, ReactNode } from "react";
import {
  BriefcaseBusiness,
  LayoutDashboard,
  FileText,
  CalendarDays,
  Search,
  RefreshCw,
  Users,
  ShieldCheck,
  Sparkles,
  MapPin,
  LogOut,
} from "lucide-react";
import "./style.css";

const API = (import.meta.env.VITE_API_BASE_URL || "http://localhost:8080").replace(/\/$/, "");

type User = {
  id?: number;
  fullName?: string;
  username: string;
  email?: string;
  role: string;
};
type Job = { id: number; title: string; companyName?: string; location?: string; employmentType?: string; description?: string; requiredSkills?: string; status?: string };
type Application = { id: number; jobId?: number; status?: string };
type Interview = { id: number; status?: string; interviewDate?: string };

export default function App() {
  const [page, setPage] = useState("Overview");
  const [jobs, setJobs] = useState<Job[]>([]);
  const [applications, setApplications] = useState<Application[]>([]);
  const [interviews, setInterviews] = useState<Interview[]>([]);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [user, setUser] = useState<User | null>(null);
  const [authChecking, setAuthChecking] = useState(true);
  const [authMode, setAuthMode] = useState<"login" | "register">("login");
  const [authBusy, setAuthBusy] = useState(false);
  const [authMessage, setAuthMessage] = useState("");
  const [loginForm, setLoginForm] = useState({ username: "", password: "" });
  const [registerForm, setRegisterForm] = useState({ fullName: "", username: "", email: "", password: "", role: "CANDIDATE" });

  useEffect(() => {
    const token = localStorage.getItem("token");
    if (!token) {
      setAuthChecking(false);
      return;
    }
    fetch(`${API}/api/auth/me`, { headers: { Authorization: `Bearer ${token}` } })
      .then(async (response) => {
        if (!response.ok) throw new Error("Your session expired. Please log in again.");
        const profile = await response.json();
        setUser(profile as User);
      })
      .catch(() => {
        localStorage.removeItem("token");
        localStorage.removeItem("user");
        setUser(null);
      })
      .finally(() => setAuthChecking(false));
  }, []);

  useEffect(() => {
    if (user) void loadData();
  }, [user]);

  async function loadData() {
    const token = localStorage.getItem("token");
    if (!token) return;
    setLoading(true);
    setError("");
    const headers = { Authorization: `Bearer ${token}` };
    try {
      const responses = await Promise.all([
        fetch(`${API}/api/jobs`, { headers }),
        fetch(`${API}/api/applications`, { headers }),
        fetch(`${API}/api/recruitment/interviews`, { headers }),
      ]);
      if (responses.some((response) => response.status === 401)) {
        logout("Your session expired. Please log in again.");
        return;
      }
      const [jobResponse, applicationResponse, interviewResponse] = responses;
      if (jobResponse.ok) { const data = await jobResponse.json(); setJobs(Array.isArray(data) ? data : []); }
      else console.error("Jobs API error:", jobResponse.status);
      if (applicationResponse.ok) { const data = await applicationResponse.json(); setApplications(Array.isArray(data) ? data : []); }
      else console.error("Applications API error:", applicationResponse.status);
      if (interviewResponse.ok) { const data = await interviewResponse.json(); setInterviews(Array.isArray(data) ? data : []); }
      else console.error("Interviews API error:", interviewResponse.status);
      if (responses.every((response) => !response.ok)) setError("The backend rejected all requests. Check the API endpoints.");
    } catch (err) {
      console.error("API connection error:", err);
      setError("Cannot connect to the backend. Check your deployed API Gateway URL.");
    } finally { setLoading(false); }
  }

  async function submitLogin(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setAuthBusy(true); setError(""); setAuthMessage("");
    try {
      const response = await fetch(`${API}/api/auth/login`, {
        method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(loginForm),
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(data.error || "Login failed. Check your username and password.");
      if (!data.token) throw new Error("The server did not return a token. Check the login response.");
      localStorage.setItem("token", data.token);
      const loggedInUser: User = { id: data.userId, fullName: data.fullName, username: data.username, email: data.email, role: data.role };
      localStorage.setItem("user", JSON.stringify(loggedInUser));
      setUser(loggedInUser); setPage("Overview"); setAuthMessage("");
    } catch (err) {
      setAuthMessage(err instanceof Error ? err.message : "Unable to log in. Check your API connection.");
    } finally { setAuthBusy(false); }
  }

  async function submitRegister(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setAuthBusy(true); setAuthMessage("");
    try {
      const response = await fetch(`${API}/api/auth/register`, {
        method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(registerForm),
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(data.error || "Registration failed.");
      setAuthMode("login");
      setLoginForm({ username: registerForm.username, password: "" });
      setAuthMessage("Account created successfully. Please log in.");
    } catch (err) {
      setAuthMessage(err instanceof Error ? err.message : "Unable to register. Check your API connection.");
    } finally { setAuthBusy(false); }
  }

  function logout(message = "") {
    localStorage.removeItem("token"); localStorage.removeItem("user");
    setUser(null); setJobs([]); setApplications([]); setInterviews([]); setPage("Overview");
    setError(""); setAuthMessage(message);
  }

  const filteredJobs = jobs.filter((job) => `${job.title} ${job.companyName ?? ""} ${job.location ?? ""}`.toLowerCase().includes(search.toLowerCase()));
  const menu = [
    { name: "Overview", icon: LayoutDashboard },
    { name: "Find jobs", icon: Search },
    { name: "My applications", icon: FileText },
    { name: "Interviews", icon: CalendarDays },
  ];

  if (authChecking) return <div style={centerStyle}><p>Checking your session…</p></div>;

  if (!user) return (
    <div style={authShellStyle}>
      <div style={authCardStyle}>
        <div style={{ display: "flex", alignItems: "center", gap: 10, color: "#2563eb", marginBottom: 8 }}><Sparkles size={30} /><strong style={{ fontSize: 25, color: "#172033" }}>talentsync</strong></div>
        <p style={{ color: "#64748b", marginTop: 0 }}>Your next opportunity starts here.</p>
        <div style={{ display: "flex", background: "#f1f5f9", padding: 4, borderRadius: 12, margin: "22px 0" }}>
          <button type="button" onClick={() => { setAuthMode("login"); setAuthMessage(""); }} style={tabStyle(authMode === "login")}>Login</button>
          <button type="button" onClick={() => { setAuthMode("register"); setAuthMessage(""); }} style={tabStyle(authMode === "register")}>Register</button>
        </div>
        {authMode === "login" ? (
          <form onSubmit={submitLogin} style={formStyle}>
            <label style={labelStyle}>Username<input required autoComplete="username" value={loginForm.username} onChange={(e) => setLoginForm({ ...loginForm, username: e.target.value })} style={inputStyle} placeholder="Enter your username" /></label>
            <label style={labelStyle}>Password<input required type="password" autoComplete="current-password" value={loginForm.password} onChange={(e) => setLoginForm({ ...loginForm, password: e.target.value })} style={inputStyle} placeholder="Enter your password" /></label>
            <button disabled={authBusy} type="submit" style={primaryButtonStyle}>{authBusy ? "Please wait…" : "Login"}</button>
            <p style={{ color: "#64748b", fontSize: 13, margin: "4px 0 0" }}>New to TalentSync? Select Register to create an account.</p>
          </form>
        ) : (
          <form onSubmit={submitRegister} style={formStyle}>
            <label style={labelStyle}>Full name<input required value={registerForm.fullName} onChange={(e) => setRegisterForm({ ...registerForm, fullName: e.target.value })} style={inputStyle} placeholder="Your full name" /></label>
            <label style={labelStyle}>Username<input required autoComplete="username" value={registerForm.username} onChange={(e) => setRegisterForm({ ...registerForm, username: e.target.value })} style={inputStyle} placeholder="Choose a username" /></label>
            <label style={labelStyle}>Email<input type="email" value={registerForm.email} onChange={(e) => setRegisterForm({ ...registerForm, email: e.target.value })} style={inputStyle} placeholder="you@example.com" /></label>
            <label style={labelStyle}>Password<input required minLength={4} type="password" autoComplete="new-password" value={registerForm.password} onChange={(e) => setRegisterForm({ ...registerForm, password: e.target.value })} style={inputStyle} placeholder="At least 4 characters" /></label>
            <label style={labelStyle}>Account type<select value={registerForm.role} onChange={(e) => setRegisterForm({ ...registerForm, role: e.target.value })} style={inputStyle}><option value="CANDIDATE">Candidate</option><option value="RECRUITER">Recruiter</option></select></label>
            <button disabled={authBusy} type="submit" style={primaryButtonStyle}>{authBusy ? "Creating account…" : "Create account"}</button>
          </form>
        )}
        {authMessage && <p role="alert" style={messageStyle}>{authMessage}</p>}
        <p style={{ fontSize: 12, color: "#94a3b8", marginTop: 20 }}>Secure sign-in powered by TalentSync authentication.</p>
      </div>
    </div>
  );

  const displayName = user.fullName || user.username;
  const roleName = (user.role || "CANDIDATE").toLowerCase();
  return (
    <div className="app-shell">
      <aside className="sidebar">
        <h2 className="brand"><Sparkles size={25} /> talentsync</h2>
        <p className="side-label">WORKSPACE</p>
        <div className="profile"><div className="avatar">{displayName.charAt(0).toUpperCase()}</div><div><b>{displayName}</b><small>{roleName} account</small></div></div>
        <p className="side-label">MENU</p>
        {menu.map(({ name, icon: Icon }) => <button key={name} className={`nav-item ${page === name ? "active" : ""}`} onClick={() => setPage(name)}><Icon size={19} /> {name}</button>)}
        <div className="sidebar-note"><Sparkles size={22} /><h4>Keep things moving</h4><p>Your next opportunity could be waiting.</p><button onClick={() => setPage("Find jobs")}>Explore now →</button></div>
        <div className="version">TALENTSYNC · v1.0.0</div>
      </aside>
      <main className="main-area">
        <header className="topbar"><span>Workspace　›　{page}</span><div style={{ display: "flex", alignItems: "center", gap: 10 }}><button className="refresh" onClick={() => void loadData()} title="Refresh data"><RefreshCw size={19} /></button><button type="button" onClick={() => logout()} style={logoutButtonStyle}><LogOut size={16} /> Logout</button></div></header>
        <section className="content">
          <p className="eyebrow">YOUR NEXT CHAPTER STARTS HERE</p>
          <div className="heading-row"><div><h1>{page === "Overview" ? `Good to see you, ${displayName}!` : page}</h1><p>Explore opportunities and keep your career journey moving.</p></div><span className="candidate-badge">● {user.role}</span></div>
          {page === "Overview" && <><section className="hero"><div><p className="hero-label">✧ YOUR TALENT WORKSPACE</p><h2>The right opportunity can change everything.</h2><p>Explore roles that match your ambition and follow every application in one place.</p><button className="hero-button" onClick={() => setPage("Find jobs")}>Explore opportunities ↗</button></div><BriefcaseBusiness className="hero-icon" size={105} /></section><div className="stats"><Stat icon={<BriefcaseBusiness />} label="Available jobs" value={jobs.length} /><Stat icon={<Users />} label="My applications" value={applications.length} /><Stat icon={<CalendarDays />} label="Interviews" value={interviews.length} /><Stat icon={<ShieldCheck />} label="Workspace status" value="Active" /></div><h2 className="section-title">Latest opportunities</h2><p className="muted">Fresh roles from your recruitment workspace</p><JobList jobs={jobs.slice(0, 3)} /><button className="text-button" onClick={() => setPage("Find jobs")}>View all jobs ↗</button></>}
          {page === "Find jobs" && <><div className="search-box"><Search size={19} /><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search job title, company, or location" /></div><JobList jobs={filteredJobs} /></>}
          {page === "My applications" && <div className="panel"><h2>Your applications</h2>{applications.length ? applications.map((app) => <div className="list-row" key={app.id}><FileText /><div><b>Application #{app.id}</b><small>Job ID: {app.jobId ?? "—"}</small></div><span className="status">{app.status ?? "Submitted"}</span></div>) : <Empty text="No applications found yet." />}</div>}
          {page === "Interviews" && <div className="panel"><h2>Upcoming interviews</h2>{interviews.length ? interviews.map((interview) => <div className="list-row" key={interview.id}><CalendarDays /><div><b>Interview #{interview.id}</b><small>{interview.interviewDate ?? "Date not provided"}</small></div><span className="status">{interview.status ?? "Scheduled"}</span></div>) : <Empty text="No interviews scheduled yet." />}</div>}
          {loading && <p className="notice">Loading data…</p>}{error && <p className="error">{error}</p>}
        </section>
      </main>
    </div>
  );
}

function Stat({ icon, label, value }: { icon: ReactNode; label: string; value: number | string }) {
  return <div className="stat-card"><div className="stat-icon">{icon}</div><div><small>{label}</small><h2>{value}</h2></div></div>;
}
function JobList({ jobs }: { jobs: Job[] }) {
  if (!jobs.length) return <Empty text="No matching jobs found." />;
  return <div className="job-list">{jobs.map((job) => <article className="job-card" key={job.id}><div className="job-icon"><BriefcaseBusiness /></div><div className="job-details"><h3>{job.title}</h3><p>{job.companyName ?? "TalentSync Partner"}</p><div className="job-meta"><span><MapPin size={14} /> {job.location ?? "Remote"}</span><span>{job.employmentType ?? "Full-time"}</span></div>{job.description && <p className="description">{job.description}</p>}{job.requiredSkills && <small>Skills: {job.requiredSkills}</small>}</div><span className="status">{job.status ?? "OPEN"}</span></article>)}</div>;
}
function Empty({ text }: { text: string }) { return <div className="empty-state"><BriefcaseBusiness size={32} /><b>Nothing to show yet</b><p>{text}</p></div>; }

const centerStyle: React.CSSProperties = { minHeight: "100vh", display: "grid", placeItems: "center", fontFamily: "Arial, sans-serif", color: "#334155" };
const authShellStyle: React.CSSProperties = { minHeight: "100vh", display: "grid", placeItems: "center", padding: 20, background: "linear-gradient(135deg, #eff6ff 0%, #f8fafc 55%, #eef2ff 100%)", fontFamily: "Inter, Arial, sans-serif" };
const authCardStyle: React.CSSProperties = { width: "100%", maxWidth: 430, boxSizing: "border-box", padding: 30, borderRadius: 20, background: "white", boxShadow: "0 20px 60px rgba(15, 23, 42, .12)" };
const formStyle: React.CSSProperties = { display: "grid", gap: 15 };
const labelStyle: React.CSSProperties = { display: "grid", gap: 7, fontSize: 14, fontWeight: 600, color: "#334155" };
const inputStyle: React.CSSProperties = { width: "100%", boxSizing: "border-box", border: "1px solid #cbd5e1", borderRadius: 10, padding: "12px 13px", fontSize: 14, outlineColor: "#2563eb", background: "white", color: "#0f172a" };
const primaryButtonStyle: React.CSSProperties = { border: 0, borderRadius: 10, padding: "13px 16px", background: "#2563eb", color: "white", fontSize: 15, fontWeight: 700, cursor: "pointer", marginTop: 3 };
const messageStyle: React.CSSProperties = { color: "#b91c1c", background: "#fef2f2", borderRadius: 9, padding: 11, fontSize: 13, lineHeight: 1.5 };
const logoutButtonStyle: React.CSSProperties = { display: "inline-flex", alignItems: "center", gap: 7, border: "1px solid #cbd5e1", borderRadius: 9, padding: "8px 12px", background: "white", color: "#334155", cursor: "pointer" };
function tabStyle(active: boolean): React.CSSProperties { return { flex: 1, border: 0, borderRadius: 9, padding: "10px 12px", fontWeight: 700, cursor: "pointer", background: active ? "white" : "transparent", color: active ? "#1d4ed8" : "#64748b", boxShadow: active ? "0 1px 4px rgba(15,23,42,.1)" : "none" }; }
