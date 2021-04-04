package com.leagueofshadows.enc;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.leagueofshadows.enc.Interfaces.GroupsUpdatedCallback;
import com.leagueofshadows.enc.Items.Group;
import com.leagueofshadows.enc.Items.User;
import com.leagueofshadows.enc.REST.Native;
import com.leagueofshadows.enc.storage.DatabaseManager;
import com.leagueofshadows.enc.storage.SQLHelper;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.leagueofshadows.enc.CreateGroupActivity.userColors;
import static com.leagueofshadows.enc.FirebaseHelper.Groups;
import static com.leagueofshadows.enc.FirebaseHelper.Users;

public class GroupInfo extends AppCompatActivity implements GroupsUpdatedCallback {

    Group group;
    String groupId;
    DatabaseManager databaseManager;
    RecyclerView listView;
    TextView name;
    ImageButton deleteButton;
    String currentUserId;
    User currentUser;
    String admins;
    public static int ADD_USER_REQUEST = 1;
    public static String ADMIN_PRIVILEGE_MESSAGE = "Name can be changed only by group Admin";
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_info);
        groupId = getIntent().getStringExtra(Util.id);
        load();
    }

    @Override
    protected void onResume() {
        super.onResume();
        App app = (App) getApplication();
        app.addGroupsUpdatedCallback(this);
    }

    @Override
    protected void onDestroy() {

        try {
            App app = (App) getApplication();
            app.removeGroupsUpdatedCallback(this);
        }catch (Exception e){
            e.printStackTrace();
        }
        super.onDestroy();
    }

    private void load(){
        progressDialog = new ProgressDialog(this);

        name = findViewById(R.id.name);
        deleteButton = findViewById(R.id.delete);
        setTitle("Group info");
        listView = findViewById(R.id.participants);

        DatabaseManager.initializeInstance(new SQLHelper(this));
        databaseManager = DatabaseManager.getInstance();

        SharedPreferences sp = getSharedPreferences(Util.preferences,MODE_PRIVATE);
        currentUserId = sp.getString(Util.userId,null);
        String currentUserNumber = sp.getString(Util.number,null);

        group = databaseManager.getGroup(groupId);
        admins = group.getAdmins();
        assert currentUserNumber != null;
        currentUser = new User(currentUserId,"you",currentUserNumber,null);
        if (!group.getUsers().contains(currentUser))
            group.getUsers().add(currentUser);
        else {
            for (User u:group.getUsers()) {
                if (u.getId().equals(currentUserId)) {
                    u.setName("you");
                    break;
                }
            }
        }

        name.setText(group.getName());
        if(admins.contains(currentUserId)){
            deleteButton.setVisibility(View.VISIBLE);
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showDeleteGroupDialog();
                }
            });
        }
        else{
            deleteButton.setVisibility(View.GONE);
        }
        ContactListAdapter contactListAdapter = new ContactListAdapter(group.getUsers(),this,currentUserId,admins);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        listView.setAdapter(contactListAdapter);
        listView.setLayoutManager(linearLayoutManager);

        findViewById(R.id.exitGroup).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showExitGroupDialog();
            }
        });
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        groupId = savedInstanceState.getString(Util.id);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState, @NonNull PersistableBundle outPersistentState) {
        outState.putString(Util.id,groupId);
        super.onSaveInstanceState(outState, outPersistentState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == ADD_USER_REQUEST && resultCode == RESULT_OK){
           load();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.group_info,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        int id = item.getItemId();
        if(id == R.id.editname){
            if(checkAdminPrivileges()){
                AlertDialog.Builder builder = new AlertDialog.Builder(this,R.style.AlertDialog);
                builder.setCancelable(true);
                View view = getLayoutInflater().inflate(R.layout.create_group_name,null);
                builder.setView(view);
                final AlertDialog alertDialog = builder.create();
                final EditText name = view.findViewById(R.id.name);
                name.setText(group.getName());
                ImageButton confirm = view.findViewById(R.id.confirm);
                confirm.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String n = name.getText().toString();
                        if(!n.equals("")){
                            alertDialog.dismiss();
                            progressDialog.setMessage("Changing name of the group...");
                            progressDialog.show();
                            editGroupName(n);
                        }else{
                            name.setError("Name shouldn't be empty");
                        }
                    }
                });
                alertDialog.show();
            }
            else
                makeToast(ADMIN_PRIVILEGE_MESSAGE);
            return true;
        }
        if(id == R.id.addUser){
            if(checkAdminPrivileges()){
                Intent intent =new Intent(this,CreateGroupActivity.class);
                intent.putExtra(Util.id,groupId);
                startActivityForResult(intent,ADD_USER_REQUEST);
            }
            else
                makeToast(ADMIN_PRIVILEGE_MESSAGE);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void editGroupName(final String n) {
        FirebaseDatabase.getInstance().getReference().child(Groups).child(groupId).child(Util.name).setValue(n).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressDialog.dismiss();
                makeToast("Something went wrong please try again");
            }
        }).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Native restHelper = new Native(GroupInfo.this);
                restHelper.sendGroupUpdatedNotification(group.getId(),group.getUsers(),currentUser);
                progressDialog.dismiss();
                databaseManager.updateGroupName(n,groupId);
                name.setText(n);
            }
        });
    }

    //TODO : more security
    //although that security can only be achieved with a backend server
    private boolean checkAdminPrivileges(){
        return group.getAdmins().contains(currentUserId);
    }

    private void makeToast(String message){
        Toast.makeText(this,message,Toast.LENGTH_SHORT).show();
    }

    private void showExitGroupDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this,R.style.AlertDialog);
        builder.setMessage("Would you like to exit from this group?");
        builder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                exitGroup();
            }
        });
        builder.setCancelable(true);
        builder.create().show();
    }

    private void exitGroup() {

        if(group.getAdmins().contains(currentUserId)){
            if(group.getUsers().size()==1)
                deleteGroup();
            else{

                ArrayList<User> u = new ArrayList<>(group.getUsers());
                u.remove(currentUser);
                Group g = new Group(groupId,group.getName(),u,u.get(0).getId(),Group.GROUP_ACTIVE);
                FirebaseDatabase.getInstance().getReference().child(Groups).child(groupId).setValue(g).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        FirebaseDatabase.getInstance().getReference().child(Groups).child(Users).child(currentUserId).child(Groups).child(groupId).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                sendUserRemovedNotification();
                                databaseManager.markGroupAsDeleted(groupId);
                                sendGroupChanged();
                                finish();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(GroupInfo.this,"Something went wrong, please try again",Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(GroupInfo.this,"Something went wrong, please try again",Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }else{
            ArrayList<User> u = new ArrayList<>(group.getUsers());
            u.remove(currentUser);
            Group g = new Group(groupId,group.getName(),u,group.getAdmins(),Group.GROUP_ACTIVE);
            FirebaseDatabase.getInstance().getReference().child(Groups).child(groupId).setValue(g).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    FirebaseDatabase.getInstance().getReference().child(Groups).child(Users).child(currentUserId).child(Groups).child(groupId).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            sendUserRemovedNotification();
                            databaseManager.markGroupAsDeleted(groupId);
                            sendGroupChanged();
                            finish();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(GroupInfo.this,"Something went wrong, please try again",Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(GroupInfo.this,"Something went wrong, please try again",Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void sendGroupChanged(){
        App app = (App) getApplication();
        if(!app.isnull()){
            for (GroupsUpdatedCallback g:app.getGroupsUpdatedCallback()) {
              g.onComplete();
            }
        }
    }

    private void sendUserRemovedNotification() {
        Native restHelper = new Native(this);
        restHelper.sendGroupUpdatedNotification(group.getId(),group.getUsers(),currentUser);
    }

    private void showDeleteGroupDialog() {
        if(admins.contains(currentUserId)){
        AlertDialog.Builder builder = new AlertDialog.Builder(this,R.style.AlertDialog);
        builder.setMessage("Are you sure you want to delete this group?");
        builder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                deleteGroup();
            }
        }).create().show();
        }
    }

    private void deleteGroup() {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child(Groups).child(groupId);
        databaseReference.removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                sendGroupDeleteNotification();
                databaseManager.markGroupAsDeleted(groupId);
                sendGroupChanged();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(GroupInfo.this,"Something went wrong, please try again",Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendGroupDeleteNotification() {
        Native restHelper = new Native(this);
        String name = getSharedPreferences(Util.preferences,MODE_PRIVATE).getString(Util.name,currentUserId);
        String text = name+" has deleted group - \""+group.getName()+"\"";
        restHelper.sendGroupDeleteNotification(groupId,group.getUsers(),text,currentUserId);
    }

    @Override
    public void onComplete() {
        load();
    }

    static class ContactListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private ArrayList<User> users;
        private Context context;
        String currentUserId;
        String admins;

        ContactListAdapter(ArrayList<User> users, Context context,String currentUserId,String admins) {
            this.users = users;
            this.context = context;
            this.currentUserId = currentUserId;
            this.admins = admins;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_contact, parent, false);
            return new MainListItem(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            
            MainListItem mainListItem = (MainListItem) holder;
            final User user = users.get(position);

            String name = user.getName();
            if(admins.contains(user.getId())){
                name = name+" - \"Group admin\"";
            }

            mainListItem.username.setText(name);
            mainListItem.number.setText(user.getNumber());

            mainListItem.alphabet.setText(user.getName().substring(0,1));
            mainListItem.alphabet.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(userColors[position])));
            mainListItem.relativeLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!user.getId().equals(currentUserId))
                    openPrivateChat(user.getId(),user.getName());
                }
            });
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        static class MainListItem extends RecyclerView.ViewHolder {

            TextView username;
            TextView alphabet;
            TextView number;
            RelativeLayout relativeLayout;

            MainListItem(View itemView) {
                super(itemView);
                username = itemView.findViewById(R.id.username);
                alphabet = itemView.findViewById(R.id.thumbnail);
                number = itemView.findViewById(R.id.number);
                relativeLayout = itemView.findViewById(R.id.container);
            }
        }
        @SuppressLint("InflateParams")
        void openPrivateChat(final String userId, String name){

            AlertDialog.Builder builder = new AlertDialog.Builder(context,R.style.AlertDialog);
            View view = LayoutInflater.from(context).inflate(R.layout.open_private_chat_dialog,null);

            TextView n = view.findViewById(R.id.name);
            n.setText("Send message to - "+name);
            n.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(context.getApplicationContext(), ChatActivity.class);
                    intent.putExtra(Util.userId,userId);
                    context.startActivity(intent);
                }
            });
            builder.setView(view);
            builder.create().show();
        }
    }
}
