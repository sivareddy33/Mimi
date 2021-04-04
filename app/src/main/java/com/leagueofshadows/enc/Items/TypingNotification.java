package com.leagueofshadows.enc.Items;

public class TypingNotification {
    private String fromUserId;
    private long time;
    private String groupId;
    private boolean isGroup;
    public TypingNotification(){}
    public TypingNotification(String userId,String groupId,long time,boolean isGroup){
        this.groupId = groupId;
        this.time = time;
        this.fromUserId = userId;
        this.isGroup = isGroup;
    }

    public String getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(String userId) {
        this.fromUserId = userId;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public boolean isGroup() {
        return isGroup;
    }

    public void setGroup(boolean group) {
        isGroup = group;
    }
}
