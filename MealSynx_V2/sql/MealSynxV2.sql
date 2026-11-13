-- ============================================================================
-- MEALSYNX V2 COMPLETE MYSQL 8.4 LTS DATABASE SETUP (GITHUB EDITION)
-- Connection Name : mealsynxf4a
-- Database Name   : mealsynxv2db
-- Authors         : D Raghuram (RA2511027040004) & B Likith Adhithya (RA2511027040022)
-- Notes           : Clean-slate schema with all tables created and ZERO demo
--                   students, workers, menu items, or warehouse stock.
--                   Only 1 default Canteen ('CAN-1' -> 'Canteen 1') and the
--                   default Admin account are seeded on first launch.
-- ============================================================================

CREATE DATABASE IF NOT EXISTS mealsynxv2db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE mealsynxv2db;

CREATE TABLE IF NOT EXISTS canteens (
    canteen_id VARCHAR(20) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    specialty_type VARCHAR(80) NOT NULL DEFAULT 'Multi-Cuisine',
    location VARCHAR(150) NOT NULL,
    max_capacity INT NOT NULL DEFAULT 400,
    active_orders INT NOT NULL DEFAULT 0,
    sales_revenue DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    ingredient_expenses DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    worker_wages DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    utilities_expenses DECIMAL(12, 2) NOT NULL DEFAULT 0.00
) ENGINE=InnoDB;

INSERT IGNORE INTO canteens (canteen_id, name, specialty_type, location) VALUES
('CAN-1', 'Canteen 1', 'Multi-Cuisine', 'Main Campus Block');

CREATE TABLE IF NOT EXISTS users (
    user_id VARCHAR(20) PRIMARY KEY,
    username VARCHAR(60),
    full_name VARCHAR(100) NOT NULL,
    register_no VARCHAR(30) UNIQUE,
    email VARCHAR(120) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    wallet_balance DECIMAL(10, 2) DEFAULT 0.00,
    security_question VARCHAR(255),
    security_answer_hash VARCHAR(255),
    favourite_category VARCHAR(50),
    typical_order_window VARCHAR(50),
    average_spending DECIMAL(10, 2) DEFAULT 0.00,
    assigned_canteen_id VARCHAR(20),
    shift_timing VARCHAR(60),
    monthly_salary DECIMAL(10, 2) DEFAULT 18000.00,
    last_salary_paid_month VARCHAR(10),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS food_items (
    food_id VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    prep_cost DECIMAL(10, 2) NOT NULL,
    base_prep_mins INT NOT NULL,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    nutritional_info VARCHAR(150) NOT NULL,
    canteen_id VARCHAR(20) NOT NULL,
    historical_daily_demand INT DEFAULT 100,
    total_orders_count INT DEFAULT 0,
    waste_rate_percent DECIMAL(5, 2) DEFAULT 0.00,
    ingredient_difficulty INT DEFAULT 2,
    avg_rating DECIMAL(3, 2) DEFAULT 0.00,
    rating_count INT DEFAULT 0
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS orders (
    order_id VARCHAR(30) PRIMARY KEY,
    customer_id VARCHAR(20) NOT NULL,
    customer_username VARCHAR(60),
    canteen_id VARCHAR(20) NOT NULL,
    order_type VARCHAR(20) NOT NULL,
    delivery_mode VARCHAR(30) NOT NULL,
    delivery_point VARCHAR(150),
    scheduled_time VARCHAR(50),
    payment_method VARCHAR(40) NOT NULL,
    special_note VARCHAR(255),
    total_amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS customer_feedback (
    feedback_id VARCHAR(30) PRIMARY KEY,
    customer_username VARCHAR(60) NOT NULL,
    canteen_id VARCHAR(20),
    food_id VARCHAR(20),
    food_name VARCHAR(100) NOT NULL,
    rating INT NOT NULL,
    comment_text TEXT NOT NULL,
    taste_sentiment VARCHAR(35),
    spiciness_sentiment VARCHAR(35),
    quantity_sentiment VARCHAR(35),
    hygiene_sentiment VARCHAR(35),
    service_sentiment VARCHAR(35),
    overall_sentiment VARCHAR(35),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;
