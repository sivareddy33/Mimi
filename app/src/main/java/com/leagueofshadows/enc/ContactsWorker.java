package com.leagueofshadows.enc;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.util.Log;

import com.leagueofshadows.enc.Interfaces.PublicKeyCallback;
import com.leagueofshadows.enc.Items.User;
import com.leagueofshadows.enc.storage.DatabaseManager2;
import com.leagueofshadows.enc.storage.SQLHelper;

import java.util.ArrayList;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class ContactsWorker extends Service {

    public static final String CHANNEL_ID = "service_contacts";
    public static final int id = 1347;
    public static final String FLAG = "FLAG";
    public static final int UPDATE_EXISTING = 1;
    ArrayList<User> users;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        users = new ArrayList<>();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        createNotificationChannel();
        Intent notificationIntent = new Intent(this, SplashActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 1, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID).setContentTitle(getString(R.string.app_name))
                .setContentText("Syncing contacts...")
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentIntent(pendingIntent).build();

        int flag = intent.getIntExtra(FLAG,0);

        startForeground(id, notification);


        if(flag==UPDATE_EXISTING) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    updateContacts();
                }
            });

        }
        else {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    getContactsList();
                }
            });
        }
        return START_NOT_STICKY;
    }

    private void updateContacts()
    {
        DatabaseManager2.initializeInstance(new SQLHelper(getApplicationContext()));
        final DatabaseManager2 databaseManager = DatabaseManager2.getInstance();
        ArrayList<User> users = databaseManager.getUsers();
        if(users.isEmpty())
        {
            stopSelf();
            return;
        }
        this.users.addAll(users);
        for (final User user:users) {
            FirebaseHelper firebaseHelper = new FirebaseHelper(getApplicationContext());
            firebaseHelper.getUserPublic(user.getId(), new PublicKeyCallback() {
                @Override
                public void onSuccess(String Base64PublicKey) {
                    databaseManager.insertPublicKey(Base64PublicKey,user.getId());
                    update(user);
                }
                @Override
                public void onCancelled(String error) {
                    update(user);
                }
            });
        }
    }

    private synchronized void update(User user) {
        users.remove(user);

        if (users.isEmpty()) {
            App app = (App) getApplication();
            if(app.getCompleteCallback()!=null)
                app.getCompleteCallback().onComplete(1);
            stopSelf();
        }
    }


    private void getContactsList()
    {
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI,null,
                null,null,null);

        if(cursor!=null)
        {
            if(cursor.moveToFirst())
            {
                do {
                    if(cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))>0) {

                        final String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                        String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                        Cursor pc = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID+" = ?", new String[]{id},null);
                        if(pc!=null)
                        {
                            if(pc.moveToFirst())
                            {
                                do {
                                    String number = pc.getString(pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                                    number = formatNumber(number);
                                    if(number.contains(".")||number.contains("#")||number.contains("$")||number.contains("[")||number.contains("]")) {
                                        continue;
                                    }
                                    final User user = new User(number,name,number,null);
                                    this.users.add(user);
                                    FirebaseHelper firebaseHelper = new FirebaseHelper(getApplicationContext());
                                    firebaseHelper.getUserPublic(user.getId(), new PublicKeyCallback() {
                                        @Override
                                        public void onSuccess(String Base64PublicKey) {
                                            user.setBase64EncodedPublicKey(Base64PublicKey);
                                            DatabaseManager2.initializeInstance(new SQLHelper(getApplicationContext()));
                                            DatabaseManager2 databaseManager = DatabaseManager2.getInstance();
                                            databaseManager.insertUser(user);
                                            update(user);
                                        }
                                        @Override
                                        public void onCancelled(String error) {
                                            update(user);
                                        }
                                    });
                                }while (pc.moveToNext());
                                pc.close();
                            }
                        }
                    }
                }while (cursor.moveToNext());
                cursor.close();
            }
            else {
                stopSelf();
            }
        }
        else {
            stopSelf();
        }
    }

    private String formatNumber(String number) {

        //TODO :add more formatting

        number = number.trim();
        number = number.replaceAll("\\s+","");
        if(number.length()==10) {
            number = "+91"+number;
        }
        else if(number.length()==11) {
            number = "+91"+number.substring(1);
        }
        return number;
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
