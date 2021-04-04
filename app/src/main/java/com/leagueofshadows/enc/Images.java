package com.leagueofshadows.enc;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.jsibbold.zoomage.ZoomageView;
import com.leagueofshadows.enc.Items.Message;
import com.leagueofshadows.enc.Items.User;
import com.leagueofshadows.enc.storage.DatabaseManager;
import com.leagueofshadows.enc.storage.SQLHelper;

import java.io.File;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import static com.leagueofshadows.enc.Util.getMessageContent;

public class Images extends AppCompatActivity {

    ArrayList<Message> images;
    String messageId;
    Message currentMessage;
    ViewPager2 viewPager2;
    String otherUserId;
    DatabaseManager databaseManager2;
    private CustomImageAdapter customImageAdapter;
    String userId;
    ArrayList<User> queriedUsers;
    ArrayList<String> userIds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_images);

        queriedUsers = new ArrayList<>();
        userIds = new ArrayList<>();
        viewPager2 = findViewById(R.id.viewPager);
        images = new ArrayList<>();
        Intent intent = getIntent();
        otherUserId = intent.getStringExtra(Util.userId);
        messageId = intent.getStringExtra(Util.messageId);
        userId = getSharedPreferences(Util.preferences,MODE_PRIVATE).getString(Util.userId,null);

        DatabaseManager.initializeInstance(new SQLHelper(this));
        databaseManager2 = DatabaseManager.getInstance();
        currentMessage = databaseManager2.getMessage(messageId,otherUserId);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        customImageAdapter = new CustomImageAdapter(images,this,otherUserId,userId);
        viewPager2.setAdapter(customImageAdapter);

        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                Message message = images.get(position);
                String title;
                if(!userId.equals(message.getFrom())) {
                    title = getUserName(message.getFrom());
                }
                else
                    title = "You";
                getSupportActionBar().setTitle(title);
                getSupportActionBar().setSubtitle(getMessageContent(message.getContent()));
            }
        });

        //get messages which contain images from local database
        getImages();
    }

    private String getUserName(String from) {
        if(userIds.contains(from)){
            return queriedUsers.get(userIds.indexOf(from)).getName();
        }else{
            User u = databaseManager2.getUser(from);
            userIds.add(from);
            queriedUsers.add(u);
            return u.getName();
        }
    }

    private void getImages() {
        images.clear();
        ArrayList<Message> messages = databaseManager2.getImages(otherUserId);
        for (Message message:messages) {
            String path;

            if(message.getFrom().equals(userId))
                path = Util.sentImagesPath + getMessageContent(message.getContent());
            else
                path = Util.imagesPath + getMessageContent(message.getContent());

            File file = new File(path);
            if(file.exists())
                images.add(message);
        }
        customImageAdapter.notifyDataSetChanged();
        viewPager2.setCurrentItem(images.indexOf(currentMessage),false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.image_options,menu);
        return true;
    }

    @SuppressLint("InflateParams")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        final int position = viewPager2.getCurrentItem();
        final Message message = images.get(position);

        String path;
        if(!message.getFrom().equals(userId))
            path = Util.imagesPath+getMessageContent(message.getContent());
        else
            path = Util.sentImagesPath+getMessageContent(message.getContent());

        final File file = new File(path);
        String mimeType = "image/*";

        switch (id)
        {
            case R.id.share:{
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType(mimeType);
                intent.putExtra(Intent.EXTRA_STREAM,FileProvider.getUriForFile(this,this.getPackageName()+".fileProvider",file));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
                break;
            }
            case R.id.details:{
                AlertDialog.Builder builder = new AlertDialog.Builder(this,R.style.AlertDialog);
                View view = getLayoutInflater().inflate(R.layout.image_info,null);
                builder.setView(view);
                builder.setCancelable(true);

                TextView name = view.findViewById(R.id.name);
                TextView size = view.findViewById(R.id.size);
                TextView resolution = view.findViewById(R.id.resolution);
                TextView location = view.findViewById(R.id.location);

                name.setText("Name - "+file.getName());
                size.setText("Size - "+getImageSize(file.length()));
                Bitmap bitmap = BitmapFactory.decodeFile(path);
                resolution.setText("Resolution - "+bitmap.getHeight()+"x"+bitmap.getWidth());
                location.setText(path);

                builder.create().show();
                break;
            }
            case R.id.delete:{

                AlertDialog.Builder builder = new AlertDialog.Builder(this,R.style.AlertDialog);
                builder.setMessage("Delete image "+file.getName());
                builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        boolean x = file.delete();
                        if(x) {
                            Toast.makeText(Images.this, "Image deleted", Toast.LENGTH_SHORT).show();
                            images.remove(message);
                            customImageAdapter.notifyItemRangeRemoved(position,1);
                        }
                    }
                }).create().show();
                break;
            }
            case R.id.send:{
                Intent intent = new Intent(this,ShareActivity.class);
                intent.setAction(Intent.ACTION_SEND);
                intent.setType(mimeType);
                intent.putExtra(Intent.EXTRA_STREAM,FileProvider.getUriForFile(this,this.getPackageName()+".fileProvider",file));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
                break;
            }
            case R.id.openInGallery:{
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(FileProvider.getUriForFile(this,this.getPackageName()+".fileProvider",file),mimeType);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
                break;
            }
            case android.R.id.home: {
                onBackPressed();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    String getImageSize(long bytes){
        if(bytes<1024){
            return bytes+" bytes";
        }else if(bytes < 1048576){
            return (float)(bytes/1024) + "KB";
        }else if(bytes < 1073741824){
            return (float)(bytes/1048576)+ "MB";
        }else{
            return "intha pedha file vachindhi ani santoshinchu malli ekkuva details adagaku";
        }
    }

    static class CustomImageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
    {

        ArrayList<Message> files;
        Context context;
        String otherUserId;
        String userId;

        CustomImageAdapter(ArrayList<Message> files,Context context,String otherUserId,String userId) {
            this.context = context;
            this.files = files;
            this.otherUserId = otherUserId;
            this.userId = userId;
        }

        static class Image extends RecyclerView.ViewHolder {
            ZoomageView zoomageView;
            Image(View view) {
                super(view);
                zoomageView = view.findViewById(R.id.imageView);
            }
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.image_item, parent, false);
            return new Image(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Message message= files.get(position);
            Image image = (Image) holder;
            String path;
            if(message.getFrom().equals(userId))
                path = Util.sentImagesPath+getMessageContent(message.getContent());
            else
                path = Util.imagesPath+getMessageContent(message.getContent());
            Glide.with(context).load(path).into(image.zoomageView);
        }

        @Override
        public int getItemCount() {
            return files.size();
        }
    }
}
