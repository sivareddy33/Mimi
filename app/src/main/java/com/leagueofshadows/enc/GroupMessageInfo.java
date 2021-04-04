package com.leagueofshadows.enc;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.leagueofshadows.enc.Items.Message;
import com.leagueofshadows.enc.Items.MessageInfo;
import com.leagueofshadows.enc.Items.User;
import com.leagueofshadows.enc.storage.DatabaseManager;
import com.leagueofshadows.enc.storage.SQLHelper;

import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;

import static com.leagueofshadows.enc.CreateGroupActivity.userColors;

public class GroupMessageInfo extends AppCompatActivity {

    ArrayList<MessageInfo> messageInfos;
    String messageId;
    String groupId;
    DatabaseManager databaseManager;
    TextView readNumber;
    TextView deliveredNumber;
    ListView listView;
    CustomAdapter customAdapter;
    String currentUserId;
    Message message;
    View image;
    View text;
    View file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_info);

        listView = findViewById(R.id.listView);
        readNumber = findViewById(R.id.read);
        deliveredNumber = findViewById(R.id.deliver);

        image = findViewById(R.id.image);
        text = findViewById(R.id.text);
        file = findViewById(R.id.file);

        messageInfos = new ArrayList<>();
        Intent intent = getIntent();

        setTitle("Message Info");
        currentUserId = getSharedPreferences(Util.preferences,MODE_PRIVATE).getString(Util.userId,null);
        messageId = intent.getStringExtra(Util.messageId);
        groupId = intent.getStringExtra(Util.id);
        DatabaseManager.initializeInstance(new SQLHelper(this));
        databaseManager = DatabaseManager.getInstance();

        customAdapter = new CustomAdapter(messageInfos,this,databaseManager);
        listView.setAdapter(customAdapter);

        load();
    }

    private void load() {
        message = databaseManager.getMessage(messageId,groupId);
        messageInfos.clear();
        messageInfos.addAll(databaseManager.getMessageInfo(messageId,groupId));

        int rc = 0;
        int dc = 0;
        MessageInfo toBeRemoved = null;
        for (MessageInfo mi:messageInfos) {
            if(mi.getUserId().equals(currentUserId))
            {
                toBeRemoved = mi;
                continue;
            }
            if (mi.getReceivedTimestamp()==null)
                dc++;
            if(mi.getSeenTimestamp()==null)
                rc++;
        }
        if(toBeRemoved != null)
            messageInfos.remove(toBeRemoved);

        readNumber.setText(rc+" remaining");
        deliveredNumber.setText(dc+" remaining");
        customAdapter.notifyDataSetChanged();
    }

    static class CustomAdapter extends BaseAdapter{

        ArrayList<MessageInfo> mi;
        Context context;
        DatabaseManager databaseManager;

        CustomAdapter(ArrayList<MessageInfo> mi,Context context,DatabaseManager databaseManager){
            this.mi = mi;
            this.context = context;
            this.databaseManager = databaseManager;
        }

        @Override
        public int getCount() {
            return mi.size();
        }

        @Override
        public Object getItem(int i) {
            return mi.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if(view==null){
                view  = LayoutInflater.from(context).inflate(R.layout.group_message_info_item,viewGroup,false);
            }
            TextView name = view.findViewById(R.id.name);
            TextView number = view.findViewById(R.id.number);
            TextView thumbnail = view.findViewById(R.id.thumbnail);
            TextView receivedTime = view.findViewById(R.id.deliver_text);
            TextView seenTime = view.findViewById(R.id.read_text);
            try {
                MessageInfo messageInfo = mi.get(i);
                User user = databaseManager.getUser(messageInfo.getUserId());
                name.setText(user.getName());
                number.setText(user.getNumber());
                thumbnail.setText(user.getName().substring(0, 1));
                if (messageInfo.getSeenTimestamp() != null)
                    seenTime.setText(messageInfo.getSeenTimestamp());
                if (messageInfo.getReceivedTimestamp() != null)
                    receivedTime.setText(messageInfo.getReceivedTimestamp());
                thumbnail.setBackgroundTintList(ColorStateList.valueOf((Color.parseColor(userColors[i]))));
            }catch (Exception e){
                e.printStackTrace();
            }
            return view;
        }
    }
}
