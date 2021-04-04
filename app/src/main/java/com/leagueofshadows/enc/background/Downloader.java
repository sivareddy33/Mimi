package com.leagueofshadows.enc.Background;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.leagueofshadows.enc.App;
import com.leagueofshadows.enc.Crypt.AESHelper;
import com.leagueofshadows.enc.Exceptions.MalFormedFileException;
import com.leagueofshadows.enc.Exceptions.RunningOnMainThreadException;
import com.leagueofshadows.enc.FirebaseHelper;
import com.leagueofshadows.enc.Interfaces.PublicKeyCallback;
import com.leagueofshadows.enc.Items.Message;
import com.leagueofshadows.enc.R;
import com.leagueofshadows.enc.Util;
import com.leagueofshadows.enc.storage.DatabaseManager;
import com.leagueofshadows.enc.storage.SQLHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import static com.leagueofshadows.enc.Crypt.AESHelper.MESSAGE_CONTENT;
import static com.leagueofshadows.enc.FirebaseHelper.Files;

public class Downloader extends Service {

    DatabaseManager databaseManager;

    @Override
    public void onCreate() {
        super.onCreate();
        DatabaseManager.initializeInstance(new SQLHelper(getApplicationContext()));
        databaseManager = DatabaseManager.getInstance();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        final String messageId = intent.getStringExtra(Util.id);
        final String otherUserId = intent.getStringExtra(Util.toUserId);
        final String userId = intent.getStringExtra(Util.userId);
        final int messageType = intent.getIntExtra(Util.messageType,Message.MESSAGE_TYPE_SINGLE_USER);

        final Message message;
        if (messageType==Message.MESSAGE_TYPE_SINGLE_USER)
            message = databaseManager.getMessage(messageId,otherUserId);
        else
            message = databaseManager.getMessage(messageId,userId);

        final String id = getSharedPreferences(Util.preferences,MODE_PRIVATE).getString(Util.userId,null);
        final int notificationId = new Random().nextInt();


        Util.createServiceNotificationChannel(this);
        final NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Util.ServiceNotificationChannelID);
        builder.setContentTitle(getResources().getString(R.string.app_name))
                .setContentText("Downloading file - "+message.getContent())
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        final int progressMax = 100;

        builder.setProgress(progressMax,0,false);
        Notification notification = builder.build();
        startForeground(notificationId,notification);

        assert otherUserId != null;
        assert userId != null;

        final String finalPath;

        try {
            JSONObject jsonObject = new JSONObject(message.getContent());
            final String privatePath = Util.privatePath+jsonObject.getString(MESSAGE_CONTENT);

            if(message.getType()==Message.MESSAGE_TYPE_IMAGE)
                finalPath = Util.imagesPath+jsonObject.getString(MESSAGE_CONTENT);
            else
                finalPath = Util.documentsPath+jsonObject.getString(MESSAGE_CONTENT);
            final File file = new File(privatePath);

            final StorageReference storageReference = FirebaseStorage.getInstance().getReference().child(Files).child(userId).child(message.getTimeStamp());
            storageReference.getFile(file).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    stopForeground(true);
                }
            }).addOnCompleteListener(new OnCompleteListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<FileDownloadTask.TaskSnapshot> task) {
                    stopForeground(true);

                    FirebaseHelper firebaseHelper = new FirebaseHelper(getApplicationContext());
                    firebaseHelper.getUserPublic(message.getFrom(), new PublicKeyCallback() {
                        @Override
                        public void onSuccess(final String Base64PublicKey, final String number) {
                            AsyncTask.execute(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        FileInputStream fileInputStream = new FileInputStream(privatePath);
                                        FileOutputStream fileOutputStream = new FileOutputStream(finalPath);
                                        AESHelper aesHelper = new AESHelper(Downloader.this);

                                        App app = (App) getApplication();
                                        databaseManager.insertPublicKey(Base64PublicKey,message.getFrom(),number);

                                        String cipherText = databaseManager.getCipherText(messageId);
                                        aesHelper.decryptFile(fileInputStream,fileOutputStream,app.getPrivateKey(),databaseManager.getUser(message.getFrom()),new File(finalPath),cipherText,id);
                                        app.getMessagesRetrievedCallback().onUpdateMessageStatus(messageId,otherUserId);


                                        if (message.getIsGroupMessage()==Message.MESSAGE_TYPE_SINGLE_USER)
                                            storageReference.delete();

                                        message.setFilePath(null);
                                        databaseManager.updateMessage(message,message.getFrom());
                                        file.delete();
                                    } catch (NoSuchPaddingException | NoSuchAlgorithmException | IOException | MalFormedFileException
                                            | IllegalBlockSizeException | BadPaddingException | InvalidKeyException | InvalidKeySpecException
                                            | InvalidAlgorithmParameterException | RunningOnMainThreadException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                        @Override
                        public void onCancelled(String error) {}
                    });
                }
            }).addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onProgress(@NonNull FileDownloadTask.TaskSnapshot taskSnapshot) {
                    int currentProgress = (int) ((100*taskSnapshot.getBytesTransferred())/taskSnapshot.getTotalByteCount());
                    builder.setProgress(progressMax,currentProgress,false);
                    notificationManagerCompat.notify(notificationId,builder.build());
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
            stopSelf();
        }

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
