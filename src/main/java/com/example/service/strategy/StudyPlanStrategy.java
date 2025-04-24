package com.example.service.strategy;

import java.io.IOException;

import com.example.model.StudyPlan;
import com.example.model.User;

/**
 * Strategy interface for different study plan generation strategies
 * Implements the Strategy design pattern
 */
public interface StudyPlanStrategy {
    /**
     * Generate a study plan for the given user
     * @param user The user to generate a study plan for
     * @return The generated study plan
     * @throws IOException If an I/O error occurs
     */
    StudyPlan generatePlan(User user) throws IOException;
} 