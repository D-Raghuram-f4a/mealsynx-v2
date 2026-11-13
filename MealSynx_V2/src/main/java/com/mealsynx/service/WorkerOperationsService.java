package com.mealsynx.service;

import com.mealsynx.dao.*;
import com.mealsynx.db.PersistentDataStore;
import com.mealsynx.model.*;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Worker Operations, Monthly Payroll Automation & NLP Improvement Advisor for MealSynx V2.
 * Includes:
 *  - Automatic Monthly Salary Reminder Notifications & One-Click Salary Payout (updates Canteen Worker Wages & Net Profit)
 *  - Per-Canteen NLP & Operational Improvement Advisor (combines Student NLP Feedback, Kitchen Waste Mistakes, and Expiring Stock)
 */
public class WorkerOperationsService {
    private final CanteenDAO canteenDAO;
    private final EmployeeDAO employeeDAO;
    private final NotificationService notificationService;
    private final PersistentDataStore store = PersistentDataStore.getInstance();

    public WorkerOperationsService(CanteenDAO canteenDAO, EmployeeDAO employeeDAO,
                                   NotificationService notificationService) {
        this.canteenDAO = canteenDAO;
        this.employeeDAO = employeeDAO;
        this.notificationService = notificationService;
    }

    public Optional<Canteen> getCanteen(String canteenId) {
        return canteenDAO.findById(canteenId);
    }

    public void recordWagesOrUtilitiesExpense(String canteenId, String expenseType, double amount) {
        canteenDAO.findById(canteenId).ifPresent(canteen -> {
            if ("WORKER_WAGES".equalsIgnoreCase(expenseType)) {
                canteen.addWorkerWagesExpense(amount);
            } else {
                canteen.addUtilitiesExpense(amount);
            }
            canteenDAO.save(canteen);
            if (canteen.isRunningInLoss()) {
                notificationService.notifyWorkerForCanteen(employeeDAO, canteenId,
                        String.format("🚨 LOSS WARNING: %s is running in a Net LOSS of ₹%.2f (Revenue: ₹%.2f vs Total Expenses: ₹%.2f)!",
                                canteen.getName(), Math.abs(canteen.getNetProfitOrLoss()),
                                canteen.getTotalSalesRevenue(), canteen.getTotalExpenses()));
            }
            store.saveToDisk();
        });
    }

    /**
     * V2 Requirement #7: Automatic Monthly Salary Reminder & Payout Calculation.
     * Checks all active workers for the current month (YYYY-MM) and pushes reminder notifications to Admins.
     */
    public List<String> checkAndNotifyMonthlySalaryReminders(YearMonth month) {
        String ym = month.toString();
        List<String> dueList = new ArrayList<>();
        double totalDue = 0.0;

        for (Worker w : employeeDAO.findAllWorkers()) {
            if (w.isSalaryDueForMonth(ym)) {
                String line = String.format("Salary Due [%s]: %s (%s | %s) — ₹%,.2f",
                        ym, w.getUsername(), w.getFullName(), w.getAssignedCanteenId(), w.getMonthlySalary());
                dueList.add(line);
                totalDue += w.getMonthlySalary();
            }
        }

        if (!dueList.isEmpty()) {
            String reminderMsg = String.format("🔔 [MONTHLY PAYROLL REMINDER — %s] %d worker(s) pending salary payout totaling ₹%,.2f. Credit salaries in Admin Portal -> Add Workers & Payroll or Stats & Performance.",
                    ym, dueList.size(), totalDue);
            for (Admin a : employeeDAO.findAllAdmins()) {
                boolean alreadyNotified = a.getNotifications().stream().anyMatch(n -> n.contains("[MONTHLY PAYROLL REMINDER — " + ym + "]"));
                if (!alreadyNotified) {
                    a.pushNotification(reminderMsg);
                }
            }
            store.saveToDisk();
        }
        return dueList;
    }

