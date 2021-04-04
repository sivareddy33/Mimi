package com.leagueofshadows.enc.Interfaces;

public interface UserTypingCallback {
    void userTypingInSingleChat(String userId);
    void userTypingInGroupChat(String userId,String groupId);
}
