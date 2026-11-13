package com.mealsynx.dao;

import com.mealsynx.db.PersistentDataStore;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class FeedbackDAO {
    public static class FeedbackEntry implements Serializable {
        private static final long serialVersionUID = 3L;
        public final String feedbackId;
        public final String customerUsername;
        public final String canteenId;
        public final String foodId;
        public final String foodName;
        public int rating;
        public String comment;
        public String tasteSentiment;
        public String spicinessSentiment;
        public String quantitySentiment;
        public String hygieneSentiment;
        public String serviceSentiment;
        public String overallSentiment;
        public String timestamp;

        public FeedbackEntry(String customerUsername, String canteenId, String foodId, String foodName,
                             int rating, String comment, String tasteSentiment,
                             String spicinessSentiment, String quantitySentiment) {
            this(customerUsername, canteenId, foodId, foodName, rating, comment,
                    tasteSentiment, spicinessSentiment, quantitySentiment,
                    "Positive", "Positive", rating >= 4 ? "Positive" : (rating == 3 ? "Neutral" : "Negative"));
        }

        public FeedbackEntry(String customerUsername, String canteenId, String foodId, String foodName,
                             int rating, String comment, String tasteSentiment,
                             String spicinessSentiment, String quantitySentiment,
                             String hygieneSentiment, String serviceSentiment, String overallSentiment) {
            this.feedbackId = "FB-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            this.customerUsername = customerUsername;
            this.canteenId = canteenId;
            this.foodId = foodId;
            this.foodName = foodName;
            this.rating = rating;
            this.comment = comment;
            this.tasteSentiment = tasteSentiment;
            this.spicinessSentiment = spicinessSentiment;
            this.quantitySentiment = quantitySentiment;
            this.hygieneSentiment = hygieneSentiment;
            this.serviceSentiment = serviceSentiment;
            this.overallSentiment = overallSentiment;
            this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM hh:mm a"));
        }
    }

    public void saveFeedback(FeedbackEntry entry) {
        PersistentDataStore.getInstance().data().feedbacks.add(0, entry);
        PersistentDataStore.getInstance().saveToDisk();
    }

    public List<FeedbackEntry> findAll() {
        return new ArrayList<>(PersistentDataStore.getInstance().data().feedbacks);
    }

    public List<FeedbackEntry> findByCanteen(String canteenId) {
        return findAll().stream()
                .filter(f -> f.canteenId != null && f.canteenId.equalsIgnoreCase(canteenId))
                .collect(Collectors.toList());
    }
}
