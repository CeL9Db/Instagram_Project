package instagram.group.instagram.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import instagram.group.instagram.model.Client;
import instagram.group.instagram.model.Group;
import instagram.group.instagram.model.Message;
import instagram.group.instagram.repository.ClientRepository;
import instagram.group.instagram.repository.GroupRepository;
import instagram.group.instagram.repository.MessageRepository;

// Tests de la logique du serveur sans base de données ni vraies sockets :
// les repositories et les UserHandler sont des mocks
@ExtendWith(MockitoExtension.class)
class InstagramServerTest
{
    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private GroupRepository groupRepository;

    @InjectMocks
    private InstagramServer server;

    private Client creerClient(int id, String nickname)
    {
        Client client = new Client();
        client.setId_client(id);
        client.setNickname(nickname);
        return client;
    }

    private Group creerGroupe(String nom, Client admin)
    {
        Group group = new Group();
        group.setName_group(nom);
        group.setAdmin(admin);
        group.setMembers(new ArrayList<>(List.of(admin)));
        return group;
    }

    // ajoute un utilisateur dans la liste privée "users" (normalement remplie par startEngine)
    @SuppressWarnings("unchecked")
    private void connecter(UserHandler handler)
    {
        List<UserHandler> users = (List<UserHandler>) ReflectionTestUtils.getField(server, "users");
        users.add(handler);
    }

    // ---------- messages ----------

    @Test
    void broadcast_envoieATousSaufALExpediteur()
    {
        UserHandler alice = mock(UserHandler.class);
        UserHandler bob = mock(UserHandler.class);
        UserHandler carol = mock(UserHandler.class);
        connecter(alice);
        connecter(bob);
        connecter(carol);

        server.broadcast("alice|coucou", alice);

        verify(bob).send("alice: coucou");
        verify(carol).send("alice: coucou");
        verify(alice, never()).send(anyString());
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void saveMessage_relieLExpediteurEtLeDestinataire()
    {
        Client alice = creerClient(1, "alice");
        Client bob = creerClient(2, "bob");
        when(clientRepository.findByNickname("alice")).thenReturn(Optional.of(alice));
        when(clientRepository.findByNickname("bob")).thenReturn(Optional.of(bob));

        server.saveMessage("alice", "salut", "bob", null);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(captor.capture());
        Message message = captor.getValue();
        assertEquals("salut", message.getMessage());
        assertEquals(alice, message.getFromEntity());
        assertEquals(bob, message.getToUser());
        assertNull(message.getToGroup());
        assertNotNull(message.getDate());
    }

    @Test
    void sendDirectMessage_destinataireConnecte_recoitLeMessage()
    {
        UserHandler bob = mock(UserHandler.class);
        server.addActiveUser("bob", bob);

        server.sendDirectMessage("alice", "bob", "salut");

        verify(bob).send("MSG_DIRECT|alice|salut");
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void sendDirectMessage_destinataireHorsLigne_messageQuandMemeEnregistre()
    {
        server.sendDirectMessage("alice", "bob", "tu es là ?");

        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void sendGroupMessage_envoieAuxMembresSaufALExpediteur()
    {
        Client alice = creerClient(1, "alice");
        Client bob = creerClient(2, "bob");
        Group group = creerGroupe("projet", alice);
        group.getMembers().add(bob);
        when(groupRepository.findByName_group("projet")).thenReturn(Optional.of(group));

        UserHandler aliceHandler = mock(UserHandler.class);
        UserHandler bobHandler = mock(UserHandler.class);
        server.addActiveUser("alice", aliceHandler);
        server.addActiveUser("bob", bobHandler);

        server.sendGroupMessage("alice", "projet", "réunion à 14h");

        verify(bobHandler).send("MSG_GROUP|projet|alice|réunion à 14h");
        verify(aliceHandler, never()).send(anyString());
    }
}
