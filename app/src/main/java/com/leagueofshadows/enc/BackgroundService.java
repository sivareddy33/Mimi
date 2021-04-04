package com.leagueofshadows.enc;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.leagueofshadows.enc.Crypt.AESHelper;
import com.leagueofshadows.enc.Exceptions.DataCorruptedException;
import com.leagueofshadows.enc.Exceptions.RunningOnMainThreadException;
import com.leagueofshadows.enc.Interfaces.MessagesRetrievedCallback;
import com.leagueofshadows.enc.Interfaces.PublicKeyCallback;
import com.leagueofshadows.enc.Interfaces.ResendMessageCallback;
import com.leagueofshadows.enc.Items.EncryptedMessage;
import com.leagueofshadows.enc.Items.Message;
import com.leagueofshadows.enc.REST.Native;
import com.leagueofshadows.enc.storage.DatabaseManager2;
import com.leagueofshadows.enc.storage.SQLHelper;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Calendar;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import static com.leagueofshadows.enc.FirebaseHelper.resend;

public class BackgroundService extends Service {

    ArrayList<String> ids;
    DatabaseManager2 databaseManager;
    DatabaseReference databaseReference;
    String userId;
    FirebaseHelper firebaseHelper;
    Native restHelper;
    AESHelper aesHelper;
    boolean notificationChannelCreated;

    static final String id = "id";
    static final String to = "to";
    static final String from = "from";
    static final String content = "content";
    static final String type = "type";
    static final String filePath = "filePath";
    static final String timeStamp = "timeStamp";

