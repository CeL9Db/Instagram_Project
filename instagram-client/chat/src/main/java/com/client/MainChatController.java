package com.client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.File;

public class MainChatController 
{
    @FXML private Button logoButton;
    @FXML private Label userLabel;
    @FXML private ListView<String> contactsList;
    @FXML private Label chatWithLabel;
    @FXML private TextArea messagesArea;
    @FXML private TextField messageField;

    private ClientServer clientServer;
    private String selectedTarget = "ALL";
    private final Map<String, List<String>> chatHistory = new HashMap<>();

    @FXML
    public void initialize() 
    {

        String nick = UserSession.getNickname();
        userLabel.setText("Utilisateur: " + (nick != null ? nick : "anonyme"));
        // partie pas fini avec les avatars
        String avatarPath = UserSession.getAvatarPath();
        if (avatarPath != null) 
        {
            ImageView avatar = new ImageView(new Image(avatarPath));
            avatar.setFitWidth(32);
            avatar.setFitHeight(32);
            userLabel.setGraphic(avatar);
        }


        // Sauvegarde des contacts/groupes déjà présentes
        // ---il y'avait un problème avec la perte de ces données---
        List<String> savedContacts = UserSession.getContacts();
        List<String> savedGroups = UserSession.getGroups();
        contactsList.getItems().clear();
        contactsList.getItems().add("Tous");
        if (!savedContacts.isEmpty()) 
            {contactsList.getItems().addAll(savedContacts);}
        if (!savedGroups.isEmpty()) 
        {
            for (String group : savedGroups) 
                {contactsList.getItems().add("Group: " + group);}
        }
        contactsList.getSelectionModel().selectFirst();
        chatWithLabel.setText("Chat général");
        showHistory(selectedTarget);

        clientServer = App.getClientServer();
        clientServer.setMessageListener(this::processServerMessage);

        // Requête pour avoir les chats et les groupes
        clientServer.send("/get_contacts");
        clientServer.send("/get_groups");

        // cellFactory pour avoir les avatars (ne fonctionne pas pour l'instant)
        // -----------------------------------------------------------------------
        // contactsList.setCellFactory(lv -> new javafx.scene.control.ListCell<String>() {
        //     private final ImageView avatar = new ImageView();
        //     @Override
        //     protected void updateItem(String item, boolean empty) {
        //         super.updateItem(item, empty);
        //         if (empty || item == null) {
        //             setText(null);
        //             setGraphic(null);
        //         } else {
        //             setText(item);
        //             // Мини-аватарка: ищем файл по нику (например, avatars/nickname.png)
        //             String nick = item.replace("Group: ", "");
        //             File avatarFile = new File("avatars/" + nick + ".png");
        //             if (avatarFile.exists()) {
        //                 avatar.setImage(new Image(avatarFile.toURI().toString()));
        //             } else {
        //                 avatar.setImage(null); // или дефолтная картинка
        //             }
        //             avatar.setFitWidth(32);
        //             avatar.setFitHeight(32);
        //             setGraphic(avatar.getImage() != null ? avatar : null);
        //         }
        //     }
        // });
        // -----------------------------------------------------------------------
        contactsList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) 
            {
                if (newV.equals("Tous")) 
                {
                    selectedTarget = "ALL";
                    chatWithLabel.setText("Chat général");
                } else if (newV.startsWith("Group: ")) {
                    selectedTarget = "GROUP:" + newV.substring("Group: ".length());
                    chatWithLabel.setText("Chat de groupe: " + newV.substring("Group: ".length()));
                } else {
                    selectedTarget = newV;
                    chatWithLabel.setText("Chat avec " + newV);
                }
                showHistory(selectedTarget);
            }
        });
    }

    @FXML
    //  donne la possibilité d'envoyer le message en fonction d'à qui on écrit
    private void sendBtnClicked() 
    {
        String text = messageField.getText().trim();
        if (text.isEmpty()) return;

        String nickname = UserSession.getNickname();
        if (nickname == null || nickname.isBlank()) // isBlank() retourne true si vide ou contient que les éspaces
        {
            messagesArea.appendText("[ERROR] You must be connected to your account to might send messages.\n");
            return;
        }

        String out;
        if (selectedTarget.equals("ALL")) 
            {out = "/msg ALL|" + text;} 
        else if (selectedTarget.startsWith("GROUP:")) 
        {
            String groupName = selectedTarget.split(":", 2)[1];
            out = "/gmsg " + groupName + "|" + text;
        } 
        else 
            {out = "/msg " + selectedTarget + "|" + text;}

        String line = "Me -> " + (selectedTarget.equals("ALL") ? "Tous" : selectedTarget.replace("GROUP:", "")) + ": " + text;
        addToHistory(selectedTarget, line);
        messagesArea.appendText(line + "\n");
        clientServer.send(out);
        messageField.clear();
    }

    @FXML
    // crée un groupe
    private void createGroupClicked() 
    {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create a group");
        dialog.setHeaderText("Create a new group");
        dialog.setContentText("Name of the group:");
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) 
        {
            String groupName = result.get().trim();
            if (!groupName.isBlank()) 
                {clientServer.send("/create_group " + groupName);}
        }
    }

    @FXML
    // rajoute un participant dans une groupe précisé
    private void addMemberClicked() 
    {
        TextInputDialog groupDialog = new TextInputDialog();
        groupDialog.setTitle("Add a membre");
        groupDialog.setHeaderText("Choose the group");
        groupDialog.setContentText("Name of the group:");
        Optional<String> groupResult = groupDialog.showAndWait();
        if (groupResult.isPresent()) 
        {
            String groupName = groupResult.get().trim();
            if (groupName.isBlank()) return;

            TextInputDialog userDialog = new TextInputDialog();
            userDialog.setTitle("Add a membre");
            userDialog.setHeaderText("Who do you want to add (pseudo) ?");
            userDialog.setContentText("Nickname:");
            Optional<String> userResult = userDialog.showAndWait();
            if (userResult.isPresent()) 
            {
                String nickname = userResult.get().trim();
                if (!nickname.isBlank()) 
                    {clientServer.send("/add_member " + groupName + " " + nickname);}
            }
        }
    }

    // processus d'envoie requête au serveur pour la traiter
    private void processServerMessage(String msg) 
    {
        if (msg.startsWith("CONTACTS|")) 
        {
            String payload = msg.substring("CONTACTS|".length());
            List<String> contactList = new ArrayList<>();
            Platform.runLater(() -> {
                contactsList.getItems().setAll("Tous");
                if (!payload.isBlank()) {
                    for (String cm : payload.split(",")) 
                    {
                        if (!cm.isBlank() && !cm.equals(UserSession.getNickname())) 
                        {
                            contactsList.getItems().add(cm);
                            contactList.add(cm);
                        }
                    }
                }
                UserSession.setContacts(contactList);
            });
            return;
        }
        if (msg.startsWith("GROUPS|")) 
        {
            String payload = msg.substring("GROUPS|".length());
            List<String> groupList = new ArrayList<>();
            Platform.runLater(() -> {
                for (String group : payload.split(",")) 
                {
                    if (!group.isBlank()) 
                    {
                        String label = "Group: " + group;
                        if (!contactsList.getItems().contains(label)) 
                            {contactsList.getItems().add(label);}
                        groupList.add(group);
                    }
                }
                UserSession.setGroups(groupList);
            });
            return;
        }
        if (msg.startsWith("GROUP_CREATED|")) 
        {
            String groupName = msg.substring("GROUP_CREATED|".length());
            addToHistory("GROUP:" + groupName, "[SYSTEM] Groupe créé: " + groupName);
            Platform.runLater(() -> messagesArea.appendText("Groupe créé: " + groupName + "\n"));
            return;
        }
        if (msg.startsWith("MEMBER_ADDED|")) 
        {
            String payload = msg.substring("MEMBER_ADDED|".length());
            addToHistory(selectedTarget, "[SYSTEM] Membre ajouté: " + payload);
            Platform.runLater(() -> messagesArea.appendText("Membre ajouté: " + payload + "\n"));
            return;
        }

        if (msg.startsWith("MSG_DIRECT|")) 
        {
            String[] p = msg.split("\\|", 3);
            if (p.length == 3) {
                String line = p[1] + " -> Moi: " + p[2];
                addToHistory(p[1], line);
                Platform.runLater(() -> {
                    if (selectedTarget.equals(p[1])) {
                        messagesArea.appendText(line + "\n");
                    }
                });
                return;
            }
        }
        if (msg.startsWith("MSG_GROUP|")) 
        {
            String[] p = msg.split("\\|", 4);
            if (p.length == 4) {
                String groupKey = "GROUP:" + p[1];
                String line = "[" + p[1] + "] " + p[2] + ": " + p[3];
                addToHistory(groupKey, line);
                Platform.runLater(() -> {
                    if (selectedTarget.equals(groupKey)) 
                        {messagesArea.appendText(line + "\n");}
                });
                return;
            }
        }

        if (msg.startsWith("ERROR:")) 
        {
            String line = "[ERROR] " + msg.substring(6);
            addToHistory(selectedTarget, line);
            Platform.runLater(() -> messagesArea.appendText(line + "\n"));
            return;
        }
        if (msg.startsWith("MSG_SENT|") || msg.startsWith("MSG_GROUP_SENT|")) 
            {return;}
        Platform.runLater(() -> messagesArea.appendText(msg + "\n"));
    }

    @FXML
    private void logoClicked() 
    {
        try 
            {App.setRoot("personnalPage");} 
        catch (Exception e) 
            {e.printStackTrace();}
    }

    // key - identificateur du chat (ALL, GROUP:groupName ou autre (user))
    // line - text du sms
    // computeIfAbsent - crée un nouveau list du chat si y'en avait pas jusqu'au présent
    private void addToHistory(String key, String line) 
        {chatHistory.computeIfAbsent(key, k -> new ArrayList<>()).add(line);}

    // sert à toujours avoir l'histoire du chat (sinon après avoir passé dans une autre page, puis en revenant on va avoir vide)
    private void showHistory(String key) 
    {
        messagesArea.clear();
        List<String> history = chatHistory.get(key);
        if (history != null) 
        {
            for (String line : history) 
                {messagesArea.appendText(line + "\n");}
        }
    }
}
