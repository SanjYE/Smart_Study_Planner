package com.example.service.strategy;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import com.example.model.DailyStudyItem;
import com.example.model.StudyPlan;
import com.example.model.User;
import com.example.service.GeminiClientSingleton;
import com.example.service.StudyPlanParser;

/**
 * Balanced strategy for study plan generation
 * Implements the Strategy design pattern
 */
public class BalancedStudyPlanStrategy implements StudyPlanStrategy {
    
    @Override
    public StudyPlan generatePlan(User user) throws IOException {
        // Create the study plan
        StudyPlan studyPlan = new StudyPlan(user);
        
        // Calculate days until exam
        LocalDate today = LocalDate.now();
        LocalDate examDate = user.getExamDate();
        long daysUntilExam = ChronoUnit.DAYS.between(today, examDate);
        
        if (daysUntilExam <= 0) {
            throw new IllegalArgumentException("Exam date must be in the future");
        }
        
        // Prepare the prompt for Gemini
        StringBuilder promptBuilder = new StringBuilder();
        
        // Create a more focused, detailed instruction
        promptBuilder.append("CREATE A BALANCED STUDY PLAN\n\n");
        promptBuilder.append("Student: ").append(user.getName()).append("\n");
        promptBuilder.append("Days until exam: ").append(daysUntilExam).append("\n");
        promptBuilder.append("Exam date: ").append(examDate).append("\n\n");
        
        promptBuilder.append("INCLUDE ONLY THE FOLLOWING SUBJECTS AND TOPICS:\n");
        
        // Add the subjects and topics with clear formatting
        Map<String, List<String>> subjectsWithTopics = user.getAllSubjectsWithTopics();
        for (Map.Entry<String, List<String>> entry : subjectsWithTopics.entrySet()) {
            promptBuilder.append("Subject: ").append(entry.getKey()).append("\n");
            promptBuilder.append("Topics: ");
            
            List<String> topics = entry.getValue();
            for (int i = 0; i < topics.size(); i++) {
                promptBuilder.append(topics.get(i));
                if (i < topics.size() - 1) {
                    promptBuilder.append(", ");
                }
            }
            promptBuilder.append("\n\n");
        }
        
        // Provide clearer formatting instructions
        promptBuilder.append("STUDY PLAN REQUIREMENTS:\n");
        promptBuilder.append("1. Focus ONLY on the exact subjects and topics listed above\n");
        promptBuilder.append("2. Create a day-by-day breakdown for all ").append(daysUntilExam).append(" days until the exam\n");
        promptBuilder.append("3. Include recommended study hours for each topic per day\n");
        promptBuilder.append("4. Ensure balanced distribution of study time across all subjects\n\n");
        
        promptBuilder.append("REQUIRED OUTPUT FORMAT:\n");
        promptBuilder.append("For each day, format as follows:\n");
        promptBuilder.append("Date: YYYY-MM-DD\n");
        promptBuilder.append("- Subject: [subject name], Topic: [topic name], Hours: [X.X]\n");
        promptBuilder.append("- Subject: [subject name], Topic: [topic name], Hours: [X.X]\n");
        promptBuilder.append("... and so on for each topic that day\n\n");
        
        promptBuilder.append("DO NOT add any subjects or topics that are not in the list above.\n");
        promptBuilder.append("DO NOT create a general plan - focus only on the specific subjects and topics provided.\n");
        
        // Get response from Gemini
        String finalPrompt = promptBuilder.toString();
        
        // Debug information
        System.out.println("Generated prompt for Gemini API:");
        System.out.println("===================================");
        System.out.println(finalPrompt);
        System.out.println("===================================");
        
        String response = GeminiClientSingleton.getInstance().generateText(finalPrompt);
        studyPlan.setRawPlanText(response);
        
        // Parse the response and update the study plan
        StudyPlanParser parser = new StudyPlanParser();
        Map<LocalDate, List<DailyStudyItem>> parsedPlan = parser.parsePlan(response, today, examDate);
        studyPlan.setDailyPlan(parsedPlan);
        
        return studyPlan;
    }
} 