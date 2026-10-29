
# Kongu Engineering College (Autonomous) - Regulation 2024 (R2024)
## Student Academic & Continuous Evaluation System using Java JDBC

---

## 📌 Project Overview
The **KEC R2024 Student Academic & Continuous Evaluation System** is a robust, database-driven Java application engineered using JDBC (Java Database Connectivity) and MySQL. It manages student academic lifecycles, tracks continuous course attendance, facilitates phased/incremental marks entry ("Then and Now"), enforces attendance eligibility rules, and computes SGPA and academic standing according to **Kongu Engineering College (Autonomous) Regulation 2024**.

---

## 🏗️ Project Architecture & Directory Structure

```text
KEC academic/
├── sql/
│   └── kec_schema.sql         # Complete MySQL DDL schema, constraints, triggers & seed data
├── src/
│   ├── DBConnection.java      # JDBC connection manager (singleton & credentials handling)
│   ├── StudentDAO.java        # CRUD operations for student registration & course catalog
│   ├── AcademicDAO.java       # Core assessment engine, attendance filters, SGPA & reporting
│   └── MainApp.java           # Interactive menu-driven console user interface
├── lib/
│   └── mysql-connector-j-26.10.0.jar  # MySQL JDBC Connector Driver JAR
├── bin/                       # Compiled Java bytecode (.class files)
└── README.md                  # Comprehensive documentation, setup guide & execution proofs
```

---

## ⚙️ KEC Regulation 2024 (R2024) Specifications

### 1. Course Classification & Assessment Weightage Engine
| Course Type | CIA Weightage | ESE Weightage | ESE Minimum Threshold | Overall Pass Minimum |
| :--- | :---: | :---: | :---: | :---: |
| **THEORY** | 40% (0.40) | 60% (0.60) | **45% (27/60)** | **50 Marks** |
| **PRACTICAL** | 60% (0.60) | 40% (0.40) | **50% (20/40)** | **50 Marks** |
| **EMBEDDED** | 50% (0.50) | 50% (0.50) | **45% (22.5/50)** | **50 Marks** |

### 2. Attendance Eligibility & Defaulter Rules
* **Attendance $\ge$ 75%**: Fully eligible to appear for End Semester Examinations (ESE).
* **65% $\le$ Attendance < 75%**: Conditionally eligible. Requires formal Medical Condonation approval.
  * If condonation is **approved**: Eligible for ESE.
  * If condonation is **not approved**: Barred from ESE $\rightarrow$ Awarded **SA** grade.
* **Attendance < 65%**: Shortage of Attendance (**SA**). Barred from ESE; awarded **SA** grade (0 Grade Points).

### 3. KEC R2024 10-Point Grading System
| Range of Marks | Letter Grade | Grade Point | Performance Description |
| :---: | :---: | :---: | :--- |
| **91 – 100** | **O** | **10** | Outstanding |
| **81 – 90** | **A+** | **9** | Excellent |
| **71 – 80** | **A** | **8** | Very Good |
| **61 – 70** | **B+** | **7** | Good |
| **50 – 60** | **B** | **6** | Average (Pass Minimum) |
| **< 50 or ESE < Threshold** | **RA** | **0** | Re-Appearance (Failed) |
| **Attendance < 65% / No Condonation** | **SA** | **0** | Shortage of Attendance (Barred) |
| **Absent in ESE** | **AB** | **0** | Absent |

### 4. SGPA & Percentage Formulas
* **SGPA (Semester Grade Point Average)**:
  $$\text{SGPA} = \frac{\sum (\text{Course Credits} \times \text{Grade Points Earned})}{\sum \text{Total Enrolled Credits}}$$
* **Equivalent Percentage**:
  $$\text{Equivalent Percentage} = \text{SGPA} \times 10$$

---

## 🗄️ Database Setup (`kec_schema.sql`)

### Prerequisites:
* MySQL Server 8.0+ running on port `3306`
* Java Development Kit (JDK 17+)

### Database Initialization:
Log in to MySQL and load the schema script located at `sql/kec_schema.sql`:

```powershell
# Using Command Prompt or PowerShell:
cmd /c "mysql -u root -pMysql@12345 < sql\kec_schema.sql"
```

### Table Definitions:
1. **`students`**: Stores Roll Number, Name, Department, and Current Semester.
2. **`courses`**: Stores Course Code, Title, Credits (1–10), and Course Type (`THEORY`, `PRACTICAL`, `EMBEDDED`).
3. **`enrollments`**: Manages course enrollment, semester, classes held, classes attended, and medical condonation approval.
4. **`assessment_marks`**: Manages phased assessment records (`cia1`, `cia2`, `assignment_mark`, `quiz_mark`, `lab_mark`, `ese_mark`, and `ese_absent`).

