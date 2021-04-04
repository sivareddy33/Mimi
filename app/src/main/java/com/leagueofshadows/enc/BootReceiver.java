package com.leagueofshadows.enc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.leagueofshadows.enc.storage.DatabaseManager;
import com.leagueofshadows.enc.storage.SQLHelper;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        try{
            if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
                DatabaseManager.initializeInstance(new SQLHelper(context));
                if(DatabaseManager.getInstance().checkForNewMessages()){
                    Util.createMessageNotificationChannel(context);
                    Intent intent1 = new Intent(context,MainActivity.class);
                    Util.showNewMessageNotification(context,"New unread messages",Util.BACKGROUND_NOTIFICATION_ID,intent1);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
