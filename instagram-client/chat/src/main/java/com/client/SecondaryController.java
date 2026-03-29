package com.client;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

public class SecondaryController {

    @FXML
    private void switchToPrimary() throws IOException {
        App.setRoot("personnalPage");
    }

    @FXML private VBox chatList;

    @FXML private void initialize()
    {
        List<String> arr = Arrays.asList("Vlad", "22ans", "university");    // à changer par les objets reçu depuis le backend
        loadChats(arr);
    }

    private void loadChats(List<String> ct)
    {
        chatList.getChildren().clear(); // si y'a des chats déjà présents on clear
        
        for(String chat : ct)
        // pour chaque buttonChat on met son largeur au max de la colonne gauche, style depuis CSS prédéfini et l'action pour ouvrir le chat sélectionné
        {
            Button btn = new Button(chat);
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.getStyleClass().add("nav-button");

            btn.setOnAction(e -> {
                System.out.print("you're in the chat (a modifier)");
            });
            chatList.getChildren().add(btn);
        }
    }

}