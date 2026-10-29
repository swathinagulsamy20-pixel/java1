import java.sql.SQLException;
import java.util.Scanner;

/**
 * MainApp - Menu-driven CLI interface for Kongu Engineering College (Autonomous)
 * Regulation 2024 (R2024) Student Academic & Continuous Evaluation System.
 */
public class MainApp {

    private static final Scanner scanner = new Scanner(System.in);
    private static final StudentDAO studentDAO = new StudentDAO();
    private static final AcademicDAO academicDAO = new AcademicDAO();

    public static void main(String[] args) {
        printBanner();

        if (!DBConnection.testConnection()) {
            System.err.println("[CRITICAL] Could not establish connection to MySQL database kec_r2024.");
            System.err.println("Please ensure MySQL service is running and schema is loaded via sql/kec_schema.sql.");
            return;
        }
        System.out.println("[SUCCESS] Connected to MySQL database (kec_r2024) successfully.\n");

        boolean running = true;
        while (running) {
            printMainMenu();
            int choice = readInt("Enter your choice: ");
            try {
                switch (choice) {
                    case 1 -> handleAddStudent();
                    case 2 -> handleAddCourse();
                    case 3 -> handleEnrollment();
                    case 4 -> handleUpdateAttendance();
                    case 5 -> handleIncrementalMarksMenu();
                    case 6 -> handleStudentMarkSheet();
                    case 7 -> handleAttendanceDefaulters();
                    case 8 -> handleClassSummary();
                    case 9 -> handleViewCatalog();
                    case 0 -> {
                        System.out.println("\nThank you for using KEC R2024 Academic Evaluation System. Goodbye!");
                        running = false;
                    }
                    default -> System.out.println("\n[ERROR] Invalid menu choice. Please select from 0 to 9.");
                }
            } catch (SQLException e) {
                System.err.println("\n[DATABASE ERROR] " + e.getMessage());
            } catch (IllegalArgumentException e) {
                System.err.println("\n[VALIDATION ERROR] " + e.getMessage());
            } catch (Exception e) {
                System.err.println("\n[ERROR] Unexpected error: " + e.getMessage());
            }
        }
    }

    private static void printBanner() {
        System.out.println("==========================================================================================");
        System.out.println("            KONGU ENGINEERING COLLEGE (AUTONOMOUS) - REGULATION 2024 (R2024)              ");
        System.out.println("                 Student Academic & Continuous Evaluation System (JDBC)                   ");
        System.out.println("==========================================================================================");
    }

    private static void printMainMenu() {
        System.out.println("\n------------------------------------------------------------------------------------------");
        System.out.println("                                      MAIN MENU                                           ");
        System.out.println("------------------------------------------------------------------------------------------");
        System.out.println("  1. Student Registration (Add New Student)");
        System.out.println("  2. Course Catalog (Add New Course: Theory / Practical / Embedded)");
        System.out.println("  3. Course Enrollment & Attendance Tracking");
        System.out.println("  4. Update Attendance & Record Medical Condonation Approval");
        System.out.println("  5. Phased / Incremental Marks Entry ('Then and Now': CIA1, CIA2, Lab, ESE, AB)");
        System.out.println("  6. Generate Student Semester Mark Sheet (Formal Grade Report & SGPA)");
        System.out.println("  7. Generate Attendance Defaulter Report (Grade SA Flagging)");
        System.out.println("  8. Generate Class Performance & Result Summary (Dept Pass/Fail & Class Avg SGPA)");
        System.out.println("  9. View Registered Students & Course Catalog");
        System.out.println("  0. Exit System");
        System.out.println("------------------------------------------------------------------------------------------");
    }

    private static void handleAddStudent() throws SQLException {
        System.out.println("\n--- [1] STUDENT REGISTRATION ---");
        String rollNo = readString("Enter Roll Number (e.g. 24ITR001): ");
        String name = readString("Enter Student Full Name: ");
        String dept = readString("Enter Department (CSE, IT, ECE, MECH, etc.): ");
        int sem = readInt("Enter Current Semester (1-8): ");

        boolean added = studentDAO.addStudent(rollNo, name, dept, sem);
        if (added) {
            System.out.println("[SUCCESS] Student registered successfully: " + rollNo.toUpperCase() + " - " + name);
        }
    }

    private static void handleAddCourse() throws SQLException {
        System.out.println("\n--- [2] COURSE CATALOG ENTRY ---");
        String code = readString("Enter Course Code (e.g. 24ITT31): ");
        String title = readString("Enter Course Title: ");
        int credits = readInt("Enter Course Credits (1-10): ");
        System.out.println("Select Course Type:");
        System.out.println("  1. THEORY (CIA: 40%, ESE: 60%, ESE Min: 45%)");
        System.out.println("  2. PRACTICAL (CIA: 60%, ESE: 40%, ESE Min: 50%)");
        System.out.println("  3. EMBEDDED (CIA: 50%, ESE: 50%, ESE Min: 45%)");
        int typeChoice = readInt("Choose Course Type (1-3): ");
        String courseType = switch (typeChoice) {
            case 1 -> "THEORY";
            case 2 -> "PRACTICAL";
            case 3 -> "EMBEDDED";
            default -> throw new IllegalArgumentException("Invalid course type option.");
        };

        boolean added = studentDAO.addCourse(code, title, credits, courseType);
        if (added) {
            System.out.println("[SUCCESS] Course added successfully: [" + code.toUpperCase() + "] " + title + " (" + courseType + ", " + credits + " Credits)");
        }
    }

