-- V1__Create_projects_table.sql
-- Migration to create the projects table

CREATE TABLE project.projects (
                          id BIGSERIAL PRIMARY KEY,
                          code VARCHAR(20) NOT NULL,
                          name VARCHAR(120) NOT NULL,
                          description VARCHAR(2000),
                          status VARCHAR(20) NOT NULL,
                          start_date DATE NOT NULL,
                          end_date DATE
);

-- Create unique constraint on code (case-insensitive)
CREATE UNIQUE INDEX uk_projects_code ON project.projects (UPPER(code));

-- Create check constraint for status enum values
ALTER TABLE project.projects ADD CONSTRAINT ck_projects_status
    CHECK (status IN ('PLANNED', 'ACTIVE', 'ON_HOLD', 'COMPLETED', 'CANCELLED'));

-- Create check constraint to ensure end_date >= start_date when end_date is not null
ALTER TABLE project.projects ADD CONSTRAINT ck_projects_end_date_after_start_date
    CHECK (end_date IS NULL OR end_date >= start_date);

-- Create check constraint for code pattern (uppercase letters, digits, and hyphens)
ALTER TABLE project.projects ADD CONSTRAINT ck_projects_code_pattern
    CHECK (code ~ '^[A-Z0-9-]+$');

-- Create check constraint for code length
ALTER TABLE project.projects ADD CONSTRAINT ck_projects_code_length
    CHECK (LENGTH(code) >= 3 AND LENGTH(code) <= 20);

-- Create check constraint for name length
ALTER TABLE project.projects ADD CONSTRAINT ck_projects_name_length
    CHECK (LENGTH(name) >= 3 AND LENGTH(name) <= 120);

-- Create check constraint for description length
ALTER TABLE project.projects ADD CONSTRAINT ck_projects_description_length
    CHECK (description IS NULL OR LENGTH(description) <= 2000);

-- Create indexes for better query performance
CREATE INDEX idx_projects_status ON project.projects (status);
CREATE INDEX idx_projects_start_date ON project.projects (start_date);
CREATE INDEX idx_projects_end_date ON project.projects (end_date);