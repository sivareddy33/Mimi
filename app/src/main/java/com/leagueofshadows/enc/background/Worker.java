package com.leagueofshadows.enc.Background;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.leagueofshadows.enc.Exceptions.DeviceOfflineException;
import com.leagueofshadows.enc.FirebaseHelper;
import com.leagueofshadows.enc.Interfaces.CompleteCallback;
import com.leagueofshadows.enc.Interfaces.PublicKeyCallback;
import com.leagueofshadows.enc.Items.EncryptedMessage;
import com.leagueofshadows.enc.Items.Group;
import com.leagueofshadows.enc.Items.Message;
import com.leagueofshadows.enc.Items.User;
import com.leagueofshadows.enc.MainActivity;
import com.leagueofshadows.enc.R;
import com.leagueofshadows.enc.SplashActivity;
import com.leagueofshadows.enc.Util;
import com.leagueofshadows.enc.storage.DatabaseManager;
import com.leagueofshadows.enc.storage.SQLHelper;

import java.util.ArrayList;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class Worker extends Service implements CompleteCallback{


    public static final int id = 1547;
    private DatabaseManager databaseManager;
    FirebaseHelper firebaseHelper;
    ArrayList<EncryptedMessage> encryptedMessages;

    @Override
    public void onCreate() {
        super.onCreate();
        encryptedMessages = new ArrayList<>();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Util.createServiceNotificationChannel(this);
        Intent notificationIntent = new Intent(this, SplashActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,1,notificationIntent,0);
        Notification notification = new NotificationCompat.Builder(this,Util.ServiceNotificationChannelID).setContentTitle(getString(R.string.app_name))
                .setContentText("getting new messages...")
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentIntent(pendingIntent).build();

        startForeground(id,notification);

        DatabaseManager.initializeInstance(new SQLHelper(getApplicationContext()));
        databaseManager = DatabaseManager.getInstance();

        firebaseHelper = new FirebaseHelper(getApplicationContext());
        String userId = getSharedPreferences(Util.preferences,MODE_PRIVATE).getString(Util.userId,null);

        try {
            firebaseHelper.getNewMessages(userId,this);
        }
        catch (DeviceOfflineException e) {
            e.printStackTrace();
        }
        return START_STICKY;
    }

    private void work()
    {
        ArrayList<EncryptedMessage> encryptedMessages = databaseManager.getEncryptedMessages();
        this.encryptedMessages.addAll(encryptedMessages);
        if(encryptedMessages.isEmpty())
        {
            stopSelf();
        }
        for(final EncryptedMessage e:encryptedMessages)
        {
            if(databaseManager.getPublicKey(e.getFrom())==null)
            {
                firebaseHelper.getUserPublic(e.getFrom(), new PublicKeyCallback() {
                    @Override
                    public void onSuccess(String Base64PublicKey,String number) {
                        databaseManager.insertPublicKey(Base64PublicKey,e.getFrom(),number);
                        update(e);
                    }
                    @Override
                    public void onCancelled(String error) {
                        Log.e("Worker",error);
                        update(e);
                    }
                });
            }
            else {
                update(e);
            }
        }

    }

    private synchronized void update(EncryptedMessage e) {

        encryptedMessages.remove(e);
        if(encryptedMessages.isEmpty()) {
            Util.createMessageNotificationChannel(this);
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            String text;
            User user = databaseManager.getUser(e.getFrom());
            if(e.getIsGroupMessage()== Message.MESSAGE_TYPE_SINGLE_USER)
                text = "New message from "+user.getName();
            else{
                Group group = databaseManager.getGroup(e.getTo());
                text = "New message from "+user.getName()+"in group -"+group.getName();
            }
            Util.showNewMessageNotification(this,text,Util.BACKGROUND_NOTIFICATION_ID,intent);
           stopSelf();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onComplete(int numberOfMessages) {
        work();
    }

    @Override
    public void onCanceled() {
        Log.e("something","wrong in getting new messages");
        stopSelf();
    }
}
