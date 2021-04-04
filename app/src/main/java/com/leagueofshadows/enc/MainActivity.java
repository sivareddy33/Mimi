package com.leagueofshadows.enc;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.leagueofshadows.enc.Interfaces.GroupsUpdatedCallback;
import com.leagueofshadows.enc.Interfaces.MessageSentCallback;
import com.leagueofshadows.enc.Interfaces.MessagesRetrievedCallback;
import com.leagueofshadows.enc.Interfaces.MessageOptionsCallback;
import com.leagueofshadows.enc.Interfaces.ResendMessageCallback;
import com.leagueofshadows.enc.Items.ChatData;
import com.leagueofshadows.enc.Items.Message;
import com.leagueofshadows.enc.Items.User;
import com.leagueofshadows.enc.Background.BackgroundService;
import com.leagueofshadows.enc.storage.DatabaseManager;
import com.leagueofshadows.enc.storage.SQLHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static android.view.View.GONE;
import static com.leagueofshadows.enc.Util.FILE_ATTACHMENT_REQUEST;
import static com.leagueofshadows.enc.Util.IMAGE_ATTACHMENT_REQUEST;
import static com.leagueofshadows.enc.Util.checkPath;
import static com.leagueofshadows.enc.Util.getMessageContent;

public class MainActivity extends AppCompatActivity implements MessagesRetrievedCallback, ResendMessageCallback, MessageSentCallback, MessageOptionsCallback, GroupsUpdatedCallback {

    ArrayList<ChatData> chatDataArrayList;
    RecyclerAdapter recyclerAdapter;
    DatabaseManager databaseManager;
    static String userId;

    final static int DELETE_CONSERVATION = 1;

    FloatingActionButton fab;
    FloatingActionButton newGroup;
    FloatingActionButton newUser;
    boolean flag = false;
    private boolean userPresent = false;
    App app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        app = (App) getApplication();
        if(app.isnull()){
            finishAndStartLogin();
        }

        Log.e("uid", FirebaseAuth.getInstance().getCurrentUser().getUid());

        userId = getSharedPreferences(Util.preferences,MODE_PRIVATE).getString(Util.userId,null);
        assert userId!=null;
        DatabaseManager.initializeInstance(new SQLHelper(this));
        databaseManager = DatabaseManager.getInstance();

