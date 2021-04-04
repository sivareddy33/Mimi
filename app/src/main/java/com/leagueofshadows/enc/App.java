package com.leagueofshadows.enc;

import android.app.Application;

import com.leagueofshadows.enc.Crypt.AESHelper;
import com.leagueofshadows.enc.Interfaces.CompleteCallback;
import com.leagueofshadows.enc.Interfaces.GroupsUpdatedCallback;
import com.leagueofshadows.enc.Interfaces.MessageSentCallback;
import com.leagueofshadows.enc.Interfaces.MessagesRetrievedCallback;
import com.leagueofshadows.enc.Interfaces.ResendMessageCallback;
import com.leagueofshadows.enc.Interfaces.UserTypingCallback;
import com.leagueofshadows.enc.REST.Native;
import com.leagueofshadows.enc.storage.DatabaseManager;
import com.leagueofshadows.enc.storage.SQLHelper;

import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.ArrayList;

import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class App extends Application {

    private PrivateKey privateKey;
    private MessagesRetrievedCallback messagesRetrievedCallback;
    private MessageSentCallback messageSentCallback;
    private CompleteCallback completeCallback;
    private ResendMessageCallback resendMessageCallback;
    private ArrayList<GroupsUpdatedCallback> groupsUpdatedCallbacks;
    private UserTypingCallback userTypingCallback;
    private SecretKey masterKey;
    private DatabaseManager databaseManager;
    private AESHelper aesHelper;
    private Native restHelper;

    public boolean isnull() {
        return privateKey == null ;
    }

    public CompleteCallback getCompleteCallback() {
        return completeCallback;
    }

    public void setCompleteCallback(CompleteCallback completeCallback) { this.completeCallback = completeCallback; }

    public void setMessageSentCallback(MessageSentCallback messageSentCallback) { this.messageSentCallback = messageSentCallback; }

    public MessageSentCallback getMessageSentCallback() {
        return messageSentCallback;
    }

    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    public MessagesRetrievedCallback getMessagesRetrievedCallback() { return messagesRetrievedCallback; }

    public void setMessagesRetrievedCallback(MessagesRetrievedCallback messagesRetrievedCallback) { this.messagesRetrievedCallback = messagesRetrievedCallback; }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public ResendMessageCallback getResendMessageCallback() {
        return resendMessageCallback;
    }

    public void setResendMessageCallback(ResendMessageCallback resendMessageCallback) { this.resendMessageCallback = resendMessageCallback; }

    public ArrayList<GroupsUpdatedCallback> getGroupsUpdatedCallback() { return groupsUpdatedCallbacks; }

    public void addGroupsUpdatedCallback(GroupsUpdatedCallback groupsUpdatedCallback) {
        if(this.groupsUpdatedCallbacks==null)
            this.groupsUpdatedCallbacks = new ArrayList<>();
        if(!this.groupsUpdatedCallbacks.contains(groupsUpdatedCallback))
        this.groupsUpdatedCallbacks.add(groupsUpdatedCallback);
    }

    public void removeGroupsUpdatedCallback(GroupsUpdatedCallback groupsUpdatedCallback){
        if(groupsUpdatedCallbacks!=null){ groupsUpdatedCallbacks.remove(groupsUpdatedCallback); }
    }

    public UserTypingCallback getUserTypingCallback() { return userTypingCallback; }

    public void setUserTypingCallback(UserTypingCallback userTypingCallback) { this.userTypingCallback = userTypingCallback; }

    public void setMasterKey(SecretKey masterKey) { this.masterKey = masterKey; }

    public SecretKey getMasterKey(){ return masterKey; }

    public DatabaseManager getDatabaseManager() {
        if(databaseManager==null){
            DatabaseManager.initializeInstance(new SQLHelper(this));
            databaseManager = DatabaseManager.getInstance();
        }
        return databaseManager;
    }

    public AESHelper getAesHelper() throws NoSuchPaddingException, NoSuchAlgorithmException {
        if(aesHelper==null){
            aesHelper = new AESHelper(this);
        }
        return aesHelper;
    }

    public Native getNativeRestHelper(){
        if(restHelper==null){
            restHelper = new Native(this);
        }
        return restHelper;
    }
}
