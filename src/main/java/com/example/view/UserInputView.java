package com.example.view;

import com.example.controller.StudyPlanController;
import com.example.model.Subject;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Callback;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * View for user input form
 * Part of the MVC architecture
 */
public class UserInputView extends VBox {
    
    private final StudyPlanController controller;
    
    private final TextField nameField;
    private final DatePicker examDatePicker;
    private final ComboBox<Subject> subjectComboBox;
    private final ObservableList<Subject> subjects;
    private final ListView<String> topicsListView;
    private final ObservableList<String> topics;
    private final TextField topicField;
    private final TextField newSubjectField;
    
    /**
     * Create a new user input view
     * @param controller The controller to use
     */
    public UserInputView(StudyPlanController controller) {
        super(10);
        this.controller = controller;
        setPadding(new Insets(20));
        
        // Initialize collections
        subjects = FXCollections.observableArrayList();
        topics = FXCollections.observableArrayList();
        
        // Name input
        Label nameLabel = new Label("Your Name:");
        nameField = new TextField();
        nameField.setPromptText("Enter your name");
        
        // Exam date input
        Label examDateLabel = new Label("Exam Date:");
        examDatePicker = new DatePicker();
        examDatePicker.setValue(LocalDate.now().plusDays(14)); // Default to 2 weeks from now
        
        // Subject selection
        Label subjectsLabel = new Label("Subjects:");
        HBox subjectInputBox = new HBox(10);
        
        newSubjectField = new TextField();
        newSubjectField.setPromptText("Enter new subject");
        
        Button addSubjectButton = new Button("Add Subject");
        addSubjectButton.setOnAction(e -> addNewSubject());
        
        subjectInputBox.getChildren().addAll(newSubjectField, addSubjectButton);
        
        subjectComboBox = new ComboBox<>(subjects);
        subjectComboBox.setPromptText("Select a subject to add topics");
        subjectComboBox.setPrefWidth(Double.MAX_VALUE);
        subjectComboBox.setCellFactory(new Callback<ListView<Subject>, ListCell<Subject>>() {
            @Override
            public ListCell<Subject> call(ListView<Subject> param) {
                return new ListCell<Subject>() {
                    @Override
                    protected void updateItem(Subject item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            setText(item.getName());
                        }
                    }
                };
            }
        });
        subjectComboBox.setButtonCell(new ListCell<Subject>() {
            @Override
            protected void updateItem(Subject item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });
        
        // Topic input
        Label topicsLabel = new Label("Topics for Selected Subject:");
        
        HBox topicInputBox = new HBox(10);
        
        topicField = new TextField();
        topicField.setPromptText("Enter a topic");
        topicField.setDisable(true);
        
        Button addTopicButton = new Button("Add Topic");
        addTopicButton.setDisable(true);
        addTopicButton.setOnAction(e -> addTopicToSubject());
        
        topicInputBox.getChildren().addAll(topicField, addTopicButton);
        
        subjectComboBox.setOnAction(e -> {
            Subject selectedSubject = subjectComboBox.getValue();
            
            if (selectedSubject != null) {
                topicField.setDisable(false);
                addTopicButton.setDisable(false);
                
                topics.clear();
                topics.addAll(selectedSubject.getTopics());
            } else {
                topicField.setDisable(true);
                addTopicButton.setDisable(true);
            }
        });
        
        topicsListView = new ListView<>(topics);
        topicsListView.setPrefHeight(200);
        
        getChildren().addAll(
                nameLabel, nameField,
                examDateLabel, examDatePicker,
                subjectsLabel, subjectInputBox, subjectComboBox,
                topicsLabel, topicInputBox, topicsListView
        );
    }
    
    /**
     * Add a new subject
     */
    private void addNewSubject() {
        String subjectName = newSubjectField.getText().trim();
        
        if (!subjectName.isEmpty()) {
            Subject subject = controller.addSubject(subjectName);
            subjects.add(subject);
            subjectComboBox.setValue(subject);
            newSubjectField.clear();
        }
    }
    
    /**
     * Add a topic to the selected subject
     */
    private void addTopicToSubject() {
        Subject selectedSubject = subjectComboBox.getValue();
        String topicName = topicField.getText().trim();
        
        if (selectedSubject != null && !topicName.isEmpty()) {
            controller.addTopic(selectedSubject, topicName);
            topics.clear();
            topics.addAll(selectedSubject.getTopics());
            topicField.clear();
        }
    }
    
    /**
     * Save the user input to the controller
     * @return true if the input is valid, false otherwise
     */
    public boolean saveUserInput() {
        String name = nameField.getText().trim();
        LocalDate examDate = examDatePicker.getValue();
        
        if (name.isEmpty()) {
            showAlert("Please enter your name");
            return false;
        }
        
        if (examDate == null || examDate.isBefore(LocalDate.now())) {
            showAlert("Please select a future exam date");
            return false;
        }
        
        if (subjects.isEmpty()) {
            showAlert("Please add at least one subject");
            return false;
        }
        
        boolean hasTopics = false;
        for (Subject subject : subjects) {
            if (!subject.getTopics().isEmpty()) {
                hasTopics = true;
                break;
            }
        }
        
        if (!hasTopics) {
            showAlert("Please add at least one topic to a subject");
            return false;
        }
        
        // Clear previous user data to avoid combining subjects across sessions
        controller.clearUserData();
        controller.setUser(name, examDate);
        
        // Need to add subjects again since they were cleared
        for (Subject subject : new ArrayList<>(subjects)) {
            Subject newSubject = controller.addSubject(subject.getName());
            for (String topic : subject.getTopics()) {
                controller.addTopic(newSubject, topic);
            }
        }
        
        return true;
    }
    
    /**
     * Reset the input form for a new session
     */
    public void resetInputForm() {
        subjects.clear();
        topics.clear();
        nameField.clear();
        examDatePicker.setValue(LocalDate.now().plusDays(14));
        subjectComboBox.setValue(null);
        topicField.clear();
        newSubjectField.clear();
        topicField.setDisable(true);
    }
    
    /**
     * Show an alert dialog
     * @param message The message to display
     */
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Invalid Input");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
} 