package instagram.group.instagram.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class UserHandler implements Runnable
{
    private final Socket socket;
    private final InstagramServer server;
    private BufferedReader in;
    private PrintWriter out;

    public UserHandler(Socket socket, InstagramServer server)
    {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run()
    {
        try
        {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
            String text = in.readLine();    // on lit le text
            while(text != null) {server.broadcast(text, this);}    // si le text n'est pas vide on appelle broadcast pour le sauvegarder après dans la bd
            // a finir pour le sauvegarde dans la bd
        } catch(IOException e) {}
        finally
        {
            server.deleteUser(this);
            close();
        }
    }

    public void send(String msg)
    {
        try
        {
            if (out == null)
            {
                out = new PrintWriter(socket.getOutputStream(), true);
                out.print(msg); // envoyer le message
                System.out.println("message send : " + msg);
            }
        } catch(Exception e) { System.err.println("ERROR, there's a problem to send a message to the user");}
    }

    private void close() {try{socket.close();} catch(IOException e) { /*close socket*/}}
}