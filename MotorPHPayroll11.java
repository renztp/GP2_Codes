/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package motorphpayroll;

/**
 * Author: Ephraim Elayda
 
 * MotorPH Payroll System
 * ----------------------------------------
 * This program processes employee payroll using CSV data.
 *
 * Features:
 * - Login authentication (employee / payroll_staff)
 * - Employee record lookup
 * - Payroll computation (June–December)
 * - Government deductions (SSS, PhilHealth, Pag-IBIG, Tax)
 *
 * Business Rules:
 * - Only hours between 8:00 AM and 5:00 PM are counted
 * - 1-hour lunch break is deducted daily
 * - First cutoff has NO deductions
 * - Second cutoff applies ALL deductions
 *
 * Data Sources:
 * - Employee Details.csv
 * - Attendance.csv
 */

import java.io.*;
import java.util.*;

public class MotorPHPayroll {
    
    // ==========================================
    // CSV COLUMN INDEX CONSTANTS (for readability)
    // ==========================================
    static final int EMP_ID = 0;
    static final int LAST_NAME = 1;
    static final int FIRST_NAME = 2;
    static final int BIRTHDAY = 3;
    static final int HOURLY_RATE = 18;
    
    // ==========================================================
    // MAIN PROGRAM CONTROLLER
    // Responsible for: login, menu navigation, program start
    // ==========================================================
    public static void main(String[] args) throws Exception {

        Scanner scanner = new Scanner(System.in);

        // Load data from CSV files
        String[][] employees = loadEmployees("Employee Details.csv");
        String[][] attendanceRecords = loadAttendance("Attendance.csv");

        printSystemHeader();

        // --- LOGIN ---
        System.out.print("Username: ");
        String username = scanner.nextLine();

        System.out.print("Password: ");
        String password = scanner.nextLine();

        if (!isValidLogin(username, password)) {
            System.out.println("Incorrect username and/or password.");
            return;
        }

        if (username.equals("employee")) {
            runEmployeeMenu(scanner, employees);
        } else {
            runPayrollStaffMenu(scanner, employees, attendanceRecords);
        }
    }

    // ==========================================================
    // SYSTEM DISPLAY METHODS
    // Responsible for: printing menus and system messages
    // ==========================================================

    static void printSystemHeader() {
        System.out.println("======================================");
        System.out.println("         MOTORPH PAYROLL SYSTEM       ");
        System.out.println("======================================");
    }

    static boolean isValidLogin(String username, String password) {
        boolean validUser = username.equals("employee") || username.equals("payroll_staff");
        boolean validPass = password.equals("12345");
        return validUser && validPass;
    }

    // ==========================================================
    // EMPLOYEE MENU
    // Allows employee to view personal details
    // ==========================================================

    static void runEmployeeMenu(Scanner scanner, String[][] attendanceRecords) {

        System.out.println("1. Enter your employee number");
        System.out.println("2. Exit the program");

        String choice = scanner.nextLine();

        if (!choice.equals("1")) return;

        System.out.print("Enter Employee Number: ");
        String employeeId = scanner.nextLine();

        String[] employee = findEmployee(attendanceRecords, employeeId);

        if (employee == null) {
            System.out.println("Employee number does not exist.");
            return;
        }

        System.out.println("Employee Number: " + employee[EMP_ID]);
        System.out.println("Employee Name: " + employee[FIRST_NAME] + " " + employee[LAST_NAME]);
        System.out.println("Birthday: " + employee[BIRTHDAY]);
    }

    // ==========================================================
    // PAYROLL STAFF MENU
    // Allows payroll staff to compute salaries
    // ==========================================================

    static void runPayrollStaffMenu(
            Scanner scanner,
            String[][] employees,
            String[][] attendanceRecords) {

        System.out.println("1. Process Payroll");
        System.out.println("2. Exit");

        if (!scanner.nextLine().equals("1")) return;

        System.out.println("1. One employee");
        System.out.println("2. All employees");
        System.out.println("3. Exit");

        String option = scanner.nextLine();

        if (option.equals("1")) {

            System.out.print("Enter employee number: ");
            String employeeId = scanner.nextLine();

            String[] employee = findEmployee(employees, employeeId);

            if (employee == null) {
                System.out.println("Employee number does not exist.");
                return;
            }

            processEmployeePayroll(employee, attendanceRecords);
        }

        if (option.equals("2")) {

            for (String[] employee : employees) {
                processEmployeePayroll(employee, attendanceRecords);
            }
        }
    }

