package com.example.service.observer;

import com.example.model.StudyPlan;

/**
 * Observer interface for study plan generation
 * Implements the Observer design pattern
 */
public interface StudyPlanObserver {
    /**
     * Called when a study plan generation starts
     */
    void onGenerationStarted();
    
    /**
     * Called when a study plan is successfully generated
     * @param studyPlan The generated study plan
     */
    void onGenerationCompleted(StudyPlan studyPlan);
    
    /**
     * Called when a study plan generation fails
     * @param exception The exception that occurred
     */
    void onGenerationFailed(Exception exception);
} 