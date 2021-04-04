package com.leagueofshadows.enc.Items;

public class FirebaseMessage {

    private EncryptedMessage encryptedMessage;
    private String OtherUserId;
    FirebaseMessage(EncryptedMessage encryptedMessage,String otherUserId){
        this.encryptedMessage = encryptedMessage;
        this.OtherUserId = otherUserId;
    }

    public EncryptedMessage getEncryptedMessage() {
        return encryptedMessage;
    }

    public void setEncryptedMessage(EncryptedMessage encryptedMessage) {
        this.encryptedMessage = encryptedMessage;
    }

    public String getOtherUserId() {
        return OtherUserId;
    }

    public void setOtherUserId(String otherUserId) {
        OtherUserId = otherUserId;
    }
}
