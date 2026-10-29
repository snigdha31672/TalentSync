import { useEffect, useState } from "react";
import type { ReactNode } from "react";
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
} from "lucide-react";
import "./style.css";

const API = "http://localhost:8080";

type Job = {
  id: number;
  title: string;
  companyName?: string;
  location?: string;
  employmentType?: string;
  description?: string;
  requiredSkills?: string;
  status?: string;
};

type Application = {
  id: number;
  jobId?: number;
  status?: string;
};

type Interview = {
  id: number;
  status?: string;
  interviewDate?: string;
};

export default function App() {
  const [page, setPage] = useState("Overview");
  const [jobs, setJobs] = useState<Job[]>([]);
  const [applications, setApplications] = useState<Application[]>([]);
  const [interviews, setInterviews] = useState<Interview[]>([]);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function loadData() {
    setLoading(true);
    setError("");

    const token = localStorage.getItem("token");

    if (!token) {
      setError("Please log in first. No authentication token was found.");
      setLoading(false);
      return;
    }

    const headers = {
      Authorization: `Bearer ${token}`,
    };

    try {
      const responses = await Promise.all([
        fetch(`${API}/api/jobs`, { headers }),
        fetch(`${API}/api/applications`, { headers }),
        fetch(`${API}/api/recruitment/interviews`, { headers }),
      ]);

      const [jobResponse, applicationResponse, interviewResponse] = responses;

      if (responses.some((response) => response.status === 401)) {
        setError("Authentication failed. Please log in again.");
        return;
      }

      if (jobResponse.ok) {
        const data = await jobResponse.json();
        setJobs(Array.isArray(data) ? data : []);
      } else {
        console.error("Jobs API error:", jobResponse.status);
      }

      if (applicationResponse.ok) {
        const data = await applicationResponse.json();
        setApplications(Array.isArray(data) ? data : []);
      } else {
        console.error("Applications API error:", applicationResponse.status);
      }

      if (interviewResponse.ok) {
        const data = await interviewResponse.json();
        setInterviews(Array.isArray(data) ? data : []);
      } else {
        console.error("Interviews API error:", interviewResponse.status);
      }

      if (responses.every((response) => !response.ok)) {
        setError("The backend rejected all requests. Check the API endpoints.");
      }
    } catch (err) {
      console.error("API connection error:", err);
      setError("Cannot connect to the backend. Check that the API Gateway is running.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadData();
  }, []);

  const filteredJobs = jobs.filter((job) =>
    `${job.title} ${job.companyName ?? ""} ${job.location ?? ""}`
      .toLowerCase()
      .includes(search.toLowerCase())
  );

  const menu = [
    { name: "Overview", icon: LayoutDashboard },
    { name: "Find jobs", icon: Search },
    { name: "My applications", icon: FileText },
    { name: "Interviews", icon: CalendarDays },
  ];

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <h2 className="brand">
          <Sparkles size={25} /> talentsync
        </h2>

        <p className="side-label">WORKSPACE</p>

        <div className="profile">
          <div className="avatar">S</div>
          <div>
            <b>Candidate</b>
            <small>Candidate account</small>
          </div>
        </div>

        <p className="side-label">MENU</p>

        {menu.map(({ name, icon: Icon }) => (
          <button
            key={name}
            className={`nav-item ${page === name ? "active" : ""}`}
            onClick={() => setPage(name)}
          >
            <Icon size={19} /> {name}
          </button>
        ))}

        <div className="sidebar-note">
          <Sparkles size={22} />
          <h4>Keep things moving</h4>
          <p>Your next opportunity could be waiting.</p>
          <button onClick={() => setPage("Find jobs")}>
            Explore now →
          </button>
        </div>

        <div className="version">TALENTSYNC · v1.0.0</div>
      </aside>

      <main className="main-area">
        <header className="topbar">
          <span>Workspace　›　{page}</span>
          <button className="refresh" onClick={loadData} title="Refresh data">
            <RefreshCw size={19} />
          </button>
        </header>

        <section className="content">
          <p className="eyebrow">YOUR NEXT CHAPTER STARTS HERE</p>

          <div className="heading-row">
            <div>
              <h1>{page === "Overview" ? "Good to see you!" : page}</h1>
              <p>Explore opportunities and keep your career journey moving.</p>
            </div>
            <span className="candidate-badge">● Candidate</span>
          </div>

          {page === "Overview" && (
            <>
              <section className="hero">
                <div>
                  <p className="hero-label">✧ YOUR TALENT WORKSPACE</p>
                  <h2>The right opportunity can change everything.</h2>
                  <p>
                    Explore roles that match your ambition and follow every
                    application in one place.
                  </p>
                  <button
                    className="hero-button"
                    onClick={() => setPage("Find jobs")}
                  >
                    Explore opportunities ↗
                  </button>
                </div>
                <BriefcaseBusiness className="hero-icon" size={105} />
              </section>

              <div className="stats">
                <Stat
                  icon={<BriefcaseBusiness />}
                  label="Available jobs"
                  value={jobs.length}
                />
                <Stat
                  icon={<Users />}
                  label="My applications"
                  value={applications.length}
                />
                <Stat
                  icon={<CalendarDays />}
                  label="Interviews"
                  value={interviews.length}
                />
                <Stat
                  icon={<ShieldCheck />}
                  label="Workspace status"
                  value="Active"
                />
              </div>

              <h2 className="section-title">Latest opportunities</h2>
              <p className="muted">Fresh roles from your recruitment workspace</p>
              <JobList jobs={jobs.slice(0, 3)} />
              <button
                className="text-button"
                onClick={() => setPage("Find jobs")}
              >
                View all jobs ↗
              </button>
            </>
          )}

          {page === "Find jobs" && (
            <>
              <div className="search-box">
                <Search size={19} />
                <input
                  value={search}
                  onChange={(event) => setSearch(event.target.value)}
                  placeholder="Search job title, company, or location"
                />
              </div>
              <JobList jobs={filteredJobs} />
            </>
          )}

          {page === "My applications" && (
            <div className="panel">
              <h2>Your applications</h2>
              {applications.length ? (
                applications.map((app) => (
                  <div className="list-row" key={app.id}>
                    <FileText />
                    <div>
                      <b>Application #{app.id}</b>
                      <small>Job ID: {app.jobId ?? "—"}</small>
                    </div>
                    <span className="status">{app.status ?? "Submitted"}</span>
                  </div>
                ))
              ) : (
                <Empty text="No applications found yet." />
              )}
            </div>
          )}

          {page === "Interviews" && (
            <div className="panel">
              <h2>Upcoming interviews</h2>
              {interviews.length ? (
                interviews.map((interview) => (
                  <div className="list-row" key={interview.id}>
                    <CalendarDays />
                    <div>
                      <b>Interview #{interview.id}</b>
                      <small>
                        {interview.interviewDate ?? "Date not provided"}
                      </small>
                    </div>
                    <span className="status">
                      {interview.status ?? "Scheduled"}
                    </span>
                  </div>
                ))
              ) : (
                <Empty text="No interviews scheduled yet." />
              )}
            </div>
          )}

          {loading && <p className="notice">Loading data…</p>}
          {error && <p className="error">{error}</p>}
        </section>
      </main>
    </div>
  );
}