    private static void handleEnrollment() throws SQLException {
        System.out.println("\n--- [3] COURSE ENROLLMENT & ATTENDANCE TRACKING ---");
        String rollNo = readString("Enter Student Roll Number: ");
        String courseCode = readString("Enter Course Code: ");
        int semester = readInt("Enter Semester: ");
        int held = readInt("Enter Total Classes Held: ");
        int attended = readInt("Enter Classes Attended: ");
        boolean condoned = readYesNo("Is Medical Condonation Approved? (y/n): ");

        academicDAO.enroll(rollNo, courseCode, semester, held, attended, condoned);
        System.out.println("[SUCCESS] Student " + rollNo.toUpperCase() + " successfully enrolled in " + courseCode.toUpperCase() + ".");
    }

    private static void handleUpdateAttendance() throws SQLException {
        System.out.println("\n--- [4] UPDATE ATTENDANCE & CONDONATION ---");
        String rollNo = readString("Enter Student Roll Number: ");
        String courseCode = readString("Enter Course Code: ");
        int semester = readInt("Enter Semester: ");
        int held = readInt("Enter Updated Total Classes Held: ");
        int attended = readInt("Enter Updated Classes Attended: ");
        boolean condoned = readYesNo("Is Medical Condonation Approved? (y/n): ");

        academicDAO.updateAttendance(rollNo, courseCode, semester, held, attended, condoned);
        double pct = (held > 0) ? (attended * 100.0) / held : 0.0;
        System.out.printf("[SUCCESS] Attendance updated: %d/%d (%.2f%%). Condonation Approved: %s%n",
                attended, held, pct, condoned ? "YES" : "NO");
    }

    private static void handleIncrementalMarksMenu() throws SQLException {
        boolean back = false;
        while (!back) {
            System.out.println("\n------------------------------------------------------------------------------------------");
            System.out.println("                 INCREMENTAL MARKS ENTRY - 'THEN AND NOW' PHASES                          ");
            System.out.println("------------------------------------------------------------------------------------------");
            System.out.println("  1. Phase 1 (Mid-Semester)      : Enter CIA Test 1 (0-100) & CIA Test 2 (0-100)");
            System.out.println("  2. Phase 2 (End of Semester)   : Enter Assignments / Quizzes / Lab Evaluations (0-100)");
            System.out.println("  3. Phase 3 (Post Semester)     : Enter ESE Marks (0-100) or Mark Absence (AB)");
            System.out.println("  4. Comprehensive / Bulk Entry  : Update Any / All Assessment Components");
            System.out.println("  0. Back to Main Menu");
            System.out.println("------------------------------------------------------------------------------------------");

            int phase = readInt("Select Marks Entry Option: ");
            switch (phase) {
                case 1 -> handlePhase1Marks();
                case 2 -> handlePhase2Marks();
                case 3 -> handlePhase3Marks();
                case 4 -> handleComprehensiveMarks();
                case 0 -> back = true;
                default -> System.out.println("[ERROR] Invalid choice. Choose 0 to 4.");
            }
        }
    }

    private static void handlePhase1Marks() throws SQLException {
        System.out.println("\n>>> PHASE 1 (MID-SEMESTER): CIA TEST 1 & CIA TEST 2");
        String rollNo = readString("Enter Roll Number: ");
        String courseCode = readString("Enter Course Code: ");
        int semester = readInt("Enter Semester: ");

        // Show "Then"
        academicDAO.displayExistingMarks(rollNo, courseCode, semester);

        // Take "Now"
        System.out.println("\n[NOW - Enter New/Updated Phase 1 Marks (Leave blank to keep existing)]");
        Double cia1 = readOptionalDouble("Enter CIA Test 1 Mark (0-100): ");
        Double cia2 = readOptionalDouble("Enter CIA Test 2 Mark (0-100): ");

        academicDAO.savePhase1Marks(rollNo, courseCode, semester, cia1, cia2);
        System.out.println("[SUCCESS] Phase 1 Marks successfully recorded for " + rollNo.toUpperCase() + " in " + courseCode.toUpperCase() + ".");
    }

