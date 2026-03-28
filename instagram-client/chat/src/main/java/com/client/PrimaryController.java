package com.client;

import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

public class PrimaryController {

    @FXML
    private void switchToSecondary() throws IOException {
        App.setRoot("secondary");
    }

// --- КНОПКИ МЕНЮ ---
    @FXML private Button btnPersonalInfo;
    @FXML private Button btnParameters;

    // pannelles
    // @FXML private VBox profileScreen;
    // @FXML private VBox parametersScreen;

    // PersonalInfo page
    @FXML
    private void showPersonalInfo(ActionEvent event) 
    {
        // 1. change le fenêtre pour profileScreen
        // profileScreen.setVisible(true);
        // parametersScreen.setVisible(false);

        // changement des couleurs des boutons
        btnPersonalInfo.getStyleClass().add("active");
        btnParameters.getStyleClass().remove("active");
    }

    // Parametres page
    @FXML
    private void showParameters(ActionEvent event) 
    {
        // change le fenêtre pour parametersScreen
        // profileScreen.setVisible(false);
        // parametersScreen.setVisible(true);

        // changement des couleurs des boutons
        btnParameters.getStyleClass().add("active");
        btnPersonalInfo.getStyleClass().remove("active");
    }
}
