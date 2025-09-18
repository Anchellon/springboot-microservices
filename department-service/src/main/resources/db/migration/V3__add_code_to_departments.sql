ALTER TABLE department.departments
    ADD COLUMN code VARCHAR(20);

-- Add unique constraint
ALTER TABLE department.departments
    ADD CONSTRAINT uk_department_code UNIQUE (code);

-- Update existing records with sample codes
UPDATE department.departments SET code = 'ENG' WHERE name = 'Engineering';
UPDATE department.departments SET code = 'HR' WHERE name = 'HR';

-- Make code NOT NULL after setting values
ALTER TABLE department.departments
    ALTER COLUMN code SET NOT NULL;