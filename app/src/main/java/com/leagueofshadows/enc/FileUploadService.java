package com.leagueofshadows.enc;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.UploadTask;
import com.leagueofshadows.enc.Exceptions.DeviceOfflineException;
import com.leagueofshadows.enc.Interfaces.MessageSentCallback;
import com.leagueofshadows.enc.Items.EncryptedMessage;
import com.leagueofshadows.enc.Items.Message;
import com.leagueofshadows.enc.REST.Native;
import com.leagueofshadows.enc.storage.DatabaseManager2;
import com.leagueofshadows.enc.storage.SQLHelper;

import java.io.File;
import java.util.Calendar;
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import static com.leagueofshadows.enc.FirebaseHelper.Files;

public class FileUploadService extends Service implements MessageSentCallback {

    Message message;
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        final String otherUserId = intent.getStringExtra(Util.toUserId);
        final String currentUserId = intent.getStringExtra(Util.userId);
        final String timeStamp = intent.getStringExtra(Util.timeStamp);
        String userName = intent.getStringExtra(Util.name);
        final String fileName = intent.getStringExtra(Util.fileName);
        final String messageContent = intent.getStringExtra(Util.content);
        final String cipherText = intent.getStringExtra(Util.cipherText);
        final String id = intent.getStringExtra(Util.id);
        final Uri uri = Uri.parse(intent.getStringExtra(Util.uri));
        final int type = intent.getIntExtra(Util.type,Message.MESSAGE_TYPE_FILE);

        final String path;

        path = Util.privatePath+fileName;

        final int notificationId = new Random().nextInt();


        createNotificationChannel(Util.ServiceNotificationChannelID,Util.ServiceNotificationChannelTitle);
        final NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Util.ServiceNotificationChannelID);
        builder.setContentTitle(getResources().getString(R.string.app_name))
                .setContentText("sending file to "+userName)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        final int progressMax = 100;

        builder.setProgress(progressMax,0,false);
        Notification notification = builder.build();
        startForeground(notificationId,notification);

        assert otherUserId != null;
        assert timeStamp != null;
        assert currentUserId != null;
        assert id != null;

        final File file = new File(path);
        FirebaseStorage.getInstance().getReference().child(Files).child(otherUserId).child(timeStamp).putFile(Uri.fromFile(file)).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Log.e("success","success");

                file.delete();
                builder.setProgress(0,0,false);
                notificationManagerCompat.cancelAll();
                message = new Message(0,id,otherUserId,currentUserId,messageContent,uri.toString(),timeStamp,
                        type,null,null,null);

                EncryptedMessage encryptedMessage = new EncryptedMessage(id,message.getTo(),message.getFrom(),cipherText,timeStamp,timeStamp,type);
                FirebaseHelper firebaseHelper = new FirebaseHelper(getApplicationContext());
                try {
                    firebaseHelper.sendTextOnlyMessage(message,encryptedMessage,FileUploadService.this,id);
                } catch (DeviceOfflineException e) {
                    e.printStackTrace();
                }
                finally {
                    stopForeground(true);
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e("failure","failure");
                App app = (App) getApplication();
                app.getMessageSentCallback().onComplete(message,false,e.toString());
                stopForeground(true);

            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {
                int currentProgress = (int) ((100*taskSnapshot.getBytesTransferred())/taskSnapshot.getTotalByteCount());
                builder.setProgress(progressMax,currentProgress,false);
                notificationManagerCompat.notify(notificationId,builder.build());
            }
        });
        return START_STICKY;
    }

    private void createNotificationChannel(String channelId,String channelTitle) {

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel serviceChannel = new NotificationChannel(channelId,channelTitle, NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(serviceChannel);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onComplete(Message message, boolean success, String error) {

        App app = (App) getApplication();
        if(success) {
            String timeStamp = Calendar.getInstance().getTime().toString();
            message.setSent(timeStamp);
            DatabaseManager2.initializeInstance(new SQLHelper(this));
            DatabaseManager2.getInstance().insertNewMessage(message, message.getTo(),message.getFrom());
            Native n = new Native(this);
            n.sendNewMessageNotification(message.getTo());
            app.getMessageSentCallback().onComplete(message, true, null);
        }
        else {
            app.getMessageSentCallback().onComplete(message,false,error);
        }
    }
}
