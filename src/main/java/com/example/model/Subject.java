package com.example.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Model class representing a study subject with multiple topics
 */
public class Subject {
    private String name;
    private List<String> topics;

    public Subject() {
        this.topics = new ArrayList<>();
    }

    public Subject(String name) {
        this.name = name;
        this.topics = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getTopics() {
        return topics;
    }

    public void addTopic(String topic) {
        this.topics.add(topic);
    }

    public void removeTopic(String topic) {
        this.topics.remove(topic);
    }

    @Override
    public String toString() {
        return name;
    }
} 