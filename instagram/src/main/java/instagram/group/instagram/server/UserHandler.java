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
    private String username;

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
            out = new PrintWriter(socket.getOutputStream(), true);
            String text;
            while ((text = in.readLine()) != null)
            {
                System.out.println("Received from client: '" + text + "'");
                if (text.trim().equalsIgnoreCase("exit")) {break;}

                if (text.startsWith("/user:")) 
                {
                    this.username = text.substring(6).trim();
                    System.out.println("User set for socket: " + username);
                    server.addActiveUser(this.username, this);
                    continue;
                }

                if (text.startsWith("/login ")) 
                {
                    handleLogin(text.substring(7));
                    continue;
                }

                if (text.startsWith("/register ")) 
                {
                    handleRegister(text.substring(10));
                    continue;
                }

                if (text.startsWith("/msg ")) 
                {
                    String payload = text.substring(5);
                    String[] parts = payload.split("\\|", 2);
                    if (parts.length != 2)  {
                        send("ERROR: Usage: /msg target|text");
                    } else {
                        if ("ALL".equalsIgnoreCase(parts[0].trim())) {
                            server.broadcast(this.username + "|" + parts[1], this);
                        } else {
                            server.sendDirectMessage(this.username, parts[0], parts[1]);
                        }
                        send("MSG_SENT|" + parts[0] + "|" + parts[1]);
                    }
                    continue;
                }

                if (text.startsWith("/gmsg ")) {
                    String payload = text.substring(6);
                    String[] parts = payload.split("\\|", 2);
                    if (parts.length != 2) {
                        send("ERROR: Usage: /gmsg groupName|text");
                    } else {
                        server.sendGroupMessage(this.username, parts[0], parts[1]);
                        send("MSG_GROUP_SENT|" + parts[0] + "|" + parts[1]);
                    }
                    continue;
                }

                if (text.startsWith("/create_group ")) 
                {
                    String groupName = text.substring(14).trim();
                    server.createGroup(groupName, this.username);
                    send("GROUP_CREATED|" + groupName);
                    continue;
                }

                if (text.startsWith("/add_member ")) 
                {
                    String payload = text.substring(12);
                    String[] parts = payload.split(" ", 2);
                    if (parts.length != 2) 
                        {send("ERROR: Usage: /add_member groupName nickname");}
                    else 
                    {
                        String result = server.addMemberToGroup(parts[0], parts[1], this.username);
                        if (result == null) 
                            {send("MEMBER_ADDED|" + parts[0] + "|" + parts[1]);} 
                        else 
                            {send("ERROR: " + result);}
                    }
                    continue;
                }

                if (this.username != null && !this.username.isBlank()) 
                    {server.broadcast(this.username + "|" + text, this);} 
                else 
                    {send("ERROR: You must login first. Use /login email password");}
            }
        } 
        catch(IOException e) 
            {System.err.println("Connection error: " + e.getMessage());}
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
                {out = new PrintWriter(socket.getOutputStream(), true);}
            out.println(msg);
            out.flush();
        } 
        catch(Exception e) 
            {System.err.println("ERROR, there's a problem to send a message to the user: " + e.getMessage());}
    }

    public String getUsername()
        {return username;}

    private void handleLogin(String params) // params = email+pass séparés par espace
    {
        System.out.println("Processing login: " + params);
        String[] parts = params.split(" ", 2);
        if (parts.length != 2)
        {
            send("ERROR: Usage: /login email password");
            return;
        }
        String email = parts[0];
        String password = parts[1];

        try 
        {
            var clientOpt = server.getClientRepository().findByEmail(email);    // on cherche un user par son email dans la bd
            if (clientOpt.isPresent() && password.equals(clientOpt.get().getPassword())) // s'il existe alors on le laisse passer
            {
                this.username = clientOpt.get().getNickname();
                server.addActiveUser(this.username, this);
                send("LOGIN_SUCCESS|" + this.username);
                System.out.println("Login successful for: " + this.username);
                server.updateContactsForUser(this);
            } else {
                send("ERROR: Invalid identifiants");
                System.out.println("Login failed for email: " + email);
            }
        } catch (Exception e) {
            send("ERROR: Login failed: " + e.getMessage());
            System.err.println("Login error: " + e.getMessage());
        }
    }

    private void handleRegister(String params)
    {
        System.out.println("Processing register: " + params);
        String[] parts = params.split(" ", 3);
        if (parts.length != 3) 
        {
            send("ERROR: Usage: /register nickname email password");
            return;
        }
        String nickname = parts[0];
        String email = parts[1];
        String password = parts[2];

        try 
        {
            if (server.getClientRepository().findByEmail(email).isPresent()) 
            {
                send("ERROR: Email already exists");
                return;
            }
            if (server.getClientRepository().findByNickname(nickname).isPresent()) 
            {
                send("ERROR: Nickname already exists");
                return;
            }

            var client = new instagram.group.instagram.model.Client();  // on crée dans mémoire un user fictif pour save info, puis save to bd
            client.setNickname(nickname);
            client.setEmail(email);
            client.setPassword(password);
            client.setStatus(true);
            client.setDate_reg(java.time.LocalDateTime.now());

            server.getClientRepository().save(client);  // save client en fonction des parametres saisis ci-dessus
            this.username = nickname;
            server.addActiveUser(this.username, this);
            send("REGISTER_SUCCESS|" + nickname);
            server.updateContactsForUser(this);
            System.out.println("Registration successful for: " + nickname);
        } catch (Exception e) {
            send("ERROR: Registration failed: " + e.getMessage());
            System.err.println("Registration error: " + e.getMessage());
        }
    }

    private void close() {try{socket.close();} catch(IOException e) { /*close socket*/}}
}