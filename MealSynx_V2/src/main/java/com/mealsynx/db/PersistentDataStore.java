package com.mealsynx.db;

import com.mealsynx.dao.FeedbackDAO;
import com.mealsynx.model.*;

import java.io.*;
import java.util.*;

/**
 * PersistentDataStore for MealSynx V2 (GitHub Edition).
 * Clean-Slate Production Policy:
 *  - Only 1 default Canteen ('CAN-1' -> 'Canteen 1', renameable by Admin) and 1 default Admin account exist initially.
 *  - Zero pre-seeded students, zero pre-seeded workers, zero pre-built menu items, zero warehouse inventory,
 *    zero calendar events, zero orders, and zero reviews on first launch.
 */
public class PersistentDataStore {
    public static class DataSnapshot implements Serializable {
        private static final long serialVersionUID = 3L;
        public final Map<String, Canteen> canteens = new LinkedHashMap<>();
        public final Map<String, Customer> customers = new LinkedHashMap<>();
        public final List<Worker> workers = new ArrayList<>();
        public final List<Admin> admins = new ArrayList<>();
        public final Map<String, FoodItem> menuItems = new LinkedHashMap<>();
        public final Map<String, Order> orders = new LinkedHashMap<>();
        public final Map<String, IngredientStock> inventory = new LinkedHashMap<>();
        public final List<String> paymentTransactions = new ArrayList<>();
        public final List<FeedbackDAO.FeedbackEntry> feedbacks = new ArrayList<>();
        public final List<CampusEvent> events = new ArrayList<>();
        public final List<WasteRecord> wasteRecords = new ArrayList<>();
        public final List<SupportTicket> supportTickets = new ArrayList<>();
        public final List<SimulationRequest> simulationRequests = new ArrayList<>();
        public WeatherSnapshot currentWeather;
    }

    private static final String LOCAL_STORE_FILE = "mealsynxv2db_persistent_store.dat";
    private static final String HOME_STORE_FILE = System.getProperty("user.home") + File.separator + ".mealsynxv2db_persistent_store.dat";

    private static PersistentDataStore instance;
    private DataSnapshot snapshot;

    private PersistentDataStore() {
        loadFromDiskOrSeed();
        Runtime.getRuntime().addShutdownHook(new Thread(this::saveToDisk));
    }

    public static synchronized PersistentDataStore getInstance() {
        if (instance == null) {
            instance = new PersistentDataStore();
        }
        return instance;
    }

    public DataSnapshot data() {
        return snapshot;
    }

    private void loadFromDiskOrSeed() {
        File localFile = new File(LOCAL_STORE_FILE);
        File homeFile = new File(HOME_STORE_FILE);
        File target = localFile.exists() ? localFile : (homeFile.exists() ? homeFile : null);

        if (target != null) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(target))) {
                Object obj = ois.readObject();
                if (obj instanceof DataSnapshot loaded && !loaded.admins.isEmpty()) {
                    this.snapshot = loaded;
                    return;
                }
            } catch (Exception ignored) {}
        }
        this.snapshot = createInitialCleanDataset();
        saveToDisk();
    }

    public synchronized void saveToDisk() {
        if (snapshot == null) return;
        writeSnapshotToFile(new File(LOCAL_STORE_FILE));
        writeSnapshotToFile(new File(HOME_STORE_FILE));
    }

    private void writeSnapshotToFile(File f) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f))) {
            oos.writeObject(snapshot);
            oos.flush();
        } catch (Exception ignored) {}
    }

    public synchronized void resetToFactoryDataset() {
        this.snapshot = createInitialCleanDataset();
        saveToDisk();
    }

    private DataSnapshot createInitialCleanDataset() {
        DataSnapshot ds = new DataSnapshot();

        Properties props = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) props.load(in);
        } catch (Exception ignored) {}

        String adminUsername = props.getProperty("admin.default.username", "admin").trim();
        String adminFullName = props.getProperty("admin.default.name", "Campus Food Intelligence Director").trim();
        String adminEmail    = props.getProperty("admin.default.email", "admin@mealsynx.edu").trim();
        String adminPassword = props.getProperty("admin.default.password", "admin123*").trim();

        String defCanteenId   = props.getProperty("canteen.default.id", "CAN-1").trim();
        String defCanteenName = props.getProperty("canteen.default.name", "Canteen 1").trim();
        String defCanteenSpec = props.getProperty("canteen.default.specialty", "Multi-Cuisine").trim();
        String defCanteenLoc  = props.getProperty("canteen.default.location", "Main Campus Block").trim();

        // 1. Only 1 Default Canteen ('CAN-1' -> 'Canteen 1') — Admin can rename, remove, or add more canteens
        Canteen c1 = new Canteen(defCanteenId, defCanteenName, defCanteenSpec, defCanteenLoc,
                400, 0, 0.00, 0.00, 0.00, 0.00);
        ds.canteens.put(c1.getCanteenId(), c1);

        // 2. Only 1 Default Admin Account (salted SHA-256 password hash)
        // Zero pre-seeded students, workers, menu items, warehouse inventory, calendar events, or reviews!
        Admin adm = new Admin("ADM-001", adminUsername, adminFullName, "ADM-CENTRAL-01",
                adminEmail, adminPassword, "Central Campus Dining & Big Data Operations");
        ds.admins.add(adm);

        DatabaseConnectionPool.getInstance().saveOrUpdateUser(
                adm.getUserId(), adm.getUsername(), adm.getFullName(), adm.getRegisterNo(),
                adm.getEmail(), adm.getPassword(), "ADMIN", 0.00,
                null, null, 0.00, defCanteenId, "08:00 AM – 06:00 PM"
        );

        ds.currentWeather = new WeatherSnapshot("SUMMER_HOT", 34.8, false, 64,
                "Chennai Campus Live Weather (34.8°C) — Open-Meteo / Google Weather Sync");
        return ds;
    }
}