---

## 🚀 Compilation & Execution Instructions

### 1. Compile Java Source Code
Run `javac` linking the JDBC driver JAR:
```powershell
javac -cp ".;lib\mysql-connector-j-26.10.0.jar" -d bin src\*.java
```

### 2. Run the Application
Execute `MainApp` with the classpath pointing to compiled classes and the driver:
```powershell
java -cp "bin;lib\mysql-connector-j-26.10.0.jar" MainApp
```

---

## 🖥️ Execution Proofs & Walkthrough

### Proof 1: Main Menu & Application Startup
```text
==========================================================================================
            KONGU ENGINEERING COLLEGE (AUTONOMOUS) - REGULATION 2024 (R2024)              
                 Student Academic & Continuous Evaluation System (JDBC)                   
==========================================================================================
[SUCCESS] Connected to MySQL database (kec_r2024) successfully.

------------------------------------------------------------------------------------------
                                      MAIN MENU                                           
------------------------------------------------------------------------------------------
  1. Student Registration (Add New Student)
  2. Course Catalog (Add New Course: Theory / Practical / Embedded)
  3. Course Enrollment & Attendance Tracking
  4. Update Attendance & Record Medical Condonation Approval
  5. Phased / Incremental Marks Entry ('Then and Now': CIA1, CIA2, Lab, ESE, AB)
  6. Generate Student Semester Mark Sheet (Formal Grade Report & SGPA)
  7. Generate Attendance Defaulter Report (Grade SA Flagging)
  8. Generate Class Performance & Result Summary (Dept Pass/Fail & Class Avg SGPA)
  9. View Registered Students & Course Catalog
  0. Exit System
------------------------------------------------------------------------------------------
```

---

### Proof 2: Student Registration & Course Addition (Menu Options 1 & 2)

#### Adding a Student:
```text
--- [1] STUDENT REGISTRATION ---
Enter Roll Number (e.g. 24ITR001): 24ECR101
Enter Student Full Name: Karthik R
Enter Department (CSE, IT, ECE, MECH, etc.): ECE
Enter Current Semester (1-8): 3
[SUCCESS] Student registered successfully: 24ECR101 - Karthik R
```

#### Adding a Course:
```text
--- [2] COURSE CATALOG ENTRY ---
Enter Course Code (e.g. 24ITT31): 24ECT32
Enter Course Title: Digital Signal Processing
Enter Course Credits (1-10): 4
Select Course Type:
  1. THEORY (CIA: 40%, ESE: 60%, ESE Min: 45%)
  2. PRACTICAL (CIA: 60%, ESE: 40%, ESE Min: 50%)
  3. EMBEDDED (CIA: 50%, ESE: 50%, ESE Min: 45%)
Choose Course Type (1-3): 1
[SUCCESS] Course added successfully: [24ECT32] Digital Signal Processing (THEORY, 4 Credits)
```

---

### Proof 3: Incremental Marks Entry ("Then and Now") (Menu Option 5)

The system displays previously recorded scores (**Then**) before recording new assessment scores (**Now**):

#### Phase 1: Mid-Semester CIA (CIA 1 & CIA 2)
```text
------------------------------------------------------------------------------------------
                 INCREMENTAL MARKS ENTRY - 'THEN AND NOW' PHASES                          
------------------------------------------------------------------------------------------
  1. Phase 1 (Mid-Semester)      : Enter CIA Test 1 (0-100) & CIA Test 2 (0-100)
  2. Phase 2 (End of Semester)   : Enter Assignments / Quizzes / Lab Evaluations (0-100)
  3. Phase 3 (Post Semester)     : Enter ESE Marks (0-100) or Mark Absence (AB)
  4. Comprehensive / Bulk Entry  : Update Any / All Assessment Components
  0. Back to Main Menu
------------------------------------------------------------------------------------------
Select Marks Entry Option: 1

>>> PHASE 1 (MID-SEMESTER): CIA TEST 1 & CIA TEST 2
Enter Roll Number: 24ITR001
Enter Course Code: 24ITT31
Enter Semester: 3

[THEN - Current Recorded Assessment Marks]
  CIA Test 1   : 88.00
  CIA Test 2   : 92.00
  Assignment   : 90.00
  Quiz         : 86.00
  Lab Eval     : --
  ESE Mark     : 88.00

[NOW - Enter New/Updated Phase 1 Marks (Leave blank to keep existing)]
Enter CIA Test 1 Mark (0-100): 90.0
Enter CIA Test 2 Mark (0-100): 94.0
[SUCCESS] Phase 1 Marks successfully recorded for 24ITR001 in 24ITT31.
```

