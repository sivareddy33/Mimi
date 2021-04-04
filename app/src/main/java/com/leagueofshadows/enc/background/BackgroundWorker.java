package com.leagueofshadows.enc.background;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
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
import com.leagueofshadows.enc.REST.Native;
import com.leagueofshadows.enc.Util;
import com.leagueofshadows.enc.storage.DatabaseManager2;
import com.leagueofshadows.enc.storage.SQLHelper;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Calendar;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.leagueofshadows.enc.FirebaseHelper.Messages;
import static com.leagueofshadows.enc.FirebaseHelper.resend;

public class BackgroundWorker extends Service implements com.google.firebase.database.ChildEventListener {

    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;
    String userId;
    static final String id = "id";
    static final String to = "to";
    static final String from = "from";
    static final String content = "content";
    static final String type = "type";
    static final String filePath = "filePath";
    static final String timeStamp = "timeStamp";
    DatabaseManager2 databaseManager;
    private Native restHelper;
    private AESHelper aesHelper;
    private FirebaseHelper firebaseHelper;

    @Override
    public void onCreate()
    {

        super.onCreate();
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference();
        userId = getSharedPreferences(Util.preferences,MODE_PRIVATE).getString(Util.userId,null);
        DatabaseManager2.initializeInstance(new SQLHelper(this));
        databaseManager = DatabaseManager2.getInstance();
        firebaseHelper = new FirebaseHelper(this);
        restHelper = new Native(this);
        try {
            aesHelper = new AESHelper(this);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if(userId==null)
            stopSelf();

        App app = (App) getApplication();
        if(app.isnull())
            stopSelf();

        databaseReference.child(Messages).child(userId).addChildEventListener(this);

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

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void showNotification(Message message) {
        //TODO : figure out notifications
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onChildAdded(@NonNull DataSnapshot d, @Nullable String s) {

        d.getRef().removeValue();
        final EncryptedMessage encryptedMessage = new EncryptedMessage();
        encryptedMessage.setId((String) d.child(id).getValue());
        encryptedMessage.setTo((String) d.child(to).getValue());
        encryptedMessage.setFrom((String) d.child(from).getValue());
        encryptedMessage.setContent((String) d.child(content).getValue());
        encryptedMessage.setFilePath((String) d.child(filePath).getValue());
        encryptedMessage.setType(Integer.parseInt(Long.toString((Long) d.child(type).getValue())));
        encryptedMessage.setTimeStamp((String) d.child(timeStamp).getValue());

        if(d.hasChild(resend))
            encryptedMessage.setResend((boolean)d.child(resend).getValue());

        String userId = encryptedMessage.getFrom();
        final String publicKey = databaseManager.getPublicKey(userId);
        if(publicKey==null) {
            firebaseHelper.getUserPublic(userId, new PublicKeyCallback() {
                @Override
                public void onSuccess(String Base64PublicKey) {
                    databaseManager.insertPublicKey(Base64PublicKey, encryptedMessage.getFrom());
                    decryptMessage(encryptedMessage,Base64PublicKey);
                }
                @Override
                public void onCancelled(String error) {
                    Log.e("error",error);
                }
            });
        }
        else {
            decryptMessage(encryptedMessage,publicKey);
        }
    }

    @Override
    public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}

    @Override
    public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {}

    @Override
    public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}

    @Override
    public void onCancelled(@NonNull DatabaseError databaseError) {}
}
