package com.example.service.factory;

import com.example.service.strategy.BalancedStudyPlanStrategy;
import com.example.service.strategy.IntensiveStudyPlanStrategy;
import com.example.service.strategy.StudyPlanStrategy;

/**
 * Factory for creating study plan strategies
 * Implements the Factory Method design pattern
 */
public class StudyPlanStrategyFactory {
    
    /**
     * Available study plan strategy types
     */
    public enum StrategyType {
        BALANCED,
        INTENSIVE
    }
    
    /**
     * Create a study plan strategy of the specified type
     * @param type The type of strategy to create
     * @return The created strategy
     */
    public StudyPlanStrategy createStrategy(StrategyType type) {
        switch (type) {
            case BALANCED:
                return new BalancedStudyPlanStrategy();
            case INTENSIVE:
                return new IntensiveStudyPlanStrategy();
            default:
                throw new IllegalArgumentException("Unknown strategy type: " + type);
        }
    }
} 