package com.leagueofshadows.enc.Interfaces;

import com.leagueofshadows.enc.Items.Message;

public interface MessagesRetrievedCallback {

    void onNewMessage(Message message);
    void onUpdateMessageStatus(String messageId,String userId);
}
