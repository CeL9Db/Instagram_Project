package com.client;

import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;

public class PrimaryController {

    @FXML
    private void switchToSecondary() throws IOException {
        App.setRoot("secondary");
    }

    @FXML
    private BorderPane rootPane;

    // buttons de menu
    @FXML private Button btnPersonalInfo;
    @FXML private Button btnParameters;
    @FXML private Button btnDisconnect;

    // PersonalInfo page
    @FXML
    private void showPersonalInfo(ActionEvent event) 
    {
        try 
        {
            Node profile = FXMLLoader.load(getClass().getResource("/com/client/personnalPage.fxml"));
            rootPane.setCenter(profile);
        } 
        catch (IOException e) 
            {e.printStackTrace();}
        btnPersonalInfo.getStyleClass().add("active");
        btnParameters.getStyleClass().remove("active");
    }

    // Parametres page
    @FXML
    private void showParameters(ActionEvent event) 
    {
        try 
        {
            Node params = FXMLLoader.load(getClass().getResource("/com/client/parametres.fxml"));
            rootPane.setCenter(params);
        } 
        catch (IOException e) 
            {e.printStackTrace();}
        btnParameters.getStyleClass().add("active");
        btnPersonalInfo.getStyleClass().remove("active");
    }

    @FXML private Button btnSave;
    @FXML private TextArea bioTextArea;
    
    @FXML
    private void initialize()
    {
        btnSave.setOnAction(e -> {
            String text = bioTextArea.getText();
            if (text != null && !text.trim().isEmpty()) 
            {
                App.getClientServer().send(text); // envoie le text au serveur
                bioTextArea.clear(); // clean memory
            } 
            else 
                {System.out.println("there's nothing to send...");}
        });
    }
}
