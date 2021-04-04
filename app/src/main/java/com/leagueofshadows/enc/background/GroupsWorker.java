package com.leagueofshadows.enc.Background;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.leagueofshadows.enc.App;
import com.leagueofshadows.enc.FirebaseHelper;
import com.leagueofshadows.enc.Interfaces.GroupsUpdatedCallback;
import com.leagueofshadows.enc.Interfaces.PublicKeyCallback;
import com.leagueofshadows.enc.Items.Group;
import com.leagueofshadows.enc.Items.User;
import com.leagueofshadows.enc.R;
import com.leagueofshadows.enc.SplashActivity;
import com.leagueofshadows.enc.Util;
import com.leagueofshadows.enc.storage.DatabaseManager;
import com.leagueofshadows.enc.storage.SQLHelper;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import static com.leagueofshadows.enc.FirebaseHelper.Groups;
import static com.leagueofshadows.enc.FirebaseHelper.Users;

public class GroupsWorker extends Service {

    DatabaseManager databaseManager;
    DatabaseReference databaseReference;
    int id = 1478;
    FirebaseHelper firebaseHelper;
    App app;
    private String currentUserId;

    @Override
    public void onCreate() {
        super.onCreate();
        app = (App) getApplication();
        DatabaseManager.initializeInstance(new SQLHelper(this));
        databaseManager = DatabaseManager.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();
        firebaseHelper = new FirebaseHelper(this);
        currentUserId =getSharedPreferences(Util.preferences,MODE_PRIVATE).getString(Util.userId,null);
        assert currentUserId != null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if(app.isnull())
        {
            Util.createServiceNotificationChannel(this);
            Intent notificationIntent = new Intent(this, SplashActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this,1,notificationIntent,0);
            Notification notification = new NotificationCompat.Builder(this,Util.ServiceNotificationChannelID).setContentTitle(getString(R.string.app_name))
                    .setContentText("updating groups...")
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setContentIntent(pendingIntent).build();

            startForeground(id,notification);
        }

        final ArrayList<String> groupIds = new ArrayList<>();

        databaseReference.child(Groups).child(Users).child(currentUserId).child(Groups).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot d:dataSnapshot.getChildren()){
                    groupIds.add((String) d.getValue());
                }
                try {
                    updateGroups(groupIds);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
        return START_NOT_STICKY;
    }

    private void updateGroups(ArrayList<String> groupIds){
        for (String id:groupIds){
            DatabaseReference dr = FirebaseDatabase.getInstance().getReference().child(Groups).child(id);
            dr.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot d) {
                    String groupName = (String) d.child(Util.name).getValue();
                    String groupId = (String) d.child(Util.id).getValue();
                    String admins = (String) d.child(Util.admins).getValue();
                    int active = Group.GROUP_NOT_ACTIVE;

                    ArrayList<User> users = new ArrayList<>();
                    for(DataSnapshot d1:d.child("users").getChildren()){
                        String name = (String) d1.child(Util.name).getValue();
                        String id = (String) d1.child(Util.id).getValue();
                        String number = (String) d1.child(Util.number).getValue();
                        String Base64PublicKey = (String) d1.child(Util.base64EncodedPublicKey).getValue();
                        if (!id.equals(currentUserId))
                            users.add(new User(id,name,number,Base64PublicKey));
                        else
                            active = Group.GROUP_ACTIVE;
                    }

                    if(active==Group.GROUP_NOT_ACTIVE){
                        FirebaseDatabase.getInstance().getReference().child(Groups).child(Users).child(currentUserId).child(Groups).removeValue();
                        databaseManager.markGroupAsDeleted(groupId);
                        return;
                    }

                    for (final User u:users) {

                        if(databaseManager.getUser(u.getId())==null)
                            databaseManager.insertUser(u);
                        firebaseHelper.getUserPublic(u.getId(), new PublicKeyCallback() {
                            @Override
                            public void onSuccess(String Base64PublicKey,String number) {
                                databaseManager.insertPublicKey(Base64PublicKey,u.getId(),number);
                            }
                            @Override
                            public void onCancelled(String error) { }
                        });
                    }
                    Group group = new Group(groupId,groupName,users,admins,active);
                    try {
                        if(groupId!=null&&groupName!=null)
                        databaseManager.addNewGroup(group);
                        if (!app.isnull()) {
                            ArrayList<GroupsUpdatedCallback> groupsUpdatedCallbacks = app.getGroupsUpdatedCallback();
                            for (GroupsUpdatedCallback g : groupsUpdatedCallbacks) {
                                g.onComplete();
                            }
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) { }
            });
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}
