package com.mealsynx.model;

import java.io.Serializable;

/**
 * Shopping Cart Line Item for MealSynx V2.
 * Supports updating quantities (+1 / -1) and per-item special cooking notes.
 */
public class CartItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private final FoodItem foodItem;
    private int quantity;
    private String specialNote;

    public CartItem(FoodItem foodItem, int quantity, String specialNote) {
        this.foodItem = foodItem;
        this.quantity = Math.max(1, quantity);
        this.specialNote = (specialNote == null || specialNote.isBlank()) ? "Standard Prep" : specialNote.trim();
    }

    public FoodItem getFoodItem() { return foodItem; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = Math.max(1, quantity); }
    public String getSpecialNote() { return specialNote; }
    public void setSpecialNote(String specialNote) {
        this.specialNote = (specialNote == null || specialNote.isBlank()) ? "Standard Prep" : specialNote.trim();
    }
    public double getLineTotal() { return quantity * foodItem.getPrice(); }
}
