package com.mealsynx.service;

import com.mealsynx.interfaces.SimulationStrategy;
import com.mealsynx.model.FoodItem;
import com.mealsynx.model.SimulationResult;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Upgraded Strategy-Pattern What-If Simulator for MealSynx V2.
 * Supports:
 *  1. Filtering by specific Canteen (CAN-1, CAN-2, CAN-3, or ALL) AND specific Food Item (or ALL).
 *  2. Natural Language Question Parser that automatically extracts numbers, canteen IDs, and dish names
 *     from structured What-If questions.
 */
public class SimulationEngine {

    public static class EventSurgeStrategy implements SimulationStrategy {
        @Override
        public String getScenarioName() { return "EVENT_SURGE"; }

        @Override
        public SimulationResult simulate(Map<String, Object> params) {
            String targetCanteen = (String) params.getOrDefault("canteenId", "ALL");
            String dishName = (String) params.getOrDefault("dishName", "ALL");
            int extraStudents = ((Number) params.getOrDefault("extraStudents", 750)).intValue();
            int attendancePct = ((Number) params.getOrDefault("attendancePct", 88)).intValue();
            int durationHrs = ((Number) params.getOrDefault("durationHrs", 4)).intValue();

            double factor = (extraStudents / 700.0) * (attendancePct / 85.0);
            int vegSurge = (int) Math.round(22 * factor);
            int nonVegSurge = (int) Math.round(29 * factor);
            int grillsDrinksSurge = (int) Math.round(37 * factor);

            String scopeLabel = targetCanteen + (dishName != null && !"ALL".equalsIgnoreCase(dishName) ? " | Dish: " + dishName : "");
            SimulationResult res = new SimulationResult(
                    "Event & Crowd Surge Simulation [" + scopeLabel + "]",
                    String.format("+%d Additional Students | %d%% Campus Attendance | %d hrs Event Window",
                            extraStudents, attendancePct, durationHrs)
            );

            if (dishName != null && !"ALL".equalsIgnoreCase(dishName)) {
                int dishSurgePct = (int) Math.round(32 * factor);
                int extraPlates = (int) Math.round(135 * factor);
                res.addCategoryDelta("Targeted Dish (" + dishName + ") Demand", "+" + dishSurgePct + "% surge (+" + extraPlates + " plates)");
                res.addPrepAdjustment(dishName + " (" + targetCanteen + ")", extraPlates);
                res.addIngredientRisk(dishName + " Primary Ingredients", "⚠ Pre-stage +" + Math.max(10, (int) Math.round(extraPlates * 0.14)) + " kg raw stock");
                res.addRecommendation("Pre-batch +" + extraPlates + " plates of " + dishName + " 45 mins prior to peak hour.");
            } else {
                if ("ALL".equalsIgnoreCase(targetCanteen) || "CAN-1".equalsIgnoreCase(targetCanteen)) {
                    res.addCategoryDelta("Canteen 1 (Pure Veg Meals & Tiffin)", "+" + vegSurge + "% demand surge");
                    res.addPrepAdjustment("Executive Veg Meals (CAN-1)", (int) Math.round(110 * factor));
                }
                if ("ALL".equalsIgnoreCase(targetCanteen) || "CAN-2".equalsIgnoreCase(targetCanteen)) {
                    res.addCategoryDelta("Canteen 2 (Non-Veg Biryani & Starters)", "+" + nonVegSurge + "% demand surge");
                    res.addPrepAdjustment("Ambur Chicken Dum Biryani (CAN-2)", (int) Math.round(145 * factor));
                }
                if ("ALL".equalsIgnoreCase(targetCanteen) || "CAN-3".equalsIgnoreCase(targetCanteen)) {
                    res.addCategoryDelta("Canteen 3 (Fast Food, Grills & Cold Drinks)", "+" + grillsDrinksSurge + "% demand surge");
                    res.addPrepAdjustment("Smoky BBQ Grill Wrap & Frappe (CAN-3)", (int) Math.round(160 * factor));
                }
                res.addIngredientRisk("Seeraga Samba Rice & Sona Masoori", "Medium Risk (Buffer +18 kg)");
                res.addIngredientRisk("Fresh Broiler Chicken & Grill Strips", "⚠ HIGH SHORTAGE RISK (+24 kg needed)");
                res.addIngredientRisk("Citrus Fruit & Cold Frappe Base", "⚠ SHORTAGE RISK (+20 L needed)");
                res.addRecommendation("Alert Canteen 2 & Canteen 3 to pre-marinate +25 kg poultry batch before 10:30 AM.");
            }
            res.addRecommendation("Redirect walk-in beverage & snack orders to Canteen 3 to balance queue load.");
            res.setProjectedRevenueDeltaPercent(Math.round(26.4 * factor * 10.0) / 10.0);
            res.setProjectedWasteDeltaPercent(-12.5);
            return res;
        }
    }

    public static class AttendanceDropStrategy implements SimulationStrategy {
        @Override
        public String getScenarioName() { return "ATTENDANCE_DROP"; }

