package com.leagueofshadows.enc;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.leagueofshadows.enc.Crypt.AESHelper;
import com.leagueofshadows.enc.Exceptions.RunningOnMainThreadException;
import com.leagueofshadows.enc.Interfaces.PublicKeyCallback;
import com.leagueofshadows.enc.Items.EncryptedMessage;
import com.leagueofshadows.enc.Items.Message;
import com.leagueofshadows.enc.REST.Native;
import com.leagueofshadows.enc.storage.DatabaseManager2;
import com.leagueofshadows.enc.storage.SQLHelper;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.leagueofshadows.enc.FirebaseHelper.Messages;

public class ResendMessageWorker extends Service  {

    ArrayList<Message> messages;
    DatabaseManager2 databaseManager;
    AESHelper aesHelper;
    FirebaseHelper firebaseHelper;
    DatabaseReference databaseReference;
    Native restHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        messages = new ArrayList<>();
        DatabaseManager2.initializeInstance(new SQLHelper(this));
        databaseManager = DatabaseManager2.getInstance();
        firebaseHelper = new FirebaseHelper(this);
        restHelper = new Native(this);
        databaseReference = FirebaseDatabase.getInstance().getReference();
        try {
            aesHelper = new AESHelper(this);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }
    }

    void update(Message message)
    {
        messages.remove(message);
        if(messages.isEmpty())
            stopSelf();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

            startProcess();

        return START_STICKY;
    }

    private void startProcess()  {

        ArrayList<Message> ms = databaseManager.getResendMessages();
        messages.addAll(ms);
        App app  = (App) getApplication();
        final PrivateKey privateKey = app.getPrivateKey();
        for (final Message m:messages)
        {
            if(m.getType()==Message.MESSAGE_TYPE_ONLYTEXT)
            {
                    firebaseHelper.getUserPublic(m.getTo(), new PublicKeyCallback() {
                        @Override
                        public void onSuccess(final String Base64PublicKey) {
                            databaseManager.insertPublicKey(Base64PublicKey,m.getTo());
                                AsyncTask.execute(new Runnable() {
                                    @Override
                                    public void run() {

                                        String cipherText = null;
                                        try {
                                            cipherText = aesHelper.encryptMessage(m.getContent(),Base64PublicKey,privateKey);
                                            EncryptedMessage encryptedMessage = new EncryptedMessage(m.getMessage_id(),m.getTo(),m.getFrom(),cipherText,
                                                    null,m.getTimeStamp(),Message.MESSAGE_TYPE_ONLYTEXT,true);
                                            databaseReference.child(Messages).child(m.getTo()).child(m.getMessage_id()).setValue(encryptedMessage).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    databaseManager.deleteResendMessage(m.getMessage_id());
                                                    restHelper.sendNewMessageNotification(m.getTo());
                                                    update(m);
                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    update(m);
                                                }
                                            });

                                        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException |
                                                BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException |
                                                InvalidKeySpecException | RunningOnMainThreadException e) {
                                            update(m);
                                            e.printStackTrace();
                                        }
                                    }
                                });
                        }
                        @Override
                        public void onCancelled(String error) { update(m); }
                    });
            }
            else {
                //TODO : other type of messages
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
