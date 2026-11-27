package instagram.group.instagram.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import instagram.group.instagram.model.Client;
import instagram.group.instagram.repository.ClientRepository;

// Tests du protocole texte entre le client et le serveur.
// La socket est simulée : on lui donne des commandes en entrée et on lit ce qu'elle renvoie.
@ExtendWith(MockitoExtension.class)
class UserHandlerTest
{
    @Mock
    private Socket socket;

    @Mock
    private InstagramServer server;

    @Mock
    private ClientRepository clientRepository;

    private ByteArrayOutputStream sortie;

    // exécute le handler avec les lignes données, comme si le client les envoyait,
    // et renvoie les lignes répondues par le serveur
    private List<String> executer(UserHandler handler, String... lignes) throws Exception
    {
        String entree = String.join("\n", lignes) + "\n";
        sortie = new ByteArrayOutputStream();
        when(socket.getInputStream()).thenReturn(new ByteArrayInputStream(entree.getBytes(StandardCharsets.UTF_8)));
        when(socket.getOutputStream()).thenReturn(sortie);

        handler.run();	// s'arrête quand il n'y a plus de lignes à lire

        return sortie.toString(StandardCharsets.UTF_8).lines().toList();
    }

    private Client creerClient(String nickname, String email, String password)
    {
        Client client = new Client();
        client.setNickname(nickname);
        client.setEmail(email);
        client.setPassword(password);
        return client;
    }

    // ---------- connexion ----------

    @Test
    void login_identifiantsCorrects_connecteLUtilisateur() throws Exception
    {
        when(server.getClientRepository()).thenReturn(clientRepository);
        when(clientRepository.findByEmail("alice@mail.fr"))
            .thenReturn(Optional.of(creerClient("alice", "alice@mail.fr", "secret")));
        UserHandler handler = new UserHandler(socket, server);

        List<String> reponses = executer(handler, "/login alice@mail.fr secret");

        assertEquals(List.of("LOGIN_SUCCESS|alice"), reponses);
        assertEquals("alice", handler.getUsername());
        verify(server).addActiveUser("alice", handler);
        verify(server).updateContactsForUser(handler);
    }

    @Test
    void login_mauvaisMotDePasse_renvoieUneErreur() throws Exception
    {
        when(server.getClientRepository()).thenReturn(clientRepository);
        when(clientRepository.findByEmail("alice@mail.fr"))
            .thenReturn(Optional.of(creerClient("alice", "alice@mail.fr", "secret")));
        UserHandler handler = new UserHandler(socket, server);

        List<String> reponses = executer(handler, "/login alice@mail.fr faux");

        assertEquals(List.of("ERROR: Invalid identifiants"), reponses);
        verify(server, never()).addActiveUser(anyString(), any());
    }

    @Test
    void login_sansMotDePasse_renvoieLUsage() throws Exception
    {
        UserHandler handler = new UserHandler(socket, server);

        List<String> reponses = executer(handler, "/login alice@mail.fr");

        assertEquals(List.of("ERROR: Usage: /login email password"), reponses);
    }
}
