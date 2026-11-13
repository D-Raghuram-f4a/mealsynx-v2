package com.mealsynx.service;

import com.mealsynx.dao.FeedbackDAO;
import com.mealsynx.dao.MenuDAO;
import com.mealsynx.dao.WasteDAO;
import com.mealsynx.db.PersistentDataStore;
import com.mealsynx.model.FoodItem;
import com.mealsynx.model.WasteRecord;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Closed-Loop Waste & Upgraded 5-Aspect NLP Sentiment Engine for MealSynx V2.
 * Supports submitting new reviews and author-only editing of existing reviews,
 * automatically recalculating NLP sentiments and dish live ratings.
 */
public class WasteService {
    private final WasteDAO wasteDAO;
    private final FeedbackDAO feedbackDAO;
    private final MenuDAO menuDAO;

    private static final String[] NEGATIVE_TASTE_PHRASES = {
        "bad taste", "poor taste", "worst taste", "bad in taste", "poor in taste", "terrible taste",
        "awful taste", "not tasty", "not good", "wasn't good", "wasnt good", "not fresh", "stale",
        "soggy", "burnt", "undercooked", "overcooked", "tasteless", "watery", "rancid", "horrible",
        "disgusting", "bad food", "poor quality", "bad quality", "cold food", "too oily", "too salty"
    };

    private static final String[] POSITIVE_TASTE_PHRASES = {
        "good", "great", "best", "tasty", "delicious", "authentic", "excellent", "awesome",
        "amazing", "yummy", "perfect", "crispy", "tender", "flavorful", "flavourful", "loved", "nice"
    };

    public WasteService(WasteDAO wasteDAO, FeedbackDAO feedbackDAO, MenuDAO menuDAO) {
        this.wasteDAO = wasteDAO;
        this.feedbackDAO = feedbackDAO;
        this.menuDAO = menuDAO;
    }

    public FeedbackDAO.FeedbackEntry submitStudentFoodReview(String studentUsername, FoodItem food,
                                                             int stars, String comment) {
        String[] nlp = computeFiveAspectNlp(stars, comment);
        FeedbackDAO.FeedbackEntry entry = new FeedbackDAO.FeedbackEntry(
                studentUsername, food.getCanteenId(), food.getFoodId(), food.getName(),
                stars, comment, nlp[0], nlp[1], nlp[2], nlp[3], nlp[4], nlp[5]
        );
        feedbackDAO.saveFeedback(entry);
        recalculateDishRating(food.getFoodId());
        PersistentDataStore.getInstance().saveToDisk();
        return entry;
    }

    public boolean updateOwnReview(FeedbackDAO.FeedbackEntry entry, String loggedInStudentUsername,
                                   int newStars, String newComment) {
        if (entry == null || loggedInStudentUsername == null
                || !entry.customerUsername.equalsIgnoreCase(loggedInStudentUsername.trim())) {
            return false;
        }
        int clampedStars = Math.max(1, Math.min(5, newStars));
        String cleanComment = (newComment == null || newComment.isBlank()) ? entry.comment : newComment.trim();
        String[] nlp = computeFiveAspectNlp(clampedStars, cleanComment);

        entry.rating = clampedStars;
        entry.comment = cleanComment;
        entry.tasteSentiment = nlp[0];
        entry.spicinessSentiment = nlp[1];
        entry.quantitySentiment = nlp[2];
        entry.hygieneSentiment = nlp[3];
        entry.serviceSentiment = nlp[4];
        entry.overallSentiment = nlp[5];
        entry.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM hh:mm a")) + " (Edited)";

        recalculateDishRating(entry.foodId);
        PersistentDataStore.getInstance().saveToDisk();
        return true;
    }

    private void recalculateDishRating(String foodId) {
        if (foodId == null) return;
        menuDAO.findById(foodId).ifPresent(food -> {
            List<Integer> allStars = feedbackDAO.findAll().stream()
                    .filter(f -> foodId.equalsIgnoreCase(f.foodId))
                    .map(f -> f.rating)
                    .collect(Collectors.toList());
            food.recalculateRatingFromReviews(allStars);
            menuDAO.save(food);
        });
    }

