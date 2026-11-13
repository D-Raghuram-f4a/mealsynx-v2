package com.mealsynx.model;

import com.mealsynx.util.PasswordUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Customer extends User {
    private static final long serialVersionUID = 1L;
    private double walletBalance;
    private String favouriteCategory;
    private String typicalOrderingTime;
    private double averageSpending;
    private String securityQuestion;
    private String securityAnswerHash;
    private final Set<String> favouriteFoodIds = new LinkedHashSet<>();
    private final Map<String, Integer> orderFrequencyMap = new LinkedHashMap<>();
    private final List<String> recentlyOrderedFoodIds = new ArrayList<>();
    private final Map<String, String> dayPreferenceMap = new HashMap<>();
    private final Map<String, String> companionDrinkMap = new HashMap<>();
    private final Map<String, CartItem> shoppingCart = new LinkedHashMap<>();

    public Customer(String userId, String username, String fullName, String registerNo, String email,
                    String password, double walletBalance, String favouriteCategory,
                    String typicalOrderingTime, double averageSpending) {
        this(userId, username, fullName, registerNo, email, password, walletBalance,
                favouriteCategory, typicalOrderingTime, averageSpending,
                "What is your favourite colour?", "blue");
    }

    public Customer(String userId, String username, String fullName, String registerNo, String email,
                    String password, double walletBalance, String favouriteCategory,
                    String typicalOrderingTime, double averageSpending,
                    String securityQuestion, String securityAnswerPlainOrHash) {
        super(userId, username, fullName, registerNo, email, password, UserRole.STUDENT);
        this.walletBalance = walletBalance;
        this.favouriteCategory = favouriteCategory;
        this.typicalOrderingTime = typicalOrderingTime;
        this.averageSpending = averageSpending;
        this.securityQuestion = (securityQuestion == null || securityQuestion.isBlank())
                ? "What is your favourite colour?" : securityQuestion.trim();
        this.securityAnswerHash = (securityAnswerPlainOrHash != null && securityAnswerPlainOrHash.contains(":"))
                ? securityAnswerPlainOrHash : PasswordUtil.hashAnswer(securityAnswerPlainOrHash);
    }

    @Override
    public String getDashboardTitle() {
        return "MealSynx Student Portal — " + getUsername() + " (" + getFullName() + ")";
    }

    public boolean verifySecurityAnswer(String answerInput) {
        return PasswordUtil.verifyAnswer(answerInput, this.securityAnswerHash);
    }

    // Shopping Cart operations (V2 Requirement #6)
    public void addToCart(FoodItem food, int qty, String note) {
        if (food == null || qty <= 0) return;
        CartItem existing = shoppingCart.get(food.getFoodId());
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + qty);
            if (note != null && !note.isBlank() && !"Standard Prep".equalsIgnoreCase(note)) {
                existing.setSpecialNote(note);
            }
        } else {
            shoppingCart.put(food.getFoodId(), new CartItem(food, qty, note));
        }
    }

    public void updateCartQuantity(String foodId, int newQty) {
        if (newQty <= 0) {
            shoppingCart.remove(foodId);
        } else if (shoppingCart.containsKey(foodId)) {
            shoppingCart.get(foodId).setQuantity(newQty);
        }
    }

    public void updateCartNote(String foodId, String newNote) {
        if (shoppingCart.containsKey(foodId)) {
            shoppingCart.get(foodId).setSpecialNote(newNote);
        }
    }

    public void removeFromCart(String foodId) {
        shoppingCart.remove(foodId);
    }

    public void clearCart() {
        shoppingCart.clear();
    }

    public Map<String, CartItem> getShoppingCart() {
        return shoppingCart;
    }

    public double getCartTotal() {
        return shoppingCart.values().stream().mapToDouble(CartItem::getLineTotal).sum();
    }

    public boolean toggleFavouriteFood(String foodId) {
        if (favouriteFoodIds.contains(foodId)) {
            favouriteFoodIds.remove(foodId);
            return false;
        } else {
            favouriteFoodIds.add(foodId);
            return true;
        }
    }

    public void addFavouriteFood(String foodId) {
        favouriteFoodIds.add(foodId);
    }

    public boolean isFavourite(String foodId) {
        return favouriteFoodIds.contains(foodId);
    }

    public void recordFoodOrdered(String foodId, String foodName, int quantity) {
        orderFrequencyMap.put(foodName, orderFrequencyMap.getOrDefault(foodName, 0) + quantity);
        recentlyOrderedFoodIds.remove(foodId);
        recentlyOrderedFoodIds.add(0, foodId);
    }

    public void setDaySpecialtyPreference(String dayOfWeek, String foodName) {
        dayPreferenceMap.put(dayOfWeek.toUpperCase(), foodName);
    }

    public void setCompanionDrinkPairing(String mainCourse, String drink) {
        companionDrinkMap.put(mainCourse, drink);
    }

    public double getWalletBalance() { return walletBalance; }
    public void setWalletBalance(double walletBalance) { this.walletBalance = walletBalance; }
    public String getFavouriteCategory() { return favouriteCategory; }
    public void setFavouriteCategory(String favouriteCategory) { this.favouriteCategory = favouriteCategory; }
    public String getTypicalOrderingTime() { return typicalOrderingTime; }
    public String getTypicalOrderWindow() { return typicalOrderingTime; }
    public double getAverageSpending() { return averageSpending; }
    public void setAverageSpending(double averageSpending) { this.averageSpending = averageSpending; }
    public String getSecurityQuestion() { return securityQuestion; }
    public void setSecurityQuestion(String securityQuestion) { this.securityQuestion = securityQuestion; }
    public String getSecurityAnswerHash() { return securityAnswerHash; }
    public void setSecurityAnswerHash(String ans) {
        this.securityAnswerHash = (ans != null && ans.contains(":")) ? ans : PasswordUtil.hashAnswer(ans);
    }
    public Set<String> getFavouriteFoodIds() { return favouriteFoodIds; }
    public Map<String, Integer> getOrderFrequencyMap() { return orderFrequencyMap; }
    public List<String> getRecentlyOrderedFoodIds() { return recentlyOrderedFoodIds; }
    public Map<String, String> getDayPreferenceMap() { return dayPreferenceMap; }
    public Map<String, String> getCompanionDrinkMap() { return companionDrinkMap; }
}
