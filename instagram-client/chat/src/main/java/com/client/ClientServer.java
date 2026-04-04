package com.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientServer 
{
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private java.util.function.Consumer<String> messageListener;
    private final List<String> pendingMessages = new ArrayList<>();
    private volatile boolean connected = false;

    // la connection au serveur
    public ClientServer()
    {
        try
        {
            socket = new Socket("localhost", 8080); // connect server
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));    // connect lire
            out = new PrintWriter(socket.getOutputStream(), true);  // connect ecrire
            startReader();
            connected = true;
        } catch(Exception e) 
            {System.out.println("Problem connection... " + e.getMessage());}
    }

    // les msg dans pendingMessages vont directement au messageListener pour les traiter
    // chaque msg est traitée asynchronnement
    public synchronized void setMessageListener(java.util.function.Consumer<String> listener) 
    {
        this.messageListener = listener;
        if (!pendingMessages.isEmpty()) 
        {
            for (String msg : pendingMessages) 
                {listener.accept(msg);}
            pendingMessages.clear();
        }
    }

    // rajoute le msg à la liste de pendingMessages pour après l'envoyer en fonction de la queue interne
    public synchronized void queueMessage(String message) 
        {pendingMessages.add(message);}

    // crée un thread pour un user
    // puis try{lecture des données}
    // synchronized (this) fait en sort qu'elle donne la possibilité d'avoir la connection assuré entre serveur et client
    // messageListener sert à traiter les messages du serveur
    // pendingMessages sert à faire la même, mais si messageListener n'est pas encore s'est installé, genre si 
    // la connection est établie mais l'interface n'est pas prêt
    // pendingMessages push ce qu'il a accumulé chez lui au serveur
    private void startReader() 
    {
        new Thread(() -> {
            try 
            {
                String line;
                while ((line = in.readLine()) != null) 
                {
                    synchronized (this) 
                    {
                        if (messageListener != null) 
                            {messageListener.accept(line);} 
                        else 
                            {pendingMessages.add(line);}
                    }
                }
            } catch (Exception e) 
                {System.out.println("Connection closed: " + e.getMessage());}
        }, "ClientServer-Reader").start();
    }

    // send msg au côté serveur
    public void send(String msg)
    {
        if (connected && out != null) 
        {
            out.println(msg);
            out.flush(); // envoie dans tous les cas le msg, même s'il y'a un problème
            System.out.println("Send to server: " + msg);
        } 
        else 
            {System.err.println("ERROR: Not connected or message is empty");}
    }

    // verif si un client est connecté
    public boolean isConnected() 
        {return connected && socket != null && !socket.isClosed();}

    // close socket
    public void close()
    {
        try 
        {
            connected = false;
            if (socket != null) 
                {socket.close();}
        } catch (Exception ignored) {}
    }
}
