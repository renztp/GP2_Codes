package com.mycompany.motorph_group48;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

public class MotorPH_Group48 {

    public static void main(String[] args) {
        String employeeFile   = "MotorPH_Employee Data - Tucker, L. - Employee Details.csv";
        String attendanceFile = "MotorPH_Employee Data - Tucker, L. - Attendance Record.csv";

        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter Employee Number or ALL: ");
        String userInput = scanner.nextLine().trim();

        try {
            if (userInput.equalsIgnoreCase("ALL")) {
                processAllEmployees(employeeFile, attendanceFile);
            } else {
                processSingleEmployee(employeeFile, attendanceFile, userInput);
            }
        } catch (Exception error) {
            System.out.println("Error: " + error.getMessage());
        }
    }

    // Reads the CSV and processes payroll for one specific employee.
    public static void processSingleEmployee(String employeeFile, String attendanceFile, String targetEmployeeNumber) throws Exception {
        String employeeNumber = "";
        String lastName       = "";
        String firstName      = "";
        String birthday       = "";
        double hourlyRate     = 0.0;
        boolean found         = false;

        try (BufferedReader reader = new BufferedReader(new FileReader(employeeFile))) {
            reader.readLine(); // Skip the header row — it only contains column names.

            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = splitCSV(line);
                if (columns.length < 19) {
                    continue;
                }

                // Stop as soon as the target employee number is matched.
                if (removeQuotes(columns[0]).equals(targetEmployeeNumber)) {
                    employeeNumber = removeQuotes(columns[0]);
                    lastName       = removeQuotes(columns[1]);
                    firstName      = removeQuotes(columns[2]);
                    birthday       = removeQuotes(columns[3]);
                    hourlyRate     = parseAmount(columns[18]);
                    found          = true;
                    break;
                }
            }
        }

        if (!found) {
            System.out.println("Employee not found.");
            return;
        }

        printEmployeeHeader(employeeNumber, lastName, firstName, birthday);

        Map<Integer, double[]> monthlyHours = processAttendance(attendanceFile, employeeNumber);

        if (monthlyHours.isEmpty()) {
            System.out.println("No attendance records found.");
        }

