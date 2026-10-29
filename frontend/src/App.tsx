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
type Job = { id: number; title: string; companyName?: string; location?: string; employmentType?: string; description?: string; requiredSkills?: string; salaryRange?: string; experienceRequired?: string; status?: string };
type Application = { id: number; jobId?: number; status?: string; candidateId?: string; candidateName?: string; candidateEmail?: string; resumeUrl?: string; coverLetter?: string };
type Interview = { id: number; applicationId?: number; jobTitle?: string; candidateName?: string; candidateId?: string; interviewStatus?: string; interviewDateTime?: string; interviewMode?: string; interviewLocation?: string; interviewer?: string; finalStatus?: string };

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
  const [jobForm, setJobForm] = useState({ title: "", companyName: "", location: "Remote", employmentType: "FULL_TIME", description: "", requiredSkills: "", salaryRange: "", experienceRequired: "Fresher", applicationDeadline: "" });
  const [applyJob, setApplyJob] = useState<Job | null>(null);
  const [editingJobId, setEditingJobId] = useState<number | null>(null);
  const [applyForm, setApplyForm] = useState({ candidateEmail: "", resumeUrl: "", coverLetter: "" });
  const [interviewForm, setInterviewForm] = useState({ applicationId: "", interviewDateTime: "", interviewMode: "ONLINE", interviewLocation: "", interviewer: "" });
  const [actionMessage, setActionMessage] = useState("");

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
        fetch(`${API}/api/recruitment${user?.role?.toUpperCase() === "CANDIDATE" ? `?candidateId=${encodeURIComponent(user.username)}` : ""}`, { headers }),
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

  async function apiAction(path: string, init: RequestInit = {}) {
    const token = localStorage.getItem("token");
    const response = await fetch(`${API}${path}`, { ...init, headers: { "Content-Type": "application/json", ...(token ? { Authorization: `Bearer ${token}` } : {}), ...(init.headers || {}) } });
    const data = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(data.error || `Request failed (${response.status})`);
    return data;
  }
  async function submitJob(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setActionMessage("");
    try { await apiAction(editingJobId ? `/api/jobs/${editingJobId}` : "/api/jobs", { method: editingJobId ? "PUT" : "POST", body: JSON.stringify({ ...jobForm, status: "OPEN", applicationDeadline: jobForm.applicationDeadline || null }) }); setActionMessage(editingJobId ? "Vacancy updated successfully." : "Vacancy published successfully."); setEditingJobId(null); setJobForm({ title: "", companyName: "", location: "Remote", employmentType: "FULL_TIME", description: "", requiredSkills: "", salaryRange: "", experienceRequired: "Fresher", applicationDeadline: "" }); await loadData(); }
    catch (e) { setActionMessage(e instanceof Error ? e.message : "Could not publish vacancy."); }
  }
  async function submitApplication(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); if (!applyJob) return; setActionMessage("");
    try { await apiAction("/api/applications", { method: "POST", body: JSON.stringify({ jobId: applyJob.id, candidateName: user?.fullName || user?.username, candidateEmail: applyForm.candidateEmail, resumeUrl: applyForm.resumeUrl, coverLetter: applyForm.coverLetter }) }); setActionMessage("Application submitted successfully."); setApplyJob(null); setApplyForm({candidateEmail:"",resumeUrl:"",coverLetter:""}); await loadData(); }
    catch (e) { setActionMessage(e instanceof Error ? e.message : "Could not submit application."); }
  }
  async function updateApplicationStatus(app: Application, status: string) {
    try { await apiAction(`/api/applications/${app.id}/status`, { method: "PATCH", body: JSON.stringify({status}) }); setActionMessage(`Application #${app.id} updated to ${status}.`); await loadData(); }
    catch (e) { setActionMessage(e instanceof Error ? e.message : "Could not update application."); }
  }
  async function updateJobStatus(job: Job, status: string) {
    try { await apiAction(`/api/jobs/${job.id}/status`, { method: "PATCH", body: JSON.stringify({status}) }); setActionMessage(`${job.title} marked ${status.toLowerCase()}.`); await loadData(); }
    catch (e) { setActionMessage(e instanceof Error ? e.message : "Could not update vacancy."); }
  }
  async function submitInterview(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setActionMessage("");
    const app = applications.find(a => a.id === Number(interviewForm.applicationId));
    const job = jobs.find(j => j.id === app?.jobId);
    try { await apiAction("/api/recruitment", { method: "POST", body: JSON.stringify({ applicationId: Number(interviewForm.applicationId), jobId: app?.jobId, jobTitle: job?.title, candidateId: app?.candidateId, candidateName: app?.candidateName, interviewDateTime: interviewForm.interviewDateTime, interviewMode: interviewForm.interviewMode, interviewLocation: interviewForm.interviewLocation, interviewer: interviewForm.interviewer }) }); setActionMessage("Interview scheduled successfully."); setInterviewForm({applicationId:"",interviewDateTime:"",interviewMode:"ONLINE",interviewLocation:"",interviewer:""}); await loadData(); }
    catch (e) { setActionMessage(e instanceof Error ? e.message : "Could not schedule interview."); }
  }
  async function updateInterview(interview: Interview, action: string, value: string) {
    try { await apiAction(`/api/recruitment/${interview.id}/${action}`, { method: "PATCH", body: JSON.stringify(action === "interview-status" ? {interviewStatus:value} : {finalStatus:value}) }); setActionMessage("Interview updated."); await loadData(); }
    catch (e) { setActionMessage(e instanceof Error ? e.message : "Could not update interview."); }
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
  const isRecruiter = user.role?.toUpperCase() === "RECRUITER";
  const menu = [
    { name: "Overview", icon: LayoutDashboard },
    ...(isRecruiter ? [
      { name: "Manage jobs", icon: BriefcaseBusiness },
      { name: "Candidates", icon: Users },
      { name: "Interviews", icon: CalendarDays },
    ] : [
      { name: "Find jobs", icon: Search },
      { name: "My applications", icon: FileText },
      { name: "Interviews", icon: CalendarDays },
    ]),
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
          {page === "Overview" && <><section className="hero"><div><p className="hero-label">✧ YOUR TALENT WORKSPACE</p><h2>The right opportunity can change everything.</h2><p>Explore roles that match your ambition and follow every application in one place.</p><button className="hero-button" onClick={() => setPage("Find jobs")}>Explore opportunities ↗</button></div><BriefcaseBusiness className="hero-icon" size={105} /></section><div className="stats"><Stat icon={<BriefcaseBusiness />} label="Available jobs" value={jobs.length} /><Stat icon={<Users />} label="My applications" value={applications.length} /><Stat icon={<CalendarDays />} label="Interviews" value={interviews.length} /><Stat icon={<ShieldCheck />} label="Workspace status" value="Active" /></div><h2 className="section-title">Latest opportunities</h2><p className="muted">Fresh roles from your recruitment workspace</p><JobList jobs={jobs.filter((job) => (job.status ?? "OPEN").toUpperCase() === "OPEN").slice(0, 3)} /><button className="text-button" onClick={() => setPage("Find jobs")}>View all jobs ↗</button></>}
          {page === "Find jobs" && <><div className="search-box"><Search size={19} /><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search job title, company, or location" /></div><div className="job-list">{filteredJobs.filter((job) => (job.status ?? "OPEN").toUpperCase() === "OPEN").map((job) => <article className="job-card" key={job.id}><div className="job-icon"><BriefcaseBusiness /></div><div className="job-details"><h3>{job.title}</h3><p>{job.companyName ?? "TalentSync Partner"}</p><div className="job-meta"><span><MapPin size={14} /> {job.location ?? "Remote"}</span><span>{job.employmentType ?? "Full-time"}</span></div><p className="description">{job.description}</p>{job.requiredSkills && <small>Skills: {job.requiredSkills}</small>}</div><button className="hero-button" onClick={() => { setApplyJob(job); setActionMessage(""); }}>Apply</button></article>)}</div>{applyJob && <div className="panel"><h2>Apply for {applyJob.title}</h2><form style={formStyle} onSubmit={(e) => void submitApplication(e)}><label style={labelStyle}>Email<input type="email" required value={applyForm.candidateEmail} onChange={(e) => setApplyForm({...applyForm,candidateEmail:e.target.value})} style={inputStyle}/></label><label style={labelStyle}>Resume URL<input type="url" required placeholder="https://..." value={applyForm.resumeUrl} onChange={(e) => setApplyForm({...applyForm,resumeUrl:e.target.value})} style={inputStyle}/></label><label style={labelStyle}>Cover letter<textarea value={applyForm.coverLetter} onChange={(e) => setApplyForm({...applyForm,coverLetter:e.target.value})} style={inputStyle}/></label><div><button className="hero-button" type="submit">Submit application</button> <button type="button" className="text-button" onClick={() => setApplyJob(null)}>Cancel</button></div></form></div>}</>}
          {page === "My applications" && <div className="panel"><h2>Your applications</h2>{applications.length ? applications.map((app) => <div className="list-row" key={app.id}><FileText /><div><b>Application #{app.id}</b><small>Job ID: {app.jobId ?? "—"}</small></div><span className="status">{app.status ?? "Submitted"}</span></div>) : <Empty text="No applications found yet." />}</div>}
          {page === "Manage jobs" && <div className="panel"><h2>{editingJobId ? "Edit vacancy" : "Post a vacancy"}</h2><form style={formStyle} onSubmit={(e) => void submitJob(e)}><div className="form-grid"><label style={labelStyle}>Job title<input required value={jobForm.title} onChange={e=>setJobForm({...jobForm,title:e.target.value})} style={inputStyle}/></label><label style={labelStyle}>Company<input required value={jobForm.companyName} onChange={e=>setJobForm({...jobForm,companyName:e.target.value})} style={inputStyle}/></label><label style={labelStyle}>Location<input required value={jobForm.location} onChange={e=>setJobForm({...jobForm,location:e.target.value})} style={inputStyle}/></label><label style={labelStyle}>Employment type<select value={jobForm.employmentType} onChange={e=>setJobForm({...jobForm,employmentType:e.target.value})} style={inputStyle}><option value="FULL_TIME">Full-time</option><option value="PART_TIME">Part-time</option><option value="INTERNSHIP">Internship</option><option value="CONTRACT">Contract</option></select></label><label style={labelStyle}>Salary range<input value={jobForm.salaryRange} onChange={e=>setJobForm({...jobForm,salaryRange:e.target.value})} style={inputStyle}/></label><label style={labelStyle}>Experience required<input value={jobForm.experienceRequired} onChange={e=>setJobForm({...jobForm,experienceRequired:e.target.value})} style={inputStyle}/></label><label style={labelStyle}>Application deadline<input type="date" value={jobForm.applicationDeadline} onChange={e=>setJobForm({...jobForm,applicationDeadline:e.target.value})} style={inputStyle}/></label></div><label style={labelStyle}>Description<textarea required value={jobForm.description} onChange={e=>setJobForm({...jobForm,description:e.target.value})} style={inputStyle}/></label><label style={labelStyle}>Required skills<input value={jobForm.requiredSkills} onChange={e=>setJobForm({...jobForm,requiredSkills:e.target.value})} style={inputStyle}/></label><button className="hero-button" type="submit">{editingJobId ? "Save changes" : "Publish vacancy"}</button>{editingJobId && <button type="button" className="text-button" onClick={()=>{setEditingJobId(null);setJobForm({title:"",companyName:"",location:"Remote",employmentType:"FULL_TIME",description:"",requiredSkills:"",salaryRange:"",experienceRequired:"Fresher",applicationDeadline:""});}}>Cancel edit</button>}</form><h2 style={{marginTop:28}}>Your vacancies</h2>{jobs.filter(j=>j.companyName !== "TalentSync Demo Partner").map(job=><div className="list-row" key={job.id}><BriefcaseBusiness/><div><b>{job.title}</b><small>{job.companyName} · {job.location} · {job.status}</small></div><div className="action-stack"><button className="text-button" onClick={()=>{setEditingJobId(job.id);setJobForm({title:job.title,companyName:job.companyName||"",location:job.location||"Remote",employmentType:job.employmentType||"FULL_TIME",description:job.description||"",requiredSkills:job.requiredSkills||"",salaryRange:job.salaryRange||"",experienceRequired:job.experienceRequired||"",applicationDeadline:""});window.scrollTo({top:0,behavior:"smooth"});}}>Edit</button><button className="text-button" onClick={()=>void updateJobStatus(job,"CLOSED")}>Close vacancy</button></div></div>)}</div>}
          {page === "Candidates" && <div className="panel"><h2>Candidate applications</h2>{applications.length ? applications.map(app=><div className="list-row candidate-row" key={app.id}><Users/><div><b>{app.candidateName || app.candidateId || "Candidate"} · Application #{app.id}</b><small>Job ID: {app.jobId ?? "—"} · {app.candidateEmail || "Email not provided"}</small>{app.resumeUrl && <a href={app.resumeUrl} target="_blank" rel="noreferrer">View resume</a>}{app.coverLetter && <p>{app.coverLetter}</p>}<small>Status: {app.status}</small></div><div className="action-stack"><button className="text-button" onClick={()=>void updateApplicationStatus(app,"SHORTLISTED")}>Shortlist</button><button className="text-button" onClick={()=>void updateApplicationStatus(app,"REJECTED")}>Reject</button><button className="text-button" onClick={()=>{setInterviewForm({...interviewForm,applicationId:String(app.id)});setPage("Interviews");}}>Schedule interview</button></div></div>) : <Empty text="Candidate applications will appear here." />}</div>}
          {page === "Interviews" && <div className="panel"><h2>{isRecruiter ? "Schedule and manage interviews" : "Upcoming interviews"}</h2>{isRecruiter && <form style={formStyle} onSubmit={(e)=>void submitInterview(e)}><div className="form-grid"><label style={labelStyle}>Application ID<input required type="number" min="1" value={interviewForm.applicationId} onChange={e=>setInterviewForm({...interviewForm,applicationId:e.target.value})} style={inputStyle}/></label><label style={labelStyle}>Date and time<input required type="datetime-local" value={interviewForm.interviewDateTime} onChange={e=>setInterviewForm({...interviewForm,interviewDateTime:e.target.value})} style={inputStyle}/></label><label style={labelStyle}>Mode<select value={interviewForm.interviewMode} onChange={e=>setInterviewForm({...interviewForm,interviewMode:e.target.value})} style={inputStyle}><option value="ONLINE">Online</option><option value="IN_PERSON">In person</option><option value="PHONE">Phone</option></select></label><label style={labelStyle}>Meeting link / location<input required value={interviewForm.interviewLocation} onChange={e=>setInterviewForm({...interviewForm,interviewLocation:e.target.value})} style={inputStyle}/></label><label style={labelStyle}>Interviewer<input required value={interviewForm.interviewer} onChange={e=>setInterviewForm({...interviewForm,interviewer:e.target.value})} style={inputStyle}/></label></div><button className="hero-button" type="submit">Schedule interview</button></form>}{interviews.length ? interviews.map(interview => <div className="list-row" key={interview.id}><CalendarDays/><div><b>{interview.jobTitle || "Interview"} · {interview.candidateName || "Candidate"} (Application #{interview.applicationId})</b><small>{interview.interviewDateTime || "Date not provided"} · {interview.interviewMode || "ONLINE"} · {interview.interviewLocation || ""}</small><small>Status: {interview.interviewStatus || "SCHEDULED"} · Result: {interview.finalStatus || "PENDING"}</small></div>{isRecruiter && <div className="action-stack"><button className="text-button" onClick={()=>void updateInterview(interview,"interview-status","COMPLETED")}>Mark completed</button><button className="text-button" onClick={()=>void updateInterview(interview,"final-status","SELECTED")}>Select</button><button className="text-button" onClick={()=>void updateInterview(interview,"final-status","REJECTED")}>Reject</button></div>}</div>) : <Empty text="No interviews scheduled yet." />}</div>}
          {actionMessage && <p role="status" className="notice">{actionMessage}</p>}
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
