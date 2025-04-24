package com.example.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.example.model.AuthenticatedUser;
import com.example.model.DailyStudyItem;
import com.example.model.StudyPlan;
import com.example.model.Subject;
import com.example.model.User;
import com.example.service.DatabaseService;
import com.example.service.StudyPlanGenerator;
import com.example.service.factory.StudyPlanStrategyFactory;
import com.example.service.observer.StudyPlanObserver;

/**
 * Controller for study plan generation and management
 * Part of the MVC architecture
 */
public class StudyPlanController {
    private final StudyPlanGenerator studyPlanGenerator;
    private final DatabaseService databaseService;
    private User currentUser;
    private StudyPlan currentStudyPlan;
    private AuthenticatedUser authenticatedUser;
    private String currentStrategy;
    private final List<StudyPlanObserver> observers;
    
    public StudyPlanController() {
        this.studyPlanGenerator = new StudyPlanGenerator();
        this.currentUser = new User();
        this.databaseService = DatabaseService.getInstance();
        this.observers = new ArrayList<>();
    }
    
    /**
     * Register a new user
     * @param username Username
     * @param password Password
     * @return true if registration successful, false otherwise
     */
    public boolean registerUser(String username, String password) {
        return databaseService.registerUser(username, password);
    }
    
    /**
     * Login a user
     * @param username Username
     * @param password Password
     * @return true if login successful, false otherwise
     */
    public boolean login(String username, String password) {
        int userId = databaseService.authenticateUser(username, password);
        if (userId > 0) {
            this.authenticatedUser = new AuthenticatedUser(userId, username);
            return true;
        }
        return false;
    }
    
    /**
     * Check if a user is logged in
     * @return true if a user is logged in, false otherwise
     */
    public boolean isUserLoggedIn() {
        return authenticatedUser != null;
    }
    
    /**
     * Logout the current user
     */
    public void logout() {
        this.authenticatedUser = null;
        this.currentStudyPlan = null;
        // Reset user to a new empty user
        this.currentUser = new User();
    }
    
    /**
     * Get the currently authenticated user
     * @return The authenticated user
     */
    public AuthenticatedUser getAuthenticatedUser() {
        return authenticatedUser;
    }
    
    /**
     * Clear all user data to start fresh
     * This prevents combining subjects across sessions
     */
    public void clearUserData() {
        this.currentUser = new User();
    }
    
    /**
     * Set the current user
     * @param name The user's name
     * @param examDate The user's exam date
     */
    public void setUser(String name, LocalDate examDate) {
        // Create a new user with the updated information but preserve existing subjects
        User newUser = new User(name, examDate);
        
        // Copy all subjects from the current user to the new user
        if (currentUser != null && currentUser.getSubjects() != null) {
            for (Subject subject : currentUser.getSubjects()) {
                newUser.addSubject(subject);
            }
        }
        
        // Set the new user as the current user
        currentUser = newUser;
        
        // Print user details for debugging
        System.out.println("User updated: " + name + ", Exam date: " + examDate);
        System.out.println("Subjects count: " + currentUser.getSubjects().size());
        for (Subject subject : currentUser.getSubjects()) {
            System.out.println("Subject: " + subject.getName() + ", Topics: " + subject.getTopics());
        }
    }
    
    /**
     * Add a subject to the current user
     * @param subjectName The name of the subject
     * @return The added subject
     */
    public Subject addSubject(String subjectName) {
        Subject subject = new Subject(subjectName);
        currentUser.addSubject(subject);
        return subject;
    }
    
    /**
     * Add a topic to a subject
     * @param subject The subject to add the topic to
     * @param topicName The name of the topic
     */
    public void addTopic(Subject subject, String topicName) {
        subject.addTopic(topicName);
    }
    
    /**
     * Generate a study plan synchronously
     * @param strategyType The type of study plan strategy to use
     * @return The generated study plan
     * @throws IOException If an I/O error occurs
     */
    public StudyPlan generateStudyPlan(StudyPlanStrategyFactory.StrategyType strategyType) throws IOException {
        currentStudyPlan = studyPlanGenerator.generatePlan(currentUser, strategyType);
        currentStrategy = strategyType.toString();
        
        // Save to database if user is logged in
        if (isUserLoggedIn()) {
            int studyPlanId = databaseService.saveStudyPlan(
                authenticatedUser.getId(),
                currentStudyPlan,
                strategyType.toString()
            );
            if (studyPlanId > 0) {
                authenticatedUser.setCurrentStudyPlanId(studyPlanId);
            }
        }
        
        return currentStudyPlan;
    }
    