        // Always show all months from June to December as required by the teacher.
        for (int month = 6; month <= 12; month++) {
            double[] cutoffHours = monthlyHours.getOrDefault(month, new double[2]);
            processPayrollForMonth(month, cutoffHours, hourlyRate);
        }
    }

    // Loops through every employee in the CSV and processes each one's payroll.
    public static void processAllEmployees(String employeeFile, String attendanceFile) throws Exception {
        try (BufferedReader reader = new BufferedReader(new FileReader(employeeFile))) {
            reader.readLine(); 

            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = splitCSV(line);

                // skip incomplete row
                if (columns.length < 19) {
                    continue;
                }

                String employeeNumber = removeQuotes(columns[0]);
                String lastName       = removeQuotes(columns[1]);
                String firstName      = removeQuotes(columns[2]);
                String birthday       = removeQuotes(columns[3]);
                double hourlyRate     = parseAmount(columns[18]);

                printEmployeeHeader(employeeNumber, lastName, firstName, birthday);

                Map<Integer, double[]> monthlyHours = processAttendance(attendanceFile, employeeNumber);

                if (monthlyHours.isEmpty()) {
                    System.out.println("No attendance records found.");
                }

                // Always show payroll rows from June to December as required.
                for (int month = 6; month <= 12; month++) {
                    double[] cutoffHours = monthlyHours.getOrDefault(month, new double[2]);
                    processPayrollForMonth(month, cutoffHours, hourlyRate);
                }

                System.out.println("\n--------------------------------------------------");
            }
        }
    }

    // Read attendance and store hours per month and cutoff
    
    public static Map<Integer, double[]> processAttendance(String attendanceFile, String targetEmployeeNumber) throws Exception {
        Map<Integer, double[]> monthlyHours = new TreeMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(attendanceFile))) {
            reader.readLine(); // Skip the header row.

            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = splitCSV(line);

                // skip incomplete row
                if (columns.length < 6) {
                    continue;
                }

                // Skip records that belong to a different employee.
                if (!removeQuotes(columns[0]).equals(targetEmployeeNumber)) {
                    continue;
                }

                String date    = removeQuotes(columns[3]);
                String[] parts = date.split("/");

                // skip bad date
                if (parts.length < 2) {
                    continue;
                }

                int month = Integer.parseInt(parts[0]);
                int day   = Integer.parseInt(parts[1]);

                // skip invalid month
                if (month < 1 || month > 12) {
                    continue;
                }

                // Create a fresh entry the first time a month is encountered.
                monthlyHours.putIfAbsent(month, new double[2]);

                double hoursWorked = calculateHoursWorked(
                    removeQuotes(columns[4]),
                    removeQuotes(columns[5])
                );

                // Days 1 to 15 belong to the first cutoff.
                // Days 16 onwards belong to the second cutoff.
                if (day <= 15) {
                    monthlyHours.get(month)[0] += hoursWorked;
                } else {
                    monthlyHours.get(month)[1] += hoursWorked;
                }
            }
        }

        return monthlyHours;
    }

    // Displays the full monthly payroll for one month.
    public static void processPayrollForMonth(int month, double[] cutoffHours, double hourlyRate) {
        String monthName = getMonthName(month);

        double firstCutoffHours  = cutoffHours[0];
        double secondCutoffHours = cutoffHours[1];

        double[] payrollValues = calculateMonthlyPayroll(firstCutoffHours, secondCutoffHours, hourlyRate);

        double firstCutoffGross  = payrollValues[0];
        double secondCutoffGross = payrollValues[1];
        double totalMonthlyGross = payrollValues[2];
        double sss               = payrollValues[3];
        double philHealth        = payrollValues[4];
        double pagIbig           = payrollValues[5];
        double withholdingTax    = payrollValues[6];
        double netSalary         = payrollValues[7];

        int lastDayOfMonth = getLastDayOfMonth(month);

        System.out.println("\n--- " + monthName + " ---");

        System.out.println("\nCutoff: " + monthName + " 1-15");
        System.out.println("  Hours Worked : " + firstCutoffHours);
        System.out.println("  Gross Pay    : " + firstCutoffGross);

        System.out.println("\nCutoff: " + monthName + " 16-" + lastDayOfMonth);
        System.out.println("  Hours Worked : " + secondCutoffHours);
        System.out.println("  Gross Pay    : " + secondCutoffGross);

        // Deductions are computed from the combined monthly total and shown once.
        System.out.println("\nMonthly Summary");
        System.out.println("  Total Gross Salary : " + totalMonthlyGross);
        System.out.println("  Deductions:");
        System.out.println("    SSS             : " + sss);
        System.out.println("    PhilHealth      : " + philHealth);
        System.out.println("    Pag-IBIG        : " + pagIbig);
        System.out.println("    Withholding Tax : " + withholdingTax);
        System.out.println("  Net Salary         : " + netSalary);
    }

    // Computes all payroll values for one month and returns them as an array.
    // [0] first cutoff gross, [1] second cutoff gross, [2] total monthly gross,
    // [3] SSS, [4] PhilHealth, [5] Pag-IBIG, [6] withholding tax, [7] net salary.
    
    // Add the 1st and 2nd cutoff amounts first before computing any deductions. All deductions are based on the combined total.

    public static double[] calculateMonthlyPayroll(double firstCutoffHours, double secondCutoffHours, double hourlyRate) {
        double firstCutoffGross  = firstCutoffHours * hourlyRate;
        double secondCutoffGross = secondCutoffHours * hourlyRate;

        // Combine both cutoff amounts before computing any deductions.
        double totalMonthlyGross = firstCutoffGross + secondCutoffGross;

        // Compute all government deductions from the combined monthly gross.
        double sss        = (totalMonthlyGross > 0) ? getSSSContribution(totalMonthlyGross) : 0.0;
        double philHealth = (totalMonthlyGross > 0) ? totalMonthlyGross * 0.02 : 0.0;
        double pagIbig    = (totalMonthlyGross > 0) ? 100.0 : 0.0;

        // Compute withholding tax on income after mandatory deductions.
        double totalDeductions = sss + philHealth + pagIbig;
        double taxableIncome   = totalMonthlyGross - totalDeductions;
        double withholdingTax  = (taxableIncome > 0) ? getWithholdingTax(taxableIncome) : 0.0;

        // Net salary is what remains after all deductions and tax.
        double netSalary = totalMonthlyGross - totalDeductions - withholdingTax;

        return new double[] {
            firstCutoffGross,
            secondCutoffGross,
            totalMonthlyGross,
            sss,
            philHealth,
            pagIbig,
            withholdingTax,
            netSalary
        };
    }

    // Calculates hours worked for a single day.

    // Business rules:
    //   - Only count time between 8:00 AM and 5:00 PM. Extra hours are ignored.
    //   - Grace period: if employee logs in between 8:01 and 8:10, treat as 8:00.
    //   - Deduct 1 hour for the mandatory lunch break.

    public static double calculateHoursWorked(String timeIn, String timeOut) {
        String[] timeInParts  = timeIn.split(":");
        String[] timeOutParts = timeOut.split(":");

        int inHour    = Integer.parseInt(timeInParts[0].trim());
        int inMinute  = Integer.parseInt(timeInParts[1].trim());
        int outHour   = Integer.parseInt(timeOutParts[0].trim());
        int outMinute = Integer.parseInt(timeOutParts[1].trim());

        // Business rule: 8:01–8:10 AM is within the grace period; treat as exactly 8:00.
        if (inHour == 8 && inMinute >= 1 && inMinute <= 10) {
            inMinute = 0;
        }

        // Early arrival: work hours only begin at 8:00 AM regardless of actual log-in.
        if (inHour < 8) {
            inHour   = 8;
            inMinute = 0;
        }

        // Cap log-out: work hours stop counting at 5:00 PM even if employee stays later.
        if (outHour > 17 || (outHour == 17 && outMinute > 0)) {
            outHour   = 17;
            outMinute = 0;
        }

        double startTime = inHour  + (inMinute  / 60.0);
        double endTime   = outHour + (outMinute / 60.0);

        // Subtract 1 hour for the mandatory lunch break.
        double totalHours = (endTime - startTime) - 1.0;

        // Prevent negative values from extremely short or invalid records.
        return Math.max(totalHours, 0.0);
    }

  
    // Returns the monthly SSS contribution based on the combined gross salary.
    
    public static double getSSSContribution(double grossSalary) {
        if (grossSalary < 10000) {
            return 450.0;
        } else if (grossSalary < 20000) {
            return 900.0;
        } else {
            return 1125.0;
        }
    }

    // compute withholding tax

    public static double getWithholdingTax(double taxableIncome) {
        if (taxableIncome <= 20832) {
            return 0.0;
        } else if (taxableIncome <= 33333) {
            return (taxableIncome - 20832) * 0.20;
        } else if (taxableIncome <= 66667) {
            return 2500.0 + (taxableIncome - 33333) * 0.25;
        } else if (taxableIncome <= 166667) {
            return 10833.0 + (taxableIncome - 66667) * 0.30;
        } else if (taxableIncome <= 666667) {
            return 40833.33 + (taxableIncome - 166667) * 0.32;
        } else {
            return 200833.33 + (taxableIncome - 666667) * 0.35;
        }
    }

    // Prints the employee header block shown before each employee's payroll data.
    // Accepts plain String and double parameters

    public static void printEmployeeHeader(String employeeNumber, String lastName, String firstName, String birthday) {
        System.out.println("\n===================================");
        System.out.println("Employee #    : " + employeeNumber);
        System.out.println("Employee Name : " + lastName + ", " + firstName);
        System.out.println("Birthday      : " + birthday);
        System.out.println("===================================");
    }

    // Splits one line of CSV text into an array of field values.
    // Commas that appear inside quoted fields are not treated as column separators.

    public static String[] splitCSV(String line) {
        StringBuilder currentValue = new StringBuilder();
        boolean insideQuotes = false;
        int commaCount = 0;

        // First pass: count unquoted commas to determine the number of columns.
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                insideQuotes = !insideQuotes;
            } else if (c == ',' && !insideQuotes) {
                commaCount++;
            }
        }

        String[] values = new String[commaCount + 1];
        int valueIndex  = 0;
        insideQuotes    = false;

        // Second pass: build each field value, treating quote characters as delimiters only.
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                insideQuotes = !insideQuotes;
            } else if (c == ',' && !insideQuotes) {
                values[valueIndex] = currentValue.toString();
                valueIndex++;
                currentValue.setLength(0);
            } else {
                currentValue.append(c);
            }
        }

        values[valueIndex] = currentValue.toString();
        return values;
    }

    // Returns the full month name for a given month number (1 = January, etc.).

    public static String getMonthName(int month) {
        String[] names = {
            "", "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        };
        return names[month];
    }

    // Returns the last calendar day for a given month number.
    // February is fixed at 28 days — leap year handling is out of scope.

    public static int getLastDayOfMonth(int month) {
        if (month == 2) {
            return 28;
        } else if (month == 4 || month == 6 || month == 9 || month == 11) {
            return 30;
        } else {
            return 31;
        }
    }

    // Removes any surrounding double-quote characters from a CSV field value.

    public static String removeQuotes(String value) {
        return value.replace("\"", "");
    }

    // Parses a numeric string that may contain quotes and comma separators
    // (e.g. "1,250.00") into a plain double value.
    
    public static double parseAmount(String value) {
        return Double.parseDouble(removeQuotes(value).replace(",", ""));
    }
}
