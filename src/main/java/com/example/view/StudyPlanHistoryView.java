package com.example.view;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import com.example.controller.StudyPlanController;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Callback;

/**
 * View for displaying study plan history
 */
public class StudyPlanHistoryView extends BorderPane {
    
    private final StudyPlanController controller;
    private final TableView<Map<String, Object>> tableView;
    private final ObservableList<Map<String, Object>> tableData;
    private final TabPane tabPane;
    private final Tab studyPlanTab;
    
    /**
     * Create a new study plan history view
     * @param controller The controller to use
     * @param tabPane The main tab pane
     * @param studyPlanTab The study plan tab
     */
    public StudyPlanHistoryView(StudyPlanController controller, TabPane tabPane, Tab studyPlanTab) {
        this.controller = controller;
        this.tabPane = tabPane;
        this.studyPlanTab = studyPlanTab;
        
        setPadding(new Insets(20));
        
        // Header
        Label headerLabel = new Label("Your Study Plan History");
        headerLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        
        // Create refresh button
        Button refreshButton = new Button("Refresh List");
        refreshButton.setOnAction(e -> refreshStudyPlans());
        
        HBox headerBox = new HBox(10, headerLabel, refreshButton);
        headerBox.setPadding(new Insets(0, 0, 10, 0));
        
        // Message for guests
        Label guestLabel = new Label("You need to be logged in to view your study plan history.");
        guestLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        guestLabel.setVisible(false);
        
        // Initialize table
        tableView = new TableView<>();
        tableData = FXCollections.observableArrayList();
        tableView.setItems(tableData);
        
        // Create columns
        TableColumn<Map<String, Object>, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty((String) cellData.getValue().get("name")));
        nameColumn.setPrefWidth(200);
        
        TableColumn<Map<String, Object>, String> examDateColumn = new TableColumn<>("Exam Date");
        examDateColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(formatExamDate((String) cellData.getValue().get("examDate"))));
        examDateColumn.setPrefWidth(120);
        
        TableColumn<Map<String, Object>, String> createdAtColumn = new TableColumn<>("Created On");
        createdAtColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(formatCreatedAt((String) cellData.getValue().get("createdAt"))));
        createdAtColumn.setPrefWidth(150);
        
        TableColumn<Map<String, Object>, String> actionsColumn = new TableColumn<>("Actions");
        actionsColumn.setCellValueFactory(param -> new SimpleStringProperty(""));
        actionsColumn.setPrefWidth(100);
        actionsColumn.setCellFactory(new Callback<TableColumn<Map<String, Object>, String>, 
                                         TableCell<Map<String, Object>, String>>() {
            @Override
            public TableCell<Map<String, Object>, String> call(TableColumn<Map<String, Object>, String> param) {
                return new TableCell<Map<String, Object>, String>() {
                    private final Button loadButton = new Button("Load");
                    
                    {
                        loadButton.setOnAction(e -> {
                            if (getTableRow() != null && getTableRow().getItem() != null) {
                                Map<String, Object> studyPlan = (Map<String, Object>) getTableRow().getItem();
                                int studyPlanId = (int) studyPlan.get("id");
                                if (controller.loadStudyPlan(studyPlanId)) {
                                    // Switch to study plan tab
                                    tabPane.getSelectionModel().select(studyPlanTab);
                                }
                            }
                        });
                    }
                    
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(loadButton);
                        }
                    }
                };
            }
        });
        
        tableView.getColumns().addAll(nameColumn, examDateColumn, createdAtColumn, actionsColumn);
        
        // Layout
        VBox contentBox = new VBox(10, headerBox, guestLabel, tableView);
        setCenter(contentBox);
        
        // Initial refresh
        if (controller.isUserLoggedIn()) {
            guestLabel.setVisible(false);
            tableView.setVisible(true);
            refreshStudyPlans();
        } else {
            guestLabel.setVisible(true);
            tableView.setVisible(false);
        }
    }
    
    /**
     * Format exam date for display
     * @param dateStr Date string in ISO format
     * @return Formatted date
     */
    private String formatExamDate(String dateStr) {
        if (dateStr == null) return "";
        try {
            LocalDate date = LocalDate.parse(dateStr);
            return date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
        } catch (Exception e) {
            return dateStr;
        }
    }
    
    /**
     * Format created at timestamp for display
     * @param timestamp Timestamp string
     * @return Formatted timestamp
     */
    private String formatCreatedAt(String timestamp) {
        if (timestamp == null) return "";
        return timestamp.substring(0, Math.min(16, timestamp.length())).replace("T", " ");
    }
    
    /**
     * Refresh the study plans list
     */
    public void refreshStudyPlans() {
        if (controller.isUserLoggedIn()) {
            tableData.clear();
            List<Map<String, Object>> plans = controller.getUserStudyPlans();
            tableData.addAll(plans);
        }
    }
    
    /**
     * Update the view based on login status
     * @param isLoggedIn True if user is logged in, false otherwise
     */
    public void updateLoginStatus(boolean isLoggedIn) {
        Label guestLabel = (Label) ((VBox) getCenter()).getChildren().get(1);
        TableView<?> tableView = (TableView<?>) ((VBox) getCenter()).getChildren().get(2);
        
        if (isLoggedIn) {
            guestLabel.setVisible(false);
            tableView.setVisible(true);
            refreshStudyPlans();
        } else {
            guestLabel.setVisible(true);
            tableView.setVisible(false);
            tableData.clear();
        }
    }
} 