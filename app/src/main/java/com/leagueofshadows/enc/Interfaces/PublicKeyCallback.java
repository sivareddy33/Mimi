package com.leagueofshadows.enc.Interfaces;

public interface PublicKeyCallback {
    void onSuccess(String Base64PublicKey,String number);
    void onCancelled(String error);
}
