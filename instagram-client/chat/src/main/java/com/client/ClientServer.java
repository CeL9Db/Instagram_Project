package com.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class ClientServer 
{
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public ClientServer()
    {
        try
        {
            socket = new Socket("localhost", 8080); // connect server
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));    // connect lire
            out = new PrintWriter(socket.getOutputStream(), true);  // connect ecrire
        } catch(Exception e) {System.out.println("Problem connection... " + e.getMessage());}
    }

    public void send(String msg)
    {
        if (out != null) {
            out.println(msg);
            System.out.println("Send to server: " + msg);
        } else {System.err.println("ERROR!");}
    }
}
