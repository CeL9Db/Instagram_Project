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

    // ---------- groupes ----------

    @Test
    void createGroup_creeLeGroupeAvecLAdminCommeMembre()
    {
        Client alice = creerClient(1, "alice");
        when(groupRepository.findByName_group("projet")).thenReturn(Optional.empty());
        when(clientRepository.findByNickname("alice")).thenReturn(Optional.of(alice));

        server.createGroup("projet", "alice");

        ArgumentCaptor<Group> captor = ArgumentCaptor.forClass(Group.class);
        verify(groupRepository).save(captor.capture());
        Group group = captor.getValue();
        assertEquals("projet", group.getName_group());
        assertEquals(alice, group.getAdmin());
        assertEquals(List.of(alice), group.getMembers());
        assertNotNull(group.getDate_reg());
    }

    @Test
    void createGroup_nomDejaPris_rienNEstCree()
    {
        when(groupRepository.findByName_group("projet"))
            .thenReturn(Optional.of(creerGroupe("projet", creerClient(1, "alice"))));

        server.createGroup("projet", "bob");

        verify(groupRepository, never()).save(any());
    }

    @Test
    void createGroup_adminInconnu_rienNEstCree()
    {
        when(groupRepository.findByName_group("projet")).thenReturn(Optional.empty());
        when(clientRepository.findByNickname("fantome")).thenReturn(Optional.empty());

        server.createGroup("projet", "fantome");

        verify(groupRepository, never()).save(any());
    }

    @Test
    void addMemberToGroup_parLAdmin_ajouteLeMembre()
    {
        Client alice = creerClient(1, "alice");
        Client carol = creerClient(3, "carol");
        Group group = creerGroupe("projet", alice);
        when(groupRepository.findByName_group("projet")).thenReturn(Optional.of(group));
        when(clientRepository.findByNickname("carol")).thenReturn(Optional.of(carol));

        String erreur = server.addMemberToGroup("projet", "carol", "alice");

        assertNull(erreur);	// null = pas d'erreur
        assertTrue(group.getMembers().contains(carol));
        verify(groupRepository).save(group);
    }

    @Test
    void addMemberToGroup_pasAdmin_refuse()
    {
        Client alice = creerClient(1, "alice");
        Group group = creerGroupe("projet", alice);
        when(groupRepository.findByName_group("projet")).thenReturn(Optional.of(group));
        when(clientRepository.findByNickname("carol")).thenReturn(Optional.of(creerClient(3, "carol")));

        String erreur = server.addMemberToGroup("projet", "carol", "bob");

        assertEquals("Only the group admin can add members", erreur);
        verify(groupRepository, never()).save(any());
    }

    @Test
    void addMemberToGroup_dejaMembre_refuse()
    {
        Client alice = creerClient(1, "alice");
        Client carol = creerClient(3, "carol");
        Group group = creerGroupe("projet", alice);
        group.getMembers().add(carol);
        when(groupRepository.findByName_group("projet")).thenReturn(Optional.of(group));
        when(clientRepository.findByNickname("carol")).thenReturn(Optional.of(carol));

        String erreur = server.addMemberToGroup("projet", "carol", "alice");

        assertEquals("Member carol is already in group projet", erreur);
        verify(groupRepository, never()).save(any());
    }

    @Test
    void addMemberToGroup_groupeInconnu_renvoieUneErreur()
    {
        when(groupRepository.findByName_group("inconnu")).thenReturn(Optional.empty());

        String erreur = server.addMemberToGroup("inconnu", "carol", "alice");

        assertEquals("Group not found: inconnu", erreur);
    }

    // ---------- contacts ----------

    @Test
    void updateContactsForUser_envoieLesAutresUtilisateursEtSesGroupes()
    {
        Client alice = creerClient(1, "alice");
        when(clientRepository.findAll())
            .thenReturn(List.of(alice, creerClient(2, "bob"), creerClient(3, "carol")));
        when(groupRepository.findByMemberNickname("alice"))
            .thenReturn(List.of(creerGroupe("projet", alice), creerGroupe("famille", alice)));
        UserHandler handler = mock(UserHandler.class);
        when(handler.getUsername()).thenReturn("alice");

        server.updateContactsForUser(handler);

        verify(handler).send("CONTACTS|bob,carol");	// alice ne se voit pas elle-même
        verify(handler).send("GROUPS|projet,famille");
    }
}
