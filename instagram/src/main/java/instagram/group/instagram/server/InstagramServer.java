package instagram.group.instagram.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import instagram.group.instagram.model.Message;

@Component
public class InstagramServer implements CommandLineRunner
{
    private final int port = 8080;
    // collection est mieux par rapport à arraylist, car ce dernier n'arrive pas à travailler avec multi-threading
    private final List<UserHandler> users = Collections.synchronizedList(new ArrayList<>());

    public void startEngine() throws IOException
    {
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Using " + port + ". Waiting for users...");
        while(true)
        {
            Socket socket = serverSocket.accept();   // on attends la réponse
            UserHandler handler = new UserHandler(socket, this);
            users.add(handler);
            System.out.println("User is connected successfully" + "\n" + "There are " + users.size() + " users connected !");
            new Thread(handler).start();    // crée des threads (1 thread = 1 user)
        }
    }

    public void broadcast(String msg, UserHandler fromUser) // envoie une message à tous les users sauf celui qui envoi
    {
        synchronized(users)
        {
            for(UserHandler user : users)
            {if(user != fromUser) {user.send(msg);}}
        }
    }
    

    public void deleteUser(UserHandler user) {users.remove(user);}

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Spring BOOT loaded successfully, waiting to load Socket");
        startEngine();
    }
}
