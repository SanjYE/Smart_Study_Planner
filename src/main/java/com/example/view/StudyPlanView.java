package com.example.view;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import com.example.controller.StudyPlanController;
import com.example.model.DailyStudyItem;
import com.example.model.StudyPlan;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Callback;

/**
 * View for displaying the generated study plan
 * Part of the MVC architecture
 */
public class StudyPlanView extends BorderPane {
    
    private final TableView<DayPlanRow> tableView;
    private final ObservableList<DayPlanRow> tableData;
    private final Label studyPlanHeaderLabel;
    private final TextArea rawPlanTextArea;
    private final ProgressBar progressBar;
    private final Label progressLabel;
    private StudyPlanController controller;
    
    /**
     * Create a new study plan view
     */
    public StudyPlanView() {
        setPadding(new Insets(20));
        
        // Header
        studyPlanHeaderLabel = new Label("Generated Study Plan");
        studyPlanHeaderLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        studyPlanHeaderLabel.getStyleClass().add("header-label");
        
        // Progress tracking
        Label progressHeader = new Label("Study Progress:");
        progressHeader.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(20); // Make it more visible
        progressBar.setStyle("-fx-accent: #4caf50;"); // Green color for progress
        
        progressLabel = new Label("0% Completed");
        progressLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        
        VBox progressBox = new VBox(5, progressHeader, progressBar, progressLabel);
        progressBox.setPadding(new Insets(10, 0, 15, 0));
        progressBox.setStyle("-fx-background-color: #f8f8f8; -fx-padding: 10px; -fx-background-radius: 4px; -fx-border-color: #e0e0e0; -fx-border-radius: 4px;");
        progressBox.getStyleClass().add("progress-box");
        
        // Initialize table
        tableView = new TableView<>();
        tableData = FXCollections.observableArrayList();
        tableView.setItems(tableData);
        VBox.setVgrow(tableView, Priority.ALWAYS);
        
        // Create columns
        TableColumn<DayPlanRow, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(cellData -> cellData.getValue().dateProperty());
        dateColumn.setPrefWidth(120);
        
        TableColumn<DayPlanRow, String> subjectColumn = new TableColumn<>("Subject");
        subjectColumn.setCellValueFactory(cellData -> cellData.getValue().subjectProperty());
        subjectColumn.setPrefWidth(150);
        
        TableColumn<DayPlanRow, String> topicColumn = new TableColumn<>("Topic");
        topicColumn.setCellValueFactory(cellData -> cellData.getValue().topicProperty());
        topicColumn.setPrefWidth(200);
        
        TableColumn<DayPlanRow, String> hoursColumn = new TableColumn<>("Hours");
        hoursColumn.setCellValueFactory(cellData -> cellData.getValue().hoursProperty());
        hoursColumn.setPrefWidth(80);
        
        TableColumn<DayPlanRow, Boolean> completedColumn = new TableColumn<>("Completed");
        completedColumn.setCellValueFactory(cellData -> cellData.getValue().completedProperty());
        completedColumn.setPrefWidth(100);
        completedColumn.setCellFactory(createCheckboxCellFactory());
        
        tableView.getColumns().addAll(dateColumn, subjectColumn, topicColumn, hoursColumn, completedColumn);
        
        // Raw plan text display
        Label rawPlanLabel = new Label("Raw Study Plan Text:");
        rawPlanLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        rawPlanTextArea = new TextArea();
        rawPlanTextArea.setEditable(false);
        rawPlanTextArea.setWrapText(true);
        rawPlanTextArea.setPrefHeight(200);
        
        // Assemble the view
        VBox tableContainer = new VBox(10, studyPlanHeaderLabel, progressBox, tableView);
        tableContainer.getStyleClass().add("table-container");
        
        rawPlanLabel.getStyleClass().add("section-header");
        rawPlanTextArea.getStyleClass().add("raw-text-area");
        VBox rawPlanContainer = new VBox(10, rawPlanLabel, rawPlanTextArea);
        rawPlanContainer.getStyleClass().add("raw-plan-container");
        
        setCenter(tableContainer);
        setBottom(rawPlanContainer);
        getStyleClass().add("study-plan-view");
    }
    
    /**
     * Set the controller for this view
     * @param controller The controller to use
     */
    public void setController(StudyPlanController controller) {
        this.controller = controller;
    }
    
