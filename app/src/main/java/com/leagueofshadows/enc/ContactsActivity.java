package com.leagueofshadows.enc;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.leagueofshadows.enc.Interfaces.CompleteCallback;
import com.leagueofshadows.enc.Interfaces.Select;
import com.leagueofshadows.enc.Items.User;
import com.leagueofshadows.enc.Background.ContactsWorker;
import com.leagueofshadows.enc.storage.DatabaseManager;
import com.leagueofshadows.enc.storage.SQLHelper;

import java.util.ArrayList;
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.leagueofshadows.enc.Background.ContactsWorker.FLAG;

public class ContactsActivity extends AppCompatActivity implements CompleteCallback, Select {

    ArrayList<User> users;
    RecyclerView listView;
    ContactListAdapter contactListAdapter;
    MenuItem syncingIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_list);
        getSupportActionBar().setTitle("People using Mimi");

        users = new ArrayList<>();
        listView = findViewById(R.id.recycler_view);
        contactListAdapter = new ContactListAdapter(users,this,this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        listView.setLayoutManager(linearLayoutManager);
        listView.setAdapter(contactListAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        App app = (App) getApplication();
        app.setCompleteCallback(this);
        load();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void load() {
        DatabaseManager.initializeInstance(new SQLHelper(this));
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        users.clear();
        users.addAll(databaseManager.getUsers());
        String currentUserId = getSharedPreferences(Util.preferences,MODE_PRIVATE).getString(Util.userId,null);
        assert currentUserId != null;
        User currentUser = new User(currentUserId,currentUserId,currentUserId,null);
        users.remove(currentUser);
        contactListAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options,menu);
        syncingIcon = menu.findItem(R.id.syncingIcon);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId()==R.id.refresh)
        {
            Intent intent1 = new Intent(ContactsActivity.this, ContactsWorker.class);
            intent1.putExtra(FLAG,0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent1);
            }
            else {
                startService(intent1);
            }
            syncingIcon.setVisible(true);
        }
        else if(item.getItemId() == R.id.contacts)
        {
            Intent intent = new Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI);
            startActivity(intent);
        }
        else if(item.getItemId()==R.id.share)
        {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT,getResources().getString(R.string.app_name));
            String shareMessage = "share message";
            intent.putExtra(Intent.EXTRA_TEXT,shareMessage);
            startActivity(intent);
        }
        else if(item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return true;
    }

    @Override
    public void onComplete(int x) {
        syncingIcon.setVisible(false);
        load();
        Toast.makeText(this,"Your contact list has been updated",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCanceled() {}

    @Override
    public void onClick(User user) {
        finish();
    }

    static class ContactListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private ArrayList<User> users;
        private Context context;
        private Select select;

        ContactListAdapter(ArrayList<User> msgList, Context context,Select select) {
            this.users = msgList;
            this.context = context;
            this.select = select;
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

            Random rand = new Random();
            int r = rand.nextInt(255);
            int g = rand.nextInt(255);
            int b = rand.nextInt(255);

            int randomColor = Color.rgb(r, g, b);

            MainListItem mainListItem = (MainListItem) holder;
            final User user = users.get(position);
            mainListItem.username.setText(user.getName());
            mainListItem.number.setText(user.getNumber());
            mainListItem.alphabet.setText(user.getName().substring(0,1));
            mainListItem.alphabet.setBackgroundTintList(ColorStateList.valueOf((randomColor)));
            mainListItem.relativeLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(context, ChatActivity.class);
                    intent.putExtra(Util.userId, user.getId());
                    context.startActivity(intent);
                    select.onClick(null);
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
    }
}