    // ==========================================================
    // PAYROLL PROCESSING
    // Responsible for computing employee salary June–December
    // ==========================================================
    /**
    * Calculates total worked hours for an employee within a cutoff period.
    *
    * Processes payroll for a single employee from June to December.
    *
    * Computes:
    * - Worked hours per cutoff
    * - Gross salary
    * - Government deductions
    * - Net salary
    * 
    * Rules applied:
    * - Only counts time between 8:00 AM and 5:00 PM
    * - Applies grace period (<= 8:10 AM treated as 8:00 AM)
    * - Caps maximum work per day to 8 hours
    *
    * @param attendanceData attendance CSV data
    * @param employeeId employee number
    * @param month target month
    * @param startDay cutoff start day
    * @param endDay cutoff end day
    * @return total worked hours for the period
    */
    static void processEmployeePayroll(String[] employee, String[][] attendanceRecords) {

        int startMonth = 6;
        int endMonth = 12;

        // Read the year directly from the attendance data so nothing is hardcoded.
        // The date column is M/D/YYYY, so the year is the third part after splitting on "/".
        int year = getYearFromAttendance(attendanceRecords, employee[EMP_ID]);

        double hourlyRate = Double.parseDouble(employee[HOURLY_RATE]);

        for (int month = startMonth; month <= endMonth; month++) {

            int lastDayOfMonth = getLastDayOfMonth(month, year);

            double firstCutoffHours =
                    calculateWorkedHours(attendanceRecords, employee[0], month, 1, 15);

            double secondCutoffHours =
                    calculateWorkedHours(attendanceRecords, employee[0], month, 16, lastDayOfMonth);

            double firstCutoffGross = firstCutoffHours * hourlyRate;
            double secondCutoffGross = secondCutoffHours * hourlyRate;

            double monthlyGross = firstCutoffGross + secondCutoffGross;

            // --- Government deductions calculated on monthly total ---
            double sss = computeSSS(monthlyGross);
            double philHealth = computePhilHealth(monthlyGross);
            double pagibig = computePagibig(monthlyGross);

            double taxableIncome = monthlyGross - sss - philHealth - pagibig;
            double withholdingTax = computeTax(taxableIncome);

            double totalDeductions = sss + philHealth + pagibig + withholdingTax;

            /*
            MotorPH payout rule:

            First cutoff (1–15):
            - Employee receives full gross salary
            - No deductions applied yet

            Second cutoff (16–end):
            - All government deductions for the month are applied
            */

            double firstCutoffNet = firstCutoffGross;
            double secondCutoffNet = secondCutoffGross - totalDeductions;

            printPayroll(
                    employee,
                    month,
                    lastDayOfMonth,
                    firstCutoffHours,
                    secondCutoffHours,
                    firstCutoffGross,
                    secondCutoffGross,
                    firstCutoffNet,
                    secondCutoffNet,
                    sss,
                    philHealth,
                    pagibig,
                    withholdingTax,
                    totalDeductions
            );
            
            
        }
    }

    // ==========================================================
    // HOURS WORKED CALCULATION
    // Only counts time between 8:00 AM and 5:00 PM
    // ==========================================================
    /**
    * Calculates total worked hours for an employee within a cutoff period.
    *
    * Rules:
    * - Only counts time between 8:00 AM – 5:00 PM
    * - Applies grace period (<= 8:10 AM treated as 8:00 AM)
    * - Caps work to 8 hours per day
    *
    * @param attendance attendance dataset
    * @param id employee ID
    * @param month target month
    * @param start cutoff start day
    * @param end cutoff end day
    * @return total worked hours
    */
    static double calculateWorkedHours(
            String[][] attendanceRecords,
            String employeeId,
            int month,
            int startDay,
            int endDay) {

        double totalMinutesWorked = 0;

        for (String[] attendanceRow : attendanceRecords) {

            if (!attendanceRow[0].equals(employeeId)) continue;

            String[] dateParts = attendanceRow[3].split("/");

            int recordMonth = Integer.parseInt(dateParts[0]);
            int recordDay = Integer.parseInt(dateParts[1]);

            if (recordMonth != month) continue;
            if (recordDay < startDay || recordDay > endDay) continue;

            int[] loginTime = parseTime(attendanceRow[4]);
            int[] logoutTime = parseTime(attendanceRow[5]);

            int loginMinutes = loginTime[0] * 60 + loginTime[1];
            int logoutMinutes = logoutTime[0] * 60 + logoutTime[1];

            if (loginMinutes <= 8 * 60 + 10) loginMinutes = 8 * 60;
            if (logoutMinutes > 17 * 60) logoutMinutes = 17 * 60;

            int minutesWorked = logoutMinutes - loginMinutes;

            if (minutesWorked > 0) {
                totalMinutesWorked += minutesWorked;
            }
        }

        return totalMinutesWorked / 60.0;
    }

    // ==========================================================
    // UTILITY METHODS
    // ==========================================================

