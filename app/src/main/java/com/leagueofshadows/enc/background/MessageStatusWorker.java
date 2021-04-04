package com.leagueofshadows.enc.Background;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.leagueofshadows.enc.Items.MessageStatus;
import com.leagueofshadows.enc.storage.DatabaseManager;
import com.leagueofshadows.enc.storage.SQLHelper;

import java.util.ArrayList;

import static com.leagueofshadows.enc.FirebaseHelper.STATUS;

public class MessageStatusWorker extends Service {
    DatabaseManager db;
    DatabaseReference dr;
    public MessageStatusWorker() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        DatabaseManager.initializeInstance(new SQLHelper(this));
        db = DatabaseManager.getInstance();
        dr = FirebaseDatabase.getInstance().getReference().child(STATUS);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                ArrayList<MessageStatus> messageStatuses = db.getMessageStatuses();
                for (final MessageStatus m:messageStatuses) {
                    DatabaseReference databaseReference = dr.child(m.getFromUserId());
                    databaseReference.push().setValue(m).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            db.deleteLocalStatusRecord(m.getId());
                        }
                    });
                }
            }
        });
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
