package com.leagueofshadows.enc.Items;

import androidx.annotation.Nullable;

public class ChatData {

    public static final int CHAT_TYPE_SINGLE_USER = 1;
    public static final int CHAT_TYPE_GROUP = 2;

    private User user;
    private Group group;
    private Message latestMessage;
    private int count;
    private long time;
    private int type;

    public ChatData(User user, Message latestMessage, int count, long time)
    {
        this.user = user;
        this.latestMessage = latestMessage;
        this.count = count;
        this.time = time;
        this.type = CHAT_TYPE_SINGLE_USER;
        this.group = null;
    }

    public ChatData(Group group,Message latestMessage,int count,long time)
    {
        this.group = group;
        this.latestMessage = latestMessage;
        this.count = count;
        this.time = time;
        this.type = CHAT_TYPE_GROUP;
        this.user = null;
    }


    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Message getLatestMessage() {
        return latestMessage;
    }

    public void setLatestMessage(Message latestMessage) {
        this.latestMessage = latestMessage;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getCount() {
        return count;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public int getType() { return type; }

    public void setType(int type) { this.type = type; }

    public Group getGroup() {
        return group;
    }

    @Override
    public boolean equals(@Nullable Object obj) {

        if(obj==null)
            return false;

        if(obj.getClass().equals(this.getClass())){
            ChatData chatData = (ChatData) obj;
            if(chatData.getType()==this.getType()){
                if(chatData.getType()==CHAT_TYPE_SINGLE_USER){
                    return chatData.getUser().getId().equals(this.user.getId());
                }else {
                    return chatData.getGroup().getId().equals(this.group.getId());
                }
            }else
                return false;
        }else {
            return false;
        }
    }
}

