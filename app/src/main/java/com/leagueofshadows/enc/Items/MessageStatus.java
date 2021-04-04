package com.leagueofshadows.enc.Items;

public class MessageStatus {
    public static int SEEN_STATUS = 1;
    public static int RECEIVED_STATUS = 2;

    private int id;
    private String messageId;
    private String timestamp;
    private String userId;
    private String fromUserId;
    private int typeOfStatus;
    private int typeOfMessage;

    public MessageStatus(){ }

    public MessageStatus(String messageId,String timestamp,String userId,String fromUserId,int typeOfStatus,int typeOfMessage,int id){
        this.messageId = messageId;
        this.timestamp = timestamp;
        this.userId = userId;
        this.fromUserId = fromUserId;
        this.typeOfMessage = typeOfMessage;
        this.typeOfStatus = typeOfStatus;
        this.id = id;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(String fromUserId) {
        this.fromUserId = fromUserId;
    }

    public int getTypeOfStatus() {
        return typeOfStatus;
    }

    public void setTypeOfStatus(int typeOfStatus) {
        this.typeOfStatus = typeOfStatus;
    }

    public int getTypeOfMessage() {
        return typeOfMessage;
    }

    public void setTypeOfMessage(int typeOfMessage) {
        this.typeOfMessage = typeOfMessage;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
