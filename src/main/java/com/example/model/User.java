package com.example.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Model class representing a user of the study planner
 */
public class User {
    private String name;
    private List<Subject> subjects;
    private LocalDate examDate;

    public User() {
        this.subjects = new ArrayList<>();
    }

    public User(String name, LocalDate examDate) {
        this.name = name;
        this.examDate = examDate;
        this.subjects = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Subject> getSubjects() {
        return subjects;
    }

    public void addSubject(Subject subject) {
        this.subjects.add(subject);
    }

    public void removeSubject(Subject subject) {
        this.subjects.remove(subject);
    }

    public LocalDate getExamDate() {
        return examDate;
    }

    public void setExamDate(LocalDate examDate) {
        this.examDate = examDate;
    }

    /**
     * Returns all subjects and their topics as a map
     * @return Map of subject names to lists of topics
     */
    public Map<String, List<String>> getAllSubjectsWithTopics() {
        Map<String, List<String>> result = new HashMap<>();
        for (Subject subject : subjects) {
            result.put(subject.getName(), subject.getTopics());
        }
        return result;
    }
} 