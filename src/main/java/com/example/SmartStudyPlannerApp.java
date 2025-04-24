package com.example;

import com.example.controller.StudyPlanController;
import com.example.service.factory.StudyPlanStrategyFactory;
import com.example.view.LoginView;
import com.example.view.StudyPlanGenerationObserver;
import com.example.view.StudyPlanHistoryView;
import com.example.view.StudyPlanView;
import com.example.view.UserInputView;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/**
 * Main application class for the Smart Study Planner
 */
public class SmartStudyPlannerApp extends Application {
    
    private static final String APP_TITLE = "AI-Powered Smart Study Planner v1.0";
    private StudyPlanController controller;
    
    @Override
    public void start(Stage primaryStage) {
        // Create controller
        controller = new StudyPlanController();
        
        // Create main layout
        BorderPane mainLayout = new BorderPane();
        
        // Create main tab pane
        TabPane tabPane = new TabPane();
        
        // Create views
        UserInputView userInputView = new UserInputView(controller);
        Tab inputTab = new Tab("Input", userInputView);
        inputTab.setClosable(false);
        
        StudyPlanView studyPlanView = new StudyPlanView();
        studyPlanView.setController(controller);
        Tab planTab = new Tab("Study Plan", studyPlanView);
        planTab.setClosable(false);
        
        // Create strategy selection
        Label strategyLabel = new Label("Study Plan Strategy:");
        ComboBox<String> strategyComboBox = new ComboBox<>();
        strategyComboBox.getItems().addAll("Balanced", "Intensive");
        strategyComboBox.setValue("Balanced");
        
        // Create generate button and progress indicator
        Button generateButton = new Button("Generate Study Plan");
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);
        progressIndicator.setPrefSize(24, 24);
        
        // Create a more visible progress bar
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(200);
        progressBar.setVisible(false);
        progressBar.setStyle("-fx-accent: #4caf50;"); // Green color
        Label progressLabel = new Label("Generating plan...");
        progressLabel.setVisible(false);
        
        // Create user status and logout
        Label userStatusLabel = new Label("Not logged in");
        Button logoutButton = new Button("Logout");
        logoutButton.setVisible(false);
        
        // Update user info section
        userStatusLabel.getStyleClass().add("status-label");
        
        StudyPlanHistoryView historyView = new StudyPlanHistoryView(controller, tabPane, planTab);
        Tab historyTab = new Tab("History", historyView);
        historyTab.setClosable(false);
        
        // Add observer
        StudyPlanGenerationObserver observer = new StudyPlanGenerationObserver(
            progressIndicator, progressBar, progressLabel, studyPlanView);
        controller.addObserver(observer);
        
        // Define a method to update UI based on login state
        Runnable updateLoginState = () -> {
            boolean isLoggedIn = controller.isUserLoggedIn();
            
            if (isLoggedIn) {
                userStatusLabel.setText("Logged in as: " + controller.getAuthenticatedUser().getUsername());
                logoutButton.setVisible(true);
            } else {
                userStatusLabel.setText("Not logged in");
                logoutButton.setVisible(false);
                studyPlanView.clearView();
            }
            
            // Update history view with login status
            historyView.updateLoginStatus(isLoggedIn);
        };
        
        // Create login view with the update method
        LoginView loginView = new LoginView(controller, tabPane);
        Tab loginTab = new Tab("Login", loginView);
        loginTab.setClosable(false);
        
        tabPane.getTabs().addAll(loginTab, inputTab, planTab, historyTab);
        
        // Initially disable non-login tabs
        inputTab.setDisable(true);
        planTab.setDisable(true);
        historyTab.setDisable(true);
        
