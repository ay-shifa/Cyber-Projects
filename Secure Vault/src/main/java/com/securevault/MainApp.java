package com.securevault;

import com.securevault.controller.LoginController;
import com.securevault.controller.ControlCredential;
import com.securevault.service.HashService;
import com.securevault.dao.MasterPasswordDAO;
import com.securevault.dao.CredentialsDAO;
import com.securevault.service.EncryptionService;
import com.securevault.model.Credentials;
import com.securevault.service.KeyDerivationService;
import com.securevault.filehandling.VaultFH;

import javafx.application.Application;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.sql.SQLException;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
//import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.util.List;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import javax.crypto.SecretKey;

import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class MainApp extends Application{

    private final LoginController controlLogin  = new LoginController();
    private final MasterPasswordDAO masterPasswordDao = new MasterPasswordDAO();
    private final HashService hashService = new HashService();
    private final ControlCredential credentialController = new ControlCredential();
    private final CredentialsDAO credentialsDao = new  CredentialsDAO();
    private final EncryptionService encryptionService = new EncryptionService();
    private final KeyDerivationService keyDerivationService = new KeyDerivationService();
    private SecretKey sessionKey;
    private int editingCredentialId = -1;
    private final VaultFH FH = new VaultFH(encryptionService);

    public void start(Stage primaryStage){
         try {
             if (masterPasswordDao.hasMasterPassword()) {
                 showLoginScreen(primaryStage);
             } else {
                 showSetupScreen(primaryStage);
             }
         }
         catch (SQLException e){
             e.printStackTrace();
         }
    }

    public void showLoginScreen(Stage primaryStage) {
        Label titleLabel = new Label("🔒 Secure Vault");
        titleLabel.getStyleClass().add("login-title");

        Label loginLabel = new Label("Enter your Master Password to continue");
        loginLabel.getStyleClass().add("login-subtitle");

        PasswordField masterPassword = new PasswordField();
        masterPassword.setPromptText("Master Password");
        masterPassword.getStyleClass().add("input-field");
        Label statuslabel = new Label();
        statuslabel.getStyleClass().add("status-label");
        Button unlockButton = new Button("Unlock Vault");
        unlockButton.getStyleClass().add("primary-button");
        unlockButton.setOnAction(event -> handleUnlock(primaryStage, masterPassword, statuslabel));

        VBox root = new VBox(20);
        root.setPadding(new Insets(60));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #f5f5f5;");
        root.getChildren().addAll(
                titleLabel,
                loginLabel,
                masterPassword,
                unlockButton,
                statuslabel);

        Scene scene = new Scene(root, 600, 400);
        scene.getStylesheets().add(
                getClass().getResource("/styling.css").toExternalForm());
        primaryStage.setTitle("SECURE VAULT");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public void showSetupScreen(Stage primaryStage) {
        Label titleLabel = new Label("🔒 Secure Vault");
        titleLabel.getStyleClass().add("login-title");

        Label introLabel = new Label("Create your Master Password to get started");
        introLabel.getStyleClass().add("login-subtitle");

        PasswordField masterPassword = new PasswordField();
        masterPassword.setPromptText("Master Password");
        masterPassword.getStyleClass().add("input-field");

        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm Master Password");
        confirmPasswordField.getStyleClass().add("input-field");

        Button setPasswordButton = new Button("Set Master Password");
        setPasswordButton.getStyleClass().add("primary-button");

        Label statuslabel = new Label();
        statuslabel.getStyleClass().add("status-label");

        setPasswordButton.setOnAction(event ->
                handleSetPassword(primaryStage, masterPassword, confirmPasswordField, statuslabel));

        VBox root = new VBox(20);
        root.setPadding(new Insets(60));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #f5f5f5;");
        root.getChildren().addAll(
                titleLabel,
                introLabel,
                masterPassword,
                confirmPasswordField,
                setPasswordButton,
                statuslabel);

        Scene scene = new Scene(root, 600, 400);
        scene.getStylesheets().add(
                getClass().getResource("/styling.css").toExternalForm());
        primaryStage.setTitle("SECURE VAULT");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public void showDashboardScreen(Stage primaryStage) {

        // ── SIDEBAR ──────────────────────────────
        Label sidebarTitle = new Label("🔒 Secure Vault");
        sidebarTitle.getStyleClass().add("sidebar-title");

        Button vaultBtn = new Button("🗄 Vault");
        vaultBtn.getStyleClass().add("sidebar-button");

        Button exportBtn = new Button("📤 Export");
        exportBtn.getStyleClass().add("sidebar-button");

        Button lockBtn = new Button("🔒 Lock");
        lockBtn.getStyleClass().add("lock-button");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox sidebar = new VBox(15);
        sidebar.getStyleClass().add("sidebar");
        sidebar.getChildren().addAll(
                sidebarTitle,
                vaultBtn,
                exportBtn,
                spacer,
                lockBtn
        );

        // ── INPUT FORM ───────────────────────────
        Label contentTitle = new Label("Add New Credential");
        contentTitle.getStyleClass().add("content-title");

        TextField websiteField = new TextField();
        websiteField.setPromptText("Website");
        websiteField.getStyleClass().add("input-field");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.getStyleClass().add("input-field");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.getStyleClass().add("input-field");

        Label strengthLabel = new Label();
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            String strength = credentialController.passwordStrength(newVal);
            strengthLabel.setText("Strength: " + strength);
            strengthLabel.getStyleClass().removeAll(
                    "strength-weak", "strength-medium", "strength-strong"
            );
            if (strength.equals("Weak"))
                strengthLabel.getStyleClass().add("strength-weak");
            else if (strength.equals("Medium"))
                strengthLabel.getStyleClass().add("strength-medium");
            else if (strength.equals("Strong"))
                strengthLabel.getStyleClass().add("strength-strong");
        });

        Label statusLabel = new Label();
        statusLabel.getStyleClass().add("status-label");

        Button saveBtn = new Button("Save Credential");
        saveBtn.getStyleClass().add("primary-button");

        // ── TABLE ────────────────────────────────
        TextField searchField = new TextField();
        searchField.setPromptText("Search by website...");
        searchField.getStyleClass().add("input-field");

        TableView<Credentials> tableView = new TableView<>();
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableView.setPrefHeight(300);
        tableView.getStyleClass().add("table-view");

        TableColumn<Credentials, String> websiteCol = new TableColumn<>("Website");
        websiteCol.setCellValueFactory(new PropertyValueFactory<>("website"));

        TableColumn<Credentials, String> usernameCol = new TableColumn<>("Username");
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));

        TableColumn<Credentials, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(150);
        actionsCol.setCellFactory(col -> new TableCell<>() {
            final Button editBtn = new Button("Edit");
            final Button deleteBtn = new Button("Delete");
            final Button copyBtn = new Button("Copy");

            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Credentials credential = getTableView().getItems().get(getIndex());
                    HBox buttons = new HBox(5, editBtn, deleteBtn, copyBtn);

                    editBtn.setOnAction(e -> {
                        websiteField.setText(credential.getWebsite());
                        usernameField.setText(credential.getUsername());
                        editingCredentialId = credential.getId();
                    });

                    deleteBtn.setOnAction(e -> {
                        try {
                            credentialsDao.deleteCredential(credential.getId());
                            loadCredentials(tableView);
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                    });

                    copyBtn.setOnAction(e -> {
                        String decrypted = encryptionService.decrypt(
                                credential.getEncryptedPassword(), sessionKey);
                        Clipboard clipboard = Clipboard.getSystemClipboard();
                        ClipboardContent content = new ClipboardContent();
                        content.putString(decrypted);
                        clipboard.setContent(content);
                        statusLabel.setText("Password copied — clears in 10 seconds");
                        PauseTransition pause = new PauseTransition(Duration.seconds(10));
                        pause.setOnFinished(ev -> {
                            clipboard.setContent(new ClipboardContent());
                            statusLabel.setText("");
                        });
                        pause.play();
                    });

                    setGraphic(buttons);
                }
            }
        });

        tableView.getColumns().addAll(websiteCol, usernameCol, actionsCol);
        loadCredentials(tableView);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                if (newVal == null || newVal.isBlank()) {
                    loadCredentials(tableView);
                } else {
                    List<Credentials> results = credentialsDao.searchByWebsite(newVal);
                    tableView.setItems(FXCollections.observableArrayList(results));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        // ── BUTTON ACTIONS ───────────────────────
        saveBtn.setOnAction(e ->
                handleCredentialButton(websiteField, usernameField, passwordField, statusLabel, tableView));

        exportBtn.setOnAction(e -> handleExport(statusLabel));

        lockBtn.setOnAction(e -> showLoginScreen(primaryStage));

        // ── CONTENT AREA ─────────────────────────
        VBox contentArea = new VBox(15);
        contentArea.getStyleClass().add("content-area");
        contentArea.getChildren().addAll(
                contentTitle,
                websiteField,
                usernameField,
                passwordField,
                strengthLabel,
                saveBtn,
                statusLabel,
                searchField,
                tableView
        );

        // ── ROOT LAYOUT ──────────────────────────
        BorderPane root = new BorderPane();
        root.setLeft(sidebar);
        root.setCenter(contentArea);

        Scene scene = new Scene(root, 900, 700);
        scene.getStylesheets().add(
                getClass().getResource("/styling.css").toExternalForm());
        primaryStage.setTitle("SECURE VAULT");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void handleLock(PasswordField masterPassword,
                            Label statuslabel){
        String enteredPassword = masterPassword.getText();
        String message = controlLogin.validatePasswordInput(enteredPassword);
        statuslabel.setText(message);
    }
    private void ConfirmPasswordSetUp(PasswordField masterPassword,
                                      PasswordField confirmPasswordField,
                                      Label statuslabel){
        String password = masterPassword.getText();
        String confirmPassword = confirmPasswordField.getText();
        String message = controlLogin.validateConfirmPassword(password,confirmPassword );
        statuslabel.setText(message);

    }
    public void handleSetPassword(Stage primaryStage, PasswordField masterPassword,
                                  PasswordField confirmPasswordField,
                                  Label statuslabel){
        String password = masterPassword.getText();
        String confirmPassword = confirmPasswordField.getText();
        String validationMessage = controlLogin.validateConfirmPassword(password, confirmPassword);

        if(!validationMessage.equals("Valid Master Password")){
            statuslabel.setText(validationMessage);
            return;
        }
        try{
            if(masterPasswordDao.hasMasterPassword()){
                statuslabel.setText("Master Password Already Exists");
                return;
            }
            byte [] salt = keyDerivationService.generateSalt();
//            String encodedSalt = Base64.getEncoder().encodeToString(salt);
            sessionKey = keyDerivationService.deriveKey(password, salt);
            String hashPassword = hashService.sha256(password);
            String createdAt = LocalDateTime.now().toString();

            masterPasswordDao.saveMasterPassword(hashPassword, salt, createdAt);
            showDashboardScreen(primaryStage);
//            statuslabel.setText("Master Password Saved Successfully");
        }catch (SQLException e){
            statuslabel.setText("Failed to save Master Password!");
            e.printStackTrace();
        }

    }

    public void handleUnlock(Stage primaryStage, PasswordField masterPassword, Label statuslabel)  {
        String enteredPassword = masterPassword.getText();
        String validationMessage = controlLogin.validatePasswordInput(enteredPassword);
        if(!validationMessage.equals("Unlock button clicked")){
             statuslabel.setText(validationMessage);
             return;
        }
        try{
            String storedHash = masterPasswordDao.getStoredPasswordHash();
            if(storedHash == null){
                statuslabel.setText("Master Password Not Found");
                return;
            }
            boolean isValid = controlLogin.verifyMasterPassword(enteredPassword, storedHash);
            if(isValid){
                byte[] salt = masterPasswordDao.getSalt();
                if(salt == null){
                    statuslabel.setText("Salt not found. Database is corrupted");
                    return;
                }
                sessionKey = keyDerivationService.deriveKey(enteredPassword, salt);
                showDashboardScreen(primaryStage);
            }else{
                statuslabel.setText("Incorrect Master Password.");
            }

        }catch (SQLException e){
            statuslabel.setText("Database error during verification.");
            e.printStackTrace();
        }
    }

    private void handleCredentialButton(TextField websiteField, TextField usernameField, PasswordField passwordField, Label statuslabel, TableView<Credentials> tableView) {
        String website = websiteField.getText();
        String username = usernameField.getText();
        String password = passwordField.getText();
        String message = credentialController.validateCredential(website, username, password);
       if(!message.equals("Credentials are valid")){
           statuslabel.setText(message);
           return;
       }

        try {
            String encryptedPassword = encryptionService.encrypt(password, sessionKey);
            if (editingCredentialId == -1) {
                String createdAt = LocalDateTime.now().toString();
                credentialsDao.saveCredential(website, username, encryptedPassword, createdAt);
                statuslabel.setText("Credential Saved Successfully");
            } else {
                credentialsDao.updateCredential(editingCredentialId, website, username, encryptedPassword);
                statuslabel.setText("Credential Updated Successfully");
                editingCredentialId = -1;
            }
            websiteField.clear();
            usernameField.clear();
            passwordField.clear();
            loadCredentials(tableView);
        } catch (SQLException e) {
            statuslabel.setText("Failed to save Credential!");
            e.printStackTrace();
        }

    }

    private void loadCredentials(TableView<Credentials> tableView) {
        try {
            List<Credentials> credentialsList = credentialsDao.getCredentialsDB();
            ObservableList<Credentials> data = FXCollections.observableArrayList(credentialsList);
            System.out.println("Credentials loaded: " + credentialsList.size());
            tableView.setItems(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void handlePasswordButton(Label passwordLabel){
        try{
            Credentials lastCredential = credentialsDao.getLastCredentials();
            if(lastCredential == null){
                passwordLabel.setText("No credentials found");
                return;
            }
            String decryptedPassword = encryptionService.decrypt(lastCredential.getEncryptedPassword(), sessionKey);
            passwordLabel.setText("Last Password:" + decryptedPassword);
        }
        catch (SQLException e){
            passwordLabel.setText("Failed to load credentials.");
            e.printStackTrace();
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
    }
    private void handleClipboardButton(Label passwordLabel) {
        try{
            Credentials lastcredential = credentialsDao.getLastCredentials();
            if(lastcredential == null){
                passwordLabel.setText("No credentials found");
                return;
            }
            String decryptedPassword = encryptionService.decrypt(lastcredential.getEncryptedPassword(), sessionKey);
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(decryptedPassword);
            clipboard.setContent(content);
            passwordLabel.setText("Last password copied to clipboard");

            PauseTransition pause = new PauseTransition(Duration.seconds(10));
            pause.setOnFinished(event -> {
                    ClipboardContent emptyClipboard = new ClipboardContent();
                    emptyClipboard.putString("");
                    clipboard.setContent(emptyClipboard);
                    passwordLabel.setText("Clipboard cleared automatically!");
            });
            pause.play();


        }catch (SQLException e){
            passwordLabel.setText("Failed to load credentials.");
            e.printStackTrace();
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
    }
    private void handleExport(Label statusLabel){
        try{
            List<Credentials> getCred = credentialsDao.getCredentialsDB();
            Path filepath = Path.of("Backup.txt");
            FH.exportCredentials(getCred, filepath, sessionKey);
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        launch(args);
    } // jo nai ho skta
}
