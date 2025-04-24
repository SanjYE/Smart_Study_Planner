package com.example.view;

import com.example.controller.StudyPlanController;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * View for user login and registration
 */
public class LoginView extends VBox {
    
    private final StudyPlanController controller;
    private final TabPane tabPane;
    private Runnable accessControlHandler;
    
    // Static flag to track if user has access as guest
    private static boolean hasGuestAccess = false;
    
    private TextField loginUsernameField;
    private PasswordField loginPasswordField;
    private TextField registerUsernameField;
    private PasswordField registerPasswordField;
    private PasswordField confirmPasswordField;
    
    /**
     * Create a new login view
     * @param controller The controller to use
     * @param tabPane The main application tab pane
     */
    public LoginView(StudyPlanController controller, TabPane tabPane) {
        super(10);
        this.controller = controller;
        this.tabPane = tabPane;
        
        setPadding(new Insets(20));
        setAlignment(Pos.CENTER);
        
        // Create header
        Text headerText = new Text("Smart Study Planner");
        headerText.setFont(Font.font("System", FontWeight.BOLD, 24));
        
        // Create tabs for login and register
        TabPane loginTabs = new TabPane();
        
        Tab loginTab = new Tab("Login");
        loginTab.setClosable(false);
        loginTab.setContent(createLoginForm());
        
        Tab registerTab = new Tab("Register");
        registerTab.setClosable(false);
        registerTab.setContent(createRegisterForm());
        
        loginTabs.getTabs().addAll(loginTab, registerTab);
        
        getChildren().addAll(headerText, loginTabs);
    }
    
    /**
     * Set the access control handler
     * @param handler Runnable to handle access control
     */
    public void setAccessControlHandler(Runnable handler) {
        this.accessControlHandler = handler;
    }
    
    /**
     * Check if user has access as guest
     * @return true if user has guest access
     */
    public static boolean hasAccessAsGuest() {
        return hasGuestAccess;
    }
    
    /**
     * Reset guest access flag
     */
    public static boolean resetGuestAccess() {
        hasGuestAccess = false;
        return hasGuestAccess;
    }
    
    /**
     * Create the login form
     * @return The login form
     */
    private GridPane createLoginForm() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        Text title = new Text("Login to Your Account");
        title.setFont(Font.font("System", FontWeight.BOLD, 16));
        grid.add(title, 0, 0, 2, 1);
        
        Label usernameLabel = new Label("Username:");
        grid.add(usernameLabel, 0, 1);
        
        loginUsernameField = new TextField();
        loginUsernameField.setPromptText("Enter your username");
        grid.add(loginUsernameField, 1, 1);
        
        Label passwordLabel = new Label("Password:");
        grid.add(passwordLabel, 0, 2);
        
        loginPasswordField = new PasswordField();
        loginPasswordField.setPromptText("Enter your password");
        grid.add(loginPasswordField, 1, 2);
        
        Button loginButton = new Button("Login");
        loginButton.setOnAction(e -> handleLogin());
        
        Button guestButton = new Button("Continue as Guest");
        guestButton.setOnAction(e -> continueAsGuest());
        
        HBox buttonBox = new HBox(10, loginButton, guestButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        grid.add(buttonBox, 1, 3);
        
        return grid;
    }
    
    /**
     * Create the registration form
     * @return The registration form
     */
    private GridPane createRegisterForm() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        Text title = new Text("Create New Account");
        title.setFont(Font.font("System", FontWeight.BOLD, 16));
        grid.add(title, 0, 0, 2, 1);
        
        Label usernameLabel = new Label("Username:");
        grid.add(usernameLabel, 0, 1);
        
        registerUsernameField = new TextField();
        registerUsernameField.setPromptText("Choose a username");
        grid.add(registerUsernameField, 1, 1);
        
        Label passwordLabel = new Label("Password:");
        grid.add(passwordLabel, 0, 2);
        
        registerPasswordField = new PasswordField();
        registerPasswordField.setPromptText("Choose a password");
        grid.add(registerPasswordField, 1, 2);
        
        Label confirmLabel = new Label("Confirm Password:");
        grid.add(confirmLabel, 0, 3);
        
        confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm your password");
        grid.add(confirmPasswordField, 1, 3);
        
        Button registerButton = new Button("Register");
        registerButton.setOnAction(e -> handleRegistration());
        
        HBox buttonBox = new HBox(10, registerButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        grid.add(buttonBox, 1, 4);
        
        return grid;
    }
    
    /**
     * Handle login button click
     */
    private void handleLogin() {
        String username = loginUsernameField.getText().trim();
        String password = loginPasswordField.getText();
        
        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Login Error", "Please enter both username and password.");
            return;
        }
        
        if (controller.login(username, password)) {
            showAlert(Alert.AlertType.INFORMATION, "Login Successful", "Welcome, " + username + "!");
            
            // Clear password field for security
            loginPasswordField.clear();
            
            // Update access control
            if (accessControlHandler != null) {
                accessControlHandler.run();
            }
            
            // First switch to input tab
            tabPane.getSelectionModel().select(1);
            
            // Force an update of the tab listener to update all UI elements
            // that depend on login status by toggling tabs
            Tab currentTab = tabPane.getSelectionModel().getSelectedItem();
            tabPane.getSelectionModel().select(0);
            tabPane.getSelectionModel().select(currentTab);
        } else {
            showAlert(Alert.AlertType.ERROR, "Login Error", "Invalid username or password.");
        }
    }
    
    /**
     * Handle registration button click
     */
    private void handleRegistration() {
        String username = registerUsernameField.getText().trim();
        String password = registerPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Registration Error", "Please fill in all fields.");
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            showAlert(Alert.AlertType.ERROR, "Registration Error", "Passwords do not match.");
            return;
        }
        
        if (controller.registerUser(username, password)) {
            showAlert(Alert.AlertType.INFORMATION, "Registration Successful", 
                    "Account created successfully. You can now login.");
            // Clear fields
            registerUsernameField.clear();
            registerPasswordField.clear();
            confirmPasswordField.clear();
        } else {
            showAlert(Alert.AlertType.ERROR, "Registration Error", 
                    "Username already exists or an error occurred.");
        }
    }
    
    /**
     * Continue as guest
     */
    private void continueAsGuest() {
        // Set guest access flag
        hasGuestAccess = true;
        
        // Update access control
        if (accessControlHandler != null) {
            accessControlHandler.run();
        }
        
        // Go to the next tab
        tabPane.getSelectionModel().select(1); // Select input tab
    }
    
    /**
     * Show an alert dialog
     * @param type Alert type
     * @param title Alert title
     * @param message Alert message
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
} 