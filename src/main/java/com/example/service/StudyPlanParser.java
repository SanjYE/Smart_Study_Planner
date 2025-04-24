package com.example.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.model.DailyStudyItem;

/**
 * Parser for converting Gemini AI responses into structured study plans
 */
public class StudyPlanParser {
    
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("MMM d, yyyy"),
            DateTimeFormatter.ofPattern("MMMM d, yyyy"),
            DateTimeFormatter.ofPattern("d MMMM yyyy")
    };
    
    /**
     * Parse the raw text response from Gemini into a structured study plan
     * 
     * @param rawPlanText The raw text from Gemini
     * @param startDate The start date of the study plan
     * @param endDate The end date of the study plan (exam date)
     * @return A map of dates to daily study items
     */
    public Map<LocalDate, List<DailyStudyItem>> parsePlan(String rawPlanText, LocalDate startDate, LocalDate endDate) {
        System.out.println("Parsing study plan...");
        
        Map<LocalDate, List<DailyStudyItem>> result = new HashMap<>();
        
        // Split into lines
        String[] lines = rawPlanText.split("\\r?\\n");
        
        LocalDate currentDate = null;
        List<DailyStudyItem> currentDayItems = null;
        
        // Process line by line
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            System.out.println("Processing line: " + line);
            
            // Try to extract a date
            LocalDate possibleDate = extractDate(line, startDate, endDate);
            if (possibleDate != null) {
                System.out.println("Found date: " + possibleDate);
                
                // We found a new date, save previous day's items if there are any
                if (currentDate != null && currentDayItems != null && !currentDayItems.isEmpty()) {
                    result.put(currentDate, new ArrayList<>(currentDayItems));
                }
                
                // Start a new day
                currentDate = possibleDate;
                currentDayItems = new ArrayList<>();
                continue;
            }
            
            // If we have a current date, try to parse study items
            if (currentDate != null && currentDayItems != null) {
                DailyStudyItem item = extractStudyItem(line);
                if (item != null) {
                    System.out.println("Found study item: " + item);
                    currentDayItems.add(item);
                }
            }
        }
        
        // Add the last day's items if we have any
        if (currentDate != null && currentDayItems != null && !currentDayItems.isEmpty()) {
            result.put(currentDate, currentDayItems);
        }
        
        // If we didn't find any properly formatted dates, try to distribute items by day
        if (result.isEmpty() && lines.length > 0) {
            System.out.println("No structured data found. Creating default plan.");
            return createDefaultPlan(rawPlanText, startDate, endDate);
        }
        
        System.out.println("Parsing complete. Found " + result.size() + " days with study items.");
        return result;
    }
    
    /**
     * Extract a date from a line of text
     */
    private LocalDate extractDate(String line, LocalDate startDate, LocalDate endDate) {
        // First, look for specific "Date:" prefix
        Pattern datePrefix = Pattern.compile("(?i)\\b(?:date|day)\\s*:\\s*(.+)");
        Matcher datePrefixMatcher = datePrefix.matcher(line);
        
        if (datePrefixMatcher.find()) {
            String dateText = datePrefixMatcher.group(1).trim();
            LocalDate date = parseDate(dateText, startDate, endDate);
            if (date != null) {
                return date;
            }
        }
        
        // Second, try to find a date pattern directly in the whole line
        LocalDate date = parseDate(line, startDate, endDate);
        if (date != null) {
            return date;
        }
        
        // Try more lenient approach for lines that just have "Day X" or similar
        Pattern dayPattern = Pattern.compile("\\b(?:day|date)\\s*(\\d+)\\b", Pattern.CASE_INSENSITIVE);
        Matcher dayMatcher = dayPattern.matcher(line);
        
        if (dayMatcher.find()) {
            try {
                int dayOffset = Integer.parseInt(dayMatcher.group(1)) - 1;
                LocalDate offsetDate = startDate.plusDays(dayOffset);
                
                if (!offsetDate.isAfter(endDate)) {
                    return offsetDate;
                }
            } catch (NumberFormatException e) {
                // Ignore and continue
            }
        }
        
        return null;
    }
    
    /**
     * Try to parse a date string using multiple formatters
     */
    private LocalDate parseDate(String dateText, LocalDate startDate, LocalDate endDate) {
        // Try each formatter
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                LocalDate date = LocalDate.parse(dateText, formatter);
                if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
                    return date;
                }
            } catch (DateTimeParseException e) {
                // Try the next formatter
            }
        }
        
        // Try to extract and parse date patterns
        Pattern datePattern = Pattern.compile("\\b\\d{1,4}[\\-/\\s]\\d{1,2}[\\-/\\s]\\d{1,4}\\b|\\b\\w+ \\d{1,2},? \\d{4}\\b|\\b\\d{1,2} \\w+ \\d{4}\\b");
        Matcher dateMatcher = datePattern.matcher(dateText);
        
        if (dateMatcher.find()) {
            String extractedDate = dateMatcher.group();
            for (DateTimeFormatter formatter : DATE_FORMATTERS) {
                try {
                    LocalDate date = LocalDate.parse(extractedDate, formatter);
                    if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
                        return date;
                    }
                } catch (DateTimeParseException e) {
                    // Try the next formatter
                }
            }
        }
        
        return null;
    }
    
    /**
     * Extract a study item from a line of text
     */
    private DailyStudyItem extractStudyItem(String line) {
        // Check for lines starting with a bullet point or dash
        if (line.startsWith("-") || line.startsWith("•") || line.startsWith("*")) {
            line = line.substring(1).trim();
        }
        
        // Try several patterns:
        
        // Pattern 1: Subject: [subject], Topic: [topic], Hours: [hours]
        Pattern pattern1 = Pattern.compile("(?i)subject\\s*:\\s*([^,]+)\\s*,\\s*topic\\s*:\\s*([^,]+)\\s*,\\s*hours\\s*:\\s*(\\d+(?:\\.\\d+)?)");
        Matcher matcher1 = pattern1.matcher(line);
        
        if (matcher1.find()) {
            String subject = matcher1.group(1).trim();
            String topic = matcher1.group(2).trim();
            double hours = Double.parseDouble(matcher1.group(3));
            return new DailyStudyItem(subject, topic, hours);
        }
        
        // Pattern 2: [Subject] - [Topic] ([X] hours)
        Pattern pattern2 = Pattern.compile("([^\\-]+)\\s*-\\s*([^\\(]+)\\s*\\(\\s*(\\d+(?:\\.\\d+)?)\\s*(?:hours|hrs|hour|hr)\\s*\\)");
        Matcher matcher2 = pattern2.matcher(line);
        
        if (matcher2.find()) {
            String subject = matcher2.group(1).trim();
            String topic = matcher2.group(2).trim();
            double hours = Double.parseDouble(matcher2.group(3));
            return new DailyStudyItem(subject, topic, hours);
        }
        
        // Pattern 3: [Subject]: [Topic] - [X] hours
        Pattern pattern3 = Pattern.compile("([^:]+)\\s*:\\s*([^\\-]+)\\s*-\\s*(\\d+(?:\\.\\d+)?)\\s*(?:hours|hrs|hour|hr)");
        Matcher matcher3 = pattern3.matcher(line);
        
        if (matcher3.find()) {
            String subject = matcher3.group(1).trim();
            String topic = matcher3.group(2).trim();
            double hours = Double.parseDouble(matcher3.group(3));
            return new DailyStudyItem(subject, topic, hours);
        }
        
        // Pattern 4: [X] hours - [Subject]: [Topic]
        Pattern pattern4 = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:hours|hrs|hour|hr)\\s*-\\s*([^:]+)\\s*:\\s*(.+)");
        Matcher matcher4 = pattern4.matcher(line);
        
        if (matcher4.find()) {
            double hours = Double.parseDouble(matcher4.group(1));
            String subject = matcher4.group(2).trim();
            String topic = matcher4.group(3).trim();
            return new DailyStudyItem(subject, topic, hours);
        }
        
        // Pattern 5: [Subject]: [Topic] ([X] hours)
        Pattern pattern5 = Pattern.compile("([^:]+)\\s*:\\s*([^\\(]+)\\s*\\(\\s*(\\d+(?:\\.\\d+)?)\\s*(?:hours|hrs|hour|hr)\\s*\\)");
        Matcher matcher5 = pattern5.matcher(line);
        
        if (matcher5.find()) {
            String subject = matcher5.group(1).trim();
            String topic = matcher5.group(2).trim();
            double hours = Double.parseDouble(matcher5.group(3));
            return new DailyStudyItem(subject, topic, hours);
        }
        
        return null;
    }
    
    /**
     * Create a default plan if the AI response couldn't be properly parsed
     */
    private Map<LocalDate, List<DailyStudyItem>> createDefaultPlan(String rawPlanText, LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, List<DailyStudyItem>> result = new HashMap<>();
        List<DailyStudyItem> allItems = new ArrayList<>();
        
        // Extract all possible study items
        String[] lines = rawPlanText.split("\\r?\\n");
        for (String line : lines) {
            DailyStudyItem item = extractStudyItem(line);
            if (item != null) {
                allItems.add(item);
            }
        }
        
        // If we still couldn't extract any items, try to create some basic ones
        if (allItems.isEmpty()) {
            // Try to extract subject and topic from text
            Pattern subjectTopicPattern = Pattern.compile("(?i)subject\\s*:\\s*([^\\n,]+).*?topic\\s*:\\s*([^\\n,]+)");
            Matcher stMatcher = subjectTopicPattern.matcher(rawPlanText);
            
            if (stMatcher.find()) {
                String subject = stMatcher.group(1).trim();
                String topic = stMatcher.group(2).trim();
                
                // Create one item per day with 2 hours recommended
                int totalDays = (int) startDate.until(endDate).getDays() + 1;
                for (int i = 0; i < totalDays; i++) {
                    DailyStudyItem item = new DailyStudyItem(subject, topic, 2.0);
                    allItems.add(item);
                }
            }
        }
        
        // Distribute items across the available days
        if (!allItems.isEmpty()) {
            int totalDays = (int) startDate.until(endDate).getDays() + 1;
            int itemsPerDay = Math.max(1, allItems.size() / totalDays);
            
            for (int day = 0; day < totalDays; day++) {
                LocalDate date = startDate.plusDays(day);
                List<DailyStudyItem> dayItems = new ArrayList<>();
                
                int startIndex = day * itemsPerDay;
                int endIndex = Math.min(startIndex + itemsPerDay, allItems.size());
                
                if (startIndex < allItems.size()) {
                    for (int i = startIndex; i < endIndex; i++) {
                        dayItems.add(allItems.get(i));
                    }
                    result.put(date, dayItems);
                }
            }
        }
        
        return result;
    }
} 