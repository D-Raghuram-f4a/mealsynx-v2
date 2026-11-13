package com.mealsynx.service;

import com.mealsynx.dao.CustomerDAO;
import com.mealsynx.dao.EmployeeDAO;
import com.mealsynx.db.PersistentDataStore;
import com.mealsynx.exception.MealSynxException;
import com.mealsynx.model.Admin;
import com.mealsynx.model.Customer;
import com.mealsynx.model.User;
import com.mealsynx.model.UserRole;
import com.mealsynx.model.Worker;

import java.util.List;
import java.util.Optional;

public class AuthService {
    public static final String[] SECURITY_QUESTIONS = {
        "What is your favourite colour?",
        "What is your favourite animal?",
        "What is your favourite movie?",
        "Who is your favourite actor?",
        "In which city were you born?"
    };

    private final CustomerDAO customerDAO;
    private final EmployeeDAO employeeDAO;
    private User currentLoggedInUser;

    public AuthService(CustomerDAO customerDAO, EmployeeDAO employeeDAO) {
        this.customerDAO = customerDAO;
        this.employeeDAO = employeeDAO;
    }

    public Optional<User> loginWithRole(UserRole selectedRole, String usernameInput, String passwordInput) {
        if (usernameInput == null || usernameInput.isBlank() || passwordInput == null || passwordInput.isBlank()) {
            return Optional.empty();
        }
        String userTrim = usernameInput.trim();
        String passTrim = passwordInput.trim();

        if (selectedRole == UserRole.STUDENT) {
            Optional<Customer> custOpt = customerDAO.findByUsernameOrIdentifier(userTrim);
            if (custOpt.isPresent()) {
                Customer c = custOpt.get();
                if (c.verifyPassword(passTrim)) {
                    currentLoggedInUser = c;
                    return Optional.of(c);
                }
            }
        } else if (selectedRole == UserRole.WORKER) {
            Optional<Worker> wrkOpt = employeeDAO.findWorker(userTrim);
            if (wrkOpt.isPresent()) {
                Worker w = wrkOpt.get();
                if (w.verifyPassword(passTrim)) {
                    currentLoggedInUser = w;
                    return Optional.of(w);
                }
            }
        } else if (selectedRole == UserRole.ADMIN) {
            Optional<Admin> admOpt = employeeDAO.findAdmin(userTrim);
            if (admOpt.isPresent()) {
                Admin a = admOpt.get();
                if (a.verifyPassword(passTrim)) {
                    currentLoggedInUser = a;
                    return Optional.of(a);
                }
            }
        }
        return Optional.empty();
    }

    public Customer registerNewStudent(String username, String fullName, String registerNo,
                                       String email, String password, double initialWalletBalanceIgnored,
                                       String favouriteCategory) {
        return registerNewStudentWithSecurity(username, fullName, registerNo, email, password,
                favouriteCategory, SECURITY_QUESTIONS[0], "blue");
    }

    public Customer registerNewStudentWithSecurity(String username, String fullName, String registerNo,
                                                   String email, String password, String favouriteCategory,
                                                   String securityQuestion, String securityAnswer) {
        String cleanUser = username == null ? "" : username.trim();
        if (cleanUser.isEmpty()) {
            throw new MealSynxException("AUTH_EMPTY_USER", "Username cannot be empty.");
        }
        if (password == null || password.trim().length() < 4) {
            throw new MealSynxException("AUTH_WEAK_PASS", "Password must be at least 4 characters.");
        }
        if (securityAnswer == null || securityAnswer.trim().isEmpty()) {
            throw new MealSynxException("AUTH_EMPTY_SEC_ANS", "Security answer is required for account recovery.");
        }

        Optional<Customer> existing = customerDAO.findByUsernameOrIdentifier(cleanUser);
        String newId = existing.map(Customer::getUserId).orElse("STU-00" + (customerDAO.findAll().size() + 1));

        double startingWallet = 0.00;
        Customer newStudent = new Customer(
                newId,
                cleanUser,
                (fullName == null || fullName.isBlank()) ? cleanUser : fullName.trim(),
                (registerNo == null || registerNo.isBlank()) ? ("RA25110" + (100 + customerDAO.findAll().size())) : registerNo.trim(),
                (email == null || email.isBlank()) ? (cleanUser.toLowerCase().replaceAll("\\s+", "") + "@campus.edu") : email.trim(),
                password.trim(),
                startingWallet,
                (favouriteCategory == null || favouriteCategory.isBlank()) ? "Fast Food & Grills" : favouriteCategory,
                "12:30 PM – 1:30 PM",
                0.00,
                securityQuestion,
                securityAnswer.trim()
        );
        newStudent.pushNotification("🔔 Welcome to MealSynx, " + cleanUser + "! Your account has been created and saved permanently.");
        customerDAO.save(newStudent);
        PersistentDataStore.getInstance().saveToDisk();
        currentLoggedInUser = newStudent;
        return newStudent;
    }

