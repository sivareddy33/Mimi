package com.leagueofshadows.enc;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.leagueofshadows.enc.Interfaces.CheckUser;
import com.leagueofshadows.enc.Interfaces.GroupsUpdatedCallback;
import com.leagueofshadows.enc.Interfaces.Select;
import com.leagueofshadows.enc.Items.Group;
import com.leagueofshadows.enc.Items.User;
import com.leagueofshadows.enc.REST.Native;
import com.leagueofshadows.enc.storage.DatabaseManager;
import com.leagueofshadows.enc.storage.SQLHelper;

import java.util.ArrayList;
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.leagueofshadows.enc.FirebaseHelper.Groups;
import static com.leagueofshadows.enc.FirebaseHelper.Users;

public class CreateGroupActivity extends AppCompatActivity implements Select, CheckUser {

    ArrayList<User> groupParticipants = new ArrayList<>();
    User currentUser;
    ArrayList<User> users;

    RecyclerView contacts;
    RecyclerView participantsListView;
    ContactListAdapter contactListAdapter;
    ParticipantsListViewAdapter participantsListViewAdapter;

    DatabaseManager databaseManager;
    Group existingGroup;

    public static String[] userColors = new String[]{"#0f4c75","#FFF44336","#FFFFC107","#FF009688","#FF9C27B0","#FFE91E63"};

