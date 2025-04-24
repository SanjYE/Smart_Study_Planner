package com.example.service;

import com.example.model.DailyStudyItem;
import com.example.model.StudyPlan;
import com.example.model.Subject;
import com.example.model.User;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for database operations using SQLite
 * Implements Singleton pattern
 */
public class DatabaseService {
    
    private static DatabaseService instance;
    private Connection connection;
    private static final String DB_URL = "jdbc:sqlite:studyplanner.db";
    
    /**
     * Private constructor to prevent direct instantiation
     */
    private DatabaseService() {
        try {
            // Create a connection to the database
            connection = DriverManager.getConnection(DB_URL);
            // Initialize the database schema if needed
            initializeDatabase();
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get the singleton instance
     * @return The database service instance
     */
    public static synchronized DatabaseService getInstance() {
        if (instance == null) {
            instance = new DatabaseService();
        }
        return instance;
    }
    
    /**
     * Initialize the database with needed tables
     * @throws SQLException if a database error occurs
     */
    private void initializeDatabase() throws SQLException {
        // Create users table
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "username TEXT UNIQUE NOT NULL," +
                    "password TEXT NOT NULL," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")");
            
            // Create study plans table
            stmt.execute("CREATE TABLE IF NOT EXISTS study_plans (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "user_id INTEGER NOT NULL," +
                    "name TEXT NOT NULL," +
                    "exam_date TEXT NOT NULL," +
                    "strategy TEXT NOT NULL," +
                    "raw_plan_text TEXT," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (user_id) REFERENCES users(id)" +
                    ")");
            
            // Create subjects table
            stmt.execute("CREATE TABLE IF NOT EXISTS subjects (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "study_plan_id INTEGER NOT NULL," +
                    "name TEXT NOT NULL," +
                    "FOREIGN KEY (study_plan_id) REFERENCES study_plans(id)" +
                    ")");
            
            // Create topics table
            stmt.execute("CREATE TABLE IF NOT EXISTS topics (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "subject_id INTEGER NOT NULL," +
                    "name TEXT NOT NULL," +
                    "FOREIGN KEY (subject_id) REFERENCES subjects(id)" +
                    ")");
            
            // Create daily items table
            stmt.execute("CREATE TABLE IF NOT EXISTS daily_items (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "study_plan_id INTEGER NOT NULL," +
                    "date TEXT NOT NULL," +
                    "subject TEXT NOT NULL," +
                    "topic TEXT NOT NULL," +
                    "hours REAL NOT NULL," +
                    "completed BOOLEAN DEFAULT 0," +
                    "FOREIGN KEY (study_plan_id) REFERENCES study_plans(id)" +
                    ")");
            
            System.out.println("Database initialized successfully");
        }
    }
    
    /**
     * Register a new user
     * @param username Username
     * @param password Password
     * @return true if registration successful, false otherwise
     */
    public boolean registerUser(String username, String password) {
        try (PreparedStatement pstmt = connection.prepareStatement(
                "INSERT INTO users (username, password) VALUES (?, ?)")) {
            pstmt.setString(1, username);
            pstmt.setString(2, password); // In a real app, password should be hashed
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error registering user: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Authenticate a user
     * @param username Username
     * @param password Password
     * @return User ID if authenticated, -1 otherwise
     */
    public int authenticateUser(String username, String password) {
        try (PreparedStatement pstmt = connection.prepareStatement(
                "SELECT id FROM users WHERE username = ? AND password = ?")) {
            pstmt.setString(1, username);
            pstmt.setString(2, password); // In a real app, password should be hashed
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error authenticating user: " + e.getMessage());
        }
        return -1;
    }
    
    /**
     * Save a study plan to the database
     * @param userId User ID
     * @param studyPlan The study plan to save
     * @return The ID of the saved study plan, or -1 if an error occurred
     */
    public int saveStudyPlan(int userId, StudyPlan studyPlan, String strategy) {
        int studyPlanId = -1;
        
        try {
            // Start transaction
            connection.setAutoCommit(false);
            
            // Save study plan
            try (PreparedStatement pstmt = connection.prepareStatement(
                    "INSERT INTO study_plans (user_id, name, exam_date, strategy, raw_plan_text) VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, userId);
                pstmt.setString(2, studyPlan.getUser().getName());
                pstmt.setString(3, studyPlan.getUser().getExamDate().toString());
                pstmt.setString(4, strategy);
                pstmt.setString(5, studyPlan.getRawPlanText());
                pstmt.executeUpdate();
                
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        studyPlanId = generatedKeys.getInt(1);
                    }
                }
            }
            
            // Save subjects and topics
            for (Subject subject : studyPlan.getUser().getSubjects()) {
                int subjectId = saveSubject(studyPlanId, subject.getName());
                if (subjectId != -1) {
                    for (String topic : subject.getTopics()) {
                        saveTopic(subjectId, topic);
                    }
                }
            }
            
            // Save daily items
            for (Map.Entry<LocalDate, List<DailyStudyItem>> entry : studyPlan.getDailyPlan().entrySet()) {
                LocalDate date = entry.getKey();
                for (DailyStudyItem item : entry.getValue()) {
                    saveDailyItem(studyPlanId, date, item);
                }
            }
            
            // Commit transaction
            connection.commit();
            return studyPlanId;
            
        } catch (SQLException e) {
            System.err.println("Error saving study plan: " + e.getMessage());
            try {
                connection.rollback();
            } catch (SQLException ex) {
                System.err.println("Error rolling back transaction: " + ex.getMessage());
            }
            return -1;
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("Error resetting auto-commit: " + e.getMessage());
            }
        }
    }
    
