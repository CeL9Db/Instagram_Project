package com.client;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController {

    @FXML private TextField nicknameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label statusLabel;

    @FXML
    private void registerClicked() 
    {
        String nick = nicknameField.getText().trim();
        String email = emailField.getText().trim();
        String pass = passwordField.getText();
        String confirm = confirmPasswordField.getText();

        if (nick.isEmpty() || email.isEmpty() || pass.isEmpty() || confirm.isEmpty()) 
        {
            statusLabel.setText("You must insert all of information demanded.");
            return;
        }
        if (!pass.equals(confirm)) 
        {
            statusLabel.setText("Passwords inserted are not equals.");
            return;
        }

        try 
        {
            var clientServer = App.getClientServer();
            if (!clientServer.isConnected()) 
            {
                statusLabel.setText("ERROR: connection to server.");
                return;
            }
            
            clientServer.setMessageListener(response -> {
                if (response.startsWith("REGISTER_SUCCESS|")) 
                {
                    String nickname = response.substring(16);
                    UserSession.setEmail(email);
                    UserSession.setNickname(nickname);
                    javafx.application.Platform.runLater(() -> {
                        try 
                            {App.setRoot("mainChat");} 
                        catch (Exception e) 
                            {statusLabel.setText("ERROR: move to chat page");}
                    });
                } else if (response.startsWith("ERROR:")) 
                {
                    javafx.application.Platform.runLater(() -> 
                        statusLabel.setText(response.substring(6)));
                }
            });

            // Small delay to ensure listener is set
            Thread.sleep(100);
            clientServer.send("/register " + nick + " " + email + " " + pass);
        } catch (Exception e) {
            statusLabel.setText("Erreur d'inscription: " + e.getMessage());
        }
    }

    @FXML
    private void goLogin() 
    {
        try 
            {App.setRoot("login");} 
        catch (Exception e) 
        {
            e.printStackTrace();
            statusLabel.setText("Problem to go to the login page.");
        }
    }
}
