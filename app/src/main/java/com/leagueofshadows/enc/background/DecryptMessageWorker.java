package com.leagueofshadows.enc.Background;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

import com.leagueofshadows.enc.App;
import com.leagueofshadows.enc.Crypt.AESHelper;
import com.leagueofshadows.enc.Exceptions.DataCorruptedException;
import com.leagueofshadows.enc.Exceptions.RunningOnMainThreadException;
import com.leagueofshadows.enc.FirebaseHelper;
import com.leagueofshadows.enc.Interfaces.MessagesRetrievedCallback;
import com.leagueofshadows.enc.Interfaces.PublicKeyCallback;
import com.leagueofshadows.enc.Interfaces.ResendMessageCallback;
import com.leagueofshadows.enc.Items.EncryptedMessage;
import com.leagueofshadows.enc.Items.Message;
import com.leagueofshadows.enc.Items.User;
import com.leagueofshadows.enc.REST.Native;
import com.leagueofshadows.enc.Util;
import com.leagueofshadows.enc.storage.DatabaseManager;
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

public class DecryptMessageWorker extends Service {

    public static final int id = 1648;
    ArrayList<EncryptedMessage> encryptedMessages;
    DatabaseManager databaseManager;
    FirebaseHelper firebaseHelper;
    Native restHelper;
    String userId;

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
        userId = getSharedPreferences(Util.preferences,MODE_PRIVATE).getString(Util.userId,null);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                startProcess();
            }
        });
        return START_NOT_STICKY;
    }

    private void startProcess() {
        DatabaseManager.initializeInstance(new SQLHelper(getApplicationContext()));
        databaseManager = DatabaseManager.getInstance();
        ArrayList<EncryptedMessage> es = databaseManager.getEncryptedMessages();

        if(es.isEmpty()) {
            stopSelf();
            return;
        }

        encryptedMessages.addAll(es);

        try {
            final AESHelper aesHelper = new AESHelper(getApplicationContext());

            for (final EncryptedMessage e : encryptedMessages)
            {
                final User user = databaseManager.getUser(e.getFrom());
                if (user.getBase64EncodedPublicKey() != null) {
                    decryptMessage(e,aesHelper,user);
                }
                else {
                    FirebaseHelper firebaseHelper = new FirebaseHelper(getApplicationContext());
                    firebaseHelper.getUserPublic(e.getFrom(), new PublicKeyCallback() {
                        @Override
                        public void onSuccess(String Base64PublicKey,String number) {
                            databaseManager.insertPublicKey(Base64PublicKey,e.getFrom(),number);
                            user.setBase64EncodedPublicKey(Base64PublicKey);
                            decryptMessage(e,aesHelper,user);
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

    void decryptMessage(final EncryptedMessage e, final AESHelper aesHelper, final User otherUser)  {

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {

                final App app = (App) getApplication();
                if (e.getType() == Message.MESSAGE_TYPE_ONLYTEXT)
                {
                    try {
                        String  m = aesHelper.DecryptMessage(e.getContent(), app.getPrivateKey(),otherUser,userId);
                        String timeStamp = Calendar.getInstance().getTime().toString();
                        Message message = new Message(0, e.getId(), e.getTo(), e.getFrom(), m, e.getFilePath(), e.getTimeStamp(), e.getType(),
                                e.getTimeStamp(), timeStamp,null,e.getIsGroupMessage());

                        databaseManager.deleteEncryptedMessage(e.getId());
                        if(!e.isResend()) {
                            if (app.getMessagesRetrievedCallback() != null) {
                                databaseManager.insertNewMessage(message,message.getFrom(),userId);
                                MessagesRetrievedCallback messagesRetrievedCallback = app.getMessagesRetrievedCallback();
                                messagesRetrievedCallback.onNewMessage(message);
                            } else {
                                databaseManager.insertNewMessage(message,message.getFrom(),userId);
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
                    } catch (Exception ex) {
                        restHelper.sendResendMessageNotification(e);
                        update(e);

                        String timeStamp = Calendar.getInstance().getTime().toString();
                        Message message = new Message(0, e.getId(), e.getTo(), e.getFrom(), null, e.getFilePath(), e.getTimeStamp(), e.getType(),
                                e.getTimeStamp(), timeStamp,null,e.getIsGroupMessage());

                        databaseManager.insertNewMessage(message,message.getFrom(),userId);
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
                    databaseManager.insertCipherText(messageString,e.getId());
                    try {
                        messageString = aesHelper.DecryptMessage(messageString,app.getPrivateKey(),otherUser,userId);
                        Message message = new Message(0,e.getId(),e.getTo(),e.getFrom(),messageString,e.getFilePath(),e.getTimeStamp()
                                ,e.getType(),e.getTimeStamp(),timeStamp,null,e.getIsGroupMessage());

                        databaseManager.insertNewMessage(message,message.getFrom(),userId);
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
                                e.getTimeStamp(), timeStamp, null,e.getIsGroupMessage());

                        databaseManager.insertNewMessage(message, message.getFrom(),userId);
                        databaseManager.deleteEncryptedMessage(e.getId());
                        if (app.getMessagesRetrievedCallback() != null) {
                            MessagesRetrievedCallback messagesRetrievedCallback = app.getMessagesRetrievedCallback();
                            messagesRetrievedCallback.onNewMessage(message);
                        } else {
                            showNotification(message);
                        }

                        firebaseHelper.getUserPublic(e.getFrom(), new PublicKeyCallback() {
                        @Override
                        public void onSuccess(String Base64PublicKey,String number) {
                            databaseManager.insertPublicKey(Base64PublicKey,e.getFrom(),number);
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
}
