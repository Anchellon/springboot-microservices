-- V2__Create_project_members_table.sql
-- Migration to create the project_members table

CREATE TABLE project.project_members (
                                 id BIGSERIAL PRIMARY KEY,
                                 project_id BIGINT NOT NULL,
                                 employee_id BIGINT NOT NULL,
                                 role VARCHAR(60) NOT NULL,
                                 allocation_percent INTEGER NOT NULL,
                                 assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create foreign key constraint to projects table
ALTER TABLE project.project_members ADD CONSTRAINT fk_project_members_project_id
    FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE;

-- Create unique constraint for (project_id, employee_id) combination
ALTER TABLE project.project_members ADD CONSTRAINT uk_project_employee
    UNIQUE (project_id, employee_id);

-- Create check constraint for allocation_percent (0-100 inclusive)
ALTER TABLE project.project_members ADD CONSTRAINT ck_project_members_allocation_percent
    CHECK (allocation_percent >= 0 AND allocation_percent <= 100);

-- Create check constraint for role length
ALTER TABLE project.project_members ADD CONSTRAINT ck_project_members_role_length
    CHECK (LENGTH(role) >= 2 AND LENGTH(role) <= 60);

-- Create indexes for better query performance
CREATE INDEX idx_project_members_project_id ON project.project_members (project_id);
CREATE INDEX idx_project_members_employee_id ON project.project_members (employee_id);
CREATE INDEX idx_project_members_role ON project.project_members (role);
CREATE INDEX idx_project_members_assigned_at ON project.project_members (assigned_at);