    private static void handlePhase2Marks() throws SQLException {
        System.out.println("\n>>> PHASE 2 (END OF SEMESTER CIA): CONTINUOUS ASSESSMENTS");
        String rollNo = readString("Enter Roll Number: ");
        String courseCode = readString("Enter Course Code: ");
        int semester = readInt("Enter Semester: ");

        // Show "Then"
        academicDAO.displayExistingMarks(rollNo, courseCode, semester);

        // Take "Now"
        System.out.println("\n[NOW - Enter New/Updated Continuous Assessment Marks (Leave blank to skip)]");
        Double assignment = readOptionalDouble("Enter Assignment Mark (0-100): ");
        Double quiz = readOptionalDouble("Enter Quiz Mark (0-100): ");
        Double lab = readOptionalDouble("Enter Lab Evaluation Mark (0-100): ");

        academicDAO.savePhase2Marks(rollNo, courseCode, semester, assignment, quiz, lab);
        System.out.println("[SUCCESS] Phase 2 Marks successfully recorded for " + rollNo.toUpperCase() + " in " + courseCode.toUpperCase() + ".");
    }

    private static void handlePhase3Marks() throws SQLException {
        System.out.println("\n>>> PHASE 3 (POST SEMESTER): END SEMESTER EXAMINATION (ESE)");
        String rollNo = readString("Enter Roll Number: ");
        String courseCode = readString("Enter Course Code: ");
        int semester = readInt("Enter Semester: ");

        // Show "Then"
        academicDAO.displayExistingMarks(rollNo, courseCode, semester);

        // Take "Now"
        System.out.println("\n[NOW - Enter ESE Mark or Absence]");
        String input = readString("Enter ESE Mark (0-100) or 'AB' for Absent: ");

        boolean isAbsent = input.equalsIgnoreCase("AB");
        Double ese = null;
        if (!isAbsent && !input.isBlank()) {
            ese = Double.parseDouble(input.trim());
        }

        academicDAO.savePhase3Marks(rollNo, courseCode, semester, ese, isAbsent);
        System.out.println("[SUCCESS] Phase 3 ESE evaluation successfully recorded for " + rollNo.toUpperCase() + " in " + courseCode.toUpperCase() + ".");
    }

    private static void handleComprehensiveMarks() throws SQLException {
        System.out.println("\n>>> COMPREHENSIVE ASSESSMENT MARKS ENTRY");
        String rollNo = readString("Enter Roll Number: ");
        String courseCode = readString("Enter Course Code: ");
        int semester = readInt("Enter Semester: ");

        // Show "Then"
        academicDAO.displayExistingMarks(rollNo, courseCode, semester);

        System.out.println("\n[NOW - Enter assessment components (Press Enter to keep unchanged)]");
        Double cia1 = readOptionalDouble("CIA Test 1 (0-100): ");
        Double cia2 = readOptionalDouble("CIA Test 2 (0-100): ");
        Double assignment = readOptionalDouble("Assignment (0-100): ");
        Double quiz = readOptionalDouble("Quiz (0-100): ");
        Double lab = readOptionalDouble("Lab Evaluation (0-100): ");

        String eseInput = readString("ESE Marks (0-100), 'AB' for Absent, or blank to keep: ");
        boolean eseProvided = !eseInput.isBlank();
        boolean isAbsent = eseInput.equalsIgnoreCase("AB");
        Double ese = (eseInput.isBlank() || isAbsent) ? null : Double.parseDouble(eseInput.trim());

        academicDAO.saveComprehensiveMarks(rollNo, courseCode, semester, cia1, cia2, assignment, quiz, lab, ese, eseProvided, isAbsent);
        System.out.println("[SUCCESS] Assessment marks updated successfully.");
    }

    private static void handleStudentMarkSheet() throws SQLException {
        System.out.println("\n--- [6] STUDENT SEMESTER MARK SHEET ---");
        String rollNo = readString("Enter Student Roll Number (e.g. 24ITR001): ");
        int semester = readInt("Enter Semester (1-8): ");
        academicDAO.printStudentMarkSheet(rollNo, semester);
    }

    private static void handleAttendanceDefaulters() throws SQLException {
        academicDAO.generateAttendanceDefaulterReport();
    }

    private static void handleClassSummary() throws SQLException {
        System.out.println("\n--- [8] CLASS PERFORMANCE & RESULT SUMMARY ---");
        int semester = readInt("Enter Semester to analyze (1-8): ");
        academicDAO.generateClassPerformanceSummary(semester);
    }

    private static void handleViewCatalog() throws SQLException {
        studentDAO.listAllStudents();
        studentDAO.listAllCourses();
    }

    // Helper I/O methods
    private static String readString(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("[ERROR] Please enter a valid integer.");
            }
        }
    }

    private static Double readOptionalDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                return null;
            }
            try {
                double val = Double.parseDouble(line);
                if (val < 0.0 || val > 100.0) {
                    System.out.println("[ERROR] Mark must be between 0.0 and 100.0.");
                    continue;
                }
                return val;
            } catch (NumberFormatException e) {
                System.out.println("[ERROR] Please enter a valid number or leave blank.");
            }
        }
    }

    private static boolean readYesNo(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim().toLowerCase();
            if (line.equals("y") || line.equals("yes")) return true;
            if (line.equals("n") || line.equals("no")) return false;
            System.out.println("[ERROR] Please answer 'y' or 'n'.");
        }
    }
}
