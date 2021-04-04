package com.leagueofshadows.enc.Items;

import java.util.ArrayList;

public class Group  {

    public static final int GROUP_ACTIVE = 1;
    public static final int GROUP_NOT_ACTIVE = 2;

    private String id;
    private String name;
    private ArrayList<User> users;
    private String admins;
    private int groupActive;


    public Group(String id, String name, ArrayList<User> users,String admins,int groupActive)
    {
        this.admins = admins;
        this.groupActive = groupActive;
        this.id = id;
        this.name = name;
        this.users = users;
    }
    public Group(){}

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

    public ArrayList<User> getUsers() {
        return users;
    }

    public void setUsers(ArrayList<User> users) {
        this.users = users;
    }
    public int getSize() {
        return users.size();
    }

    public String getAdmins() {
        return admins;
    }

    public void setAdmins(String admins) {
        this.admins = admins;
    }

    public int getGroupActive() {
        return groupActive;
    }

    public void setGroupActive(int groupActive) {
        this.groupActive = groupActive;
    }
}
