package com.leagueofshadows.enc.Items;

public class MessageInfo {

    private String userId;
    private String receivedTimestamp;
    private String seenTimestamp;

    public MessageInfo(String userId, String receivedTimestamp, String seenTimestamp) {
        this.receivedTimestamp = receivedTimestamp;
        this.seenTimestamp = seenTimestamp;
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getReceivedTimestamp() {
        return receivedTimestamp;
    }

    public void setReceivedTimestamp(String receivedTimestamp) {
        this.receivedTimestamp = receivedTimestamp;
    }

    public String getSeenTimestamp() {
        return seenTimestamp;
    }

    public void setSeenTimestamp(String seenTimestamp) {
        this.seenTimestamp = seenTimestamp;
    }
}