    ProgressDialog progressDialog;
    boolean allSuccess = true;
    int count=0;
    boolean editMode = false;
    private int MinGroupSize = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_group);
        setTitle("Create group");

        users = new ArrayList<>();

        SharedPreferences sp = getSharedPreferences(Util.preferences,MODE_PRIVATE);
        String id = sp.getString(Util.userId,null);
        String name = sp.getString(Util.name,null);
        String number = sp.getString(Util.number,null);
        String publicKey = sp.getString(Util.PublicKeyString,null);

        contacts = findViewById(R.id.contacts);
        participantsListView = findViewById(R.id.icons);

        contactListAdapter = new ContactListAdapter(users,this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        contacts.setLayoutManager(linearLayoutManager);
        contacts.setAdapter(contactListAdapter);

        participantsListViewAdapter = new ParticipantsListViewAdapter(groupParticipants,this);
        LinearLayoutManager linearLayoutManager1 = new LinearLayoutManager(this);
        linearLayoutManager1.setOrientation(RecyclerView.HORIZONTAL);
        participantsListView.setLayoutManager(linearLayoutManager1);
        participantsListView.setAdapter(participantsListViewAdapter);

        assert id != null;
        assert name != null;
        assert number != null;

        currentUser = new User(id,name,number,publicKey);
        addParticipant(currentUser);

        DatabaseManager.initializeInstance(new SQLHelper(this));
        databaseManager = DatabaseManager.getInstance();

        findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(groupParticipants.size()<=MinGroupSize) {
                    Toast.makeText(CreateGroupActivity.this,"dheeniki group endhuku ra... direct message cheyi",Toast.LENGTH_SHORT).show();
                    return;
                }

                if(editMode) {
                    askConfirmation(null);
                    return;
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(CreateGroupActivity.this,R.style.AlertDialog);
                View view1 = LayoutInflater.from(CreateGroupActivity.this).inflate(R.layout.create_group_name,null);
                builder.setView(view1);
                builder.setCancelable(true);

                final AlertDialog alertDialog = builder.create();
                final EditText name = view1.findViewById(R.id.name);
                ImageButton ok = view1.findViewById(R.id.confirm);
                ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String n = name.getText().toString();
                        if(!n.equals("")) {
                            askConfirmation(n);
                            alertDialog.dismiss();
                        }
                        else {
                            Toast.makeText(CreateGroupActivity.this,"Name cannot be empty",Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                alertDialog.show();
            }
        });

        progressDialog = new ProgressDialog(this);
        loadContacts();
        String groupId = getIntent().getStringExtra(Util.id);
        if(groupId!=null){
            editMode = true;
            existingGroup = databaseManager.getGroup(groupId);
            for (User u: existingGroup.getUsers()) {
                addParticipant(u);
            }
        }
    }

    private void askConfirmation(final String name) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this,R.style.AlertDialog);
        String message;
        if(editMode)
            message = "make the changes to group - \""+ existingGroup.getName()+"\"";
        else
            message = "Create Group with name - \""+name+"\" and with "+groupParticipants.size()+" participants";
        builder.setMessage(message);
        builder.setCancelable(true);
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                createGroup(name);
            }
        }).create().show();
    }

    private void loadContacts() {
        users.clear();
        users.addAll(databaseManager.getUsers());
        users.remove(currentUser);
        contactListAdapter.notifyDataSetChanged();
    }

    void addParticipant(User user)
    {
        if (groupParticipants.size()<6) {
            groupParticipants.add(user);
            participantsListViewAdapter.notifyDataSetChanged();
            int position = users.indexOf(user);
            contactListAdapter.notifyItemChanged(position);
        }
        else {
            Toast.makeText(this,"Currently we only support maximum 6 participants ",Toast.LENGTH_SHORT).show();
        }
    }

    void createGroup(final String name)
    {
        progressDialog.setMessage("Creating group...");
        progressDialog.show();

        final DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        final String groupId;
        final String finalName;
        final String admins;
        if(editMode){
            groupId = existingGroup.getId();
            finalName = existingGroup.getName();
            admins = existingGroup.getAdmins();
        }
        else{
            groupId = databaseReference.child(Groups).push().getKey();
            finalName = name;
            admins = currentUser.getId();
        }


        final ArrayList<String> userIds = new ArrayList<>();
        for (User u:groupParticipants) {
            u.setName(u.getNumber());
            userIds.add(u.getId());
        }
        existingGroup = new Group(groupId,finalName,groupParticipants,admins,Group.GROUP_ACTIVE);

        databaseReference.child(Groups).child(groupId).setValue(existingGroup).addOnSuccessListener(new OnSuccessListener<Void>() {

            @Override
            public void onSuccess(Void aVoid) {

                for(String id:userIds){
                    DatabaseReference dr = FirebaseDatabase.getInstance().getReference().child(Groups).child(Users).child(id).child(Groups).child(groupId);
                    dr.setValue(groupId).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            count++;
                            if(count==userIds.size())
                                finalMethod(groupId,name);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            count++;
                            allSuccess = false;
                            if (count==userIds.size())
                                finalMethod(groupId,name);
                        }
                    });
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                allSuccess = false;
                finalMethod(null,null);
            }
        });
    }

    void finalMethod(String groupId,String name)
    {
        if(allSuccess) {
            Native restHelper = new Native(this);
            if(editMode){
                ArrayList<User> users = new ArrayList<>();
                users.addAll(databaseManager.getGroup(groupId).getUsers());
                users.addAll(existingGroup.getUsers());
                restHelper.sendGroupUpdatedNotification(groupId,users, currentUser);
                databaseManager.updateGroupUsers(existingGroup);
                App app = (App) getApplication();
                if(!app.isnull()){
                    ArrayList<GroupsUpdatedCallback> groupsUpdatedCallbacks = app.getGroupsUpdatedCallback();
                    if(groupsUpdatedCallbacks!=null){
                        for (GroupsUpdatedCallback gr:groupsUpdatedCallbacks) {
                            gr.onComplete();
                        }
                    }
                }
            }
            else {
                groupParticipants.remove(currentUser);
                Group group = new Group(groupId, name, groupParticipants, currentUser.getId(), Group.GROUP_ACTIVE);
                databaseManager.addNewGroup(group);
                restHelper.sendNewGroupNotification(groupParticipants, currentUser, name);
                App app = (App) getApplication();
                if(!app.isnull()){
                    ArrayList<GroupsUpdatedCallback> groupsUpdatedCallbacks = app.getGroupsUpdatedCallback();
                    if(groupsUpdatedCallbacks!=null){
                        for (GroupsUpdatedCallback gr:groupsUpdatedCallbacks) {
                            gr.onComplete();
                        }
                    }
                }
            }
            finish();
        }
        else {
            Toast.makeText(this,"Something went wrong,please try again",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(User user) {

        if(user.equals(currentUser))
            return;

        if(groupParticipants.contains(user)){
            groupParticipants.remove(user);
            contactListAdapter.notifyItemChanged(users.indexOf(user));
            participantsListViewAdapter.notifyDataSetChanged();
        }
        else
            addParticipant(user);
    }

    @Override
    public boolean check(User user) {
        return groupParticipants.contains(user);
    }

    static class ContactListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private ArrayList<User> users;
        private Select select;
        private CheckUser checkUser;

        ContactListAdapter(ArrayList<User> msgList,Context context) {
            this.users = msgList;
            this.select = (Select) context;
            this.checkUser = (CheckUser) context;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_create_group, parent, false);
            return new ContactListAdapter.MainListItem(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

            Random rand = new Random();
            int r = rand.nextInt(255);
            int g = rand.nextInt(255);
            int b = rand.nextInt(255);

            int randomColor = Color.rgb(r, g, b);

            MainListItem mainListItem = (MainListItem) holder;
            final User user = users.get(position);

            if(checkUser.check(user)) {

                mainListItem.alphabet.setVisibility(View.INVISIBLE);
                mainListItem.imageView.setVisibility(View.VISIBLE);
                mainListItem.alphabet.setText("");
            }
            else {
                mainListItem.alphabet.setVisibility(View.VISIBLE);
                mainListItem.imageView.setVisibility(View.INVISIBLE);
                mainListItem.alphabet.setText(user.getName().substring(0,1));
                mainListItem.alphabet.setBackgroundTintList(ColorStateList.valueOf((randomColor)));
            }

            mainListItem.username.setText(user.getName());
            mainListItem.number.setText(user.getNumber());
            mainListItem.relativeLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    select.onClick(user);
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
            ImageView imageView;
            RelativeLayout relativeLayout;

            MainListItem(View itemView) {
                super(itemView);
                username = itemView.findViewById(R.id.name);
                alphabet = itemView.findViewById(R.id.thumbnail);
                number = itemView.findViewById(R.id.number);
                imageView = itemView.findViewById(R.id.tick);
                relativeLayout = itemView.findViewById(R.id.container);
            }
        }
    }

    static class ParticipantsListViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

        ArrayList<User> participants;
        Context context;

        ParticipantsListViewAdapter(ArrayList<User> participants, Context context){
            this.participants = participants;
            this.context = context;
        }

        static class Item extends RecyclerView.ViewHolder{
            TextView textView;
            Item(@NonNull View itemView) {
                super(itemView);
                textView = itemView.findViewById(R.id.icon);
            }
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
           View view = LayoutInflater.from(context).inflate(R.layout.create_group_icon,parent,false);
           return new Item(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {

            Item h = (Item) holder;

            if(position>=participants.size())
            {
                h.textView.setBackgroundTintList(ColorStateList.valueOf((Color.BLACK)));
                h.textView.setText("");
                h.textView.setOnClickListener(null);
            }

            else {
                final User user = participants.get(position);
                h.textView.setBackgroundTintList(ColorStateList.valueOf((Color.parseColor(userColors[position]))));
                h.textView.setText(user.getName().substring(0, 1));
                h.textView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialog);

                        View view1 = LayoutInflater.from(context).inflate(R.layout.create_group_icon_click_dialog, null);
                        builder.setView(view1);
                        builder.setCancelable(true);

                        TextView name = view1.findViewById(R.id.name);
                        TextView number = view1.findViewById(R.id.number);
                        TextView thumbnail = view1.findViewById(R.id.thumbnail);
                        ImageButton delete = view1.findViewById(R.id.delete);

                        thumbnail.setText(user.getName().substring(0, 1));
                        thumbnail.setBackgroundTintList(ColorStateList.valueOf((Color.parseColor(userColors[position]))));
                        name.setText(user.getName());
                        number.setText(user.getNumber());

                        final AlertDialog alertDialog = builder.create();
                        delete.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Select select = (Select) context;
                                select.onClick(user);
                                alertDialog.dismiss();
                            }
                        });
                        alertDialog.show();
                    }
                });
            }
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public int getItemCount() {
            return 6;
        }
    }
}