    /**
     * Generate a study plan asynchronously
     * @param strategyType The type of study plan strategy to use
     * @return A CompletableFuture that will complete with the generated study plan
     */
    public CompletableFuture<StudyPlan> generateStudyPlanAsync(StudyPlanStrategyFactory.StrategyType strategyType) {
        currentStrategy = strategyType.toString();
        return studyPlanGenerator.generatePlanAsync(currentUser, strategyType)
                .thenApply(studyPlan -> {
                    currentStudyPlan = studyPlan;
                    
                    // Save to database if user is logged in
                    if (isUserLoggedIn()) {
                        int studyPlanId = databaseService.saveStudyPlan(
                            authenticatedUser.getId(),
                            currentStudyPlan,
                            strategyType.toString()
                        );
                        if (studyPlanId > 0) {
                            authenticatedUser.setCurrentStudyPlanId(studyPlanId);
                        }
                    }
                    
                    return studyPlan;
                });
    }
    
    /**
     * Update the completion status of a study item
     * @param date The date of the item
     * @param subject The subject of the item
     * @param topic The topic of the item
     * @param completed The new completion status
     * @return true if update successful, false otherwise
     */
    public boolean updateItemCompletion(LocalDate date, String subject, String topic, boolean completed) {
        // Update in memory model
        List<DailyStudyItem> dailyItems = currentStudyPlan.getDailyPlan().get(date);
        boolean updatedInMemory = false;
        
        if (dailyItems != null) {
            for (DailyStudyItem item : dailyItems) {
                if (item.getSubject().equals(subject) && item.getTopic().equals(topic)) {
                    item.setCompleted(completed);
                    updatedInMemory = true;
                    break;
                }
            }
        }
        
        // Update in database if user is logged in and has a loaded study plan
        if (isUserLoggedIn() && authenticatedUser.hasLoadedStudyPlan()) {
            return databaseService.updateItemCompletion(
                authenticatedUser.getCurrentStudyPlanId(),
                date,
                subject,
                topic,
                completed
            ) && updatedInMemory;
        }
        
        return updatedInMemory;
    }
    
    /**
     * Get completion statistics for the current study plan
     * @return Map with total and completed counts
     */
    public Map<String, Integer> getCompletionStats() {
        if (isUserLoggedIn() && authenticatedUser.hasLoadedStudyPlan()) {
            return databaseService.getCompletionStats(authenticatedUser.getCurrentStudyPlanId());
        } else if (currentStudyPlan != null) {
            // Calculate in-memory stats if not persisted
            int total = 0;
            int completed = 0;
            
            for (List<DailyStudyItem> items : currentStudyPlan.getDailyPlan().values()) {
                for (DailyStudyItem item : items) {
                    total++;
                    if (item.isCompleted()) {
                        completed++;
                    }
                }
            }
            
            Map<String, Integer> stats = Map.of("total", total, "completed", completed);
            return stats;
        }
        
        return Map.of("total", 0, "completed", 0);
    }
    
    /**
     * Get a list of study plans for the current user
     * @return List of study plan details
     */
    public List<Map<String, Object>> getUserStudyPlans() {
        if (isUserLoggedIn()) {
            return databaseService.getUserStudyPlans(authenticatedUser.getId());
        }
        return List.of();
    }
    
    /**
     * Load a study plan from the database
     * @param studyPlanId Study plan ID
     * @return true if load successful, false otherwise
     */
    public boolean loadStudyPlan(int studyPlanId) {
        if (!isUserLoggedIn()) {
            return false;
        }
        
        StudyPlan studyPlan = databaseService.loadStudyPlan(studyPlanId);
        if (studyPlan != null) {
            currentStudyPlan = studyPlan;
            currentUser = studyPlan.getUser();
            authenticatedUser.setCurrentStudyPlanId(studyPlanId);
            
            // Notify observers that a study plan was loaded - more explicitly
            for (StudyPlanObserver observer : observers) {
                observer.onGenerationCompleted(studyPlan);
            }
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Get the current study plan
     * @return The current study plan
     */
    public StudyPlan getCurrentStudyPlan() {
        return currentStudyPlan;
    }
    
    /**
     * Get the current user
     * @return The current user
     */
    public User getCurrentUser() {
        return currentUser;
    }
    
    /**
     * Add an observer to be notified of study plan generation events
     * @param observer The observer to add
     */
    public void addObserver(StudyPlanObserver observer) {
        studyPlanGenerator.addObserver(observer);
        observers.add(observer);
    }
    
    /**
     * Remove an observer from the notification list
     * @param observer The observer to remove
     */
    public void removeObserver(StudyPlanObserver observer) {
        studyPlanGenerator.removeObserver(observer);
        observers.remove(observer);
    }
    
    /**
     * Shutdown the study plan generator and database connection
     */
    public void shutdown() {
        studyPlanGenerator.shutdown();
        databaseService.close();
    }
} 