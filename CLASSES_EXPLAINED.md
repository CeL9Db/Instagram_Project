# Documentation des classes et méthodes

Ce fichier décrit les classes et les méthodes de "instagram" et de "instagram-client":
- `instagram`: serveur interne + bd
- `instagram-client/chat`: interface JavaFX avec ses controlleurs internes

---

## `instagram` (serveur interne)

### `InstagramApplication`
- Classe de démarrage de l’application Spring Boot.
- Attributs : aucun.
- Méthodes :
    - `main(String[] args)` : point d’entrée, lance Spring Boot.
    - `run(String... args)` : implémente CommandLineRunner, démarre le serveur de sockets via `startEngine()`.

**Rôle :** Démarre la partie serveur, initialise tous les composants Spring et le serveur de sockets.

### `InstagramServer`
- Classe serveur qui implémente le serveur de sockets pour le chat.
- Attributs :
    - `port` : port pour les connexions entrantes (généralement 8080).
    - `users` : liste synchronisée de tous les utilisateurs connectés (`UserHandler`).
- Méthodes :
    - `startEngine()` : démarre le socket serveur, accepte les connexions, crée un UserHandler pour chaque client.
    - `broadcast(String msg, UserHandler fromUser)` : diffuse un message à tous sauf l’expéditeur.
    - `deleteUser(UserHandler user)` : retire un utilisateur de la liste lors de la déconnexion.
    - `run(String... args)` : démarre le serveur au lancement de Spring Boot.

**Rôle :** Gère le cycle de vie du serveur de sockets, maintient la liste des utilisateurs, assure la diffusion des messages.

    #### `startEngine()`
    - Crée un `ServerSocket` sur le port configuré.
    - Boucle infinie `while(true)` pour accepter les connexions entrantes.
    - Pour chaque connexion :
    - crée un nouvel objet `UserHandler` afin de gérer chaque user à part dans son propre thread pour ne pas avoir des problemes liés à des multi-threading.
    - ajoute ce gestionnaire à la liste `users` qui contien l'ensemble des users.
    - affiche des logs pour déboger.

    #### `broadcast(String msg, UserHandler fromUser)`
    - Envoie un message à tous les clients connectés à part celui qui envoie.
    - Utilise `synchronized(users)` pour éviter les conflits d’accès concurrent. On n'utilise pas l'habituel ArrayList car il ne supporte pas le multi-threads.

    #### `deleteUser(UserHandler user)`
    - Supprime un user de la liste synchronisée `users` lorsque la connexion se ferme avec lui.

    #### `run(String... args)`
    - Méthode héritée de `CommandLineRunner`.
    - Appelée automatiquement par Spring Boot au démarrage.
    - Lance `startEngine()` pour activer le serveur socket.

### `UserHandler`
- Représente un utilisateur connecté au serveur.
- Attributs :
    - `socket` : socket de connexion avec le client.
    - `server` : référence vers InstagramServer pour l’interaction.
    - `in` : flux pour lire les messages du client.
    - `out` : flux pour envoyer des messages au client.
- Méthodes :
    - `UserHandler(Socket socket, InstagramServer server)` : constructeur, initialise la connexion et le serveur.
    - `run()` : boucle principale de lecture des messages du client, appelle broadcast pour la diffusion.
    - `send(String msg)` : envoie un message au client.
    - `close()` : ferme la connexion et libère les ressources.

**Rôle :** Gère chaque client individuellement, relaie les messages, gère la fermeture de connexion.

    #### `UserHandler(Socket socket, InstagramServer server)`
    - Constructeur qui initialise la connexion et le serveur référencé.

    #### `run()`
    - Ouvre les flux de lecture et d’écriture sur le `socket`.
    - Lit les lignes envoyées par le client.
    - Pour chaque ligne reçue :
    - appelle `server.broadcast(text, this)` pour transmettre le texte aux autres clients.
    - Si une erreur d’E/S survient, la connexion se ferme.
    - Dans `finally`, appelle `deleteUser(this)` et ferme le socket.

    #### `send(String msg)`
    - Envoie un texte vers le client associé.
    - Vérifie si `out` doit être créé.
    - Utilise `out.println(msg)` pour finalement envoyer la donnée.
    - Permet au serveur de transmettre les messages reçus d’un client aux autres.

    #### `close()`
    - Ferme le socket du client.
    - Utilisée lorsque la connexion est terminée ou lorsqu’une erreur survient.

