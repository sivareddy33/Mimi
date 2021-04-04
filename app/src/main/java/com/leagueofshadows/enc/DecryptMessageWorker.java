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

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class DecryptMessageWorker extends Service {

    public static final String CHANNEL_ID = "Decrypting service";
    public static final int id = 1648;
    ArrayList<EncryptedMessage> encryptedMessages;
    DatabaseManager2 databaseManager;
    FirebaseHelper firebaseHelper;
    Native restHelper;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        encryptedMessages = new ArrayList<>();
        firebaseHelper = new FirebaseHelper(this);
        restHelper = new Native(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        createNotificationChannel();
        Intent notificationIntent = new Intent(this, SplashActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 1, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID).setContentTitle(getString(R.string.app_name))
                .setContentText("Decrypting new Messages...")
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentIntent(pendingIntent).build();

        startForeground(id,notification);

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                startProcess();
            }
        });

        return START_NOT_STICKY;
    }

    private void startProcess() {
        DatabaseManager2.initializeInstance(new SQLHelper(getApplicationContext()));
        databaseManager = DatabaseManager2.getInstance();
        ArrayList<EncryptedMessage> es = databaseManager.getEncryptedMessages();

        if(es.isEmpty())
        {
            stopSelf();
            return;
        }

        encryptedMessages.addAll(es);

        try {
            final AESHelper aesHelper = new AESHelper(getApplicationContext());

            for (final EncryptedMessage e : encryptedMessages)
            {
                final String Base64PulicKey = databaseManager.getPublicKey(e.getFrom());
                if (Base64PulicKey != null) {
                    decryptMessage(e,aesHelper,Base64PulicKey);
                }
                else {
                    FirebaseHelper firebaseHelper = new FirebaseHelper(getApplicationContext());
                    firebaseHelper.getUserPublic(e.getFrom(), new PublicKeyCallback() {
                        @Override
                        public void onSuccess(String Base64PublicKey) {
                            databaseManager.insertPublicKey(Base64PublicKey,e.getFrom());
                            decryptMessage(e,aesHelper,Base64PublicKey);
                        }

                        @Override
                        public void onCancelled(String error) {
                            Log.e("Worker",error);
                            update(e);
                        }
                    });
                }
            }
        }
        catch (NoSuchAlgorithmException | NoSuchPaddingException ex) {
            ex.printStackTrace();
        }

    }

    private synchronized void update(EncryptedMessage e) {
        encryptedMessages.remove(e);

        if(encryptedMessages.isEmpty()) {
                stopSelf();
        }
    }

    void decryptMessage(final EncryptedMessage e, final AESHelper aesHelper, final String Base64PulicKey)  {

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                final App app = (App) getApplication();
                if (e.getType() == EncryptedMessage.MESSAGE_TYPE_ONLYTEXT)
                {
                    try {
                        String  m = aesHelper.DecryptMessage(e.getContent(), app.getPrivateKey(), Base64PulicKey);
                        String timeStamp = Calendar.getInstance().getTime().toString();
                        Message message = new Message(0, e.getId(), e.getTo(), e.getFrom(), m, e.getFilePath(), e.getTimeStamp(), e.getType(),
                                e.getTimeStamp(), timeStamp,null);


                        databaseManager.deleteEncryptedMessage(e.getId());
                        if(!e.isResend()) {
                            if (app.getMessagesRetrievedCallback() != null) {
                                databaseManager.insertNewMessage(message,message.getFrom(),message.getTo());
                                MessagesRetrievedCallback messagesRetrievedCallback = app.getMessagesRetrievedCallback();
                                messagesRetrievedCallback.onNewMessage(message);
                            } else {
                                databaseManager.insertNewMessage(message,message.getFrom(),message.getTo());
                                showNotification(message);
                            }
                        }
                        else
                        {
                            databaseManager.updateMessage(message,message.getFrom());
                            if (app.getResendMessageCallback() != null) {

                                ResendMessageCallback resendMessageCallback = app.getResendMessageCallback();
                                resendMessageCallback.newResendMessageCallback(message);
                            }
                        }
                        update(e);
                    } catch (NoSuchPaddingException | NoSuchAlgorithmException | IllegalBlockSizeException | BadPaddingException |
                            InvalidKeyException | InvalidKeySpecException | InvalidAlgorithmParameterException |
                            DataCorruptedException | RunningOnMainThreadException ex) {
                        restHelper.sendResendMessageNotification(e);
                        update(e);

                        String timeStamp = Calendar.getInstance().getTime().toString();
                        Message message = new Message(0, e.getId(), e.getTo(), e.getFrom(), null, e.getFilePath(), e.getTimeStamp(), e.getType(),
                                e.getTimeStamp(), timeStamp,null);
                        databaseManager.insertNewMessage(message,message.getFrom(),message.getTo());
                        databaseManager.deleteEncryptedMessage(e.getId());
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
                        messageString = aesHelper.DecryptMessage(messageString,app.getPrivateKey(),Base64PulicKey);
                        Message message = new Message(0,e.getId(),e.getTo(),e.getFrom(),messageString,e.getFilePath(),e.getTimeStamp()
                                ,e.getType(),e.getTimeStamp(),timeStamp,null);
                        databaseManager.insertNewMessage(message,message.getFrom(),message.getTo());
                        restHelper.sendMessageReceivedStatus(e);
                        update(e);
                        if(app.getMessagesRetrievedCallback()!=null) {
                            app.getMessagesRetrievedCallback().onNewMessage(message);
                        }
                        else {
                            showNotification(message);
                        }

                    } catch (NoSuchPaddingException | NoSuchAlgorithmException | BadPaddingException | InvalidKeyException |
                            InvalidKeySpecException | InvalidAlgorithmParameterException | DataCorruptedException |
                            RunningOnMainThreadException | IllegalBlockSizeException ex) {


                        Message message = new Message(0, e.getId(), e.getTo(), e.getFrom(), null, e.getFilePath(), e.getTimeStamp(), e.getType(),
                                e.getTimeStamp(), timeStamp, null);
                        databaseManager.insertNewMessage(message, message.getFrom(),message.getTo());
                        if (app.getMessagesRetrievedCallback() != null) {
                            MessagesRetrievedCallback messagesRetrievedCallback = app.getMessagesRetrievedCallback();
                            messagesRetrievedCallback.onNewMessage(message);
                        } else {
                            showNotification(message);
                        }

                        firebaseHelper.getUserPublic(e.getFrom(), new PublicKeyCallback() {
                        @Override
                        public void onSuccess(String Base64PublicKey) {
                            databaseManager.insertPublicKey(Base64PublicKey,e.getFrom());
                            update(e);
                        }
                        @Override
                        public void onCancelled(String error) {}
                        });

                        ex.printStackTrace();
                    }

                }
            }
        });
    }

    private void showNotification(Message message) {
        //TODO : figure out notifications
    }

    private void createNotificationChannel() {

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel serviceChannel = new NotificationChannel(CHANNEL_ID,getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(serviceChannel);
        }
    }
}
