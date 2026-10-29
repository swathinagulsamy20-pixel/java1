-- ============================================================================
-- Kongu Engineering College (Autonomous) - Regulation 2024 (R2024)
-- Student Academic & Continuous Evaluation System Database Schema
-- Database: kec_r2024
-- ============================================================================

CREATE DATABASE IF NOT EXISTS kec_r2024
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE kec_r2024;

-- 1. Students Table
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS assessment_marks;
DROP TABLE IF EXISTS attendance;
DROP TABLE IF EXISTS enrollments;
DROP TABLE IF EXISTS courses;
DROP TABLE IF EXISTS students;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE students (
    student_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    roll_no VARCHAR(20) NOT NULL UNIQUE,
    student_name VARCHAR(100) NOT NULL,
    department VARCHAR(20) NOT NULL,
    current_semester TINYINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_student_sem CHECK (current_semester BETWEEN 1 AND 8)
) ENGINE=InnoDB;

-- 2. Courses Table
CREATE TABLE courses (
    course_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_code VARCHAR(20) NOT NULL UNIQUE,
    course_title VARCHAR(150) NOT NULL,
    credits TINYINT NOT NULL,
    course_type ENUM('THEORY', 'PRACTICAL', 'EMBEDDED') NOT NULL,
    CONSTRAINT chk_course_credits CHECK (credits BETWEEN 1 AND 10)
) ENGINE=InnoDB;

-- 3. Enrollments Table with Attendance Tracking
CREATE TABLE enrollments (
    enrollment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    semester TINYINT NOT NULL,
    classes_held INT NOT NULL DEFAULT 0,
    classes_attended INT NOT NULL DEFAULT 0,
    condonation_approved BOOLEAN NOT NULL DEFAULT FALSE,
    enrolled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_enrollment_student FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE,
    CONSTRAINT fk_enrollment_course FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE,
    CONSTRAINT uq_student_course_sem UNIQUE (student_id, course_id, semester),
    CONSTRAINT chk_enrollment_sem CHECK (semester BETWEEN 1 AND 8),
    CONSTRAINT chk_classes CHECK (classes_held >= 0 AND classes_attended >= 0 AND classes_attended <= classes_held)
) ENGINE=InnoDB;

-- 4. Assessment Marks Table for Incremental ("Then and Now") Assessments
CREATE TABLE assessment_marks (
    mark_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    enrollment_id BIGINT NOT NULL UNIQUE,
    cia1 DECIMAL(5,2) NULL,
    cia2 DECIMAL(5,2) NULL,
    assignment_mark DECIMAL(5,2) NULL,
    quiz_mark DECIMAL(5,2) NULL,
    lab_mark DECIMAL(5,2) NULL,
    ese_mark DECIMAL(5,2) NULL,
    ese_absent BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_marks_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments(enrollment_id) ON DELETE CASCADE,
    CONSTRAINT chk_cia1 CHECK (cia1 IS NULL OR (cia1 >= 0 AND cia1 <= 100)),
    CONSTRAINT chk_cia2 CHECK (cia2 IS NULL OR (cia2 >= 0 AND cia2 <= 100)),
    CONSTRAINT chk_assignment CHECK (assignment_mark IS NULL OR (assignment_mark >= 0 AND assignment_mark <= 100)),
    CONSTRAINT chk_quiz CHECK (quiz_mark IS NULL OR (quiz_mark >= 0 AND quiz_mark <= 100)),
    CONSTRAINT chk_lab CHECK (lab_mark IS NULL OR (lab_mark >= 0 AND lab_mark <= 100)),
    CONSTRAINT chk_ese CHECK (ese_mark IS NULL OR (ese_mark >= 0 AND ese_mark <= 100)),
    CONSTRAINT chk_absent_ese CHECK (ese_absent = FALSE OR ese_mark IS NULL)
) ENGINE=InnoDB;

-- ============================================================================
-- Seed Sample Data for Demonstration & Verification
-- ============================================================================

-- Insert Students
INSERT INTO students (roll_no, student_name, department, current_semester) VALUES
('24ITR001', 'Swathi M', 'IT', 3),
('24ITR002', 'Dharun Kumar R', 'IT', 3),
('24ITR003', 'Priya S', 'IT', 3),
('24CSR010', 'Arun V', 'CSE', 3),
('24CSR011', 'Kavitha P', 'CSE', 3),
('24ECR025', 'Naveen Raj K', 'ECE', 3);

-- Insert Courses covering THEORY, PRACTICAL, and EMBEDDED
INSERT INTO courses (course_code, course_title, credits, course_type) VALUES
('24ITT31', 'Database Design and Management', 3, 'THEORY'),
('24ITT32', 'Data Structures and Algorithms', 4, 'THEORY'),
('24ITP31', 'Database Systems Laboratory', 2, 'PRACTICAL'),
('24ITE31', 'Object Oriented Programming with Java', 4, 'EMBEDDED'),
('24CST31', 'Discrete Mathematics', 4, 'THEORY'),
('24ECT31', 'Signals and Systems', 4, 'THEORY');

