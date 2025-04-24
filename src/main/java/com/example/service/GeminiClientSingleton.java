package com.example.service;

import java.io.IOException;

import com.example.GeminiClient;

/**
 * Singleton implementation for the GeminiClient
 * Implements the Singleton design pattern
 */
public class GeminiClientSingleton {
    private static GeminiClientSingleton instance;
    
    private GeminiClientSingleton() {
        // Private constructor to prevent direct instantiation
    }
    
    /**
     * Get the singleton instance of GeminiClientSingleton
     * @return the singleton instance
     */
    public static synchronized GeminiClientSingleton getInstance() {
        if (instance == null) {
            instance = new GeminiClientSingleton();
        }
        return instance;
    }
    
    /**
     * Generate text using the Gemini API
     * @param prompt The prompt to send to Gemini
     * @return The generated text response
     * @throws IOException If an I/O error occurs
     */
    public String generateText(String prompt) throws IOException {
        return GeminiClient.generateText(prompt);
    }
} 