function Stat({
  icon,
  label,
  value,
}: {
  icon: ReactNode;
  label: string;
  value: number | string;
}) {
  return (
    <div className="stat-card">
      <div className="stat-icon">{icon}</div>
      <div>
        <small>{label}</small>
        <h2>{value}</h2>
      </div>
    </div>
  );
}

function JobList({ jobs }: { jobs: Job[] }) {
  if (!jobs.length) {
    return <Empty text="No matching jobs found." />;
  }

  return (
    <div className="job-list">
      {jobs.map((job) => (
        <article className="job-card" key={job.id}>
          <div className="job-icon">
            <BriefcaseBusiness />
          </div>
          <div className="job-details">
            <h3>{job.title}</h3>
            <p>{job.companyName ?? "TalentSync Partner"}</p>
            <div className="job-meta">
              <span>
                <MapPin size={14} /> {job.location ?? "Remote"}
              </span>
              <span>{job.employmentType ?? "Full-time"}</span>
            </div>
            {job.description && (
              <p className="description">{job.description}</p>
            )}
            {job.requiredSkills && <small>Skills: {job.requiredSkills}</small>}
          </div>
          <span className="status">{job.status ?? "OPEN"}</span>
        </article>
      ))}
    </div>
  );
}

function Empty({ text }: { text: string }) {
  return (
    <div className="empty-state">
      <BriefcaseBusiness size={32} />
      <b>Nothing to show yet</b>
      <p>{text}</p>
    </div>
  );
}