        // Method to handle tab access based on authentication
        Runnable updateTabAccess = () -> {
            boolean accessGranted = controller.isUserLoggedIn() || LoginView.hasAccessAsGuest();
            inputTab.setDisable(!accessGranted);
            planTab.setDisable(!accessGranted);
            historyTab.setDisable(!accessGranted);
            
            // If attempting to access a disabled tab, force back to login
            if (!accessGranted && tabPane.getSelectionModel().getSelectedItem() != loginTab) {
                tabPane.getSelectionModel().select(loginTab);
            }
        };
        
        // Provide access control method to login view
        loginView.setAccessControlHandler(updateTabAccess);
        
        // Set up logout button action
        logoutButton.setOnAction(e -> {
            controller.logout();
            updateLoginState.run();
            // Reset guest access flag
            LoginView.resetGuestAccess();
            // Update tab access
            updateTabAccess.run();
            tabPane.getSelectionModel().select(0); // Return to login tab
        });
        
        // Set up generation button action
        generateButton.setOnAction(e -> {
            if (userInputView.saveUserInput()) {
                try {
                    // Get selected strategy
                    String selectedStrategy = strategyComboBox.getValue();
                    StudyPlanStrategyFactory.StrategyType strategyType = 
                            "Intensive".equals(selectedStrategy) ? 
                            StudyPlanStrategyFactory.StrategyType.INTENSIVE : 
                            StudyPlanStrategyFactory.StrategyType.BALANCED;
                    
                    // Generate plan asynchronously
                    controller.generateStudyPlanAsync(strategyType);
                    
                    // Reset user input form for next plan
                    userInputView.resetInputForm();
                    
                    // Set tab to study plan
                    tabPane.getSelectionModel().select(planTab);
                } catch (Exception ex) {
                    showErrorAlert("Failed to generate study plan: " + ex.getMessage());
                }
            }
        });
        
        // Create bottom controls
        HBox leftControls = new HBox(10, strategyLabel, strategyComboBox, generateButton, progressIndicator);
        HBox progressControls = new HBox(10, progressBar, progressLabel);
        progressControls.setAlignment(Pos.CENTER_LEFT);
        HBox rightControls = new HBox(10, userStatusLabel, logoutButton);
        HBox controlsBox = new HBox();
        controlsBox.setPadding(new Insets(10));
        controlsBox.getChildren().addAll(leftControls, progressControls, rightControls);
        HBox.setMargin(progressControls, new Insets(0, 0, 0, 20));
        HBox.setMargin(rightControls, new Insets(0, 0, 0, 30));
        
        // Set up the main layout
        mainLayout.setCenter(tabPane);
        mainLayout.setBottom(controlsBox);
        
        // Create scene
        Scene scene = new Scene(mainLayout, 900, 700);
        
        // Add some basic CSS styling to improve the UI
        try {
            String cssPath = "/styles.css";
            scene.getStylesheets().add(SmartStudyPlannerApp.class.getResource(cssPath).toExternalForm());
            System.out.println("Successfully loaded CSS from: " + cssPath);
        } catch (Exception e) {
            System.err.println("Could not load CSS: " + e.getMessage());
            // Fallback styling - apply some basic styling directly
            mainLayout.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; -fx-font-size: 12px; -fx-background-color: #f5f5f5;");
        }
        
        // Set up the stage
        primaryStage.setTitle(APP_TITLE);
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // Update login status when tab changes
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            updateLoginState.run();
            
            // Update tab access control
            updateTabAccess.run();
            
            // Refresh history tab if selected
            if (newTab == historyTab && controller.isUserLoggedIn()) {
                historyView.refreshStudyPlans();
            }
        });
        
        // Apply initial access control
        updateTabAccess.run();
        
        // Print startup message
        System.out.println("==============================================");
        System.out.println(APP_TITLE + " started successfully");
        System.out.println("==============================================");
    }
    
    /**
     * Show an error alert
     * @param message The error message
     */
    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    @Override
    public void stop() {
        // Clean up resources
        if (controller != null) {
            controller.shutdown();
        }
    }
    
    /**
     * Main method
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
} 