        @Override
        public SimulationResult simulate(Map<String, Object> params) {
            String targetCanteen = (String) params.getOrDefault("canteenId", "ALL");
            String dishName = (String) params.getOrDefault("dishName", "ALL");
            int fromPct = ((Number) params.getOrDefault("fromAttendance", 85)).intValue();
            int toPct = ((Number) params.getOrDefault("toAttendance", 60)).intValue();
            double dropRatio = Math.max(0.05, (fromPct - toPct) / (double) Math.max(1, fromPct));
            int dropPct = (int) Math.round(dropRatio * 100);

            String scopeLabel = targetCanteen + (dishName != null && !"ALL".equalsIgnoreCase(dishName) ? " | Dish: " + dishName : "");
            SimulationResult res = new SimulationResult(
                    "Attendance Drop / Exam / Holiday Simulation [" + scopeLabel + "]",
                    String.format("Campus Attendance drops from %d%% → %d%% (-%d%% footfall)", fromPct, toPct, dropPct)
            );
            res.addCategoryDelta("Overall Meal Demand (" + scopeLabel + ")", "-" + dropPct + "% ↓");
            res.addCategoryDelta("Recommended Kitchen Batch Reduction", "-" + (dropPct + 3) + "% ↓");
            res.addCategoryDelta("Prevented Overproduction Waste", String.format("%.1f kg food saved ↓", dropRatio * 95.0));

            res.addIngredientRisk("Perishable Batter & Paneer (CAN-1)", "Hold 30% in chiller for next day");
            res.addIngredientRisk("Fresh Poultry & Fish (CAN-2)", "Trim morning procurement by " + dropPct + "%");

            if (dishName != null && !"ALL".equalsIgnoreCase(dishName)) {
                int cutPlates = -(int) Math.round(210 * dropRatio);
                res.addPrepAdjustment(dishName + " (" + targetCanteen + ")", cutPlates);
                res.addRecommendation("Scale back " + dishName + " batch by " + Math.abs(cutPlates) + " plates and switch to cooked-to-order.");
            } else {
                res.addPrepAdjustment("Executive Veg Meals (CAN-1)", -(int) Math.round(260 * dropRatio));
                res.addPrepAdjustment("Ambur Chicken Dum Biryani (CAN-2)", -(int) Math.round(310 * dropRatio));
                res.addPrepAdjustment("Fast Food & Grills (CAN-3)", -(int) Math.round(220 * dropRatio));
            }

            res.addRecommendation("Switch Canteens to cooked-to-order batching after 1:15 PM.");
            res.addRecommendation("Alert canteen workers to freeze extra batter & grill marinades immediately.");
            res.setProjectedRevenueDeltaPercent(-dropPct);
            res.setProjectedWasteDeltaPercent(-42.0);
            return res;
        }
    }

    public static class PriceElasticityStrategy implements SimulationStrategy {
        @Override
        public String getScenarioName() { return "PRICE_ELASTICITY"; }

        @Override
        public SimulationResult simulate(Map<String, Object> params) {
            String targetCanteen = (String) params.getOrDefault("canteenId", "ALL");
            String dishName = (String) params.getOrDefault("dishName", "Ambur Chicken Dum Biryani");
            if (dishName == null || "ALL".equalsIgnoreCase(dishName) || dishName.isBlank()) {
                dishName = "Ambur Chicken Dum Biryani";
            }
            double oldPrice = ((Number) params.getOrDefault("oldPrice", 130.0)).doubleValue();
            double newPrice = ((Number) params.getOrDefault("newPrice", 150.0)).doubleValue();

            double priceChangePct = ((newPrice - oldPrice) / Math.max(1.0, oldPrice)) * 100.0;
            double demandChangePct = -0.58 * priceChangePct;
            int baseQty = 315;
            int newQty = (int) Math.round(baseQty * (1.0 + demandChangePct / 100.0));
            double oldRev = baseQty * oldPrice;
            double newRev = newQty * newPrice;
            double revDeltaPct = ((newRev - oldRev) / oldRev) * 100.0;

            SimulationResult res = new SimulationResult(
                    "Menu Price Elasticity Simulation — " + dishName + " [" + targetCanteen + "]",
                    String.format("Price changed from ₹%.0f → ₹%.0f (%+.1f%% | Elasticity ε = -0.58)", oldPrice, newPrice, priceChangePct)
            );
            res.addCategoryDelta("Projected Daily Plates Sold (" + dishName + ")", String.format("%d → %d plates (%+.1f%%)", baseQty, newQty, demandChangePct));
            res.addCategoryDelta("Projected Daily Dish Revenue", String.format("₹%.0f → ₹%.0f (%+.1f%%)", oldRev, newRev, revDeltaPct));
            res.addCategoryDelta("Unit Profit Margin Impact", priceChangePct >= 0 ? "Margin Increases ↑ (Watch volume dip)" : "Volume Increases ↑ (Student Value Boost)");

            res.addIngredientRisk(dishName + " Raw Stock", (newQty - baseQty) >= 0
                    ? "Procure +" + Math.max(4, (int) Math.round((newQty - baseQty) * 0.14)) + " kg extra stock"
                    : "Trim daily order by " + Math.max(3, (int) Math.round(Math.abs(newQty - baseQty) * 0.14)) + " kg");
            res.addPrepAdjustment(dishName, newQty - baseQty);
            res.addRecommendation(String.format("Adjust %s daily kitchen plan by %+d plates to avoid overproduction waste.", dishName, newQty - baseQty));
            res.addRecommendation("Bundle with a ₹30 beverage combo to preserve student order frequency.");
            res.setProjectedRevenueDeltaPercent(Math.round(revDeltaPct * 10.0) / 10.0);
            res.setProjectedWasteDeltaPercent(-9.0);
            return res;
        }
    }