    /**
     * Credits monthly salary to all due workers for the given month, records the wage expense
     * against their assigned canteen (directly impacting Canteen Net Profit), and notifies each worker.
     */
    public synchronized double creditDueMonthlySalaries(YearMonth month, PaymentDAO paymentDAO) {
        String ym = month.toString();
        double totalCredited = 0.0;

        for (Worker w : employeeDAO.findAllWorkers()) {
            if (w.isSalaryDueForMonth(ym)) {
                double sal = w.getMonthlySalary();
                w.setLastSalaryPaidMonth(ym);
                w.pushNotification(String.format("💰 [SALARY CREDITED — %s] Your monthly salary of ₹%,.2f for %s has been credited by Admin!",
                        ym, sal, w.getAssignedCanteenId()));
                employeeDAO.saveWorker(w);

                canteenDAO.findById(w.getAssignedCanteenId()).ifPresent(c -> {
                    c.addWorkerWagesExpense(sal);
                    canteenDAO.save(c);
                });

                if (paymentDAO != null) {
                    paymentDAO.recordTransaction("SAL-" + ym + "-" + w.getUserId(), w.getUsername(),
                            "MONTHLY-SALARY-" + ym, sal, "ADMIN_PAYROLL_TRANSFER");
                }
                totalCredited += sal;
            }
        }

        if (totalCredited > 0) {
            for (Admin a : employeeDAO.findAllAdmins()) {
                a.pushNotification(String.format("✅ [PAYROLL COMPLETED — %s] Credited ₹%,.2f in monthly salaries across canteen workers; Canteen Net Profit updated.", ym, totalCredited));
            }
            store.saveToDisk();
        }
        return totalCredited;
    }