    /**
     * Save a subject to the database
     * @param studyPlanId Study plan ID
     * @param subjectName Subject name
     * @return Subject ID
     * @throws SQLException if a database error occurs
     */
    private int saveSubject(int studyPlanId, String subjectName) throws SQLException {
        try (PreparedStatement pstmt = connection.prepareStatement(
                "INSERT INTO subjects (study_plan_id, name) VALUES (?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, studyPlanId);
            pstmt.setString(2, subjectName);
            pstmt.executeUpdate();
            
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        }
        return -1;
    }
    
    /**
     * Save a topic to the database
     * @param subjectId Subject ID
     * @param topicName Topic name
     * @throws SQLException if a database error occurs
     */
    private void saveTopic(int subjectId, String topicName) throws SQLException {
        try (PreparedStatement pstmt = connection.prepareStatement(
                "INSERT INTO topics (subject_id, name) VALUES (?, ?)")) {
            pstmt.setInt(1, subjectId);
            pstmt.setString(2, topicName);
            pstmt.executeUpdate();
        }
    }
    
    /**
     * Save a daily item to the database
     * @param studyPlanId Study plan ID
     * @param date Date
     * @param item Daily study item
     * @throws SQLException if a database error occurs
     */
    private void saveDailyItem(int studyPlanId, LocalDate date, DailyStudyItem item) throws SQLException {
        try (PreparedStatement pstmt = connection.prepareStatement(
                "INSERT INTO daily_items (study_plan_id, date, subject, topic, hours) VALUES (?, ?, ?, ?, ?)")) {
            pstmt.setInt(1, studyPlanId);
            pstmt.setString(2, date.toString());
            pstmt.setString(3, item.getSubject());
            pstmt.setString(4, item.getTopic());
            pstmt.setDouble(5, item.getHoursRecommended());
            pstmt.executeUpdate();
        }
    }
    
    /**
     * Get all study plans for a user
     * @param userId User ID
     * @return List of study plan IDs and names
     */
    public List<Map<String, Object>> getUserStudyPlans(int userId) {
        List<Map<String, Object>> plans = new ArrayList<>();
        
        try (PreparedStatement pstmt = connection.prepareStatement(
                "SELECT id, name, exam_date, created_at FROM study_plans WHERE user_id = ? ORDER BY created_at DESC")) {
            pstmt.setInt(1, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> plan = new HashMap<>();
                    plan.put("id", rs.getInt("id"));
                    plan.put("name", rs.getString("name"));
                    plan.put("examDate", rs.getString("exam_date"));
                    plan.put("createdAt", rs.getString("created_at"));
                    plans.add(plan);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting user study plans: " + e.getMessage());
        }
        
        return plans;
    }
    
    /**
     * Load a study plan from the database
     * @param studyPlanId Study plan ID
     * @return The loaded study plan, or null if an error occurred
     */
    public StudyPlan loadStudyPlan(int studyPlanId) {
        try {
            // Get study plan details
            String userName = "";
            LocalDate examDate = LocalDate.now();
            String rawPlanText = "";
            
            try (PreparedStatement pstmt = connection.prepareStatement(
                    "SELECT name, exam_date, raw_plan_text FROM study_plans WHERE id = ?")) {
                pstmt.setInt(1, studyPlanId);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        userName = rs.getString("name");
                        examDate = LocalDate.parse(rs.getString("exam_date"));
                        rawPlanText = rs.getString("raw_plan_text");
                    } else {
                        return null;
                    }
                }
            }
            
            // Create user
            User user = new User(userName, examDate);
            
            // Create study plan
            StudyPlan studyPlan = new StudyPlan(user);
            studyPlan.setRawPlanText(rawPlanText);
            
            // Load subjects and topics
            loadSubjectsAndTopics(studyPlanId, user);
            
            // Load daily items
            loadDailyItems(studyPlanId, studyPlan);
            
            return studyPlan;
            
        } catch (SQLException e) {
            System.err.println("Error loading study plan: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Load subjects and topics for a study plan
     * @param studyPlanId Study plan ID
     * @param user User to add subjects and topics to
     * @throws SQLException if a database error occurs
     */
    private void loadSubjectsAndTopics(int studyPlanId, User user) throws SQLException {
        try (PreparedStatement pstmt = connection.prepareStatement(
                "SELECT id, name FROM subjects WHERE study_plan_id = ?")) {
            pstmt.setInt(1, studyPlanId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int subjectId = rs.getInt("id");
                    String subjectName = rs.getString("name");
                    
                    Subject subject = new Subject(subjectName);
                    
                    // Load topics for this subject
                    try (PreparedStatement topicStmt = connection.prepareStatement(
                            "SELECT name FROM topics WHERE subject_id = ?")) {
                        topicStmt.setInt(1, subjectId);
                        
                        try (ResultSet topicRs = topicStmt.executeQuery()) {
                            while (topicRs.next()) {
                                subject.addTopic(topicRs.getString("name"));
                            }
                        }
                    }
                    
                    user.addSubject(subject);
                }
            }
        }
    }
    
    /**
     * Load daily items for a study plan
     * @param studyPlanId Study plan ID
     * @param studyPlan Study plan to add daily items to
     * @throws SQLException if a database error occurs
     */
    private void loadDailyItems(int studyPlanId, StudyPlan studyPlan) throws SQLException {
        Map<LocalDate, List<DailyStudyItem>> dailyPlan = new HashMap<>();
        
        try (PreparedStatement pstmt = connection.prepareStatement(
                "SELECT date, subject, topic, hours, completed FROM daily_items WHERE study_plan_id = ? ORDER BY date")) {
            pstmt.setInt(1, studyPlanId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    LocalDate date = LocalDate.parse(rs.getString("date"));
                    String subject = rs.getString("subject");
                    String topic = rs.getString("topic");
                    double hours = rs.getDouble("hours");
                    boolean completed = rs.getBoolean("completed");
                    
                    DailyStudyItem item = new DailyStudyItem(subject, topic, hours);
                    item.setCompleted(completed);
                    
                    if (!dailyPlan.containsKey(date)) {
                        dailyPlan.put(date, new ArrayList<>());
                    }
                    
                    dailyPlan.get(date).add(item);
                }
            }
        }
        
        studyPlan.setDailyPlan(dailyPlan);
    }
    
    /**
     * Update completion status of a daily study item
     * @param studyPlanId Study plan ID
     * @param date Date
     * @param subject Subject name
     * @param topic Topic name
     * @param completed New completion status
     * @return true if update successful, false otherwise
     */
    public boolean updateItemCompletion(int studyPlanId, LocalDate date, String subject, String topic, boolean completed) {
        try (PreparedStatement pstmt = connection.prepareStatement(
                "UPDATE daily_items SET completed = ? WHERE study_plan_id = ? AND date = ? AND subject = ? AND topic = ?")) {
            pstmt.setBoolean(1, completed);
            pstmt.setInt(2, studyPlanId);
            pstmt.setString(3, date.toString());
            pstmt.setString(4, subject);
            pstmt.setString(5, topic);
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating item completion: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get completion statistics for a study plan
     * @param studyPlanId Study plan ID
     * @return Map with total and completed counts
     */
    public Map<String, Integer> getCompletionStats(int studyPlanId) {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("total", 0);
        stats.put("completed", 0);
        
        try (PreparedStatement pstmt = connection.prepareStatement(
                "SELECT COUNT(*) as total, SUM(CASE WHEN completed = 1 THEN 1 ELSE 0 END) as completed " +
                "FROM daily_items WHERE study_plan_id = ?")) {
            pstmt.setInt(1, studyPlanId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    stats.put("total", rs.getInt("total"));
                    stats.put("completed", rs.getInt("completed"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting completion stats: " + e.getMessage());
        }
        
        return stats;
    }
    
    /**
     * Close the database connection
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed");
            }
        } catch (SQLException e) {
            System.err.println("Error closing database connection: " + e.getMessage());
        }
    }
} 