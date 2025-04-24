package com.example.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.model.StudyPlan;
import com.example.model.User;
import com.example.service.factory.StudyPlanStrategyFactory;
import com.example.service.observer.StudyPlanObserver;
import com.example.service.strategy.StudyPlanStrategy;

/**
 * Generator for study plans that notifies observers
 * Implements the Observable part of the Observer design pattern
 */
public class StudyPlanGenerator {
    private final List<StudyPlanObserver> observers;
    private final ExecutorService executorService;
    private final StudyPlanStrategyFactory strategyFactory;
    
    public StudyPlanGenerator() {
        this.observers = new ArrayList<>();
        this.executorService = Executors.newSingleThreadExecutor();
        this.strategyFactory = new StudyPlanStrategyFactory();
    }
    
    /**
     * Add an observer to be notified of study plan generation events
     * @param observer The observer to add
     */
    public void addObserver(StudyPlanObserver observer) {
        observers.add(observer);
    }
    
    /**
     * Remove an observer from the notification list
     * @param observer The observer to remove
     */
    public void removeObserver(StudyPlanObserver observer) {
        observers.remove(observer);
    }
    
    /**
     * Generate a study plan asynchronously
     * @param user The user to generate a plan for
     * @param strategyType The type of study plan strategy to use
     * @return A CompletableFuture that will complete with the generated study plan
     */
    public CompletableFuture<StudyPlan> generatePlanAsync(User user, StudyPlanStrategyFactory.StrategyType strategyType) {
        notifyGenerationStarted();
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                StudyPlanStrategy strategy = strategyFactory.createStrategy(strategyType);
                StudyPlan studyPlan = strategy.generatePlan(user);
                notifyGenerationCompleted(studyPlan);
                return studyPlan;
            } catch (Exception e) {
                notifyGenerationFailed(e);
                throw new RuntimeException("Failed to generate study plan", e);
            }
        }, executorService);
    }
    
    /**
     * Generate a study plan synchronously
     * @param user The user to generate a plan for
     * @param strategyType The type of study plan strategy to use
     * @return The generated study plan
     * @throws IOException If an I/O error occurs
     */
    public StudyPlan generatePlan(User user, StudyPlanStrategyFactory.StrategyType strategyType) throws IOException {
        notifyGenerationStarted();
        
        try {
            StudyPlanStrategy strategy = strategyFactory.createStrategy(strategyType);
            StudyPlan studyPlan = strategy.generatePlan(user);
            notifyGenerationCompleted(studyPlan);
            return studyPlan;
        } catch (Exception e) {
            notifyGenerationFailed(e);
            if (e instanceof IOException) {
                throw (IOException) e;
            } else {
                throw new IOException("Failed to generate study plan", e);
            }
        }
    }
    
    private void notifyGenerationStarted() {
        for (StudyPlanObserver observer : observers) {
            observer.onGenerationStarted();
        }
    }
    
    private void notifyGenerationCompleted(StudyPlan studyPlan) {
        for (StudyPlanObserver observer : observers) {
            observer.onGenerationCompleted(studyPlan);
        }
    }
    
    private void notifyGenerationFailed(Exception exception) {
        for (StudyPlanObserver observer : observers) {
            observer.onGenerationFailed(exception);
        }
    }
    
    /**
     * Shutdown the executor service
     */
    public void shutdown() {
        executorService.shutdown();
    }
} 