-- Enroll Student 1: 24ITR001 in Semester 3 (All Courses, High Attendance >= 75%)
INSERT INTO enrollments (student_id, course_id, semester, classes_held, classes_attended, condonation_approved) VALUES
(1, 1, 3, 50, 48, FALSE), -- 96.0% -> Eligible
(1, 2, 3, 60, 56, FALSE), -- 93.3% -> Eligible
(1, 3, 3, 30, 29, FALSE), -- 96.7% -> Eligible
(1, 4, 3, 60, 58, FALSE); -- 96.7% -> Eligible

-- Enroll Student 2: 24ITR002 (Condonation Case: 70% with Condonation Approved)
INSERT INTO enrollments (student_id, course_id, semester, classes_held, classes_attended, condonation_approved) VALUES
(2, 1, 3, 50, 35, TRUE),  -- 70.0% -> Condonation Approved -> Eligible
(2, 2, 3, 60, 43, TRUE),  -- 71.7% -> Condonation Approved -> Eligible
(2, 3, 3, 30, 24, TRUE),  -- 80.0% -> Eligible
(2, 4, 3, 60, 44, TRUE);  -- 73.3% -> Condonation Approved -> Eligible

-- Enroll Student 3: 24ITR003 (Shortage of Attendance Defaulter: < 65% -> SA)
INSERT INTO enrollments (student_id, course_id, semester, classes_held, classes_attended, condonation_approved) VALUES
(3, 1, 3, 50, 28, FALSE), -- 56.0% -> SA Defaulter
(3, 2, 3, 60, 34, FALSE), -- 56.7% -> SA Defaulter
(3, 3, 3, 30, 18, FALSE), -- 60.0% -> SA Defaulter
(3, 4, 3, 60, 32, FALSE); -- 53.3% -> SA Defaulter

-- Enroll CSE Student 4: 24CSR010 in Sem 3
INSERT INTO enrollments (student_id, course_id, semester, classes_held, classes_attended, condonation_approved) VALUES
(4, 5, 3, 60, 55, FALSE);

-- Marks for Student 1 (24ITR001): Phased Complete Marks Entry (Exemplary Student)
-- 24ITT31 (Theory): CIA1=88, CIA2=92, Assign=90, Quiz=86 -> CIA Avg=89.00 -> 40% = 35.60; ESE=88 -> 60% = 52.80; Total=88.40 -> Grade A+
-- 24ITT32 (Theory): CIA1=94, CIA2=96, Assign=92, Quiz=94 -> CIA Avg=94.00 -> 40% = 37.60; ESE=95 -> 60% = 57.00; Total=94.60 -> Grade O
-- 24ITP31 (Practical): CIA1=90, CIA2=95, Lab=98 -> CIA Avg=94.33 -> 60% = 56.60; ESE=92 -> 40% = 36.80; Total=93.40 -> Grade O
-- 24ITE31 (Embedded): CIA1=85, CIA2=90, Assign=88, Lab=92 -> CIA Avg=88.75 -> 50% = 44.38; ESE=86 -> 50% = 43.00; Total=87.38 -> Grade A+
INSERT INTO assessment_marks (enrollment_id, cia1, cia2, assignment_mark, quiz_mark, lab_mark, ese_mark, ese_absent) VALUES
(1, 88.0, 92.0, 90.0, 86.0, NULL, 88.0, FALSE),
(2, 94.0, 96.0, 92.0, 94.0, NULL, 95.0, FALSE),
(3, 90.0, 95.0, NULL, NULL, 98.0, 92.0, FALSE),
(4, 85.0, 90.0, 88.0, NULL, 92.0, 86.0, FALSE);

-- Marks for Student 2 (24ITR002): Moderate marks, passes with B+ and A
INSERT INTO assessment_marks (enrollment_id, cia1, cia2, assignment_mark, quiz_mark, lab_mark, ese_mark, ese_absent) VALUES
(5, 65.0, 70.0, 68.0, 72.0, NULL, 64.0, FALSE),
(6, 72.0, 75.0, 70.0, 74.0, NULL, 70.0, FALSE),
(7, 80.0, 82.0, NULL, NULL, 85.0, 78.0, FALSE),
(8, 68.0, 72.0, 70.0, NULL, 75.0, 66.0, FALSE);

-- Marks for Student 3 (24ITR003): Attendance < 65% -> Barred from ESE (SA)
INSERT INTO assessment_marks (enrollment_id, cia1, cia2, assignment_mark, quiz_mark, lab_mark, ese_mark, ese_absent) VALUES
(9,  45.0, 50.0, 48.0, 52.0, NULL, NULL, FALSE),
(10, 40.0, 42.0, 50.0, 45.0, NULL, NULL, FALSE),
(11, 55.0, 60.0, NULL, NULL, 58.0, NULL, FALSE),
(12, 48.0, 50.0, 52.0, NULL, 55.0, NULL, FALSE);