    /**
     * V2 Requirement #9: Per-Canteen Worker NLP & Operational Improvement Advisor.
     * Synthesizes actionable suggestions from:
     *  1. Student NLP feedback (poor taste, too spicy/mild, low portion, hygiene/service complaints)
     *  2. Kitchen waste & mistake logs (preparation errors, spoilage, overproduction)
     *  3. Warehouse low-stock or expiring ingredients
     */
    public List<String> generateWorkerNlpImprovementSuggestions(String canteenId) {
        List<String> suggestions = new ArrayList<>();

        // 1. Analyze Student NLP Reviews for this Canteen
        List<FeedbackDAO.FeedbackEntry> reviews = store.data().feedbacks.stream()
                .filter(f -> canteenId.equalsIgnoreCase(f.canteenId))
                .collect(Collectors.toList());

        long badTasteCount = reviews.stream().filter(f -> f.tasteSentiment != null && (f.tasteSentiment.contains("Negative") || f.tasteSentiment.contains("Needs"))).count();
        long tooSpicyCount = reviews.stream().filter(f -> f.spicinessSentiment != null && f.spicinessSentiment.contains("Too Spicy")).count();
        long tooMildCount  = reviews.stream().filter(f -> f.spicinessSentiment != null && f.spicinessSentiment.contains("Too Mild")).count();
        long lowPortionCnt = reviews.stream().filter(f -> f.quantitySentiment != null && f.quantitySentiment.contains("Low Portion")).count();
        long hygieneCnt    = reviews.stream().filter(f -> f.hygieneSentiment != null && f.hygieneSentiment.contains("Negative")).count();

        if (badTasteCount > 0) {
            Map<String, Long> dishNegs = reviews.stream()
                    .filter(f -> f.tasteSentiment != null && (f.tasteSentiment.contains("Negative") || f.tasteSentiment.contains("Needs")))
                    .collect(Collectors.groupingBy(f -> f.foodName, Collectors.counting()));
            dishNegs.forEach((dish, cnt) -> suggestions.add(
                    String.format("🍲 [NLP Taste Alert — %s] %d student review(s) flagged poor/inconsistent taste. Standardize masala/seasoning ratios and check cooking temperature.", dish, cnt)));
        }
        if (tooSpicyCount > 0) {
            suggestions.add(String.format("🌶 [NLP Spice Calibration] %d review(s) reported dishes as 'Too Spicy'. Reduce red-chilli/pepper base by 15%% and offer spice add-ons separately.", tooSpicyCount));
        }
        if (tooMildCount > 0) {
            suggestions.add(String.format("🧂 [NLP Spice Calibration] %d review(s) noted bland/mild seasoning. Offer roasted pepper/chutney accompaniments at the counter.", tooMildCount));
        }
        if (lowPortionCnt > 0) {
            suggestions.add(String.format("⚖ [NLP Portion Control] %d review(s) flagged 'Low Portion'. Use standardized 250g/350g serving ladles during peak hours.", lowPortionCnt));
        }
        if (hygieneCnt > 0) {
            suggestions.add(String.format("🧼 [NLP Freshness & Hygiene] %d review(s) mentioned temperature/freshness issues. Serve directly from heated bain-marie trays (≥65°C).", hygieneCnt));
        }

        // 2. Analyze Kitchen Waste & Mistake Logs for this Canteen
        List<WasteRecord> wastes = store.data().wasteRecords.stream()
                .filter(w -> canteenId.equalsIgnoreCase(w.getCanteenId()))
                .collect(Collectors.toList());

        for (WasteRecord w : wastes) {
            if (w.getReason() == WasteRecord.WasteReason.PREPARATION_ERROR) {
                suggestions.add(String.format("👨‍🍳 [Kitchen Mistake Recovery — %s] %d plates (%.1f kg) wasted due to PREPARATION_ERROR. Conduct pre-shift recipe briefing and timer checks.",
                        w.getFoodName(), w.getWastedQty(), w.getWasteWeightKg()));
            } else if (w.getReason() == WasteRecord.WasteReason.OVERPRODUCTION && w.getWastedQty() >= 10) {
                suggestions.add(String.format("📉 [Batch Scaling — %s] %d plates (%.1f kg) wasted from OVERPRODUCTION. Trim initial morning batch by 12%% and switch to staggered batching after 1:15 PM.",
                        w.getFoodName(), w.getWastedQty(), w.getWasteWeightKg()));
            } else if (w.getReason() == WasteRecord.WasteReason.SPOILAGE || w.getReason() == WasteRecord.WasteReason.EXPIRED_INGREDIENTS) {
                suggestions.add(String.format("❄ [Cold-Chain & FIFO — %s] %.1f kg lost to %s. Enforce First-Expiry-First-Out (FEFO) in chiller storage.",
                        w.getFoodName(), w.getWasteWeightKg(), w.getReason()));
            }
        }

        // 3. Analyze Warehouse Stock & Expiry Risks for this Canteen
        for (IngredientStock s : store.data().inventory.values()) {
            if (canteenId.equalsIgnoreCase(s.getCanteenId())) {
                if (s.isQualityOrExpiryLow()) {
                    suggestions.add(String.format("⏰ [Expiry Action — %s] %.1f %s expires in %d day(s). Prioritize in today's special prep to prevent spoilage.",
                            s.getName(), s.getCurrentStock(), s.getUnit(), s.getDaysUntilExpiry()));
                } else if (s.isLowStock()) {
                    suggestions.add(String.format("📦 [Restock Advisory — %s] Current stock (%.1f %s) is near safety threshold. Recommended purchase: %.1f %s.",
                            s.getName(), s.getCurrentStock(), s.getUnit(), s.getRecommendedPurchaseQty(), s.getUnit()));
                }
            }
        }

        if (suggestions.isEmpty()) {
            suggestions.add("✅ All NLP student feedback, kitchen preparation metrics, and warehouse stock levels for " + canteenId + " are currently optimal!");
        }
        return suggestions;
    }

    public SimulationRequest submitWhatIfSimulationRequest(String workerUsername, String canteenId,
                                                           String scenarioType, String requestDetails) {
        String id = "SIMREQ-" + (101 + store.data().simulationRequests.size());
        SimulationRequest req = new SimulationRequest(id, workerUsername, canteenId, scenarioType, requestDetails);
        store.data().simulationRequests.add(0, req);
        for (Admin a : employeeDAO.findAllAdmins()) {
            a.pushNotification("🔔 [Worker Sim Request " + id + "] " + workerUsername + " (" + canteenId + ") requested " + scenarioType + ": " + requestDetails);
        }
        store.saveToDisk();
        return req;
    }

    public List<SimulationRequest> getAllSimulationRequests() {
        return new ArrayList<>(store.data().simulationRequests);
    }
}
