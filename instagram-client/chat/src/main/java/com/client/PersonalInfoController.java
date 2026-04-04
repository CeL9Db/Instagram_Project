package com.client;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import java.io.File;

public class PersonalInfoController 
{
    @FXML private Button btnSave;
    @FXML private Button btnBackToChats;
    @FXML private Button btnDisconnect;
    @FXML private Button btnUploadAvatar;
    @FXML private ImageView avatarImageView;
    @FXML private TextArea bioTextArea;

    @FXML
    private void initialize() 
    {
        btnSave.setOnAction(e -> {
            String text = bioTextArea.getText();
            if (text != null && !text.trim().isEmpty()) 
            {
                App.getClientServer().send(text); // envoie le text au serveur
                bioTextArea.clear();
            } else {
                System.out.println("there's nothing to send...");
            }
        });

        btnBackToChats.setOnAction(e -> {
            try 
                {App.setRoot("mainChat");} 
            catch (Exception ex) 
                {ex.printStackTrace();}
        });

        btnDisconnect.setOnAction(e -> {
            try 
            {
                UserSession.clear();
                App.setRoot("login");
            } catch (Exception ex) 
                {ex.printStackTrace();}
        });

        btnUploadAvatar.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().addAll( // filter pour le type/schéma d'avatar
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
            );
            File file = fileChooser.showOpenDialog(btnUploadAvatar.getScene().getWindow());
            if (file != null) 
            {
                String path = file.toURI().toString();
                Image image = new Image(path);
                avatarImageView.setImage(image);
                UserSession.setAvatarPath(path);
            }
        });

        // pas sûr de son fonctionnement correct, mais à la base il doit nous restorer l'avatar
        // comme la fonctionnalité des avatars n'est pas fini, lui ne marche pas pour le moment
        // ------------------------------------------------------------------------------
        // String avatarPath = UserSession.getAvatarPath();
        // if (avatarPath != null) {
        //     avatarImageView.setImage(new Image(avatarPath));
        // }
        // ------------------------------------------------------------------------------
    }
}