    @Override
    public void onCreate() {
        super.onCreate();
        ids = new ArrayList<>();
        notificationChannelCreated = false;
        DatabaseManager2.initializeInstance(new SQLHelper(this));
        databaseManager = DatabaseManager2.getInstance();
        firebaseHelper = new FirebaseHelper(this);
        restHelper = new Native(this);

        try {
            aesHelper = new AESHelper(this);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }

        userId = getSharedPreferences(Util.preferences,MODE_PRIVATE).getString(Util.userId,null);
        ids.add(userId);
        ids.addAll(databaseManager.getGroups());

        databaseReference = FirebaseDatabase.getInstance().getReference();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if(userId==null)
            stopSelf();

        App app = (App) getApplication();
        if(app.isnull())
            stopSelf();

        for (final String node:ids) {

            databaseReference = databaseReference.child(FirebaseHelper.Messages).child(node);
            databaseReference.addChildEventListener(new ChildEventListener() {

                @Override
                public void onChildAdded(@NonNull DataSnapshot d, @Nullable String s) {

                    Log.e("message",d.toString());
                    d.getRef().removeValue();
                    try {
                        final EncryptedMessage encryptedMessage = new EncryptedMessage();
                        encryptedMessage.setId((String) d.child(id).getValue());
                        encryptedMessage.setTo((String) d.child(to).getValue());
                        encryptedMessage.setFrom((String) d.child(from).getValue());
                        encryptedMessage.setContent((String) d.child(content).getValue());
                        encryptedMessage.setFilePath((String) d.child(filePath).getValue());
                        encryptedMessage.setType(Integer.parseInt(Long.toString((Long) d.child(type).getValue())));
                        encryptedMessage.setTimeStamp((String) d.child(timeStamp).getValue());

                        if (d.hasChild(resend))
                            encryptedMessage.setResend((boolean) d.child(resend).getValue());

                        String userId = encryptedMessage.getFrom();
                        final String publicKey = databaseManager.getPublicKey(userId);
                        if (publicKey == null) {
                            firebaseHelper.getUserPublic(userId, new PublicKeyCallback() {
                                @Override
                                public void onSuccess(String Base64PublicKey) {
                                    databaseManager.insertPublicKey(Base64PublicKey, encryptedMessage.getFrom());
                                    decryptMessage(encryptedMessage, Base64PublicKey);
                                }

                                @Override
                                public void onCancelled(String error) {
                                    Log.e("error", error);
                                }
                            });
                        } else {
                            decryptMessage(encryptedMessage, publicKey);
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}

                @Override
                public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {}

                @Override
                public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) { Log.e(id+" - database error",databaseError.toString()); }
            });
        }

        return START_STICKY;
    }

    void decryptMessage(final EncryptedMessage e, final String Base64PublicKey)  {

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {

                if (databaseManager.check(e.getId(),e.getFrom())||e.isResend()) {
                    final App app = (App) getApplication();
                    if (e.getType() == EncryptedMessage.MESSAGE_TYPE_ONLYTEXT)
                    {
                        try {
                            String m = aesHelper.DecryptMessage(e.getContent(), app.getPrivateKey(), Base64PublicKey);
                            String timeStamp = Calendar.getInstance().getTime().toString();
                            Message message = new Message(0, e.getId(), e.getTo(), e.getFrom(), m, e.getFilePath(), e.getTimeStamp(), e.getType(),
                                    e.getTimeStamp(), timeStamp, null);

                            restHelper.sendMessageReceivedStatus(e);

                            if (!e.isResend()) {
                                databaseManager.insertNewMessage(message, message.getFrom(),message.getTo());
                                if (app.getMessagesRetrievedCallback() != null) {
                                    MessagesRetrievedCallback messagesRetrievedCallback = app.getMessagesRetrievedCallback();
                                    messagesRetrievedCallback.onNewMessage(message);
                                } else {
                                    showNotification(message);
                                }
                            } else {
                                databaseManager.updateMessage(message, message.getFrom());
                                if (app.getResendMessageCallback() != null) {
                                    ResendMessageCallback resendMessageCallback = app.getResendMessageCallback();
                                    resendMessageCallback.newResendMessageCallback(message);
                                }
                            }
                        } catch (NoSuchPaddingException | NoSuchAlgorithmException | IllegalBlockSizeException | BadPaddingException |
                                InvalidKeyException | InvalidKeySpecException | InvalidAlgorithmParameterException |
                                DataCorruptedException | RunningOnMainThreadException ex) {
                            restHelper.sendResendMessageNotification(e);
                            //new change
                            firebaseHelper.getUserPublic(e.getFrom(), new PublicKeyCallback() {
                                @Override
                                public void onSuccess(String Base64PublicKey) {
                                    databaseManager.insertPublicKey(Base64PublicKey,e.getFrom());
                                }
                                @Override
                                public void onCancelled(String error) {}
                            });

                            String timeStamp = Calendar.getInstance().getTime().toString();
                            Message message = new Message(0, e.getId(), e.getTo(), e.getFrom(), null,
                                    e.getFilePath(), e.getTimeStamp(), e.getType(), e.getTimeStamp(), timeStamp, null);

                            databaseManager.insertNewMessage(message, message.getFrom(),message.getTo());
                            if (app.getMessagesRetrievedCallback() != null) {
                                MessagesRetrievedCallback messagesRetrievedCallback = app.getMessagesRetrievedCallback();
                                messagesRetrievedCallback.onNewMessage(message);
                            } else {
                                showNotification(message);
                            }
                            ex.printStackTrace();

                        }
                    }
                    else {
                        String timeStamp = Calendar.getInstance().getTime().toString();
                        String messageString = e.getContent();
                        try {
                            messageString = aesHelper.DecryptMessage(messageString,app.getPrivateKey(),Base64PublicKey);
                            Message message = new Message(0,e.getId(),e.getTo(),e.getFrom(),messageString,e.getFilePath(),e.getTimeStamp()
                                    ,e.getType(),e.getTimeStamp(),timeStamp,null);
                            databaseManager.insertNewMessage(message,message.getFrom(),message.getTo());
                            restHelper.sendMessageReceivedStatus(e);
                            if(app.getMessagesRetrievedCallback()!=null) {
                                app.getMessagesRetrievedCallback().onNewMessage(message);
                            }
                            else {
                                showNotification(message);
                            }

                        } catch (NoSuchPaddingException | NoSuchAlgorithmException | BadPaddingException | InvalidKeyException |
                                InvalidKeySpecException | InvalidAlgorithmParameterException | DataCorruptedException |
                                RunningOnMainThreadException | IllegalBlockSizeException ex) {
                            firebaseHelper.getUserPublic(e.getFrom(), new PublicKeyCallback() {
                                @Override
                                public void onSuccess(String Base64PublicKey) {
                                    databaseManager.insertPublicKey(Base64PublicKey,e.getFrom());
                                }
                                @Override
                                public void onCancelled(String error) {}
                            });

                            Message message = new Message(0, e.getId(), e.getTo(), e.getFrom(), null, e.getFilePath(), e.getTimeStamp(), e.getType(),
                                    e.getTimeStamp(), timeStamp, null);
                            databaseManager.insertNewMessage(message, message.getFrom(),message.getTo());
                            if (app.getMessagesRetrievedCallback() != null) {
                                MessagesRetrievedCallback messagesRetrievedCallback = app.getMessagesRetrievedCallback();
                                messagesRetrievedCallback.onNewMessage(message);
                            } else {
                                showNotification(message);
                            }
                            ex.printStackTrace();
                        }

                    }
                }
            }
        });
    }

    private void showNotification(Message message) {
        if(notificationChannelCreated)
            createNotificationChannel(Util.NewMessageNotificationChannelID,Util.NewMessageNotificationChannelTitle);

        Intent notificationIntent = new Intent(getApplicationContext(),SplashActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(),1,notificationIntent,0);
        Notification notification = new NotificationCompat.Builder(getApplicationContext(),Util.NewMessageNotificationChannelID).setContentTitle(getString(R.string.app_name))
                .setContentText("New message from "+message.getFrom())
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentIntent(pendingIntent).setAutoCancel(false)
                .build();
        NotificationManagerCompat.from(getApplicationContext()).notify((int) System.currentTimeMillis(),notification);
    }

    private void createNotificationChannel(String channelId,String channelTitle) {

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            notificationChannelCreated = true;
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
}
