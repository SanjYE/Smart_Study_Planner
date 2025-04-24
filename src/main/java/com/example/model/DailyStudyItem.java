package com.example.model;

/**
 * Model class representing a daily study item in the study plan
 */
public class DailyStudyItem {
    private String subject;
    private String topic;
    private double hoursRecommended;
    private boolean completed;

    public DailyStudyItem(String subject, String topic, double hoursRecommended) {
        this.subject = subject;
        this.topic = topic;
        this.hoursRecommended = hoursRecommended;
        this.completed = false; // Default to not completed
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public double getHoursRecommended() {
        return hoursRecommended;
    }

    public void setHoursRecommended(double hoursRecommended) {
        this.hoursRecommended = hoursRecommended;
    }
    
    public boolean isCompleted() {
        return completed;
    }
    
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    @Override
    public String toString() {
        return subject + " - " + topic + " (" + hoursRecommended + " hours)" + (completed ? " ✓" : "");
    }
} 