---

## `instagram-client/chat` (partie JavaFX)

### `App`
- Classe principale du client JavaFX.
- Attributs :
    - `scene` : scène principale de l’application.
- Méthodes :
    - `start(Stage stage)` : initialise et lance la fenêtre principale.
    - `setRoot(String fxml)` : change le layout racine (FXML) pour naviguer entre les écrans.
    - `loadFXML(String fxml)` : charge un fichier FXML depuis les ressources.
    - `main(String[] args)` : lance JavaFX via launch().

**Rôle :** Gère le cycle de vie de l’application cliente, permet de changer d’écran.

    #### `start(Stage stage)`
    - Méthode appelée au lancement de l’application JavaFX.
    - Charge le FXML de l’écran principal.
    - Crée une `Scene` et l’affiche dans la `Stage`.

    #### `setRoot(String fxml)`
    - Remplace la racine de la `Scene` par un autre fichier FXML.
    - Permet de changer d’écran dans l’application.

    #### `loadFXML(String fxml)`
    - Charge un fichier `.fxml` depuis le package de ressources.
    - Retourne le `Parent` correspondant au layout.

    #### `main(String[] args)`
    - Démarre JavaFX avec `launch()`.

### `ClientServer`
- Classe pour gérer la connexion au serveur côté client.
- Attributs :
    - `socket` : connexion TCP avec le serveur.
    - `in` : flux pour lire les messages du serveur.
    - `out` : flux pour envoyer des messages au serveur.
    - `messageListener` : callback pour traiter les messages entrants.
- Méthodes :
    - `ClientServer()` : constructeur, établit la connexion avec le serveur.
    - `send(String msg)` : envoie une chaîne au serveur.
    - `setMessageListener(Consumer<String>)` : définit le gestionnaire des messages entrants.
    - `listen()` : lance un thread pour lire les messages du serveur.

**Rôle :** Assure la communication bidirectionnelle entre client et serveur, reçoit les messages de façon asynchrone.

    #### `ClientServer()`
    - Constructeur qui essaie d’ouvrir une connexion socket vers le serveur.
    - Utilise actuellement `new Socket("localhost", 8080)`.
    - Si le serveur n’est pas accessible, le catch affiche `Problem connection...`.

    #### `send(String msg)`
    - Envoie une chaîne de caractères au serveur si la connexion est disponible.
    - Affiche un message de debug en console.
    - Si `out` est `null`, affiche `ERROR!`.

### `PrimaryController`
- Contrôleur de l’écran principal JavaFX.
- Attributs :
    - Boutons, panneaux, références vers d’autres contrôleurs.
- Méthodes :
    - `switchToSecondary()` : navigue vers secondary.fxml.
    - `showPersonalInfo(ActionEvent event)` : ouvre la page personnelle de l’utilisateur.
    - `showParameters(ActionEvent event)` : ouvre les paramètres.

**Rôle :** Gère la navigation et les actions utilisateur sur l’écran principal.

### `SecondaryController`
- Contrôleur du second écran JavaFX.
- Méthodes :
    - `switchToPrimary()` : retourne à l’écran principal (personnalPage.fxml).

**Rôle :** Contrôleur de démonstration pour la navigation entre les écrans.

---

## à noter :

- Le serveur et le client sont deux applications séparées qui sont liées à l'aide des sockets.
- Le serveur écoute les connexions sur un port (`8080`).
- Le client doit se connecter à ce port en utilisant le bon nom d’hôte.
- Actuellement, le serveur ne stocke pas encore les messages en base de données.
- Le client ne lit pas encore les messages reçus du serveur dans un thread séparé.
- Pour que la communication fonctionne, il faut compléter `UserHandler.run()` et améliorer `ClientServer` pour lire aussi les réponses du serveur.