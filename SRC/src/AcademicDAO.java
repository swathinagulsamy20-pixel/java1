import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AcademicDAO handles course enrollment, attendance tracking, phased assessment marks entry,
 * KEC R2024 evaluation rules & weightage engine, SGPA computation, and reporting.
 */
public class AcademicDAO {

    /**
     * Data Transfer Object representing complete course evaluation details.
     */
    public static class CourseEvaluation {
        public String courseCode;
        public String courseTitle;
        public String courseType;
        public int credits;
        public int classesHeld;
        public int classesAttended;
        public boolean condonationApproved;
        public double attendancePct;

        public Double cia1;
        public Double cia2;
        public Double assignment;
        public Double quiz;
        public Double lab;
        public double ciaRaw;
        public double ciaWeighted;

        public Double eseRaw;
        public double eseWeighted;
        public boolean eseAbsent;
        public boolean eseProvided;

        public double totalMark;
        public String grade;
        public int gradePoint;
        public String status;
        public String remarks;
    }

    /**
     * Enrolls a student in a course with initial attendance.
     */
    public void enroll(String rollNo, String courseCode, int semester,
                       int classesHeld, int classesAttended, boolean condonationApproved) throws SQLException {
        validateAttendance(classesHeld, classesAttended);
        String sql = "INSERT INTO enrollments (student_id, course_id, semester, classes_held, classes_attended, condonation_approved) " +
                     "SELECT s.student_id, c.course_id, ?, ?, ?, ? " +
                     "FROM students s CROSS JOIN courses c " +
                     "WHERE s.roll_no = ? AND c.course_code = ? " +
                     "ON DUPLICATE KEY UPDATE classes_held = VALUES(classes_held), " +
                     "classes_attended = VALUES(classes_attended), condonation_approved = VALUES(condonation_approved)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, semester);
            ps.setInt(2, classesHeld);
            ps.setInt(3, classesAttended);
            ps.setBoolean(4, condonationApproved);
            ps.setString(5, rollNo.trim().toUpperCase());
            ps.setString(6, courseCode.trim().toUpperCase());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Student Roll Number (" + rollNo + ") or Course Code (" + courseCode + ") not found.");
            }
        }
    }

    /**
     * Updates attendance and condonation for an existing enrollment.
     */
    public void updateAttendance(String rollNo, String courseCode, int semester,
                                 int classesHeld, int classesAttended, boolean condonationApproved) throws SQLException {
        validateAttendance(classesHeld, classesAttended);
        String sql = "UPDATE enrollments e " +
                     "JOIN students s ON e.student_id = s.student_id " +
                     "JOIN courses c ON e.course_id = c.course_id " +
                     "SET e.classes_held = ?, e.classes_attended = ?, e.condonation_approved = ? " +
                     "WHERE s.roll_no = ? AND c.course_code = ? AND e.semester = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, classesHeld);
            ps.setInt(2, classesAttended);
            ps.setBoolean(3, condonationApproved);
            ps.setString(4, rollNo.trim().toUpperCase());
            ps.setString(5, courseCode.trim().toUpperCase());
            ps.setInt(6, semester);

            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new SQLException("Enrollment not found for Roll No: " + rollNo + ", Course: " + courseCode + ", Sem: " + semester);
            }
        }
    }

    /**
     * Retrieves the enrollment ID for a student course semester record.
     */
    public long getEnrollmentId(String rollNo, String courseCode, int semester) throws SQLException {
        String sql = "SELECT e.enrollment_id FROM enrollments e " +
                     "JOIN students s ON e.student_id = s.student_id " +
                     "JOIN courses c ON e.course_id = c.course_id " +
                     "WHERE s.roll_no = ? AND c.course_code = ? AND e.semester = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, rollNo.trim().toUpperCase());
            ps.setString(2, courseCode.trim().toUpperCase());
            ps.setInt(3, semester);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("enrollment_id");
                } else {
                    throw new SQLException("No enrollment record found for " + rollNo + " in course " + courseCode + " (Sem " + semester + ").");
                }
            }
        }
    }

    /**
     * Displays existing marks ("Then") for a student course assessment.
     */
    public void displayExistingMarks(String rollNo, String courseCode, int semester) throws SQLException {
        long enrollmentId = getEnrollmentId(rollNo, courseCode, semester);
        String sql = "SELECT cia1, cia2, assignment_mark, quiz_mark, lab_mark, ese_mark, ese_absent " +
                     "FROM assessment_marks WHERE enrollment_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, enrollmentId);
            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("\n[THEN - Current Recorded Assessment Marks]");
                if (rs.next()) {
                    System.out.printf("  CIA Test 1   : %s%n", formatNullable(rs.getObject("cia1")));
                    System.out.printf("  CIA Test 2   : %s%n", formatNullable(rs.getObject("cia2")));
                    System.out.printf("  Assignment   : %s%n", formatNullable(rs.getObject("assignment_mark")));
                    System.out.printf("  Quiz         : %s%n", formatNullable(rs.getObject("quiz_mark")));
                    System.out.printf("  Lab Eval     : %s%n", formatNullable(rs.getObject("lab_mark")));
                    System.out.printf("  ESE Mark     : %s%n", rs.getBoolean("ese_absent") ? "ABSENT (AB)" : formatNullable(rs.getObject("ese_mark")));
                } else {
                    System.out.println("  No marks entered yet (Fresh assessment record).");
                }
            }
        }
    }

    /**
     * Phase 1 Incremental Entry: CIA Test 1 and CIA Test 2.
     */
    public void savePhase1Marks(String rollNo, String courseCode, int semester,
                                Double cia1, Double cia2) throws SQLException {
        validateMark(cia1, "CIA Test 1");
        validateMark(cia2, "CIA Test 2");
        long enrollmentId = getEnrollmentId(rollNo, courseCode, semester);

        String sql = "INSERT INTO assessment_marks (enrollment_id, cia1, cia2) " +
                     "VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE " +
                     "cia1 = COALESCE(VALUES(cia1), cia1), " +
                     "cia2 = COALESCE(VALUES(cia2), cia2)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, enrollmentId);
            setNullable(ps, 2, cia1);
            setNullable(ps, 3, cia2);
            ps.executeUpdate();
        }
    }

    /**
     * Phase 2 Incremental Entry: Assignment, Quiz, and Lab Evaluations.
     */
    public void savePhase2Marks(String rollNo, String courseCode, int semester,
                                Double assignment, Double quiz, Double lab) throws SQLException {
        validateMark(assignment, "Assignment Mark");
        validateMark(quiz, "Quiz Mark");
        validateMark(lab, "Lab Evaluation");
        long enrollmentId = getEnrollmentId(rollNo, courseCode, semester);

        String sql = "INSERT INTO assessment_marks (enrollment_id, assignment_mark, quiz_mark, lab_mark) " +
                     "VALUES (?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE " +
                     "assignment_mark = COALESCE(VALUES(assignment_mark), assignment_mark), " +
                     "quiz_mark = COALESCE(VALUES(quiz_mark), quiz_mark), " +
                     "lab_mark = COALESCE(VALUES(lab_mark), lab_mark)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, enrollmentId);
            setNullable(ps, 2, assignment);
            setNullable(ps, 3, quiz);
            setNullable(ps, 4, lab);
            ps.executeUpdate();
        }
    }

    /**
     * Phase 3 Incremental Entry: End Semester Examination (ESE) Marks or Mark Absence (AB).
     */
    public void savePhase3Marks(String rollNo, String courseCode, int semester,
                                Double ese, boolean isAbsent) throws SQLException {
        if (isAbsent && ese != null) {
            throw new IllegalArgumentException("When student is marked Absent (AB), ESE mark should be empty.");
        }
        if (!isAbsent && ese != null) {
            validateMark(ese, "ESE Mark");
        }
        long enrollmentId = getEnrollmentId(rollNo, courseCode, semester);

        String sql = "INSERT INTO assessment_marks (enrollment_id, ese_mark, ese_absent) " +
                     "VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE ese_mark = VALUES(ese_mark), ese_absent = VALUES(ese_absent)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, enrollmentId);
            setNullable(ps, 2, ese);
            ps.setBoolean(3, isAbsent);
            ps.executeUpdate();
        }
    }

    /**
     * Full / Comprehensive Marks Entry (updates any or all components).
     */
    public void saveComprehensiveMarks(String rollNo, String courseCode, int semester,
                                       Double cia1, Double cia2, Double assignment, Double quiz,
                                       Double lab, Double ese, boolean eseProvided, boolean isAbsent) throws SQLException {
        validateMark(cia1, "CIA Test 1");
        validateMark(cia2, "CIA Test 2");
        validateMark(assignment, "Assignment Mark");
        validateMark(quiz, "Quiz Mark");
        validateMark(lab, "Lab Evaluation");
        if (isAbsent && ese != null) {
            throw new IllegalArgumentException("Cannot provide ESE marks when absent.");
        }
        if (!isAbsent && ese != null) {
            validateMark(ese, "ESE Mark");
        }
        long enrollmentId = getEnrollmentId(rollNo, courseCode, semester);

        String sql = "INSERT INTO assessment_marks (enrollment_id, cia1, cia2, assignment_mark, quiz_mark, lab_mark, ese_mark, ese_absent) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE " +
                     "cia1 = COALESCE(VALUES(cia1), cia1), " +
                     "cia2 = COALESCE(VALUES(cia2), cia2), " +
                     "assignment_mark = COALESCE(VALUES(assignment_mark), assignment_mark), " +
                     "quiz_mark = COALESCE(VALUES(quiz_mark), quiz_mark), " +
                     "lab_mark = COALESCE(VALUES(lab_mark), lab_mark), " +
                     "ese_mark = IF(?, VALUES(ese_mark), ese_mark), " +
                     "ese_absent = IF(?, ?, ese_absent)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, enrollmentId);
            setNullable(ps, 2, cia1);
            setNullable(ps, 3, cia2);
            setNullable(ps, 4, assignment);
            setNullable(ps, 5, quiz);
            setNullable(ps, 6, lab);
            setNullable(ps, 7, ese);
            ps.setBoolean(8, isAbsent);
            ps.setBoolean(9, eseProvided);
            ps.setBoolean(10, eseProvided);
            ps.setBoolean(11, isAbsent);
            ps.executeUpdate();
        }
    }

    /**
     * Core KEC R2024 Evaluation Engine for a single course enrollment record.
     */
    public CourseEvaluation evaluateCourse(ResultSet rs) throws SQLException {
        CourseEvaluation eval = new CourseEvaluation();
        eval.courseCode = rs.getString("course_code");
        eval.courseTitle = rs.getString("course_title");
        eval.courseType = rs.getString("course_type");
        eval.credits = rs.getInt("credits");
        eval.classesHeld = rs.getInt("classes_held");
        eval.classesAttended = rs.getInt("classes_attended");
        eval.condonationApproved = rs.getBoolean("condonation_approved");

        eval.attendancePct = (eval.classesHeld == 0) ? 100.0 : ((double) eval.classesAttended / eval.classesHeld) * 100.0;

        eval.cia1 = getNullableDouble(rs, "cia1");
        eval.cia2 = getNullableDouble(rs, "cia2");
        eval.assignment = getNullableDouble(rs, "assignment_mark");
        eval.quiz = getNullableDouble(rs, "quiz_mark");
        eval.lab = getNullableDouble(rs, "lab_mark");

        eval.eseProvided = (rs.getObject("ese_mark") != null || rs.getBoolean("ese_absent"));
        eval.eseAbsent = rs.getBoolean("ese_absent");
        eval.eseRaw = getNullableDouble(rs, "ese_mark");

        // CIA Raw Calculation: Average of available CIA items
        List<Double> ciaComponents = new ArrayList<>();
        if (eval.cia1 != null) ciaComponents.add(eval.cia1);
        if (eval.cia2 != null) ciaComponents.add(eval.cia2);
        if (eval.assignment != null) ciaComponents.add(eval.assignment);
        if (eval.quiz != null) ciaComponents.add(eval.quiz);
        if (eval.lab != null) ciaComponents.add(eval.lab);

        eval.ciaRaw = ciaComponents.isEmpty() ? 0.0 :
                ciaComponents.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        // KEC R2024 Weightages and ESE Minimums
        double ciaWeight;
        double eseWeight;
        double eseMin;

        switch (eval.courseType) {
            case "THEORY" -> {
                ciaWeight = 0.40;
                eseWeight = 0.60;
                eseMin = 45.0; // 45% minimum threshold in ESE
            }
            case "PRACTICAL" -> {
                ciaWeight = 0.60;
                eseWeight = 0.40;
                eseMin = 50.0; // 50% minimum threshold in ESE
            }
            case "EMBEDDED" -> {
                ciaWeight = 0.50;
                eseWeight = 0.50;
                eseMin = 45.0; // 45% minimum threshold in ESE
            }
            default -> {
                ciaWeight = 0.40;
                eseWeight = 0.60;
                eseMin = 45.0;
            }
        }

        eval.ciaWeighted = eval.ciaRaw * ciaWeight;

        // Attendance & Eligibility Rules
        boolean barredDueToAttendance = false;
        if (eval.attendancePct < 65.0) {
            barredDueToAttendance = true;
            eval.remarks = "SA: Attendance < 65% (Barred from ESE)";
        } else if (eval.attendancePct < 75.0 && !eval.condonationApproved) {
            barredDueToAttendance = true;
            eval.remarks = "SA: Attendance 65-75% without Condonation";
        } else if (eval.attendancePct < 75.0 && eval.condonationApproved) {
            eval.remarks = "Conditionally Eligible (Medical Condonation Approved)";
        } else {
            eval.remarks = "Eligible for ESE (Attendance >= 75%)";
        }

        if (barredDueToAttendance) {
            eval.grade = "SA";
            eval.gradePoint = 0;
            eval.status = "FAIL (SA)";
            eval.totalMark = 0.0;
            eval.eseWeighted = 0.0;
            return eval;
        }

        if (eval.eseAbsent) {
            eval.grade = "AB";
            eval.gradePoint = 0;
            eval.status = "ABSENT";
            eval.totalMark = eval.ciaWeighted;
            eval.eseWeighted = 0.0;
            return eval;
        }

        if (!eval.eseProvided || eval.eseRaw == null) {
            eval.grade = "PENDING";
            eval.gradePoint = 0;
            eval.status = "PENDING";
            eval.totalMark = eval.ciaWeighted;
            eval.eseWeighted = 0.0;
            return eval;
        }

        // When ESE is present
        eval.eseWeighted = eval.eseRaw * eseWeight;
        eval.totalMark = Math.round((eval.ciaWeighted + eval.eseWeighted) * 100.0) / 100.0;

        // ESE Threshold and Aggregate Pass Check
        if (eval.eseRaw < eseMin) {
            eval.grade = "RA";
            eval.gradePoint = 0;
            eval.status = "FAIL (RA)";
            eval.remarks += " | ESE < " + eseMin + "% threshold";
        } else if (eval.totalMark < 50.0) {
            eval.grade = "RA";
            eval.gradePoint = 0;
            eval.status = "FAIL (RA)";
            eval.remarks += " | Aggregate < 50 pass minimum";
        } else {
            // 10-Point Grading System (KEC R2024)
            eval.status = "PASS";
            if (eval.totalMark >= 91.0) {
                eval.grade = "O";
                eval.gradePoint = 10;
            } else if (eval.totalMark >= 81.0) {
                eval.grade = "A+";
                eval.gradePoint = 9;
            } else if (eval.totalMark >= 71.0) {
                eval.grade = "A";
                eval.gradePoint = 8;
            } else if (eval.totalMark >= 61.0) {
                eval.grade = "B+";
                eval.gradePoint = 7;
            } else {
                eval.grade = "B";
                eval.gradePoint = 6;
            }
        }

        return eval;
    }

    /**
     * REPORT 1: Generates and prints the Student Semester Mark Sheet with SGPA.
     */
    public void printStudentMarkSheet(String rollNo, int semester) throws SQLException {
        String studentSql = "SELECT roll_no, student_name, department, current_semester FROM students WHERE roll_no = ?";
        String coursesSql = "SELECT c.course_code, c.course_title, c.credits, c.course_type, " +
                            "e.classes_held, e.classes_attended, e.condonation_approved, " +
                            "m.cia1, m.cia2, m.assignment_mark, m.quiz_mark, m.lab_mark, m.ese_mark, m.ese_absent " +
                            "FROM enrollments e " +
                            "JOIN students s ON e.student_id = s.student_id " +
                            "JOIN courses c ON e.course_id = c.course_id " +
                            "LEFT JOIN assessment_marks m ON e.enrollment_id = m.enrollment_id " +
                            "WHERE s.roll_no = ? AND e.semester = ? " +
                            "ORDER BY c.course_code";

        try (Connection con = DBConnection.getConnection()) {
            String studentName = "";
            String dept = "";
            int currSem = 0;

            try (PreparedStatement ps = con.prepareStatement(studentSql)) {
                ps.setString(1, rollNo.trim().toUpperCase());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        studentName = rs.getString("student_name");
                        dept = rs.getString("department");
                        currSem = rs.getInt("current_semester");
                    } else {
                        System.out.println("\n[ERROR] Student with Roll Number '" + rollNo + "' not found.");
                        return;
                    }
                }
            }

            List<CourseEvaluation> evaluations = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement(coursesSql)) {
                ps.setString(1, rollNo.trim().toUpperCase());
                ps.setInt(2, semester);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        evaluations.add(evaluateCourse(rs));
                    }
                }
            }

            if (evaluations.isEmpty()) {
                System.out.println("\n[INFO] No enrolled courses found for Roll Number: " + rollNo + " in Semester: " + semester);
                return;
            }

            // Print Header
            System.out.println("\n==========================================================================================================");
            System.out.println("                         KONGU ENGINEERING COLLEGE (AUTONOMOUS), PERUNDURAI                               ");
            System.out.println("                              REGULATION 2024 - GRADE REPORT / MARK SHEET                                 ");
            System.out.println("==========================================================================================================");
            System.out.printf("  Roll Number : %-18s Student Name : %-25s Semester : %d%n", rollNo.toUpperCase(), studentName, semester);
            System.out.printf("  Department  : %-18s Current Sem  : Sem %-23d Academic Year: 2024-2025%n", dept, currSem);
            System.out.println("----------------------------------------------------------------------------------------------------------");
            System.out.printf("| %-8s | %-28s | %-4s | %-9s | %-6s | %-7s | %-7s | %-6s | %-6s | %-2s | %-4s |%n",
                    "Code", "Course Title", "Type", "Attend %", "CIA/100", "CIA-Wt", "ESE-Wt", "Total", "Grade", "GP", "Pass");
            System.out.println("----------------------------------------------------------------------------------------------------------");

            double totalCreditsEnrolled = 0;
            double totalCreditsEarned = 0;
            double totalQualityPoints = 0;
            boolean hasPending = false;
            boolean hasArrears = false;

            for (CourseEvaluation e : evaluations) {
                totalCreditsEnrolled += e.credits;

                String eseDisplay = e.eseAbsent ? "AB" : (e.eseProvided && e.eseRaw != null ? String.format("%.1f", e.eseWeighted) : "--");
                String totalDisplay = (e.grade.equals("SA") || e.grade.equals("PENDING") || e.eseAbsent) ? "--" : String.format("%.1f", e.totalMark);

                System.out.printf("| %-8s | %-28s | %-4s | %6.1f%%  | %7.1f | %7.1f | %7s | %6s | %-5s | %2d | %-4s |%n",
                        e.courseCode,
                        e.courseTitle.length() > 28 ? e.courseTitle.substring(0, 25) + "..." : e.courseTitle,
                        e.courseType.substring(0, Math.min(4, e.courseType.length())),
                        e.attendancePct,
                        e.ciaRaw,
                        e.ciaWeighted,
                        eseDisplay,
                        totalDisplay,
                        e.grade,
                        e.gradePoint,
                        e.status.startsWith("PASS") ? "PASS" : (e.grade.equals("PENDING") ? "PNDG" : "FAIL"));

                if (e.grade.equals("PENDING")) {
                    hasPending = true;
                } else {
                    totalQualityPoints += (e.credits * e.gradePoint);
                    if (e.gradePoint > 0) {
                        totalCreditsEarned += e.credits;
                    } else {
                        hasArrears = true;
                    }
                }
            }

            System.out.println("----------------------------------------------------------------------------------------------------------");

            // Calculate SGPA and Equivalent Percentage
            double sgpa = (totalCreditsEnrolled > 0) ? (totalQualityPoints / totalCreditsEnrolled) : 0.0;
            double percentage = sgpa * 10.0;

            System.out.println("\n--- ACADEMIC PERFORMANCE SUMMARY ---");
            System.out.printf("  Total Enrolled Credits : %.0f%n", totalCreditsEnrolled);
            System.out.printf("  Total Earned Credits   : %.0f%n", totalCreditsEarned);
            System.out.printf("  Total Quality Points   : %.2f%n", totalQualityPoints);
            System.out.printf("  Semester GPA (SGPA)    : %.2f / 10.00%n", sgpa);
            System.out.printf("  Equivalent Percentage  : %.2f%%%n", percentage);
            System.out.printf("  Final Semester Status  : %s%n",
                    hasPending ? "RESULT IN PROGRESS (PENDING ASSESSMENTS)" :
                    (hasArrears ? "RE-APPEAR / ARREAR COURSES PRESENT" : "PASSED - FIRST CLASS WITH DISTINCTION"));
            System.out.println("==========================================================================================================\n");
        }
    }

    /**
     * REPORT 2: Generates the Attendance Defaulter Report (Attendance < 65% or unapproved condonation).
     */
    public void generateAttendanceDefaulterReport() throws SQLException {
        String sql = "SELECT s.roll_no, s.student_name, s.department, e.semester, " +
                     "c.course_code, c.course_title, e.classes_held, e.classes_attended, e.condonation_approved " +
                     "FROM enrollments e " +
                     "JOIN students s ON e.student_id = s.student_id " +
                     "JOIN courses c ON e.course_id = c.course_id " +
                     "WHERE e.classes_held > 0 AND " +
                     "((e.classes_attended * 100.0 / e.classes_held) < 65.0 OR " +
                     " ((e.classes_attended * 100.0 / e.classes_held) < 75.0 AND e.condonation_approved = FALSE)) " +
                     "ORDER BY s.department, e.semester, s.roll_no";

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            System.out.println("\n==========================================================================================================");
            System.out.println("                           KEC R2024 - ATTENDANCE DEFAULTER REPORT (GRADE SA)                             ");
            System.out.println("                  Criteria: Attendance < 65% OR 65-75% without Medical Condonation                        ");
            System.out.println("==========================================================================================================");
            System.out.printf("| %-10s | %-20s | %-5s | %-4s | %-9s | %-6s | %-6s | %-8s | %-12s |%n",
                    "Roll No", "Student Name", "Dept", "Sem", "Course", "Held", "Attd", "Attend %", "Barred Grade");
            System.out.println("----------------------------------------------------------------------------------------------------------");

            boolean found = false;
            while (rs.next()) {
                found = true;
                int held = rs.getInt("classes_held");
                int attended = rs.getInt("classes_attended");
                double pct = (attended * 100.0) / held;
                boolean condonation = rs.getBoolean("condonation_approved");
                String reason = pct < 65.0 ? "SA (< 65%)" : "SA (No Condon)";

                System.out.printf("| %-10s | %-20s | %-5s | Sem%d | %-9s | %-6d | %-6d | %6.2f%%  | %-12s |%n",
                        rs.getString("roll_no"),
                        rs.getString("student_name").length() > 20 ? rs.getString("student_name").substring(0, 18) + ".." : rs.getString("student_name"),
                        rs.getString("department"),
                        rs.getInt("semester"),
                        rs.getString("course_code"),
                        held, attended, pct, reason);
            }

            if (!found) {
                System.out.println("| CONGRATULATIONS! No attendance defaulters found in the system.                                         |");
            }
            System.out.println("==========================================================================================================\n");
        }
    }

    /**
     * Department Performance Helper Class.
     */
    public static class DeptStats {
        public String department;
        public int totalStudents = 0;
        public int passedStudents = 0;
        public int failedStudents = 0;
        public double sumSgpa = 0.0;
        public double highestSgpa = 0.0;
        public String topperRollNo = "";
    }

    /**
     * REPORT 3: Generates Class Performance and Result Summary (Department-wise pass/fail count and class average SGPA).
     */
    public void generateClassPerformanceSummary(int semester) throws SQLException {
        String studentListSql = "SELECT DISTINCT s.student_id, s.roll_no, s.student_name, s.department " +
                                "FROM students s " +
                                "JOIN enrollments e ON s.student_id = e.student_id " +
                                "WHERE e.semester = ? " +
                                "ORDER BY s.department, s.roll_no";

        String studentCoursesSql = "SELECT c.course_code, c.course_title, c.credits, c.course_type, " +
                                   "e.classes_held, e.classes_attended, e.condonation_approved, " +
                                   "m.cia1, m.cia2, m.assignment_mark, m.quiz_mark, m.lab_mark, m.ese_mark, m.ese_absent " +
                                   "FROM enrollments e " +
                                   "JOIN courses c ON e.course_id = c.course_id " +
                                   "LEFT JOIN assessment_marks m ON e.enrollment_id = m.enrollment_id " +
                                   "WHERE e.student_id = ? AND e.semester = ?";

        Map<String, DeptStats> deptMap = new LinkedHashMap<>();

        try (Connection con = DBConnection.getConnection();
             PreparedStatement stPs = con.prepareStatement(studentListSql);
             PreparedStatement crsPs = con.prepareStatement(studentCoursesSql)) {

            stPs.setInt(1, semester);
            try (ResultSet sRs = stPs.executeQuery()) {
                while (sRs.next()) {
                    long studentId = sRs.getLong("student_id");
                    String rollNo = sRs.getString("roll_no");
                    String dept = sRs.getString("department");

                    DeptStats stats = deptMap.computeIfAbsent(dept, d -> {
                        DeptStats s = new DeptStats();
                        s.department = d;
                        return s;
                    });

                    stats.totalStudents++;

                    // Evaluate all courses for this student
                    crsPs.setLong(1, studentId);
                    crsPs.setInt(2, semester);

                    double qualityPoints = 0;
                    double totalCredits = 0;
                    boolean allPassed = true;

                    try (ResultSet cRs = crsPs.executeQuery()) {
                        while (cRs.next()) {
                            CourseEvaluation eval = evaluateCourse(cRs);
                            totalCredits += eval.credits;
                            qualityPoints += (eval.credits * eval.gradePoint);

                            if (eval.gradePoint == 0) {
                                allPassed = false;
                            }
                        }
                    }

                    double studentSgpa = (totalCredits > 0) ? (qualityPoints / totalCredits) : 0.0;
                    stats.sumSgpa += studentSgpa;

                    if (studentSgpa > stats.highestSgpa) {
                        stats.highestSgpa = studentSgpa;
                        stats.topperRollNo = rollNo;
                    }

                    if (allPassed && totalCredits > 0) {
                        stats.passedStudents++;
                    } else {
                        stats.failedStudents++;
                    }
                }
            }
        }

        System.out.println("\n==========================================================================================================");
        System.out.println("                 KEC R2024 - CLASS PERFORMANCE & RESULT SUMMARY (SEMESTER " + semester + ")                         ");
        System.out.println("==========================================================================================================");
        System.out.printf("| %-12s | %-14s | %-12s | %-12s | %-12s | %-14s | %-14s |%n",
                "Department", "Total Students", "All Passed", "Arrears/SA", "Pass Pct (%)", "Class Avg SGPA", "Top SGPA");
        System.out.println("----------------------------------------------------------------------------------------------------------");

        if (deptMap.isEmpty()) {
            System.out.println("| No student enrollment records found for Semester " + semester + ".                                                |");
        } else {
            for (DeptStats s : deptMap.values()) {
                double passPct = s.totalStudents > 0 ? (s.passedStudents * 100.0) / s.totalStudents : 0.0;
                double avgSgpa = s.totalStudents > 0 ? s.sumSgpa / s.totalStudents : 0.0;

                System.out.printf("| %-12s | %-14d | %-12d | %-12d | %10.2f%% | %14.2f | %-4.2f (%-6s) |%n",
                        s.department,
                        s.totalStudents,
                        s.passedStudents,
                        s.failedStudents,
                        passPct,
                        avgSgpa,
                        s.highestSgpa,
                        s.topperRollNo);
            }
        }
        System.out.println("==========================================================================================================\n");
    }

    // Helper validation and SQL parameter methods
    private void validateAttendance(int held, int attended) {
        if (held < 0 || attended < 0 || attended > held) {
            throw new IllegalArgumentException("Attendance must satisfy: 0 <= attended (" + attended + ") <= held (" + held + ").");
        }
    }

    private void validateMark(Double mark, String label) {
        if (mark != null && (mark < 0.0 || mark > 100.0)) {
            throw new IllegalArgumentException(label + " must be between 0.0 and 100.0.");
        }
    }

    private void setNullable(PreparedStatement ps, int index, Double value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.DECIMAL);
        } else {
            ps.setDouble(index, value);
        }
    }

    private Double getNullableDouble(ResultSet rs, String colName) throws SQLException {
        Object obj = rs.getObject(colName);
        return (obj == null) ? null : ((Number) obj).doubleValue();
    }

    private String formatNullable(Object val) {
        return (val == null) ? "--" : String.format("%.2f", ((Number) val).doubleValue());
    }
}
