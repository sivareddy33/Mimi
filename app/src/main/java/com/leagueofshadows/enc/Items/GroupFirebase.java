package com.leagueofshadows.enc.Items;

import java.util.ArrayList;

public class GroupFirebase {
    private String id;
    private String name;
    private ArrayList<String> users;

    public GroupFirebase(String id, String name, ArrayList<String> users)
    {
        this.id = id;
        this.name = name;
        this.users = users;
    }

    public GroupFirebase(){}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<String> getUsers() {
        return users;
    }

    public void setUsers(ArrayList<String> users) {
        this.users = users;
    }
    public int getSize() {
        return users.size();
    }
}
