package com.leagueofshadows.enc.Items;

import com.leagueofshadows.enc.Crypt.AESHelper;
import com.leagueofshadows.enc.storage.DatabaseManager;

import org.json.JSONObject;

import java.util.ArrayList;

import static com.leagueofshadows.enc.Items.Message.MESSAGE_TYPE_ONLYTEXT;
import static com.leagueofshadows.enc.Items.Message.MESSAGE_TYPE_SINGLE_USER;

public class EncryptedMessage {

    private String id;
    private String to;
    private String from;
    private String content;
    private String timeStamp;
    private String filePath;
    private int type;
    private boolean resend;
    private int isGroupMessage;

    public EncryptedMessage(String id, String to, String from, String content, String filePath, String timeStamp, int type,int isGroupMessage)
    {
        this.id = id;
        this.to = to;
        this.from = from;
        this.content = content;
        this.timeStamp = timeStamp;
        this.type = type;
        this.filePath = filePath;
        this.resend = false;
        this.isGroupMessage = isGroupMessage;
    }

    public EncryptedMessage(String id, String to, String from, String content, String filePath, String timeStamp, int type,boolean resend,int isGroupMessage)
    {
        this.isGroupMessage = isGroupMessage;
        this.id = id;
        this.to = to;
        this.from = from;
        this.content = content;
        this.timeStamp = timeStamp;
        this.type = type;
        this.filePath = filePath;
        this.resend = resend;
    }

    public EncryptedMessage(){
        this.resend = false;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
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

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public boolean isResend() {
        return resend;
    }

    public void setResend(boolean resend) {
        this.resend = resend;
    }

    public int getIsGroupMessage() { return isGroupMessage; }

    public void setIsGroupMessage(int isGroupMessage) { this.isGroupMessage = isGroupMessage; }

    public ArrayList<FirebaseMessage> getFirebaseMessageList(DatabaseManager databaseManager){
        ArrayList<FirebaseMessage> firebaseMessages = new ArrayList<>();

        if(this.isGroupMessage==MESSAGE_TYPE_SINGLE_USER){
            firebaseMessages.add(new FirebaseMessage(this,this.to));
        }
        try {
            JSONObject jsonObject = new JSONObject(this.content);

            String iv = jsonObject.getString(AESHelper.MESSAGE_IV);
            String messageHash = jsonObject.getString(AESHelper.MESSAGE_HASH_BYTES);
            JSONObject keys = jsonObject.getJSONObject(AESHelper.MESSAGE_KEYS);
            String content = jsonObject.getString(AESHelper.MESSAGE_CONTENT);
            String fileHash = null;
            if(type!=MESSAGE_TYPE_ONLYTEXT)
                fileHash = jsonObject.getString(AESHelper.MESSAGE_FILE_HASH_BYTES);

            ArrayList<String> userIds = databaseManager.getGroupParticipants(to);
            for (String id : userIds) {

                if(keys.has(id)) {

                    JSONObject j = new JSONObject();
                    j.put(AESHelper.MESSAGE_IV, iv);
                    j.put(AESHelper.MESSAGE_CONTENT, content);
                    j.put(AESHelper.MESSAGE_HASH_BYTES, messageHash);
                    if (fileHash != null)
                        j.put(AESHelper.MESSAGE_FILE_HASH_BYTES, fileHash);

                    JSONObject keysObject = new JSONObject();
                    keysObject.put(id, keys.getString(id));

                    j.put(AESHelper.MESSAGE_KEYS, keysObject);
                    firebaseMessages.add(new FirebaseMessage(new EncryptedMessage(this.id, this.to, this.from, j.toString(), this.filePath, this.timeStamp, this.type, this.resend, this.isGroupMessage),id));
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return firebaseMessages;
    }
}
