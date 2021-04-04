package com.leagueofshadows.enc.Interfaces;

import com.leagueofshadows.enc.Items.Message;

public interface MessageSentCallback {
    void onComplete(Message message,boolean success,String error);
}
