package com.leagueofshadows.enc.Items;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class Message {

    private String message_id;
    private int id;
    private String to;
    private String from;
    private String content;
    private String filePath;
    private String timeStamp;
    private int type;
    private String sent;
    private String received;
    private String seen;
    private int isGroupMessage;

    public static final int MESSAGE_TYPE_ONLYTEXT = 1;
    public static final int MESSAGE_TYPE_IMAGE = 2;
    public static final int MESSAGE_TYPE_FILE = 3;

    public static final int MESSAGE_TYPE_GROUP_MESSAGE = 5;
    public static final int MESSAGE_TYPE_SINGLE_USER = 6;


    public Message() {}

    public Message(int id, @NonNull String message_id, @NonNull String to, @NonNull String from, String content, String filePath, @NonNull String timeStamp, int type, String sent, String received, String seen,int isGroupMessage)
    {
        this.message_id = message_id;
        this.id = id;
        this.to = to;
        this.from = from;
        this.content = content;
        this.filePath = filePath;
        this.timeStamp = timeStamp;
        this.type = type;
        this.sent = sent;
        this.received = received;
        this.seen = seen;
        this.isGroupMessage = isGroupMessage;
    }
    public Message(int id, @NonNull String message_id, @NonNull String to, @NonNull String from, String content,
                   String filePath, @NonNull String timeStamp, int type, String sent, String received, String seen,boolean resend,int isGroupMessage)
    {
        this.message_id = message_id;
        this.id = id;
        this.to = to;
        this.from = from;
        this.content = content;
        this.filePath = filePath;
        this.timeStamp = timeStamp;
        this.type = type;
        this.sent = sent;
        this.received = received;
        this.seen = seen;
        this.isGroupMessage = isGroupMessage;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getSent() {
        return sent;
    }

    public void setSent(String sent) {
        this.sent = sent;
    }

    public String getReceived() {
        return received;
    }

    public void setReceived(String received) {
        this.received = received;
    }

    public String getSeen() {
        return seen;
    }

    public void setSeen(String seen) {
        this.seen = seen;
    }

    public String getMessage_id() {
        return message_id;
    }

    public void setMessage_id(String message_id) {
        this.message_id = message_id;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if(obj!=null) {
            if(obj.getClass().equals(this.getClass())) {
                Message m = (Message) obj;
                try {
                    return this.message_id.equals(m.getMessage_id());
                } catch (Exception e) {
                    return false;
                }
            }
            else
                return false;
        }
        else
            return false;
    }

    public int getIsGroupMessage() {
        return isGroupMessage;
    }

    public void setIsGroupMessage(int isGroupMessage) {
        this.isGroupMessage = isGroupMessage;
    }
}
