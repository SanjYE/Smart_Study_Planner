package com.example.view;

import com.example.model.StudyPlan;
import com.example.service.observer.StudyPlanObserver;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;

/**
 * Concrete implementation of StudyPlanObserver for UI updates
 * Implements the Observer design pattern
 */
public class StudyPlanGenerationObserver implements StudyPlanObserver {
    
    private final ProgressIndicator progressIndicator;
    private final ProgressBar progressBar;
    private final Label progressLabel;
    private final StudyPlanView studyPlanView;
    
    /**
     * Create a new study plan generation observer
     * @param progressIndicator The progress indicator to update
     * @param progressBar The progress bar to update
     * @param progressLabel The progress label to update
     * @param studyPlanView The study plan view to update
     */
    public StudyPlanGenerationObserver(ProgressIndicator progressIndicator, ProgressBar progressBar, 
                                      Label progressLabel, StudyPlanView studyPlanView) {
        this.progressIndicator = progressIndicator;
        this.progressBar = progressBar;
        this.progressLabel = progressLabel;
        this.studyPlanView = studyPlanView;
    }

    @Override
    public void onGenerationStarted() {
        Platform.runLater(() -> {
            progressIndicator.setVisible(true);
            progressIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
            progressBar.setVisible(true);
            progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
            progressLabel.setVisible(true);
        });
    }

    @Override
    public void onGenerationCompleted(StudyPlan studyPlan) {
        Platform.runLater(() -> {
            progressIndicator.setVisible(false);
            progressBar.setVisible(false);
            progressLabel.setVisible(false);
            studyPlanView.updateStudyPlan(studyPlan);
            showSuccessAlert();
        });
    }

    @Override
    public void onGenerationFailed(Exception exception) {
        Platform.runLater(() -> {
            progressIndicator.setVisible(false);
            progressBar.setVisible(false);
            progressLabel.setVisible(false);
            showErrorAlert(exception.getMessage());
        });
    }
    
    /**
     * Update the study plan view with a study plan (without showing alerts)
     * This is used when loading a study plan from history
     * @param studyPlan The study plan to display
     */
    public void updateStudyPlanView(StudyPlan studyPlan) {
        Platform.runLater(() -> {
            studyPlanView.updateStudyPlan(studyPlan);
        });
    }
    
    /**
     * Show a success alert
     */
    private void showSuccessAlert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText("Study plan generated successfully!");
        alert.showAndWait();
    }
    
    /**
     * Show an error alert
     * @param message The error message
     */
    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText("Failed to generate study plan: " + message);
        alert.showAndWait();
    }
} 