    /**
     * Create a cell factory for checkbox column
     * @return Cell factory for checkboxes
     */
    private Callback<TableColumn<DayPlanRow, Boolean>, TableCell<DayPlanRow, Boolean>> createCheckboxCellFactory() {
        return column -> new TableCell<DayPlanRow, Boolean>() {
            private final CheckBox checkBox = new CheckBox();
            
            {
                // Set up the checkbox
                checkBox.setOnAction(event -> {
                    if (getTableRow() != null && getTableRow().getItem() != null) {
                        DayPlanRow row = (DayPlanRow) getTableRow().getItem();
                        boolean newValue = checkBox.isSelected();
                        row.setCompleted(newValue);
                        
                        // Update in controller and database
                        if (controller != null) {
                            LocalDate date = LocalDate.parse(row.getDate(), 
                                    DateTimeFormatter.ofPattern("MMM d, yyyy"));
                            controller.updateItemCompletion(date, row.getSubject(), 
                                    row.getTopic(), newValue);
                            
                            // Update progress
                            updateProgress();
                        }
                    }
                });
            }
            
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    checkBox.setSelected(item != null && item);
                    setGraphic(checkBox);
                }
            }
        };
    }
    
    /**
     * Update the progress display
     */
    private void updateProgress() {
        if (controller != null) {
            Map<String, Integer> stats = controller.getCompletionStats();
            int total = stats.get("total");
            int completed = stats.get("completed");
            
            double progress = total > 0 ? (double) completed / total : 0;
            progressBar.setProgress(progress);
            
            int percentage = (int) (progress * 100);
            progressLabel.setText(percentage + "% Completed (" + completed + " of " + total + " items)");
        }
    }
    
    /**
     * Update the view with a new study plan
     * @param studyPlan The study plan to display
     */
    public void updateStudyPlan(StudyPlan studyPlan) {
        if (studyPlan == null) {
            clearView();
            return;
        }
        
        // Update header
        studyPlanHeaderLabel.setText("Study Plan for " + studyPlan.getUser().getName() + 
                " (Exam on " + studyPlan.getUser().getExamDate() + ")");
        
        // Update raw text
        rawPlanTextArea.setText(studyPlan.getRawPlanText());
        
        // Clear and update table data
        tableData.clear();
        
        // Get the daily plan from the study plan
        Map<LocalDate, List<DailyStudyItem>> dailyPlan = studyPlan.getDailyPlan();
        
        // Format for dates
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy");
        
        // Add rows for each day and item
        for (Map.Entry<LocalDate, List<DailyStudyItem>> entry : dailyPlan.entrySet()) {
            LocalDate date = entry.getKey();
            String formattedDate = date.format(dateFormatter);
            
            for (DailyStudyItem item : entry.getValue()) {
                DayPlanRow row = new DayPlanRow(
                        formattedDate,
                        item.getSubject(),
                        item.getTopic(),
                        String.format("%.1f", item.getHoursRecommended()),
                        item.isCompleted()
                );
                tableData.add(row);
            }
        }
        
        // Update progress
        updateProgress();
    }
    
    /**
     * Helper class for table rows
     */
    public static class DayPlanRow {
        private final SimpleStringProperty date;
        private final SimpleStringProperty subject;
        private final SimpleStringProperty topic;
        private final SimpleStringProperty hours;
        private final SimpleBooleanProperty completed;
        
        public DayPlanRow(String date, String subject, String topic, String hours, boolean completed) {
            this.date = new SimpleStringProperty(date);
            this.subject = new SimpleStringProperty(subject);
            this.topic = new SimpleStringProperty(topic);
            this.hours = new SimpleStringProperty(hours);
            this.completed = new SimpleBooleanProperty(completed);
        }
        
        public String getDate() {
            return date.get();
        }
        
        public SimpleStringProperty dateProperty() {
            return date;
        }
        
        public String getSubject() {
            return subject.get();
        }
        
        public SimpleStringProperty subjectProperty() {
            return subject;
        }
        
        public String getTopic() {
            return topic.get();
        }
        
        public SimpleStringProperty topicProperty() {
            return topic;
        }
        
        public String getHours() {
            return hours.get();
        }
        
        public SimpleStringProperty hoursProperty() {
            return hours;
        }
        
        public boolean isCompleted() {
            return completed.get();
        }
        
        public void setCompleted(boolean value) {
            completed.set(value);
        }
        
        public SimpleBooleanProperty completedProperty() {
            return completed;
        }
    }
    
    /**
     * Clear the view when user logs out
     */
    public void clearView() {
        studyPlanHeaderLabel.setText("Generated Study Plan");
        rawPlanTextArea.clear();
        tableData.clear();
        progressBar.setProgress(0);
        progressLabel.setText("0% Completed");
    }
} 