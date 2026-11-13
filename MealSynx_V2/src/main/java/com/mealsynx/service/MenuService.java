package com.mealsynx.service;

import com.mealsynx.dao.CanteenDAO;
import com.mealsynx.dao.MenuDAO;
import com.mealsynx.model.Canteen;
import com.mealsynx.model.FoodItem;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MenuService {
    private final MenuDAO menuDAO;
    private final CanteenDAO canteenDAO;

    public MenuService(MenuDAO menuDAO, CanteenDAO canteenDAO) {
        this.menuDAO = menuDAO;
        this.canteenDAO = canteenDAO;
    }

    public List<FoodItem> getAllFoodItems() {
        return menuDAO.findAll();
    }

    public List<FoodItem> getMenuForCanteen(String canteenId, String searchKeyword) {
        List<FoodItem> base = ("ALL".equalsIgnoreCase(canteenId) || canteenId == null || canteenId.isBlank())
                ? menuDAO.findAll()
                : menuDAO.findByCanteen(canteenId);

        return base.stream()
                .filter(f -> searchKeyword == null || searchKeyword.isBlank()
                        || f.getName().toLowerCase().contains(searchKeyword.trim().toLowerCase())
                        || f.getCategory().toLowerCase().contains(searchKeyword.trim().toLowerCase())
                        || f.getFoodId().toLowerCase().contains(searchKeyword.trim().toLowerCase()))
                .collect(Collectors.toList());
    }

    public Optional<FoodItem> findById(String foodId) {
        return menuDAO.findById(foodId);
    }

    /**
     * V2 Rule #6: When a canteen worker adds a new food item, it starts with 0 orders and 0 reviews
     * (customerRating = 0.0, ratingCount = 0). Ratings and recommendations update only after students review it.
     */
    public FoodItem addNewFoodItem(String canteenId, String name, String category,
                                   double price, double prepCost, int basePrepMins, String nutritionalInfo) {
        String newId = "FD-" + (100 + menuDAO.findAll().size() + 1);
        FoodItem item = new FoodItem(
                newId, name, category, price, prepCost, basePrepMins, true,
                nutritionalInfo, canteenId, 100, 0, 0.0, 2, 0.0, 0
        );
        return menuDAO.save(item);
    }

    public boolean removeFoodItem(String foodId) {
        return menuDAO.deleteById(foodId);
    }

    public void updatePrice(String foodId, double newPrice) {
        menuDAO.findById(foodId).ifPresent(item -> {
            item.setPrice(newPrice);
            menuDAO.save(item);
        });
    }

    public void updateKitchenPlanTarget(String foodId, int targetPlates) {
        menuDAO.findById(foodId).ifPresent(item -> {
            item.setCustomKitchenPlanTarget(targetPlates);
            menuDAO.save(item);
        });
    }

    public void toggleAvailability(String foodId) {
        menuDAO.findById(foodId).ifPresent(item -> {
            item.setAvailable(!item.isAvailable());
            menuDAO.save(item);
        });
    }

    public int getApproxPrepTimeForCanteenItem(FoodItem item) {
        Canteen c = canteenDAO.findById(item.getCanteenId()).orElse(null);
        if (c == null) return item.getBasePrepTimeMinutes();
        return c.calculateApproxPrepTime(item.getBasePrepTimeMinutes());
    }

    public List<FoodItem> getTopPerformingItemsForCanteen(String canteenId) {
        return menuDAO.findByCanteen(canteenId).stream()
                .sorted(Comparator.comparingInt(FoodItem::calculatePerformanceScore).reversed())
                .collect(Collectors.toList());
    }
}
