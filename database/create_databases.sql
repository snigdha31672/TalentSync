-- TalentSync Microservices Database Creation Script
-- Run this as superuser 'postgres' (e.g. in pgAdmin 4 Query Tool or via psql)

-- 1. Create separate databases for each business microservice
CREATE DATABASE auth_db;
CREATE DATABASE job_db;
CREATE DATABASE application_db;
CREATE DATABASE recruitment_db;

-- Connect to auth_db and create schema
\c auth_db;

CREATE TABLE IF NOT EXISTS app_user (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(150),
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Connect to job_db and create schema
\c job_db;

CREATE TABLE IF NOT EXISTS job (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    company_name VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    required_skills TEXT NOT NULL,
    location VARCHAR(150) NOT NULL,
    employment_type VARCHAR(50) NOT NULL,
    salary_range VARCHAR(100),
    experience_required VARCHAR(50),
    posted_date DATE DEFAULT CURRENT_DATE,
    application_deadline DATE,
    status VARCHAR(20) DEFAULT 'OPEN',
    recruiter_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Connect to application_db and create schema
\c application_db;

CREATE TABLE IF NOT EXISTS application (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL,
    candidate_id VARCHAR(100) NOT NULL,
    candidate_name VARCHAR(150),
    candidate_email VARCHAR(150),
    resume_url VARCHAR(500) NOT NULL,
    cover_letter TEXT,
    applied_date DATE DEFAULT CURRENT_DATE,
    status VARCHAR(50) DEFAULT 'SUBMITTED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_candidate_job UNIQUE (candidate_id, job_id)
);

-- Connect to recruitment_db and create schema
\c recruitment_db;

CREATE TABLE IF NOT EXISTS recruitment (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL,
    job_id BIGINT,
    job_title VARCHAR(200),
    candidate_id VARCHAR(100),
    candidate_name VARCHAR(150),
    interview_date_time VARCHAR(100),
    interview_mode VARCHAR(50) DEFAULT 'ONLINE',
    interview_location VARCHAR(500),
    interviewer VARCHAR(150),
    interview_status VARCHAR(50) DEFAULT 'SCHEDULED',
    feedback TEXT,
    final_status VARCHAR(50) DEFAULT 'PENDING',
    scheduled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_application_recruitment UNIQUE (application_id)
);