    public Optional<String> getStudentSecurityQuestion(String usernameOrId) {
        return customerDAO.findByUsernameOrIdentifier(usernameOrId).map(Customer::getSecurityQuestion);
    }

    public boolean resetStudentPasswordWithSecurityAnswer(String usernameOrId, String answerInput, String newPassword) {
        if (newPassword == null || newPassword.trim().length() < 4) {
            throw new MealSynxException("AUTH_WEAK_PASS", "New password must be at least 4 characters.");
        }
        Optional<Customer> stuOpt = customerDAO.findByUsernameOrIdentifier(usernameOrId);
        if (stuOpt.isEmpty()) {
            throw new MealSynxException("AUTH_NOT_FOUND", "No student account found for '" + usernameOrId + "'.");
        }
        Customer stu = stuOpt.get();
        if (!stu.verifySecurityAnswer(answerInput)) {
            throw new MealSynxException("AUTH_BAD_SEC_ANS", "Incorrect security answer! Password reset denied.");
        }
        stu.updatePasswordWithHash(newPassword.trim());
        stu.pushNotification("🔔 Your password was reset using your Security Question.");
        customerDAO.save(stu);
        PersistentDataStore.getInstance().saveToDisk();
        return true;
    }

    public Worker addNewWorkerByAdmin(String username, String fullName, String password,
                                      String assignedCanteenId, String shiftTiming, String specialization) {
        return addNewWorkerByAdmin(username, fullName, password, assignedCanteenId, shiftTiming, specialization, 18000.00);
    }

    public Worker addNewWorkerByAdmin(String username, String fullName, String password,
                                      String assignedCanteenId, String shiftTiming, String specialization,
                                      double monthlySalary) {
        String cleanUser = username.trim();
        String finalPass = (password == null || password.isBlank()) ? (cleanUser + "123*") : password.trim();
        int idx = employeeDAO.findAllWorkers().size() + 101;
        String workerId = "WRK-" + idx;
        String empCode = "EMP-" + assignedCanteenId.replace("-", "") + "-" + idx;

        Worker worker = new Worker(
                workerId, cleanUser,
                (fullName == null || fullName.isBlank()) ? cleanUser : fullName.trim(),
                empCode,
                cleanUser.toLowerCase().replaceAll("\\s+", "") + "@mealsynx.edu",
                finalPass,
                assignedCanteenId,
                (shiftTiming == null || shiftTiming.isBlank()) ? "09:00 AM – 05:00 PM" : shiftTiming.trim(),
                (specialization == null || specialization.isBlank()) ? "Kitchen & Counter Operations" : specialization.trim(),
                monthlySalary > 0 ? monthlySalary : 18000.00,
                null
        );
        worker.pushNotification("🔔 Welcome " + cleanUser + "! Assigned to " + assignedCanteenId + " (Monthly Salary: ₹" + String.format("%,.2f", worker.getMonthlySalary()) + ").");
        Worker saved = employeeDAO.saveWorker(worker);
        PersistentDataStore.getInstance().saveToDisk();
        return saved;
    }

    public void saveUpdatedWorker(Worker worker) {
        employeeDAO.saveWorker(worker);
        PersistentDataStore.getInstance().saveToDisk();
    }

    public boolean removeWorkerByAdmin(String workerIdOrUsername) {
        return employeeDAO.deleteWorker(workerIdOrUsername);
    }

    public Admin addNewAdminByAdmin(String username, String fullName, String password, String department) {
        String cleanUser = username.trim();
        String finalPass = (password == null || password.isBlank()) ? (cleanUser + "123*") : password.trim();
        int idx = employeeDAO.findAllAdmins().size() + 1;
        String adminId = "ADM-00" + idx;
        String admCode = "ADM-CENTRAL-0" + idx;

        Admin newAdmin = new Admin(
                adminId, cleanUser,
                (fullName == null || fullName.isBlank()) ? cleanUser : fullName.trim(),
                admCode,
                cleanUser.toLowerCase().replaceAll("\\s+", "") + "@mealsynx.edu",
                finalPass,
                (department == null || department.isBlank()) ? "Campus Food Intelligence & Operations" : department.trim()
        );
        newAdmin.pushNotification("🔔 Admin account created for " + cleanUser + ".");
        Admin saved = employeeDAO.saveAdmin(newAdmin);
        PersistentDataStore.getInstance().saveToDisk();
        return saved;
    }

    public List<Worker> getAllWorkers() { return employeeDAO.findAllWorkers(); }
    public List<Admin> getAllAdmins() { return employeeDAO.findAllAdmins(); }
    public void logout() { this.currentLoggedInUser = null; }
    public User getCurrentLoggedInUser() { return currentLoggedInUser; }
}
