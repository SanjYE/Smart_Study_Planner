package com.example.model;

/**
 * Model class representing an authenticated user
 */
public class AuthenticatedUser {
    private int id;
    private String username;
    private int currentStudyPlanId;
    
    public AuthenticatedUser(int id, String username) {
        this.id = id;
        this.username = username;
        this.currentStudyPlanId = -1; // No study plan loaded by default
    }
    
    public int getId() {
        return id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public int getCurrentStudyPlanId() {
        return currentStudyPlanId;
    }
    
    public void setCurrentStudyPlanId(int currentStudyPlanId) {
        this.currentStudyPlanId = currentStudyPlanId;
    }
    
    public boolean hasLoadedStudyPlan() {
        return currentStudyPlanId > 0;
    }
} 