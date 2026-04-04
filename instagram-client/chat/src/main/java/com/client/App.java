package com.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application 
{

    private static Scene scene;
    private static ClientServer clientServer;

    @Override
    public void start(Stage stage) throws IOException
    {
        clientServer = new ClientServer();
        scene = new Scene(loadFXML("login"), 1280, 800);
        stage.setScene(scene);
        stage.show();
    }

    static void setRoot(String fxml) throws IOException 
        {scene.setRoot(loadFXML(fxml));}

    // getter pour les attributs de ClientServer 
    public static ClientServer getClientServer() 
        {return clientServer;}

    private static Parent loadFXML(String fxml) throws IOException 
    {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) 
        {launch();}

}