package com.example.model;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Model class representing a generated study plan
 */
public class StudyPlan {
    private User user;
    private Map<LocalDate, List<DailyStudyItem>> dailyPlan;
    private String rawPlanText;

    public StudyPlan(User user) {
        this.user = user;
        this.dailyPlan = new HashMap<>();
    }

    public User getUser() {
        return user;
    }

    public Map<LocalDate, List<DailyStudyItem>> getDailyPlan() {
        return dailyPlan;
    }

    public void setDailyPlan(Map<LocalDate, List<DailyStudyItem>> dailyPlan) {
        this.dailyPlan = dailyPlan;
    }

    public void addDailyItems(LocalDate date, List<DailyStudyItem> items) {
        dailyPlan.put(date, items);
    }

    public String getRawPlanText() {
        return rawPlanText;
    }

    public void setRawPlanText(String rawPlanText) {
        this.rawPlanText = rawPlanText;
    }
} 