---

### Proof 4: Generated Student Semester Mark Sheet with Calculated SGPA (Menu Option 6)

```text
--- [6] STUDENT SEMESTER MARK SHEET ---
Enter Student Roll Number (e.g. 24ITR001): 24ITR001
Enter Semester (1-8): 3

==========================================================================================================
                         KONGU ENGINEERING COLLEGE (AUTONOMOUS), PERUNDURAI                               
                              REGULATION 2024 - GRADE REPORT / MARK SHEET                                 
==========================================================================================================
  Roll Number : 24ITR001           Student Name : Swathi M                  Semester : 3
  Department  : IT                 Current Sem  : Sem 3                       Academic Year: 2024-2025
----------------------------------------------------------------------------------------------------------
| Code     | Course Title                 | Type | Attend %  | CIA/100 | CIA-Wt  | ESE-Wt  | Total  | Grade  | GP | Pass |
----------------------------------------------------------------------------------------------------------
| 24ITE31  | Object Oriented Programmi... | EMBE |   96.7%  |    88.8 |    44.4 |    43.0 |   87.4 | A+    |  9 | PASS |
| 24ITP31  | Database Systems Laboratory  | PRAC |   96.7%  |    94.3 |    56.6 |    36.8 |   93.4 | O     | 10 | PASS |
| 24ITT31  | Database Design and Manag... | THEO |   96.0%  |    89.0 |    35.6 |    52.8 |   88.4 | A+    |  9 | PASS |
| 24ITT32  | Data Structures and Algor... | THEO |   93.3%  |    94.0 |    37.6 |    57.0 |   94.6 | O     | 10 | PASS |
----------------------------------------------------------------------------------------------------------

--- ACADEMIC PERFORMANCE SUMMARY ---
  Total Enrolled Credits : 13
  Total Earned Credits   : 13
  Total Quality Points   : 123.00
  Semester GPA (SGPA)    : 9.46 / 10.00
  Equivalent Percentage  : 94.62%
  Final Semester Status  : PASSED - FIRST CLASS WITH DISTINCTION
==========================================================================================================
```

---

### Proof 5: Attendance Defaulter Report (Menu Option 7)

```text
==========================================================================================================
                           KEC R2024 - ATTENDANCE DEFAULTER REPORT (GRADE SA)                             
                  Criteria: Attendance < 65% OR 65-75% without Medical Condonation                        
==========================================================================================================
| Roll No    | Student Name         | Dept  | Sem  | Course    | Held   | Attd   | Attend % | Barred Grade |
----------------------------------------------------------------------------------------------------------
| 24ITR003   | Priya S              | IT    | Sem3 | 24ITT31   | 50     | 28     |  56.00%  | SA (< 65%)   |
| 24ITR003   | Priya S              | IT    | Sem3 | 24ITT32   | 60     | 34     |  56.67%  | SA (< 65%)   |
| 24ITR003   | Priya S              | IT    | Sem3 | 24ITP31   | 30     | 18     |  60.00%  | SA (< 65%)   |
| 24ITR003   | Priya S              | IT    | Sem3 | 24ITE31   | 60     | 32     |  53.33%  | SA (< 65%)   |
==========================================================================================================
```

---

### Proof 6: Department-wise Class Performance & Result Summary (Menu Option 8)

```text
==========================================================================================================
                 KEC R2024 - CLASS PERFORMANCE & RESULT SUMMARY (SEMESTER 3)                         
==========================================================================================================
| Department   | Total Students | All Passed   | Arrears/SA   | Pass Pct (%) | Class Avg SGPA | Top SGPA       |
----------------------------------------------------------------------------------------------------------
| CSE          | 1              | 0            | 1            |       0.00% |           0.00 | 0.00 (      ) |
| IT           | 3              | 2            | 1            |      66.67% |           5.64 | 9.46 (24ITR001) |
==========================================================================================================
```

---

## 🔒 Security & Code Quality Highlights
* **SQL Injection Prevention**: All queries utilize JDBC `PreparedStatement` with parameterized inputs.
* **ACID Transactions & Resource Management**: Automatic resource closing using Java `try-with-resources`.
* **Data Integrity**: Database constraints (`CHECK`, `FOREIGN KEY` cascades, `UNIQUE` pairs) ensure data validity.
* **Clean Code Architecture**: Separation of concerns between connection layer (`DBConnection`), data access layer (`StudentDAO`, `AcademicDAO`), and presentation layer (`MainApp`).
