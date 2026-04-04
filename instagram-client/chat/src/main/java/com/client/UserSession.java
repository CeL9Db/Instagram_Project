package com.client;

import java.util.ArrayList;
import java.util.List;

public class UserSession {
    private static Integer id;
    private static String email;
    private static String nickname;
    private static String avatarPath;
    private static List<String> contacts = new ArrayList<>();
    private static List<String> groups = new ArrayList<>();

    public static void setId(Integer id) { UserSession.id = id; }
    public static void setEmail(String email) { UserSession.email = email; }
    public static void setNickname(String nickname) { UserSession.nickname = nickname; }
    public static void setAvatarPath(String path) { UserSession.avatarPath = path; }

    public static Integer getId() { return id; }
    public static String getEmail() { return email; }
    public static String getNickname() { return nickname; }
    public static String getAvatarPath() { return avatarPath; }

    public static List<String> getContacts() { return contacts; }
    public static void setContacts(List<String> c) { contacts = new ArrayList<>(c); }
    public static List<String> getGroups() { return groups; }
    public static void setGroups(List<String> g) { groups = new ArrayList<>(g); }

    public static void clear() {
        id = null; email = null; nickname = null; avatarPath = null;
        contacts.clear();
        groups.clear();
    }
}