    private String[] computeFiveAspectNlp(int stars, String comment) {
        String lower = (comment == null ? "" : comment.toLowerCase().trim());

        String taste = evaluateTasteSentiment(lower, stars);

        String spice;
        if (lower.contains("not spicy enough") || lower.contains("less spicy") || lower.contains("bland") || lower.contains("too mild") || lower.contains("no spice")) {
            spice = "Negative (Too Mild)";
        } else if ((lower.contains("too spicy") || lower.contains("very spicy") || lower.contains("extremely spicy") || lower.contains("burning") || lower.contains("overly spicy"))
                && !lower.contains("not too spicy")) {
            spice = "Negative (Too Spicy)";
        } else {
            spice = "Optimal";
        }

        String qty;
        if (lower.contains("low portion") || lower.contains("small portion") || lower.contains("less quantity")
                || lower.contains("too little") || lower.contains("not enough") || lower.contains("wasnt sufficient")
                || lower.contains("wasn't sufficient") || lower.contains("insufficient") || lower.contains("tiny")
                || (lower.contains("quantity") && (lower.contains("low") || lower.contains("less") || lower.contains("poor")))) {
            qty = "Negative (Low Portion)";
        } else if (lower.contains("generous") || lower.contains("plenty") || lower.contains("huge portion") || lower.contains("good quantity") || lower.contains("filling")) {
            qty = "Positive (Generous)";
        } else {
            qty = stars >= 3 ? "Positive" : "Needs Review";
        }

        String hygiene;
        if (lower.contains("stale") || lower.contains("unhygienic") || lower.contains("dirty") || lower.contains("hair")
                || lower.contains("spoiled") || lower.contains("smell") || lower.contains("not fresh") || lower.contains("cold")) {
            hygiene = "Negative (Freshness/Hygiene Alert)";
        } else if (lower.contains("fresh") || lower.contains("hot") || lower.contains("clean") || lower.contains("hygienic")) {
            hygiene = "Positive (Fresh & Hot)";
        } else {
            hygiene = "Positive";
        }

        String service;
        if (lower.contains("late") || lower.contains("slow") || lower.contains("delayed") || lower.contains("rude") || lower.contains("long wait")) {
            service = "Negative (Slow/Service Issue)";
        } else if (lower.contains("fast") || lower.contains("quick") || lower.contains("on time") || lower.contains("polite")) {
            service = "Positive (Fast)";
        } else {
            service = "Positive";
        }

        int negCount = 0;
        if (taste.startsWith("Negative") || taste.startsWith("Needs")) negCount++;
        if (spice.startsWith("Negative")) negCount++;
        if (qty.startsWith("Negative")) negCount++;
        if (hygiene.startsWith("Negative")) negCount++;
        if (service.startsWith("Negative")) negCount++;

        String overall;
        if (negCount >= 2 || taste.startsWith("Negative") || stars <= 2) {
            overall = "Negative";
        } else if (negCount == 1 || stars == 3) {
            overall = "Mixed / Needs Improvement";
        } else {
            overall = "Positive";
        }

        return new String[]{taste, spice, qty, hygiene, service, overall};
    }

    private String evaluateTasteSentiment(String lower, int stars) {
        for (String negPhrase : NEGATIVE_TASTE_PHRASES) {
            if (lower.contains(negPhrase)) return "Negative (Poor Taste)";
        }
        if (lower.matches(".*\\b(bad|poor|worst|terrible|awful|horrible|pathetic)\\b.*")) {
            return "Negative (Poor Taste)";
        }
        boolean hasNegatedPositive = lower.matches(".*\\b(not|never|wasn't|wasnt|isn't|isnt|hardly)\\s+(very\\s+)?(good|great|tasty|fresh|nice|worth).*");
        if (hasNegatedPositive) return "Negative (Poor Taste)";
        for (String posPhrase : POSITIVE_TASTE_PHRASES) {
            if (lower.contains(posPhrase)) return "Positive";
        }
        return stars >= 4 ? "Positive" : (stars == 3 ? "Neutral" : "Negative (Poor Taste)");
    }

    public WasteRecord recordDailyBatchWaste(String canteenId, String foodName,
                                             int predictedQty, int preparedQty, int soldQty,
                                             WasteRecord.WasteReason reason) {
        int wasted = Math.max(0, preparedQty - soldQty);
        double weightKg = Math.round(wasted * 0.38 * 10.0) / 10.0;
        String id = "WST-" + (101 + wasteDAO.findAll().size());
        WasteRecord rec = new WasteRecord(id, canteenId, foodName, LocalDate.now(),
                predictedQty, preparedQty, soldQty, wasted, weightKg, reason);
        wasteDAO.save(rec);
        PersistentDataStore.getInstance().saveToDisk();
        return rec;
    }

    public List<WasteRecord> getAllWasteRecords() { return wasteDAO.findAll(); }
    public double getTotalWasteKg() { return wasteDAO.getTotalWasteKg(); }
    public Map<String, Double> getWasteReasonBreakdown() { return wasteDAO.getReasonPercentageBreakdown(); }
    public List<FeedbackDAO.FeedbackEntry> getAllFeedbacks() { return feedbackDAO.findAll(); }
}
