# Contrôleurs, session et avatars (nouveau)

## MainChatController
- Contrôleur principal du chat JavaFX.
- Attributs :
    - `contactsList` : liste des contacts et groupes.
    - `messagesArea` : (sera remplacé par un ListView) — zone d’affichage des messages.
    - `userLabel` : affiche le pseudo et la mini-avatar de l’utilisateur.
    - `clientServer` : objet pour la communication avec le serveur.
    - `selectedTarget` : cible de discussion actuelle (utilisateur ou groupe).
    - `chatHistory` : historique des messages pour chaque discussion.
- Méthodes :
    - `initialize()` : initialise l’UI, charge les contacts, configure le cell factory pour les mini-avatars.
    - `processServerMessage(String msg)` : traite les messages entrants du serveur.
    - `sendBtnClicked()` : envoie un message.
    - `addToHistory(String key, String line)` : ajoute une ligne à l’historique du chat.
    - `showHistory(String key)` : affiche l’historique de la discussion sélectionnée.
    - `logoClicked()` : navigue vers la page de profil.

**Rôle :** Gère la logique du chat, l’affichage des messages, les mini-avatars, la navigation entre discussions et groupes.

## PersonalInfoController
- Contrôleur de la page de profil utilisateur.
- Attributs :
    - `avatarImageView` : ImageView pour l’avatar.
    - `btnUploadAvatar` : bouton pour téléverser un avatar.
- Méthodes :
    - `initialize()` : initialise la page, charge l’avatar actuel.
    - `btnUploadAvatar.setOnAction` : gestionnaire pour téléverser un nouvel avatar via FileChooser.

**Rôle :** Permet à l’utilisateur de voir et modifier son avatar, ainsi que ses informations personnelles.

## UserSession
- Classe pour stocker les données de session de l’utilisateur côté client.
- Attributs (statiques) :
    - `id`, `email`, `nickname`, `avatarPath` : données principales de l’utilisateur.
    - `contacts`, `groups` : listes des contacts et groupes.
- Méthodes :
    - Getters et setters pour tous les attributs.
    - `clear()` : réinitialise toutes les données de session.

**Rôle :** Permet de sauvegarder et restaurer rapidement l’état utilisateur entre les écrans sans requêtes répétées au serveur.

## Mécanisme des mini-avatars
- Dans la liste des contacts (`contactsList`), chaque utilisateur a une mini-avatar (32x32px).
- L’avatar est recherché dans `avatars/<nickname>.png`.
- Si le fichier n’existe pas, on peut afficher une image par défaut.
- Pour afficher l’avatar dans le chat (à côté du message), il faut remplacer le `TextArea` par un `ListView` avec des cellules personnalisées : chaque cellule est un HBox avec un ImageView (avatar) et un Label (texte du message).

## Conseils d’extension
- Pour un affichage complet des avatars dans le chat, créez une classe MessageItem (pseudo, texte, chemin de l’avatar) et utilisez un cell factory personnalisé pour les messages.
- Pour stocker les avatars côté serveur, utilisez une base de données ou le système de fichiers, et stockez le chemin/nom du fichier dans le profil utilisateur.
