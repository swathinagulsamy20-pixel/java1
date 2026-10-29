import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * StudentDAO handles CRUD operations for Student Registration and Course Catalog
 * for Kongu Engineering College (Autonomous) Regulation 2024.
 */
public class StudentDAO {

    /**
     * Registers a new student.
     * @param rollNo Roll number (e.g., 24ITR001)
     * @param name Student full name
     * @param department Department abbreviation (CSE, IT, ECE, MECH, etc.)
     * @param semester Current semester (1 to 8)
     * @return true if added successfully
     * @throws SQLException on database error
     */
    public boolean addStudent(String rollNo, String name, String department, int semester) throws SQLException {
        if (rollNo == null || rollNo.trim().isEmpty()) {
            throw new IllegalArgumentException("Roll number cannot be empty.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Student name cannot be empty.");
        }
        if (department == null || department.trim().isEmpty()) {
            throw new IllegalArgumentException("Department cannot be empty.");
        }
        if (semester < 1 || semester > 8) {
            throw new IllegalArgumentException("Semester must be between 1 and 8.");
        }

        String sql = "INSERT INTO students (roll_no, student_name, department, current_semester) VALUES (?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, rollNo.trim().toUpperCase());
            ps.setString(2, name.trim());
            ps.setString(3, department.trim().toUpperCase());
            ps.setInt(4, semester);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Adds a new course to the catalog.
     * @param courseCode Unique course code (e.g., 24ITR31)
     * @param courseTitle Course title
     * @param credits Number of credits (1 to 10)
     * @param courseType THEORY, PRACTICAL, or EMBEDDED
     * @return true if added successfully
     * @throws SQLException on database error
     */
    public boolean addCourse(String courseCode, String courseTitle, int credits, String courseType) throws SQLException {
        if (courseCode == null || courseCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Course code cannot be empty.");
        }
        if (courseTitle == null || courseTitle.trim().isEmpty()) {
            throw new IllegalArgumentException("Course title cannot be empty.");
        }
        if (credits < 1 || credits > 10) {
            throw new IllegalArgumentException("Credits must be between 1 and 10.");
        }
        String typeUpper = courseType.trim().toUpperCase();
        if (!typeUpper.equals("THEORY") && !typeUpper.equals("PRACTICAL") && !typeUpper.equals("EMBEDDED")) {
            throw new IllegalArgumentException("Course type must be THEORY, PRACTICAL, or EMBEDDED.");
        }

        String sql = "INSERT INTO courses (course_code, course_title, credits, course_type) VALUES (?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, courseCode.trim().toUpperCase());
            ps.setString(2, courseTitle.trim());
            ps.setInt(3, credits);
            ps.setString(4, typeUpper);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Checks if a student exists by roll number.
     */
    public boolean studentExists(String rollNo) throws SQLException {
        String sql = "SELECT 1 FROM students WHERE roll_no = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, rollNo.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Checks if a course exists by course code.
     */
    public boolean courseExists(String courseCode) throws SQLException {
        String sql = "SELECT 1 FROM courses WHERE course_code = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, courseCode.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Prints all registered students.
     */
    public void listAllStudents() throws SQLException {
        String sql = "SELECT roll_no, student_name, department, current_semester, created_at FROM students ORDER BY roll_no";
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            System.out.println("\n+-----------------------------------------------------------------------------------------+");
            System.out.println("|                                REGISTERED STUDENTS LIST                                 |");
            System.out.println("+------------+--------------------------------+------------+----------+-------------------+");
            System.out.printf("| %-10s | %-30s | %-10s | %-8s | %-17s |%n",
                    "Roll No", "Student Name", "Department", "Semester", "Registered Date");
            System.out.println("+------------+--------------------------------+------------+----------+-------------------+");

            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("| %-10s | %-30s | %-10s | Sem %-4d | %-17s |%n",
                        rs.getString("roll_no"),
                        rs.getString("student_name"),
                        rs.getString("department"),
                        rs.getInt("current_semester"),
                        rs.getString("created_at").substring(0, 10));
            }
            if (!found) {
                System.out.println("| No registered students found in database.                                               |");
            }
            System.out.println("+------------+--------------------------------+------------+----------+-------------------+");
        }
    }

    /**
     * Prints all courses in the catalog.
     */
    public void listAllCourses() throws SQLException {
        String sql = "SELECT course_code, course_title, credits, course_type FROM courses ORDER BY course_code";
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            System.out.println("\n+-----------------------------------------------------------------------------------------+");
            System.out.println("|                                   COURSE CATALOG                                        |");
            System.out.println("+-------------+----------------------------------------------+---------+------------------+");
            System.out.printf("| %-11s | %-44s | %-7s | %-16s |%n",
                    "Course Code", "Course Title", "Credits", "Course Type");
            System.out.println("+-------------+----------------------------------------------+---------+------------------+");

            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("| %-11s | %-44s | %-7d | %-16s |%n",
                        rs.getString("course_code"),
                        rs.getString("course_title"),
                        rs.getInt("credits"),
                        rs.getString("course_type"));
            }
            if (!found) {
                System.out.println("| No courses found in catalog.                                                            |");
            }
            System.out.println("+-------------+----------------------------------------------+---------+------------------+");
        }
    }
}
