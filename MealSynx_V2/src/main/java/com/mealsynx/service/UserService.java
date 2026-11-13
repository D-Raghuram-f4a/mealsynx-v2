package com.mealsynx.service;

import com.mealsynx.dao.CanteenDAO;
import com.mealsynx.dao.CustomerDAO;
import com.mealsynx.dao.MenuDAO;
import com.mealsynx.dao.OrderDAO;
import com.mealsynx.dao.PaymentDAO;
import com.mealsynx.db.PersistentDataStore;
import com.mealsynx.model.Canteen;
import com.mealsynx.model.Customer;
import com.mealsynx.model.FoodItem;
import com.mealsynx.model.Order;
import com.mealsynx.model.WeatherSnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class UserService {
    private final CustomerDAO customerDAO;
    private final MenuDAO menuDAO;
    private final OrderDAO orderDAO;
    private final CanteenDAO canteenDAO;
    private final PaymentDAO paymentDAO;
    private final NotificationService notificationService;

    public UserService(CustomerDAO customerDAO, MenuDAO menuDAO, OrderDAO orderDAO,
                       CanteenDAO canteenDAO, PaymentDAO paymentDAO, NotificationService notificationService) {
        this.customerDAO = customerDAO;
        this.menuDAO = menuDAO;
        this.orderDAO = orderDAO;
        this.canteenDAO = canteenDAO;
        this.paymentDAO = paymentDAO;
        this.notificationService = notificationService;
    }

    public List<Customer> getAllStudents() {
        return customerDAO.findAll();
    }

    public Optional<Customer> findStudent(String usernameOrId) {
        return customerDAO.findByUsernameOrIdentifier(usernameOrId);
    }

    public boolean toggleStudentFavourite(Customer student, String foodId) {
        boolean nowFav = student.toggleFavouriteFood(foodId);
        customerDAO.save(student);
        PersistentDataStore.getInstance().saveToDisk();
        return nowFav;
    }

    /**
     * V2 Rule #6: Smart Food Recommendations are based on verified student reviews (ratingCount > 0),
     * past orders, and favourites.
     */
    public List<String> getSmartFoodRecommendations(Customer student, WeatherSnapshot weather) {
        List<String> recommendations = new ArrayList<>();
        List<FoodItem> allAvailable = menuDAO.findAll().stream().filter(FoodItem::isAvailable).collect(Collectors.toList());
        if (allAvailable.isEmpty()) {
            recommendations.add("No menu items added yet. Canteen workers will add dishes to the menu soon.");
            return recommendations;
        }

        Set<String> favIds = student.getFavouriteFoodIds();
        List<Order> pastOrders = orderDAO.findByCustomer(student.getUsername());

        Set<String> preferredCategories = new LinkedHashSet<>();
        for (String fid : favIds) {
            menuDAO.findById(fid).ifPresent(f -> preferredCategories.add(f.getCategory()));
        }
        for (Order o : pastOrders) {
            for (Order.OrderLineItem li : o.getItems()) {
                menuDAO.findById(li.getFoodId()).ifPresent(f -> preferredCategories.add(f.getCategory()));
            }
        }

        for (FoodItem item : allAvailable) {
            Canteen c = canteenDAO.findById(item.getCanteenId()).orElse(null);
            String cName = c != null ? c.getName() : item.getCanteenId();
            int approxMins = c != null ? c.calculateApproxPrepTime(item.getBasePrepTimeMinutes()) : item.getBasePrepTimeMinutes();

            boolean matchesCategory = preferredCategories.contains(item.getCategory());
            boolean isFav = favIds.contains(item.getFoodId());
            boolean hasHighRating = item.getRatingCount() > 0 && item.getCustomerRating() >= 4.0;

            if (isFav || hasHighRating || matchesCategory) {
                String ratingLbl = item.getRatingCount() > 0
                        ? String.format("★%.2f (%d reviews)", item.getCustomerRating(), item.getRatingCount())
                        : "New Dish";
                String reason = isFav ? "In your ❤️ Favourites"
                        : (hasHighRating ? "Highly rated by students" : "Matches your " + item.getCategory() + " preference");
                recommendations.add(String.format("💡 %s (₹%.0f | %s | ~%d mins | %s) — %s",
                        item.getName(), item.getPrice(), ratingLbl, approxMins, cName, reason));
            }
        }

        if (recommendations.isEmpty()) {
            for (FoodItem item : allAvailable.stream().limit(5).toList()) {
                Canteen c = canteenDAO.findById(item.getCanteenId()).orElse(null);
                String cName = c != null ? c.getName() : item.getCanteenId();
                recommendations.add(String.format("💡 %s (₹%.0f | New Dish | %s) — Available to order & review",
                        item.getName(), item.getPrice(), cName));
            }
        }

        return recommendations.stream().limit(7).collect(Collectors.toList());
    }

    public List<String> getTrendingFoodsAllCanteens() {
        List<FoodItem> allAvailable = menuDAO.findAll().stream().filter(FoodItem::isAvailable).collect(Collectors.toList());
        if (allAvailable.isEmpty()) {
            return List.of("No dishes available yet.");
        }
        return allAvailable.stream()
                .sorted(Comparator.comparingInt(FoodItem::getTotalOrdersCount)
                        .thenComparingDouble(FoodItem::getCustomerRating).reversed())
                .limit(8)
                .map(item -> {
                    Canteen c = canteenDAO.findById(item.getCanteenId()).orElse(null);
                    String cName = c != null ? c.getName() : item.getCanteenId();
                    int prep = c != null ? c.calculateApproxPrepTime(item.getBasePrepTimeMinutes()) : item.getBasePrepTimeMinutes();
                    String ratingStr = item.getRatingCount() > 0
                            ? String.format("★%.2f (%d reviews)", item.getCustomerRating(), item.getRatingCount())
                            : "No reviews yet";
                    return String.format("🔥 %s — ₹%.0f | %s | %d orders | ~%d mins | %s",
                            item.getName(), item.getPrice(), ratingStr,
                            item.getTotalOrdersCount(), prep, cName);
                })
                .collect(Collectors.toList());
    }

    public List<FoodItem> getRecentlyOrderedItems(Customer student) {
        Set<String> seenFoodIds = new LinkedHashSet<>();
        List<FoodItem> recentItems = new ArrayList<>();

        for (Order o : orderDAO.findByCustomer(student.getUsername())) {
            for (Order.OrderLineItem li : o.getItems()) {
                if (seenFoodIds.add(li.getFoodId())) {
                    menuDAO.findById(li.getFoodId()).ifPresent(recentItems::add);
                }
            }
        }
        for (String fid : student.getRecentlyOrderedFoodIds()) {
            if (seenFoodIds.add(fid)) {
                menuDAO.findById(fid).ifPresent(recentItems::add);
            }
        }
        return recentItems;
    }

    public List<FoodItem> getFavouriteItemsList(Customer student) {
        List<FoodItem> favs = new ArrayList<>();
        for (String fid : student.getFavouriteFoodIds()) {
            menuDAO.findById(fid).ifPresent(favs::add);
        }
        return favs;
    }

    /**
     * V2 Rule #4 & #6: A student can ONLY review food items that they have actually ordered!
     */
    public List<FoodItem> getFoodsOrderedByStudentForReview(Customer student) {
        return getRecentlyOrderedItems(student);
    }

    public Optional<Customer> addMoneyToStudentWallet(String studentUsername, double amountToAdd) {
        if (amountToAdd <= 0) return Optional.empty();
        Optional<Customer> studentOpt = customerDAO.findByUsernameOrIdentifier(studentUsername);
        if (studentOpt.isPresent()) {
            Customer student = studentOpt.get();
            double newBal = student.getWalletBalance() + amountToAdd;
            student.setWalletBalance(newBal);
            customerDAO.save(student);

            String txnId = "TOPUP-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            paymentDAO.recordTransaction(txnId, student.getUsername(), "ADMIN-TOPUP", amountToAdd, "ADMIN_CASH_DEPOSIT");
            notificationService.notifyStudent(student,
                    String.format("Admin added ₹%.2f to your Food Wallet! New Balance: ₹%.2f", amountToAdd, newBal));
            PersistentDataStore.getInstance().saveToDisk();
            return Optional.of(student);
        }
        return Optional.empty();
    }
}
