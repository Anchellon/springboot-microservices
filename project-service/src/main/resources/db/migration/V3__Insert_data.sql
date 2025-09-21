-- V3__Insert_dummy_data.sql
-- Migration to insert dummy data for testing

-- Insert sample projects
INSERT INTO project.projects (code, name, description, status, start_date, end_date) VALUES
                                                                                 ('EMP-MGMT', 'Employee Management System', 'Complete overhaul of HR management system with modern UI and enhanced features', 'ACTIVE', '2024-01-15', '2024-12-31'),
                                                                                 ('MOBILE-APP', 'Customer Mobile App', 'Native mobile application for iOS and Android platforms', 'ACTIVE', '2024-03-01', '2024-10-15'),
                                                                                 ('DATA-MIGRATION', 'Legacy Data Migration', 'Migrate customer data from old CRM system to new cloud-based solution', 'COMPLETED', '2023-11-01', '2024-02-28'),
                                                                                 ('API-GATEWAY', 'Microservices API Gateway', 'Centralized API gateway for all microservices communication', 'PLANNED', '2024-06-01', '2024-11-30'),
                                                                                 ('SECURITY-AUDIT', 'Security Compliance Audit', 'Complete security assessment and implementation of compliance measures', 'ON_HOLD', '2024-02-01', NULL),
                                                                                 ('E-COMMERCE', 'E-commerce Platform Redesign', 'Redesign of main e-commerce platform with better performance', 'ACTIVE', '2024-04-01', '2024-09-30'),
                                                                                 ('ANALYTICS-DASHBOARD', 'Business Analytics Dashboard', 'Real-time business intelligence and analytics dashboard', 'PLANNED', '2024-07-01', '2025-01-31'),
                                                                                 ('CLOUD-MIGRATION', 'Cloud Infrastructure Migration', 'Migration of on-premise infrastructure to AWS cloud', 'CANCELLED', '2024-01-01', NULL);

-- Insert project members (using employee IDs from your data: 1=Alice, 2=Bob, 3=Carla)
INSERT INTO project.project_members (project_id, employee_id, role, allocation_percent) VALUES
-- EMP-MGMT project (project_id = 1)
(1, 1, 'Project Manager', 80),
(1, 2, 'Senior Developer', 90),
(1, 3, 'UI/UX Designer', 75),

-- MOBILE-APP project (project_id = 2)
(2, 2, 'Lead Developer', 100),
(2, 3, 'Mobile Developer', 85),

-- DATA-MIGRATION project (project_id = 3) - completed
(3, 1, 'Technical Lead', 60),
(3, 2, 'Data Engineer', 70),

-- API-GATEWAY project (project_id = 4) - planned
(4, 1, 'Solution Architect', 50),
(4, 2, 'Backend Developer', 80),

-- SECURITY-AUDIT project (project_id = 5) - on hold
(5, 1, 'Security Consultant', 40),
(5, 3, 'Compliance Analyst', 60),

-- E-COMMERCE project (project_id = 6)
(6, 2, 'Full Stack Developer', 95),
(6, 3, 'Frontend Developer', 90),

-- ANALYTICS-DASHBOARD project (project_id = 7) - planned
(7, 1, 'Business Analyst', 70),
(7, 3, 'Data Visualization Specialist', 80);

-- Note: CLOUD-MIGRATION project (project_id = 8) has no members as it was cancelled