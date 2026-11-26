package com.client;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController 
{

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;    // sert à informer l'état de connection (erreur, succes, etc)

    @FXML
    // le processus du login (page d'accueil)
    private void onLoginClicked()
    {
        // on récupère les éléments depuis les fields
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        //si vide alors on passe pas à l'étape prochaine
        if (email.isEmpty() || password.isEmpty()) 
        {
            statusLabel.setText("You must insert your email and password.");
            return;
        }

        try 
        {
            var clientServer = App.getClientServer();
            if (!clientServer.isConnected()) 
            {
                statusLabel.setText("Error: connection to server");
                return;
            }
            
            clientServer.setMessageListener(response -> {
                if (response.startsWith("LOGIN_SUCCESS|")) 
                {
                    String nickname = response.substring("LOGIN_SUCCESS|".length());
                    UserSession.setEmail(email);
                    UserSession.setNickname(nickname);
                    javafx.application.Platform.runLater(() -> {
                        try 
                            {App.setRoot("mainChat");}
                        catch (Exception e) 
                            {statusLabel.setText("ERROR: move to chat page");}
                    });
                } else if (response.startsWith("ERROR:")) {
                    javafx.application.Platform.runLater(() -> 
                        statusLabel.setText(response.substring(6)));
                } else {
                    clientServer.queueMessage(response);
                }
            });

            Thread.sleep(100);
            clientServer.send("/login " + email + " " + password);
        } catch (Exception e) {
            statusLabel.setText("ERROR connection: " + e.getMessage());
        }
    }

    @FXML
    private void onGoRegister() 
    {
        try 
            {App.setRoot("register");}
        catch (Exception e) 
        {
            e.printStackTrace();
            statusLabel.setText("ERROR: move to register page");
        }
    }
}