    static int[] parseTime(String time) {
        String[] parts = time.split(":");
        return new int[]{
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1])
        };
    }

    // Reads the year from the first matching attendance record for the given employee.
    // The date column uses M/D/YYYY format, so the year is the third part after splitting on "/".
    // Returns -1 if no record is found (should not happen with valid data).
    static int getYearFromAttendance(String[][] attendanceRecords, String employeeId) {

        for (String[] row : attendanceRecords) {
            if (!row[0].equals(employeeId)) continue;

            String[] dateParts = row[3].split("/");
            if (dateParts.length >= 3) {
                return Integer.parseInt(dateParts[2].trim());
            }
        }

        return -1;
    }

    // Returns the last calendar day of the given month and year.
    // Leap year rule: divisible by 4, EXCEPT centuries unless also divisible by 400.
    // Example: 2024 is a leap year (divisible by 4, not a century) → February has 29 days.
    static int getLastDayOfMonth(int month, int year) {

        if (month == 2) {
            boolean isLeapYear = (year % 4 == 0) && (year % 100 != 0 || year % 400 == 0);
            return isLeapYear ? 29 : 28;
        }

        if (month == 4 || month == 6 || month == 9 || month == 11) return 30;

        return 31;
    }

    // ==========================================================
    // GOVERNMENT DEDUCTIONS
    // ==========================================================
   
    /**
    * Computes SSS contribution based on salary brackets.
    *
    * @param salary monthly gross salary
    * @return SSS contribution
    */
   
    static double computeSSS(double salary) {

        if (salary < 3250) return 135;
        if (salary >= 24750) return 1125;

        int bracket = (int) ((salary - 3250) / 500);
        return 157.5 + bracket * 22.5;
    }

    static double computePhilHealth(double salary) {

        if (salary <= 10000) return 150;
        if (salary >= 60000) return 900;

        return (salary * 0.03) / 2;
    }

    static double computePagibig(double salary) {

        double rate = salary <= 1500 ? 0.01 : 0.02;
        double contribution = salary * rate;

        return contribution > 100 ? 100 : contribution;
    }

    static double computeTax(double income) {

        if (income <= 20832) return 0;
        if (income <= 33332) return (income - 20833) * 0.20;
        if (income <= 66666) return 2500 + (income - 33333) * 0.25;
        if (income <= 166666) return 10833 + (income - 66667) * 0.30;
        if (income <= 666666) return 40833.33 + (income - 166667) * 0.32;

        return 200833.33 + (income - 666667) * 0.35;
    }

    // ==========================================================
    // DATA ACCESS METHODS (CSV)
    // ==========================================================

    static String[] findEmployee(String[][] employees, String employeeId) {

        for (String[] employee : employees) {
            if (employee[0].equals(employeeId)) {
                return employee;
            }
        }

        return null;
    }
        // Regex explanation:
        // Splits CSV correctly even if values contain commas inside quotes
        static String[][] loadEmployees(String file) throws Exception {

        BufferedReader reader = new BufferedReader(new FileReader(file));
        List<String[]> employees = new ArrayList<>();

        String line;
        boolean header = true;

        while ((line = reader.readLine()) != null) {

            if (header) {
                header = false;
                continue;
            }

            String[] row =
                    line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

            for (int i = 0; i < row.length; i++) {
                row[i] = row[i].replace("\"", "").trim();
            }

            if (row.length > 18) {
                row[18] = row[18].replace(",", "");
            }

            employees.add(row);
        }

        reader.close();

        return employees.toArray(String[][]::new);
    }

    static String[][] loadAttendance(String file) throws Exception {

        BufferedReader reader = new BufferedReader(new FileReader(file));
        List<String[]> attendance = new ArrayList<>();

        String line;
        boolean header = true;

        while ((line = reader.readLine()) != null) {

            if (header) {
                header = false;
                continue;
            }

            String[] row = line.split(",");
            attendance.add(row);
        }

        reader.close();

        return attendance.toArray(String[][]::new);
    }

    // ==========================================================
    // PAYROLL OUTPUT
    // ==========================================================

    static void printPayroll(
            String[] employee,
            int month,
            int lastDay,
            double hours1,
            double hours2,
            double gross1,
            double gross2,
            double net1,
            double net2,
            double sss,
            double phil,
            double pagibig,
            double tax,
            double totalDeduction) {

        System.out.println("--------------------------------");
        System.out.println("Employee #: " + employee[0]);
        System.out.println("Employee Name: " + employee[2] + " " + employee[1]);
        System.out.println("Birthday: " + employee[3]);

        System.out.println("Cutoff Date: Month " + month + " 1-15");
        System.out.println("Total Hours Worked: " + hours1);
        System.out.println("Gross Salary: " + gross1);
        System.out.println("Net Salary: " + net1);

        System.out.println("Cutoff Date: Month " + month + " 16-" + lastDay);
        System.out.println("Total Hours Worked: " + hours2);
        System.out.println("Gross Salary: " + gross2);

        System.out.println("SSS: " + sss);
        System.out.println("PhilHealth: " + phil);
        System.out.println("Pag-IBIG: " + pagibig);
        System.out.println("Tax: " + tax);

        System.out.println("Total Deductions: " + totalDeduction);
        System.out.println("Net Salary: " + net2);
        System.out.println("--------------------------------");
    }
}

