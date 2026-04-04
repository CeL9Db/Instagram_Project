package instagram.group.instagram.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import instagram.group.instagram.model.Client;
import instagram.group.instagram.model.Group;
import instagram.group.instagram.model.Message;
import instagram.group.instagram.repository.ClientRepository;
import instagram.group.instagram.repository.GroupRepository;
import instagram.group.instagram.repository.MessageRepository;

@Component
public class InstagramServer implements CommandLineRunner
{
    private final int port = 8080;
    private final List<UserHandler> users = Collections.synchronizedList(new ArrayList<>());

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private GroupRepository groupRepository;

    private final Map<String, UserHandler> activeUsers = new ConcurrentHashMap<>();

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

    public void broadcast(String fullText, UserHandler fromUser)
    {
        // Protocol: nickname|actual message
        String[] parts = fullText.split("\\|", 2);
        String sender = parts.length > 0 ? parts[0] : "unknown";
        String messageText = parts.length > 1 ? parts[1] : "";

        saveMessage(sender, messageText);

        String outbound = sender + ": " + messageText;
        synchronized(users)
        {
            for (UserHandler user : users) 
            {
                if (user != fromUser) 
                    {user.send(outbound);}
            }
        }
    }

    public void saveMessage(String sender, String text) 
        {saveMessage(sender, text, null, null);}

    public void saveMessage(String sender, String text, String toUser, String toGroup) 
    {
        try 
        {
            Message message = new Message();
            message.setMessage(text);
            message.setDate(LocalDateTime.now());

            if (sender != null && !sender.isBlank()) 
                {clientRepository.findByNickname(sender).ifPresent(message::setFromEntity);}
            if (toUser != null && !toUser.isBlank()) 
                {clientRepository.findByNickname(toUser).ifPresent(message::setToUser);}
            if (toGroup != null && !toGroup.isBlank()) 
                {groupRepository.findByName_group(toGroup).ifPresent(message::setToGroup);}

            messageRepository.save(message);
        } 
        catch (Exception e) 
            {System.err.println("Failed to save message: " + e.getMessage());}
    }

    public ClientRepository getClientRepository() 
        {return clientRepository;}

    public void deleteUser(UserHandler user) 
    {
        users.remove(user);
        if (user.getUsername() != null) 
        {
            activeUsers.remove(user.getUsername());
            broadcastContactsUpdate();
        }
    }

    public void addActiveUser(String username, UserHandler userHandler) 
    {
        activeUsers.put(username, userHandler);
        broadcastContactsUpdate();
    }

    // utile pour renouveller l'information lors de la création d'une nouvelle groupe, nouveau user, donc sms global pour tous
    public void broadcastContactsUpdate() 
    {
        String contactList = clientRepository.findAll().stream()
            .map(Client::getNickname)
            .collect(Collectors.joining(","));
        for (UserHandler handler : users) 
        {
            handler.send("CONTACTS|" + contactList);
            String username = handler.getUsername();
            if (username == null || username.isBlank()) 
                {handler.send("GROUPS|");} 
            else 
            {
                String groupList = groupRepository.findByMemberNickname(username).stream()
                    .map(Group::getName_group)
                    .collect(Collectors.joining(","));
                handler.send("GROUPS|" + groupList);
            }
        }
    }

    // utile pour renouveller l'information lors de connection d'un user ou d'un clic sur le bouton quelque part (rentrer dans la page personnelle ou autre)
    public void updateContactsForUser(UserHandler user) 
    {
        String username = user.getUsername();
        
        // on récupère chaque user se trouvant dans la bd à part celui à qui on est connecté
        List<Client> allClients = clientRepository.findAll();
        StringBuilder contactsBuilder = new StringBuilder();
        for (Client c : allClients) 
        {
            if (!c.getNickname().equals(username)) 
            {
                if (contactsBuilder.length() > 0) contactsBuilder.append(",");
                contactsBuilder.append(c.getNickname());
            }
        }
        String contacts = contactsBuilder.toString();
        user.send("CONTACTS|" + contacts);
        
        // Groups only for this user
        if (username == null || username.isBlank()) 
            {user.send("GROUPS|");} 
        else 
        {
            String groupList = groupRepository.findByMemberNickname(username).stream()
                .map(Group::getName_group)
                .collect(Collectors.joining(","));
            user.send("GROUPS|" + groupList);
        }
    }

    public void sendDirectMessage(String from, String to, String text)
    {
        saveMessage(from, text, to, null);
        UserHandler target = activeUsers.get(to);
        if (target != null) {target.send("MSG_DIRECT|" + from + "|" + text);}
    }

    public void sendGroupMessage(String from, String groupName, String text) 
    {
        saveMessage(from, text, null, groupName);
        groupRepository.findByName_group(groupName).ifPresent(grp -> {
            for (Client member : grp.getMembers()) 
            {
                if (!member.getNickname().equals(from)) 
                {
                    UserHandler target = activeUsers.get(member.getNickname());
                    if (target != null) {target.send("MSG_GROUP|" + groupName + "|" + from + "|" + text);}
                }
            }
        });
    }

    public void createGroup(String groupName, String owner) 
    {
        if (groupRepository.findByName_group(groupName).isPresent()) return;
        var adminOpt = clientRepository.findByNickname(owner);
        if (adminOpt.isEmpty()) 
        {
            System.err.println("Cannot create group, admin not found: " + owner);
            return;
        }
        Client admin = adminOpt.get();
        Group group = new Group();
        group.setName_group(groupName);
        group.setAdmin(admin);
        group.setDate_reg(LocalDateTime.now());
        group.setMembers(new ArrayList<>());
        group.getMembers().add(admin);
        try {
            groupRepository.save(group);
            broadcastContactsUpdate();
        } catch (Exception e) 
            {System.err.println("Failed to create group: " + e.getMessage());}
    }

    public String addMemberToGroup(String groupName, String memberNick, String requester) 
    {
        var g = groupRepository.findByName_group(groupName);
        var c = clientRepository.findByNickname(memberNick);
        if (g.isEmpty()) 
            {return "Group not found: " + groupName;}
        if (c.isEmpty()) 
            {return "Member not found: " + memberNick;}

        Group group = g.get();
        Client member = c.get();

        if (group.getAdmin() == null || !group.getAdmin().getNickname().equals(requester)) 
            {return "Only the group admin can add members";}

        if (group.getMembers() == null) group.setMembers(new ArrayList<>());
        if (group.getMembers().stream().anyMatch(m -> m.getId_client().equals(member.getId_client()))) 
            {return "Member " + memberNick + " is already in group " + groupName;}

        group.getMembers().add(member);
        System.out.println("Adding member " + memberNick + " to group " + groupName + " by " + requester);
        try 
        {
            groupRepository.save(group);
            broadcastContactsUpdate();
            System.out.println("Member " + memberNick + " added to group " + groupName);
            return null;
        } catch (Exception e) {
            System.err.println("Failed to add member to group: " + e.getMessage());
            return "Failed to add member: " + e.getMessage();
        }
    }

    @Override
    public void run(String... args) throws Exception 
    {
        System.out.println("Spring BOOT loaded successfully, waiting to load Socket");
        startEngine();
    }
}