    private final Map<String, SimulationStrategy> strategies = new LinkedHashMap<>();

    public SimulationEngine() {
        register(new EventSurgeStrategy());
        register(new AttendanceDropStrategy());
        register(new PriceElasticityStrategy());
    }

    private void register(SimulationStrategy s) {
        strategies.put(s.getScenarioName(), s);
    }

    public SimulationResult runSimulation(String key, Map<String, Object> params) {
        SimulationStrategy s = strategies.getOrDefault(key, strategies.get("EVENT_SURGE"));
        return s.simulate(params);
    }

    /**
     * V2 Natural Language What-If Question Parser:
     * Supported question formats:
     *  - "What if +600 extra students at CAN-2 for Ambur Chicken Dum Biryani?"
     *  - "What if attendance drops from 90% to 65% at CAN-1?"
     *  - "What if price of Ghee Roast Masala Dosa changes from 65 to 80?"
     */
    public SimulationResult runNaturalLanguageSimulation(String questionText, List<FoodItem> allMenuItems) {
        String q = questionText == null ? "" : questionText.trim();
        String lower = q.toLowerCase();

        // Detect Canteen
        String canteenId = "ALL";
        Matcher cm = Pattern.compile("\\b(can-[0-9a-z]+|canteen\\s*[123])\\b", Pattern.CASE_INSENSITIVE).matcher(q);
        if (cm.find()) {
            String raw = cm.group(1).toUpperCase().replaceAll("\\s+", "");
            if (raw.startsWith("CANTEEN")) {
                canteenId = "CAN-" + raw.substring("CANTEEN".length());
            } else {
                canteenId = raw;
            }
        }

        // Detect Dish Name from catalogue
        String matchedDish = "ALL";
        if (allMenuItems != null) {
            for (FoodItem item : allMenuItems) {
                if (lower.contains(item.getName().toLowerCase())) {
                    matchedDish = item.getName();
                    if ("ALL".equals(canteenId)) canteenId = item.getCanteenId();
                    break;
                }
            }
            if ("ALL".equals(matchedDish)) {
                for (FoodItem item : allMenuItems) {
                    for (String token : item.getName().split("\\s+")) {
                        if (token.length() >= 5 && lower.contains(token.toLowerCase())) {
                            matchedDish = item.getName();
                            if ("ALL".equals(canteenId)) canteenId = item.getCanteenId();
                            break;
                        }
                    }
                    if (!"ALL".equals(matchedDish)) break;
                }
            }
        }

        // Extract numbers in question
        List<Double> nums = new ArrayList<>();
        Matcher nm = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)").matcher(q.replaceAll("(?i)can-[0-9]+|canteen\\s*[0-9]+", ""));
        while (nm.find()) {
            try { nums.add(Double.parseDouble(nm.group(1))); } catch (Exception ignored) {}
        }

        Map<String, Object> params = new HashMap<>();
        params.put("canteenId", canteenId);
        params.put("dishName", matchedDish);

        if (lower.contains("price") || lower.contains("cost") || lower.contains("₹") || lower.contains("rs")) {
            double oldP = nums.size() >= 1 ? nums.get(0) : 120.0;
            double newP = nums.size() >= 2 ? nums.get(1) : oldP * 1.15;
            params.put("oldPrice", oldP);
            params.put("newPrice", newP);
            return runSimulation("PRICE_ELASTICITY", params);
        } else if (lower.contains("drop") || lower.contains("exam") || lower.contains("holiday") || lower.contains("fall") || lower.contains("rain")) {
            int fromAtt = nums.size() >= 1 ? nums.get(0).intValue() : 90;
            int toAtt = nums.size() >= 2 ? nums.get(1).intValue() : Math.max(40, fromAtt - 25);
            if (fromAtt < toAtt) { int tmp = fromAtt; fromAtt = toAtt; toAtt = tmp; }
            params.put("fromAttendance", fromAtt);
            params.put("toAttendance", toAtt);
            return runSimulation("ATTENDANCE_DROP", params);
        } else {
            int extra = nums.size() >= 1 ? nums.get(0).intValue() : 600;
            int att = nums.size() >= 2 ? nums.get(1).intValue() : 88;
            params.put("extraStudents", extra);
            params.put("attendancePct", att);
            return runSimulation("EVENT_SURGE", params);
        }
    }
}
