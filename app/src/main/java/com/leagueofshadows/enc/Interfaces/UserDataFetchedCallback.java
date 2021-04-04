package com.leagueofshadows.enc.Interfaces;

public interface UserDataFetchedCallback {
    void onComplete(boolean userExists,String uid,String Base64PublicKey,String number);
    void onError(String error);
}
