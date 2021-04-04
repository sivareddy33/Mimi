package com.leagueofshadows.enc;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.material.tabs.TabLayout;
import com.leagueofshadows.enc.Interfaces.Select;
import com.leagueofshadows.enc.Items.ChatData;
import com.leagueofshadows.enc.Items.User;
import com.leagueofshadows.enc.storage.DatabaseManager;
import com.leagueofshadows.enc.storage.SQLHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

public class ShareActivity extends AppCompatActivity implements Select {

    ArrayList<ChatData> chatDataArrayListUsers;
    ArrayList<ChatData> chatDataArrayListGroups;
    Intent receivedIntent;
    private static ChatListAdapter chatListAdapterUsers;
    private static ChatListAdapter chatListAdapterGroups;
    private TabLayout tabLayout;
    private User currentUser;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_tabbed);

        receivedIntent = getIntent();

        chatDataArrayListUsers = new ArrayList<>();
        chatDataArrayListGroups = new ArrayList<>();

        chatListAdapterUsers = new ChatListAdapter(chatDataArrayListUsers,this,this,receivedIntent);
        chatListAdapterGroups = new ChatListAdapter(chatDataArrayListGroups,this,this,receivedIntent);

        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        ViewPager mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        tabLayout =  findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
        setUpTabIcons();
        load();
    }

    private void setUpTabIcons() {
        try {
            tabLayout.getTabAt(0).setIcon(R.drawable.baseline_person_white_48);
            tabLayout.getTabAt(1).setIcon(R.drawable.baseline_people_white_48);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    void load()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DatabaseManager.initializeInstance(new SQLHelper(ShareActivity.this));
                DatabaseManager databaseManager = new DatabaseManager();
                chatDataArrayListUsers.clear();
                chatDataArrayListGroups.clear();

                String currentUserId = getSharedPreferences(Util.preferences,MODE_PRIVATE).getString(Util.userId,null);
                assert currentUserId != null;
                User user = new User(currentUserId,currentUserId,currentUserId,null);
                ChatData ch1 = new ChatData(user,null,0,0);

                chatDataArrayListUsers.addAll(databaseManager.getUsersForShare());
                chatDataArrayListUsers.remove(ch1);
                chatDataArrayListGroups.addAll(databaseManager.getGroupsForShare());
                sort(chatDataArrayListUsers);
                sort(chatDataArrayListGroups);

                chatListAdapterUsers.notifyDataSetChanged();
                chatListAdapterGroups.notifyDataSetChanged();
            }
        });
    }

    private void sort(ArrayList<ChatData> chatDataArrayList) {
        Collections.sort(chatDataArrayList, new Comparator<ChatData>() {
            @Override
            public int compare( ChatData u1, ChatData u2) { return (int)(u2.getTime()-u1.getTime()); }
        });
    }

    @Override
    public void onClick(User user) { finish(); }

    private static class SectionsPagerAdapter extends FragmentPagerAdapter {

        SectionsPagerAdapter(FragmentManager fm) { super(fm); }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() { return 2; }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            if(position==0)
                return "Contacts";
            else
                return "Groups";
        }
    }

    public static class PlaceholderFragment extends Fragment
    {

        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() { }

        static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(@NonNull final LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            final int pos =  getArguments().getInt(ARG_SECTION_NUMBER);
            View rootView;
            if(pos==1) {
                rootView = inflater.inflate(R.layout.fragment_list,container,false);
                RecyclerView recyclerView = rootView.findViewById(R.id.recycler_view);
                LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
                recyclerView.setLayoutManager(linearLayoutManager);
                recyclerView.setAdapter(chatListAdapterUsers);
            }

            else {
                rootView = inflater.inflate(R.layout.fragment_list,container,false);
                RecyclerView recyclerView = rootView.findViewById(R.id.recycler_view);
                LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
                recyclerView.setLayoutManager(linearLayoutManager);
                recyclerView.setAdapter(chatListAdapterGroups);
            }
            return rootView;
        }
    }

    static class ChatListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private ArrayList<ChatData> objects;
        private Context context;
        private Select select;
        private Intent receivedIntent;

        ChatListAdapter(ArrayList<ChatData> objects, Context context,Select select,Intent receivedIntent) {
            this.objects = objects;
            this.context = context;
            this.select = select;
            this.receivedIntent = receivedIntent;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            if(viewType==ChatData.CHAT_TYPE_SINGLE_USER) {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_contact, parent, false);
                return new ContactListItem(view);
            }
            else {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_group_share, parent, false);
                return new GroupListItem(view);
            }
        }

        @Override
        public int getItemViewType(int position) {
            return objects.get(position).getType();
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

            final ChatData obj  = objects.get(position);
            if(getItemViewType(position)==ChatData.CHAT_TYPE_SINGLE_USER)
            {
                Random rand = new Random();
                int r = rand.nextInt(255);
                int g = rand.nextInt(255);
                int b = rand.nextInt(255);

                int randomColor = Color.rgb(r, g, b);
                ContactListItem mainListItem = (ContactListItem) holder;

                final String name;
                String number;

                name = obj.getUser().getName();
                number = obj.getUser().getNumber();

                mainListItem.username.setText(name);
                mainListItem.number.setText(number);
                mainListItem.alphabet.setText(name.substring(0,1));
                mainListItem.alphabet.setBackgroundTintList(ColorStateList.valueOf((randomColor)));

                mainListItem.relativeLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(context, ChatActivity.class);
                        intent.putExtra(Util.userId, obj.getUser().getId());
                        if (receivedIntent.getAction()!=null)
                        {
                            if(receivedIntent.getAction().equals(Intent.ACTION_SEND))
                            {
                                intent.setAction(Intent.ACTION_SEND);
                                intent.setType(receivedIntent.getType());
                                intent.putExtra(Intent.EXTRA_TEXT,receivedIntent.getStringExtra(Intent.EXTRA_TEXT));
                                intent.putExtra(Intent.EXTRA_SUBJECT,receivedIntent.getStringExtra(Intent.EXTRA_SUBJECT));
                                intent.putExtra(Intent.EXTRA_STREAM,receivedIntent.getParcelableExtra(Intent.EXTRA_STREAM));
                            }
                        }
                        context.startActivity(intent);
                        select.onClick(null);
                    }
                });
            }
            else {
                GroupListItem groupListItem = (GroupListItem) holder;
                groupListItem.username.setText(obj.getGroup().getName());
                groupListItem.relativeLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(context, GroupChatActivity.class);
                        intent.putExtra(Util.userId, obj.getGroup().getId());
                        if (receivedIntent.getAction() != null) {
                            if (receivedIntent.getAction().equals(Intent.ACTION_SEND)) {
                                intent.setAction(Intent.ACTION_SEND);
                                intent.setType(receivedIntent.getType());
                                intent.putExtra(Intent.EXTRA_TEXT, receivedIntent.getStringExtra(Intent.EXTRA_TEXT));
                                intent.putExtra(Intent.EXTRA_SUBJECT, receivedIntent.getStringExtra(Intent.EXTRA_SUBJECT));
                                intent.putExtra(Intent.EXTRA_STREAM, receivedIntent.getParcelableExtra(Intent.EXTRA_STREAM));
                            }
                        }
                        context.startActivity(intent);
                        select.onClick(null);
                    }
                });
            }
        }

        @Override
        public int getItemCount() { return objects.size(); }

        static class ContactListItem extends RecyclerView.ViewHolder {

            TextView username;
            TextView alphabet;
            TextView number;
            RelativeLayout relativeLayout;

            ContactListItem(View itemView) {
                super(itemView);
                username = itemView.findViewById(R.id.username);
                alphabet = itemView.findViewById(R.id.thumbnail);
                number = itemView.findViewById(R.id.number);
                relativeLayout = itemView.findViewById(R.id.container);
            }
        }
        static class GroupListItem extends RecyclerView.ViewHolder{

            TextView username;
            RelativeLayout relativeLayout;

            GroupListItem(View itemView) {
                super(itemView);
                username = itemView.findViewById(R.id.name);
                relativeLayout = itemView.findViewById(R.id.container);
            }
        }
    }
}