        RecyclerView recyclerView = findViewById(R.id.listView);
        chatDataArrayList = new ArrayList<>();

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);

        recyclerAdapter = new RecyclerAdapter(chatDataArrayList,this,this,databaseManager);
        recyclerView.setAdapter(recyclerAdapter);

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { setFloatingActionLayout(); }
        });

        newGroup = findViewById(R.id.newGroup);
        newUser = findViewById(R.id.newUser);

        newGroup.setVisibility(GONE);
        newUser.setVisibility(GONE);

        checkPath(IMAGE_ATTACHMENT_REQUEST);
        checkPath(FILE_ATTACHMENT_REQUEST);

        newGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this,CreateGroupActivity.class));
            }
        });

        newUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this,ContactsActivity.class));
            }
        });
        startService(new Intent(this, BackgroundService.class));
        userPresent = true;
    }

    private void finishAndStartLogin() {
        Intent intent = new Intent(MainActivity.this,Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setFloatingActionLayout() {
        if(flag) {
            fab.setImageResource(R.drawable.add);
            newUser.animate().translationY(0);
            newGroup.animate().translationY(0);
            newUser.setVisibility(GONE);
            newGroup.setVisibility(GONE);
        }
        else {
            fab.setImageResource(R.drawable.baseline_close_white_48);
            newUser.setVisibility(View.VISIBLE);
            newGroup.setVisibility(View.VISIBLE);
            newUser.animate().translationY(getResources().getDimension(R.dimen.st_130));
            newGroup.animate().translationY(getResources().getDimension(R.dimen.st_65));
        }
        flag = !flag;
    }


    @Override
    protected void onResume() {
        super.onResume();
        userPresent = true;
        if(app.isnull()) {
           finishAndStartLogin();
        }
        cancelNewMessageNotifications();
        app.setMessagesRetrievedCallback(this);
        app.setResendMessageCallback(this);
        app.setMessageSentCallback(this);
        app.addGroupsUpdatedCallback(this);
        loadUserData();
    }

    @Override
    protected void onStop() {
        userPresent = false;
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        stopService(new Intent(this,BackgroundService.class));
        super.onDestroy();
    }

    void cancelNewMessageNotifications(){
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.cancelAll();
    }

    void loadUserData()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                chatDataArrayList.clear();
                chatDataArrayList.addAll( databaseManager.getUserData());
                sort(chatDataArrayList);
                recyclerAdapter.notifyDataSetChanged();
            }
        });
    }

    private void sort(ArrayList<ChatData> chatDataArrayList) {

        Collections.sort(chatDataArrayList, new Comparator<ChatData>() {
            @Override
            public int compare(ChatData u1, ChatData u2) {
                return Long.compare(u2.getTime(), u1.getTime());
            }
        });
    }

    @Override
    public void onNewMessage(Message message) {
        if(!userPresent){
            Util.sendNewMessageNotification(message,databaseManager,this,Util.getNotificationIntent(message,this));
        }
        loadUserData();
    }

    @Override
    public void onUpdateMessageStatus(String messageId, String userId) {}

    @Override
    public void newResendMessageCallback(Message message) {
        loadUserData();
    }

    @Override
    public void onComplete(Message message, boolean success, String error) {
    }

    @Override
    public void onOptionsSelected(int option, final int position) {
        if(option==DELETE_CONSERVATION)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this,R.style.AlertDialog);

            String name;
            final String id;

            ChatData chatData = chatDataArrayList.get(position);
            if(chatData.getType()==ChatData.CHAT_TYPE_SINGLE_USER) {
                name = chatData.getUser().getName();
                id = chatData.getUser().getId();
            }
            else{
                name = chatData.getGroup().getName();
                id = chatData.getGroup().getId();
            }
            builder.setMessage("Delete conversation with "+name+" ?");

            builder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    databaseManager.deleteConversation(id);
                    loadUserData();
                }
            }).setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            }).create().show();
        }
    }

    @Override
    public void onComplete() { loadUserData();}

    static class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.MyViewHolder> {

        private ArrayList<ChatData> chatDataArrayList;
        Context context;
        private MessageOptionsCallback messageOptionsCallback;
        DatabaseManager databaseManager;

       /* void set(ArrayList<UserData> userDataArrayList) {
            this.userDataArrayList = userDataArrayList;
        }*/

        static class MyViewHolder extends RecyclerView.ViewHolder {
            TextView thumbNail;
            TextView name;
            TextView message;
            TextView time;
            TextView count;
            SwipeRevealLayout swipe;
            RelativeLayout container;
            ImageButton deleteButton;
            ImageView groupIcon;

            MyViewHolder(View view) {
                super(view);
                thumbNail = view.findViewById(R.id.thumbnail);
                name = view.findViewById(R.id.name);
                message = view.findViewById(R.id.message);
                time = view.findViewById(R.id.time);
                count = view.findViewById(R.id.count);
                container = view.findViewById(R.id.container);
                swipe = view.findViewById(R.id.swipe);
                deleteButton = view.findViewById(R.id.delete_button);
                groupIcon = view.findViewById(R.id.group_thumbnail);
            }
        }

        RecyclerAdapter(ArrayList<ChatData> chatDataArrayList, Context context, MessageOptionsCallback messageOptionsCallback, DatabaseManager databaseManager) {
            this.context = context;
            this.chatDataArrayList = chatDataArrayList;
            this.messageOptionsCallback = messageOptionsCallback;
            this.databaseManager = databaseManager;
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_main, parent, false);
            return new MyViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull final MyViewHolder holder, final int position) {
            final ChatData chatData = chatDataArrayList.get(position);

            if(chatData.getType()==ChatData.CHAT_TYPE_SINGLE_USER) {

                holder.groupIcon.setVisibility(GONE);
                holder.thumbNail.setVisibility(View.VISIBLE);
                holder.name.setText(chatData.getUser().getName());
                holder.thumbNail.setText(chatData.getUser().getName().substring(0, 1));
            }
            else
            {
                holder.thumbNail.setVisibility(View.INVISIBLE);
                holder.name.setText(chatData.getGroup().getName());
                holder.groupIcon.setVisibility(View.VISIBLE);
            }

            if (chatData.getLatestMessage() != null) {
                if (chatData.getLatestMessage().getFrom().equals(userId)) {
                    holder.message.setText("you: " + getMessageContent(chatData.getLatestMessage().getContent()));
                } else {
                    if(chatData.getType()==ChatData.CHAT_TYPE_SINGLE_USER)
                    holder.message.setText(chatData.getUser().getName() + ": " + getMessageContent(chatData.getLatestMessage().getContent()));
                    else {
                        User user = databaseManager.getUser(chatData.getLatestMessage().getFrom());
                        holder.message.setText(user.getName() + ": " + getMessageContent(chatData.getLatestMessage().getContent()));
                    }
                }
            } else {
                holder.message.setText("Send your first message");
            }

            if(chatData.getLatestMessage()!=null)
            holder.time.setText(formatTime(chatData.getLatestMessage().getTimeStamp()));

            int count = chatData.getCount();
            if (count != 0) {
                holder.count.setVisibility(View.VISIBLE);
                holder.count.setText(String.valueOf(chatData.getCount()));
                holder.time.setTextColor(context.getResources().getColor(R.color.msg_received_time, null));
            } else {
                holder.count.setText("");
                holder.count.setVisibility(View.INVISIBLE);
                holder.time.setTextColor(context.getResources().getColor(R.color.main_screen_normal, null));
            }
            holder.container.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (chatData.getType() == ChatData.CHAT_TYPE_SINGLE_USER) {
                        Intent intent = new Intent(context.getApplicationContext(), ChatActivity.class);
                        intent.putExtra(Util.userId, chatData.getUser().getId());
                        context.startActivity(intent);
                    } else {
                        Intent intent = new Intent(context.getApplicationContext(), GroupChatActivity.class);
                        intent.putExtra(Util.userId, chatData.getGroup().getId());
                        context.startActivity(intent);
                    }
                }
            });
            holder.swipe.close(false);
            holder.deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (holder.swipe.isOpen()) {
                        messageOptionsCallback.onOptionsSelected(DELETE_CONSERVATION, position);
                    }
                }
            });
        }

        private String formatTime(String received) {
            received =received.substring(4,16);
            return received;
        }

        @Override
        public int getItemCount() {
            return chatDataArrayList.size();
        }
    }
}