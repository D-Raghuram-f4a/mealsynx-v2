package com.mealsynx.model;

public class Worker extends User {
    private static final long serialVersionUID = 1L;
    private String assignedCanteenId;
    private String shiftTiming;
    private String specialization;
    private double monthlySalary;
    private String lastSalaryPaidMonth; // Format: YYYY-MM

    public Worker(String userId, String username, String fullName, String employeeCode,
                  String email, String password, String assignedCanteenId,
                  String shiftTiming, String specialization) {
        this(userId, username, fullName, employeeCode, email, password,
                assignedCanteenId, shiftTiming, specialization, 18000.00, null);
    }

    public Worker(String userId, String username, String fullName, String employeeCode,
                  String email, String password, String assignedCanteenId,
                  String shiftTiming, String specialization,
                  double monthlySalary, String lastSalaryPaidMonth) {
        super(userId, username, fullName, employeeCode, email, password, UserRole.WORKER);
        this.assignedCanteenId = assignedCanteenId;
        this.shiftTiming = shiftTiming;
        this.specialization = specialization;
        this.monthlySalary = monthlySalary > 0 ? monthlySalary : 18000.00;
        this.lastSalaryPaidMonth = lastSalaryPaidMonth;
    }

    @Override
    public String getDashboardTitle() {
        return "MealSynx Worker Portal — " + getUsername() + " [" + assignedCanteenId + "]";
    }

    public boolean isSalaryDueForMonth(String yearMonthStr) {
        return lastSalaryPaidMonth == null || !lastSalaryPaidMonth.equals(yearMonthStr);
    }

    public String getAssignedCanteenId() { return assignedCanteenId; }
    public void setAssignedCanteenId(String assignedCanteenId) { this.assignedCanteenId = assignedCanteenId; }
    public String getShiftTiming() { return shiftTiming; }
    public void setShiftTiming(String shiftTiming) { this.shiftTiming = shiftTiming; }
    public String getSpecialization() { return specialization; }
    public double getMonthlySalary() { return monthlySalary; }
    public void setMonthlySalary(double monthlySalary) { this.monthlySalary = monthlySalary; }
    public String getLastSalaryPaidMonth() { return lastSalaryPaidMonth; }
    public void setLastSalaryPaidMonth(String lastSalaryPaidMonth) { this.lastSalaryPaidMonth = lastSalaryPaidMonth; }
}
