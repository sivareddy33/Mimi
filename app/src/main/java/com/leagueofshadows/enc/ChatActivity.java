package com.leagueofshadows.enc;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.leagueofshadows.enc.Background.Downloader;
import com.leagueofshadows.enc.Background.FileUploadWorker;
import com.leagueofshadows.enc.Crypt.AESHelper;
import com.leagueofshadows.enc.Exceptions.DeviceOfflineException;
import com.leagueofshadows.enc.Exceptions.RunningOnMainThreadException;
import com.leagueofshadows.enc.Interfaces.MessageOptionsCallback;
import com.leagueofshadows.enc.Interfaces.MessageSentCallback;
import com.leagueofshadows.enc.Interfaces.MessagesRetrievedCallback;
import com.leagueofshadows.enc.Interfaces.PublicKeyCallback;
import com.leagueofshadows.enc.Interfaces.ResendMessageCallback;
import com.leagueofshadows.enc.Interfaces.ScrollEndCallback;
import com.leagueofshadows.enc.Interfaces.UserTypingCallback;
import com.leagueofshadows.enc.Items.EncryptedMessage;
import com.leagueofshadows.enc.Items.Message;
import com.leagueofshadows.enc.Items.TypingNotification;
import com.leagueofshadows.enc.Items.User;
import com.leagueofshadows.enc.REST.Native;
import com.leagueofshadows.enc.storage.DatabaseManager;
import com.leagueofshadows.enc.storage.SQLHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import static android.view.View.GONE;
import static com.leagueofshadows.enc.FirebaseHelper.Messages;
import static com.leagueofshadows.enc.Util.FILE_ATTACHMENT_REQUEST;
import static com.leagueofshadows.enc.Util.IMAGE_ATTACHMENT_REQUEST;
import static com.leagueofshadows.enc.Util.IMAGE_SELECTED;
import static com.leagueofshadows.enc.Util.LOAD_THUMBNAIL_SIZE;
import static com.leagueofshadows.enc.Util.MESSAGE_CONTENT;
import static com.leagueofshadows.enc.Util.MESSAGE_COPY;
import static com.leagueofshadows.enc.Util.MESSAGE_DELETE;
import static com.leagueofshadows.enc.Util.MESSAGE_INFO;
import static com.leagueofshadows.enc.Util.MESSAGE_REPLIED;
import static com.leagueofshadows.enc.Util.MESSAGE_REPLIED_ID;
import static com.leagueofshadows.enc.Util.MESSAGE_REPLY;
import static com.leagueofshadows.enc.Util.MESSAGE_SHARE;
import static com.leagueofshadows.enc.Util.OPEN_CAMERA_REQUEST;
import static com.leagueofshadows.enc.Util.RECEIVE_ERROR;
import static com.leagueofshadows.enc.Util.RECEIVE_FILE;
import static com.leagueofshadows.enc.Util.RECEIVE_IMAGE;
import static com.leagueofshadows.enc.Util.RECEIVE_TEXT;
import static com.leagueofshadows.enc.Util.SEND_FILE;
import static com.leagueofshadows.enc.Util.SEND_IMAGE;
import static com.leagueofshadows.enc.Util.SEND_TEXT;
import static com.leagueofshadows.enc.Util.checkPath;
import static com.leagueofshadows.enc.Util.getCompressionFactor;
import static com.leagueofshadows.enc.Util.getFileName;
import static com.leagueofshadows.enc.Util.getMessageContent;
import static com.leagueofshadows.enc.Util.messageCopy;
import static com.leagueofshadows.enc.Util.prepareMessage;

@SuppressLint("InflateParams")
public class ChatActivity extends AppCompatActivity implements MessagesRetrievedCallback, MessageSentCallback,
        ScrollEndCallback, PublicKeyCallback, ResendMessageCallback, MessageOptionsCallback, UserTypingCallback, TextWatcher {

    ArrayList<Message> messages;
    ArrayList<String> messageIds;
    ArrayList<String> unSeenMessageIds;
    RecyclerView listView;
    RecyclerAdapter recyclerAdapter;
    User otherUser;
    String otherUserId;
    String currentUserId;
    User[] otherUserArray;
    DatabaseManager databaseManager;
    SharedPreferences sp;
    FirebaseHelper firebaseHelper;
    DatabaseReference databaseReference;

    EditText messageField;
    RelativeLayout chatReplyLayout;
    TextView replyName;
    TextView replyMessageText;
    ImageButton closeReplyLayout;
    ImageView imageThumbnail;

    FloatingActionButton send;
    AESHelper aesHelper;
    Native restHelper;

    private Message replyMessage;

    ImageButton addFile;
    ImageButton addImage;
    ImageButton openCamera;
    FloatingActionButton attachment;
    boolean isAttachmentLayoutOpen = false;
    boolean initialLoading = true;

     RecyclerView.SmoothScroller smoothScroller;
     private LinearLayoutManager layoutManager;

     private boolean userPresent;
     App app;

     @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Intent receivedIntent = getIntent();
        otherUserId = receivedIntent.getStringExtra(Util.userId);

        app = (App) getApplication();
        if(app.isnull() || otherUserId==null){
            finishAndStartLogin();
        }

        DatabaseManager.initializeInstance(new SQLHelper(this));
        databaseManager = DatabaseManager.getInstance();
        databaseManager.setNewMessageCounter(otherUserId);

        databaseReference = FirebaseDatabase.getInstance().getReference().child(Messages);

        otherUser = databaseManager.getUser(otherUserId);
        otherUserArray = new User[]{otherUser};

        setTitle(otherUser.getName());
        sp = getSharedPreferences(Util.preferences,MODE_PRIVATE);
        currentUserId = sp.getString(Util.userId,null);
        assert currentUserId!=null;

        messages = new ArrayList<>();
        messageIds = new ArrayList<>();
        unSeenMessageIds = new ArrayList<>();
        firebaseHelper = new FirebaseHelper(this);
         try {
             aesHelper = new AESHelper(this);
         } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
             e.printStackTrace();
         }

         restHelper = new Native(this);
         listView = findViewById(R.id.listView);
         layoutManager = new LinearLayoutManager(this);
         layoutManager.setStackFromEnd(true);

         recyclerAdapter = new RecyclerAdapter(messages,this,currentUserId,otherUserId,this,this,otherUser.getName());
         recyclerAdapter.setHasStableIds(true);

         listView.setHasFixedSize(false);
         listView.setItemViewCacheSize(20);

         listView.setAdapter(recyclerAdapter);
         listView.setLayoutManager(layoutManager);
         try {
             ((SimpleItemAnimator) listView.getItemAnimator()).setSupportsChangeAnimations(false);
         }catch (Exception e){
             e.printStackTrace();
         }

         addFile = findViewById(R.id.file);
         addImage = findViewById(R.id.image);
         openCamera = findViewById(R.id.camera);

         addFile.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View view) {
                 Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                 intent.setType("*/*");
                 startActivityForResult(intent,FILE_ATTACHMENT_REQUEST);
             }
         });
         addImage.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View view) {
                 Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                 intent.setType("image/*");
                 startActivityForResult(Intent.createChooser(intent,"Select picture"),IMAGE_ATTACHMENT_REQUEST);
             }
         });
         openCamera.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View view) {
                 Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                 File file = new File(getApplicationContext().getFilesDir(),"current.jpg");
                 Uri uri = FileProvider.getUriForFile(ChatActivity.this,"com.leagueofshadows.enc.fileProvider",file);
                 intent.putExtra(MediaStore.EXTRA_OUTPUT,uri);
                 intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                 startActivityForResult(Intent.createChooser(intent,"take picture using"),OPEN_CAMERA_REQUEST);
             }
         });

         attachment = findViewById(R.id.attachment);
         attachment.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View view) {
                 setAttachmentLayout();
             }
         });

         smoothScroller = new LinearSmoothScroller(this){
            @Override
            protected int getVerticalSnapPreference() {
                return LinearSmoothScroller.SNAP_TO_END;
            }
        };

        messageField = findViewById(R.id.chat_edit_text);
        messageField.addTextChangedListener(this);

        chatReplyLayout = findViewById(R.id.chat_reply);
        chatReplyLayout.setVisibility(GONE);

        replyName = findViewById(R.id.reply_name);
        replyMessageText = findViewById(R.id.reply_message);
        closeReplyLayout = findViewById(R.id.close_reply);

        closeReplyLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               closeReplyLayout();
            }
        });
        imageThumbnail = findViewById(R.id.thumbnail);

        send = findViewById(R.id.send);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });

        String x = sp.getString(otherUserId,null);
        if (x!=null) {
            messageField.setText(x);
            sp.edit().remove(otherUserId).apply();
        }

        if(receivedIntent.getAction()!=null&&receivedIntent.getAction().equals(Intent.ACTION_SEND))
        {
            try {
                String type = receivedIntent.getType();

                assert type != null;

                if (type.startsWith("text/")) {
                    messageField.setText(receivedIntent.getStringExtra(Intent.EXTRA_TEXT));
                } else if (type.startsWith("image/")) {
                    Uri uri = receivedIntent.getParcelableExtra(Intent.EXTRA_STREAM);
                    Intent intent = new Intent(this, ImagePreview.class);

                    assert uri != null;

                    intent.putExtra(Util.uri, uri.toString());
                    intent.putExtra(Util.name, otherUser.getName());
                    startActivityForResult(intent, IMAGE_SELECTED);
                } else if (type.startsWith("application/")) {
                    showMessageDialog((Uri)receivedIntent.getParcelableExtra(Intent.EXTRA_STREAM));
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
        userPresent = true;
    }

     @Override
     protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
         try {
             otherUserId = savedInstanceState.getString(Util.userId);
         }catch (Exception e) {
             e.printStackTrace();
         }
     }

     @Override
     protected void onSaveInstanceState(@NonNull Bundle outState) {
         outState.putString(Util.userId,otherUserId);
         super.onSaveInstanceState(outState);
     }

     @Override
     protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

         super.onActivityResult(requestCode, resultCode, data);
         if (resultCode==RESULT_OK)
         {
             if (requestCode==FILE_ATTACHMENT_REQUEST) {
                 assert data != null;
                 final Uri uri = data.getData();
                 try {
                     String fileName = getFileName(uri,this);
                     MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
                     String mimeType = mimeTypeMap.getMimeTypeFromExtension(fileName);
                     assert mimeType != null;

                     if (mimeType.startsWith("image/"))
                         sendImage(uri);
                     else
                         showMessageDialog(uri);
                 }catch (Exception e) {
                     showMessageDialog(uri);
                     e.printStackTrace();
                 }
             }
             if (requestCode==IMAGE_ATTACHMENT_REQUEST) {
                 try{
                     assert data != null;
                     Uri uri = data.getData();
                     Intent intent = new Intent(this,ImagePreview.class);
                     intent.putExtra(Util.uri,uri.toString());
                     intent.putExtra(Util.name,otherUser.getName());
                     startActivityForResult(intent,IMAGE_SELECTED);
                 }catch (Exception e) {
                     e.printStackTrace();
                 }
             }
             if (requestCode==OPEN_CAMERA_REQUEST) {
                 File file = new File(getApplicationContext().getFilesDir(),"current.jpg");
                 Intent intent = new Intent(this,ImagePreview.class);
                 intent.putExtra(Util.path,file.getPath());
                 intent.putExtra(Util.camera,Util.camera);
                 intent.putExtra(Util.name,otherUser.getName());
                 startActivityForResult(intent,IMAGE_SELECTED);
             }
             if (requestCode==IMAGE_SELECTED) {
                 try {
                     Uri uri = Uri.parse(data.getStringExtra(Util.uri));
                     if(uri!=null) {
                         sendImage(uri);
                     }
                 }
                 catch (Exception e){
                     e.printStackTrace();
                 }
             }
         }
         else { Toast.makeText(this,"Canceled",Toast.LENGTH_SHORT).show(); }
     }

     @Override
     protected void onResume() {
         super.onResume();
         userPresent = true;
         sendSeenStatusOfUnseenMessages();
         Util.clearNewMessageNotification(otherUserId.hashCode(),this);

         if(app.isnull()) {
             finishAndStartLogin();
         }
         app.setUserTypingCallback(this);
         app.setMessagesRetrievedCallback(this);
         app.setResendMessageCallback(this);
         app.setMessageSentCallback(this);
         firebaseHelper.getUserPublic(otherUserId,this);
         if(messages.isEmpty()) {
             getMessages();
         }
     }
    private void finishAndStartLogin() {
        Intent intent = new Intent(this,Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

     private void sendSeenStatusOfUnseenMessages() {
         databaseManager.setNewMessageCounter(otherUserId);
         try {
             for (String messageId : unSeenMessageIds) {
                 int pos = messageIds.indexOf(messageId);
                 markMessageAsRead(messages.get(pos));
             }
         }catch (Exception e){
             e.printStackTrace();
         }
     }

     @Override
     protected void onStop() {
         userPresent = false;
         super.onStop();
     }

     @Override
     protected void onDestroy() {
         String x = messageField.getText().toString();
         if (!x.equals("")){
             sp.edit().putString(otherUserId,x).apply();
         }
         super.onDestroy();
     }

     private void setAttachmentLayout() {
         if(!isAttachmentLayoutOpen)
         {
             isAttachmentLayoutOpen = true;
             attachment.setImageResource(R.drawable.add);
             addFile.animate().translationY(getResources().getDimension(R.dimen.st_55));
             addFile.setVisibility(View.VISIBLE);
             addImage.animate().translationY(getResources().getDimension(R.dimen.st_100));
             addImage.setVisibility(View.VISIBLE);
             openCamera.animate().translationY(getResources().getDimension(R.dimen.st_145));
             openCamera.setVisibility(View.VISIBLE);
         }
         else
         {
             isAttachmentLayoutOpen = false;
             attachment.setImageResource(R.drawable.baseline_attachment_white_24);
             addFile.animate().translationY(getResources().getDimension(R.dimen.st_normal));
             addImage.animate().translationY(getResources().getDimension(R.dimen.st_normal));
             openCamera.animate().translationY(getResources().getDimension(R.dimen.st_normal));
             addFile.setVisibility(View.INVISIBLE);
             addImage.setVisibility(View.INVISIBLE);
             openCamera.setVisibility(View.INVISIBLE);
         }
     }

     void getMessages() {
         final ArrayList<Message> m = databaseManager.getMessages(otherUser.getId(),messages.size(),Util.MESSAGE_CACHE);
         final int x = messages.size();
         if (m.size()==0)
             return;

         for (int i = m.size()-1;i>=0;i--) {
             Message message = m.get(i);
             if(message.getSeen()==null && message.getFrom().equals(otherUserId))
             {
                 String timeStamp = Calendar.getInstance().getTime().toString();
                 message.setSeen(timeStamp);
                 databaseManager.updateMessageSeenStatus(timeStamp,message.getMessage_id(),otherUserId,null,currentUserId);
                 if(message.getContent()!=null)
                     firebaseHelper.sendMessageSeenStatus(message,timeStamp);
             }
             messages.add(0,message);
             messageIds.add(0,message.getMessage_id());
         }
         listView.post(new Runnable() {
             @Override
             public void run() {
                 if(initialLoading){
                     recyclerAdapter.notifyDataSetChanged();
                     initialLoading=false;
                 }
                 else {
                     recyclerAdapter.notifyItemRangeInserted(0, m.size());
                 }
                 /*int x = messages.size()-1;
                 if(x>=0) {
                     smoothScroller.setTargetPosition(messages.size() - 1);
                     layoutManager.startSmoothScroll(smoothScroller);
                 }*/
             }
         });
     }

     private void sendMessage() {
         AsyncTask.execute(new Runnable() {
             @Override
             public void run() {
                 String messageString = messageField.getText().toString();
                 if(!messageString.equals(""))
                 {
                     messageString = messageString.trim().replaceAll("\n","");
                     messageString = prepareMessage(messageString,replyMessage,currentUserId);
                     closeReplyLayout();
                     App app = (App) getApplication();
                     try {
                         String cipherText = aesHelper.encryptMessage(messageString,otherUserArray,app.getPrivateKey());
                         String timeStamp = Calendar.getInstance().getTime().toString();

                         String id = databaseReference.push().getKey();
                         assert id != null;

                         Message message = new Message(0,id,otherUserId,currentUserId,messageString,null,
                                 timeStamp,Message.MESSAGE_TYPE_ONLYTEXT,null,null,null,Message.MESSAGE_TYPE_SINGLE_USER);

                         EncryptedMessage e = new EncryptedMessage(id,otherUserId,currentUserId,cipherText,null,timeStamp,Message.MESSAGE_TYPE_ONLYTEXT,Message.MESSAGE_TYPE_SINGLE_USER);
                         firebaseHelper.sendTextOnlyMessage(message,e,ChatActivity.this,id);
                         updateNewSentMessage(message);
                     } catch (RunningOnMainThreadException | DeviceOfflineException e) {
                         e.printStackTrace();
                     }
                 }
             }
         });
     }

     void showMessageDialog(final Uri uri)
     {
         AlertDialog.Builder builder = new AlertDialog.Builder(this,R.style.AlertDialog);
         builder.setMessage("Send file - "+getFileName(uri,this)+" to "+otherUser.getName());
         builder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
             @Override
             public void onClick(DialogInterface dialogInterface, int i) {
                 sendFile(uri);
             }
         }).setNegativeButton("cancel", new DialogInterface.OnClickListener() {
             @Override
             public void onClick(DialogInterface dialogInterface, int i) {
                 dialogInterface.dismiss();
             }
         }).create().show();
     }
     @SuppressLint("SimpleDateFormat")
     private void sendImage(final Uri uri)
     {
         AsyncTask.execute(new Runnable() {
             @Override
             public void run() {
                 Message message = null;
                 try {
                     Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
                     int compressionFactor = getCompressionFactor(bitmap.getByteCount());
                     String path = Util.sentImagesPath;
                     checkPath(IMAGE_ATTACHMENT_REQUEST);
                     SimpleDateFormat simpleDateFormat = new SimpleDateFormat("YYYYMMDD-HHmmss");
                     String fileName = "IMG-"+simpleDateFormat.format(new Date())+".jpg";
                     String timeStamp = Calendar.getInstance().getTime().toString();

                     path = path+fileName;
                     FileOutputStream fileOutputStream = new FileOutputStream(path);
                     bitmap.compress(Bitmap.CompressFormat.JPEG,compressionFactor,fileOutputStream);

                     String id = databaseReference.push().getKey();

                     assert id != null;
                     String messageString = prepareMessage(fileName,replyMessage,currentUserId);
                     closeReplyLayout();
                     message = new Message(0,id,otherUserId,currentUserId,messageString,path,timeStamp,Message.MESSAGE_TYPE_IMAGE,
                             null,null,null,Message.MESSAGE_TYPE_SINGLE_USER);

                     updateNewSentMessage(message);
                     FileInputStream fileInputStream = new FileInputStream(path);
                     fileOutputStream = new FileOutputStream(Util.privatePath+fileName);
                     App app = (App) getApplication();
                     FileInputStream fileInputStream1 = new FileInputStream(path);
                     String cipherText = aesHelper.encryptFile(fileInputStream,fileInputStream1,fileOutputStream,app.getPrivateKey(),otherUserArray,messageString);

                     Intent intent = new Intent(ChatActivity.this, FileUploadWorker.class);
                     intent.putExtra(Util.toUserId,otherUserId);
                     intent.putExtra(Util.userId,currentUserId);
                     intent.putExtra(Util.fileName,fileName);
                     intent.putExtra(Util.content,messageString);
                     intent.putExtra(Util.timeStamp,timeStamp);
                     intent.putExtra(Util.cipherText,cipherText);
                     intent.putExtra(Util.name,otherUser.getName());
                     intent.putExtra(Util.uri,uri.toString());
                     intent.putExtra(Util.id,id);
                     intent.putExtra(Util.type, Message.MESSAGE_TYPE_IMAGE);
                     intent.putExtra(Util.messageType,Message.MESSAGE_TYPE_SINGLE_USER);
                     if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                         startForegroundService(intent);
                     else
                         startService(intent);

                 } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeySpecException |
                         BadPaddingException | RunningOnMainThreadException | InvalidKeyException |
                         InvalidAlgorithmParameterException | IOException | IllegalBlockSizeException e) {
                     if(message!=null) {
                         int position = messageIds.indexOf(message.getMessage_id());
                         messages.remove(message);
                         messageIds.remove(message.getMessage_id());
                         updateMessageRemoved(position);
                     }
                     e.printStackTrace();
                 }
             }
         });
     }

     private void sendFile(final Uri uri)  {

         final String fileName = getFileName(uri,this);
         final String timeStamp = Calendar.getInstance().getTime().toString();
         final String id = databaseReference.push().getKey();

         assert id != null;
         final String messageString = prepareMessage(fileName,replyMessage,currentUserId);
         closeReplyLayout();
         final Message message = new Message(0,id,otherUserId,currentUserId,messageString,uri.toString(),timeStamp,
                 Message.MESSAGE_TYPE_FILE,null,null,null,Message.MESSAGE_TYPE_SINGLE_USER);
         updateNewSentMessage(message);

         AsyncTask.execute(new Runnable() {
             @Override
             public void run() {
                 try {
                     String path = Util.sentDocumentsPath;
                     checkPath(FILE_ATTACHMENT_REQUEST);
                     path = path+fileName;
                     FileInputStream fileInputStream = (FileInputStream) getContentResolver().openInputStream(uri);

                     assert fileInputStream != null;
                     App app = (App) getApplication();

                     FileOutputStream fileOutputStream1 = new FileOutputStream(path);

                     byte[] buffer = new byte[8192];
                     int count;
                     while((count = fileInputStream.read(buffer))>0) {
                         fileOutputStream1.write(buffer,0,count);
                     }
                     fileInputStream = new FileInputStream(path);
                     FileInputStream fileInputStream1 = new FileInputStream(path);
                     final FileOutputStream fileOutputStream  = new FileOutputStream(Util.privatePath+fileName);

                     String cipherText = aesHelper.encryptFile(fileInputStream,fileInputStream1,fileOutputStream,app.getPrivateKey(),otherUserArray,messageString);

                     Intent intent = new Intent(ChatActivity.this, FileUploadWorker.class);
                     intent.putExtra(Util.toUserId,otherUserId);
                     intent.putExtra(Util.userId,currentUserId);
                     intent.putExtra(Util.fileName,fileName);
                     intent.putExtra(Util.cipherText,cipherText);
                     intent.putExtra(Util.content,messageString);
                     intent.putExtra(Util.timeStamp,timeStamp);
                     intent.putExtra(Util.name,otherUser.getName());
                     intent.putExtra(Util.uri,uri.toString());
                     intent.putExtra(Util.id,id);
                     intent.putExtra(Util.messageType,Message.MESSAGE_TYPE_SINGLE_USER);
                     if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                         startForegroundService(intent);
                     else
                         startService(intent);

                 } catch (NoSuchAlgorithmException | NoSuchPaddingException | RunningOnMainThreadException |
                         IllegalBlockSizeException | BadPaddingException | InvalidKeySpecException |
                         InvalidKeyException | IOException | InvalidAlgorithmParameterException e) {
                            int position = messageIds.indexOf(message.getMessage_id());
                            messages.remove(message);
                            messageIds.remove(message.getMessage_id());
                            updateMessageRemoved(position);
                     e.printStackTrace();
                 }
             }
         });
     }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String userId = intent.getStringExtra(Util.userId);
        try {
            if (!userId.equals(currentUserId)) {
                onCreate(null);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //options for messages
     @SuppressWarnings("ResultOfMethodCallIgnored")
    void deleteMessage(final Message message, final int position)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this,R.style.AlertDialog);
        View view = getLayoutInflater().inflate(R.layout.delete_check_box,null);
        final CheckBox checkBox = view.findViewById(R.id.delete);
        final TextView text = view.findViewById(R.id.text);
        if(message.getType()==Message.MESSAGE_TYPE_ONLYTEXT) {
            checkBox.setChecked(false);
            checkBox.setVisibility(GONE);
        }
        else {
            checkBox.setVisibility(View.VISIBLE);
            checkBox.setChecked(false);
        }
        builder.setView(view);
        String m;
        if(message.getFrom().equals(currentUserId))
            m = "Delete message?";
        else
            m = "Delete message from "+otherUser.getName()+"?";
        text.setText(m);
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                databaseManager.deleteMessage(message,currentUserId);
                messages.remove(message);
                messageIds.remove(message.getMessage_id());
                recyclerAdapter.notifyItemRangeRemoved(position,1);
                if(checkBox.isChecked())
                {
                    String messageContent = getMessageContent(message.getContent());
                    String path;
                    if(message.getType()==Message.MESSAGE_TYPE_IMAGE)
                    {
                        if(message.getFrom().equals(currentUserId))
                            path = Util.sentImagesPath+messageContent;
                        else
                            path = Util.imagesPath+messageContent;
                        File file = new File(path);
                        if(file.exists())
                            file.delete();
                    }
                    if(message.getType()==Message.MESSAGE_TYPE_FILE)
                    {
                        if(message.getFrom().equals(currentUserId))
                            path = Util.sentDocumentsPath+messageContent;
                        else
                            path = Util.documentsPath+messageContent;
                        File file = new File(path);
                        if(file.exists())
                            file.delete();
                    }
                }
            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.create().show();
    }

    void messageInfo(final Message message)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this,R.style.AlertDialog);
        View view = getLayoutInflater().inflate(R.layout.message_info,null);

        TextView deliveredTime = view.findViewById(R.id.deliver);
        TextView readTime = view.findViewById(R.id.read);

        if(message.getReceived()!=null)
            deliveredTime.setText(message.getReceived());
        if(message.getSeen()!=null)
            readTime.setText(message.getSeen());

        builder.setView(view);
        builder.create().show();
    }

     private void shareMessage(Message message) {
         try {
             Intent intent = new Intent(this, ShareActivity.class);
             intent.setAction(Intent.ACTION_SEND);
             if (message.getType() == Message.MESSAGE_TYPE_ONLYTEXT) {
                 intent.setType("text/*");
                 intent.putExtra(Intent.EXTRA_TEXT, getMessageContent(message.getContent()));
             }
             if (message.getType() == Message.MESSAGE_TYPE_IMAGE) {
                 intent.setType("image/*");
                 String path;
                 if (message.getFrom().equals(currentUserId))
                     path = Util.sentImagesPath + getMessageContent(message.getContent());
                 else
                     path = Util.imagesPath + getMessageContent(message.getContent());
                 File file = new File(path);
                 intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(this, this.getPackageName() + ".fileProvider", file));
             }
             if (message.getType() == Message.MESSAGE_TYPE_FILE) {
                 intent.setType("application/*");
                 String path;
                 if (message.getFrom().equals(currentUserId))
                     path = Util.sentDocumentsPath + getMessageContent(message.getContent());
                 else
                     path = Util.documentsPath + getMessageContent(message.getContent());
                 File file = new File(path);
                 intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(this, this.getPackageName() + ".fileProvider", file));
             }
             startActivity(intent);
         }catch (Exception e){
             e.printStackTrace();
         }
     }

    void replyToMessage(Message message)
    {
        chatReplyLayout.setVisibility(View.VISIBLE);
        String name;
        if(message.getFrom().equals(currentUserId))
            name = "you";
        else
            name = otherUser.getName();

        replyName.setText(name);
        replyMessage = message;
        if(message.getType()==Message.MESSAGE_TYPE_IMAGE){
            imageThumbnail.setVisibility(View.VISIBLE);
            try {
                replyMessageText.setText("Photo");
                Glide.with(this).load(Util.getThumbnailString(message,currentUserId)).override(LOAD_THUMBNAIL_SIZE).into(imageThumbnail);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        else {
            imageThumbnail.setVisibility(GONE);
            replyMessageText.setText(getMessageContent(message.getContent()));
        }
    }

     void updateRecyclerAdapter(final int position) {
         runOnUiThread(new Runnable() {
             @Override
             public void run() {
                 recyclerAdapter.notifyItemChanged(position);
             }
         });
     }

     void updateMessageRemoved(final int position) {
         runOnUiThread(new Runnable() {
             @Override
             public void run() { recyclerAdapter.notifyItemRangeRemoved(position,1); }});
     }

     //MessageSentCallback override methods

     @Override
     public void onComplete(Message message, boolean success, String error) {
        if(success) {

            final String messageId = message.getMessage_id();
            if(messageIds.contains(messageId))
            {
                int position = messageIds.indexOf(messageId);
                message = databaseManager.getMessage(messageId,message.getTo());
                if(message!=null) {
                    messages.set(position, message);
                    updateRecyclerAdapter(position);
                }else{
                    Log.e("Chat activity","message null - OnComplete");
                }
            }
        }
        else {
            if(messageIds.contains(message.getMessage_id()))
            {
                int position = messageIds.indexOf(message.getMessage_id());
                messages.remove(position);
                messageIds.remove(message.getMessage_id());
                updateMessageRemoved(position);
            }
        }
     }

     public void updateNewSentMessage(final Message message) {
         runOnUiThread(new Runnable() {
             @Override
             public void run() {
                 messageField.setText("");
                 messages.add(message);
                 messageIds.add(message.getMessage_id());
                 recyclerAdapter.notifyDataSetChanged();
                 smoothScroller.setTargetPosition(messages.size()-1);
                 layoutManager.startSmoothScroll(smoothScroller);
             }
         });
     }

     //MessageRetrievedCallback override methods

     @Override
     public void onNewMessage(final Message message) {

         if(message.getFrom().equals(otherUserId) && message.getTo().equals(currentUserId))
         {
             if(userPresent) {
                 databaseManager.setNewMessageCounter(otherUserId);
                 markMessageAsRead(message);
             }else{
                 if(!unSeenMessageIds.contains(message.getMessage_id())) {
                     unSeenMessageIds.add(message.getMessage_id());

                 Util.sendNewMessageNotification(message,databaseManager,this,Util.getNotificationIntent(message,ChatActivity.this));
                 }
             }
             runOnUiThread(new Runnable() {
                 @Override
                 public void run() {
                     if(!messageIds.contains(message.getMessage_id())) {
                         Message m = databaseManager.getMessage(message.getMessage_id(),message.getFrom());
                         if(m!=null) {
                             messages.add(m);
                             messageIds.add(m.getMessage_id());
                             recyclerAdapter.notifyDataSetChanged();
                             smoothScroller.setTargetPosition(messages.size() - 1);
                             layoutManager.startSmoothScroll(smoothScroller);
                         }
                         else{
                             Log.e("message null","ChatActivity - onNewMessage");
                         }
                     }
                 }
             });
         }
         else {
             Util.sendNewMessageNotification(message,databaseManager,this,Util.getNotificationIntent(message,this));
         }
     }

     void markMessageAsRead(Message message){
         String timeStamp = Calendar.getInstance().getTime().toString();
         message.setSeen(timeStamp);
         firebaseHelper.sendMessageSeenStatus(message,timeStamp);
         databaseManager.updateMessageSeenStatus(timeStamp, message.getMessage_id(), otherUserId, null, currentUserId);
     }

     @Override
     public void onUpdateMessageStatus(final String messageId, final String userId) {

        if (userId.equals(otherUserId))
        {
            if(messageIds.contains(messageId))
            {
                int position = messageIds.indexOf(messageId);
                Message message = databaseManager.getMessage(messageId,userId);
                if(message!=null) {
                    messages.set(position, message);
                    updateRecyclerAdapter(position);
                }
                else{
                    Log.e("message null","ChatActivity - onUpdateMessageStatus");
                }
            }
        }
     }

     void closeReplyLayout() {
         runOnUiThread(new Runnable() {
             @Override
             public void run() {
                 chatReplyLayout.setVisibility(GONE);
                 replyMessage = null;
             }
         });
     }
     //ScrollEndCallback override methods

     @Override
     public void scrollEndReached() { getMessages(); }

     //PublicKeyCallback override methods

     @Override
     public void onSuccess(String Base64PublicKey,String number) {
         databaseManager.insertPublicKey(Base64PublicKey,otherUserId,otherUser.getNumber());
         otherUser = databaseManager.getUser(otherUserId);
         otherUserArray = new User[]{otherUser};
     }

     @Override
     public void onCancelled(String error) {}

     //MessageResendCallback override methods

     @Override
     public void newResendMessageCallback(Message message) {
         if(messageIds.contains(message.getMessage_id()))
         {
             message = databaseManager.getMessage(message.getMessage_id(),message.getFrom());
             if(message!=null) {
                 int position = messageIds.indexOf(message.getMessage_id());
                 messages.set(position, message);
                 updateRecyclerAdapter(position);
             }
             else{
                 Log.e("message null","ChatActivity - newResendMessageCallback");
             }
         }
     }

     //Message options callback

     @Override
     public void onOptionsSelected(int option, int position) {
         try {
             Message message = messages.get(position);
             switch (option) {
                 case MESSAGE_COPY: {
                     messageCopy(message,this);
                     return;
                 }
                 case MESSAGE_DELETE: {
                     deleteMessage(message, position);
                     return;
                 }
                 case MESSAGE_INFO: {
                     messageInfo(message);
                     return;
                 }
                 case MESSAGE_REPLY: {
                     replyToMessage(message);
                     return;
                 }
                 case MESSAGE_SHARE:{
                     shareMessage(message);
                 }
             }
         }catch (Exception e){
             e.printStackTrace();
         }
     }

     // user typing callback methods

    void setTypingSubTitle(boolean x){
         String y = null;
         if(x)
             y = "typing...";
        final String finalY = y;
        runOnUiThread(new Runnable() {
             @Override
             public void run() {
                 try {
                     getSupportActionBar().setSubtitle(finalY);
                 }catch (Exception e){
                     e.printStackTrace();
                 }
             }
         });
    }

     @Override
     public void userTypingInSingleChat(String userId) {
         if (userId.equals(otherUserId)){
             setTypingSubTitle(true);
             ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
             executor.schedule(new Runnable() {
                 @Override
                 public void run() {
                     runOnUiThread(new Runnable() {
                         @Override
                         public void run() {
                             setTypingSubTitle(false);
                         }
                     });
                 }
             },Util.timeOfTyping, TimeUnit.SECONDS);
         }
     }

     @Override
     public void userTypingInGroupChat(String userId, String groupId) { }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        if(charSequence.length()==0){
            firebaseHelper.sendTypingNotification(new TypingNotification(currentUserId,null,System.currentTimeMillis(),false),otherUserId,false);
        }
    }
    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

    @Override
    public void afterTextChanged(Editable editable) {}


    static class RecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
         private final String name;
         private ArrayList<Message> messages;
         private ScrollEndCallback scrollEndCallback;
         private String otherUserId;
         private String currentUserId;
         private MessageOptionsCallback messageOptionsCallback;
         private Context context;

         RecyclerAdapter(ArrayList<Message> messages, ScrollEndCallback scrollEndCallback,
                         String currentUserId, String otherUserId, MessageOptionsCallback messageOptionsCallback, Context context, String name) {

             this.messages = messages;
             this.scrollEndCallback = scrollEndCallback;
             this.currentUserId = currentUserId;
             this.otherUserId = otherUserId;
             this.context = context;
             this.messageOptionsCallback = messageOptionsCallback;
             this.name = name;
         }

        static class TextReceivedError extends RecyclerView.ViewHolder{

            TextView time;
            ImageView corner;
            TextReceivedError(View view) {
                super(view);
                time = view.findViewById(R.id.time);
                corner = view.findViewById(R.id.corner);
            }
        }

        static class TextReceived extends RecyclerView.ViewHolder {

            TextView message;
            TextView time;
            SwipeRevealLayout container;
            ImageView corner;
            ImageButton copyButton;
            ImageButton replyButton;
            ImageButton deleteButton;
            RelativeLayout replyLayout;
            TextView replyName;
            TextView replyMessage;
            TextView lengthCorrector;
            LinearLayout bubble;
            ImageView replyThumbnail;
            ImageButton shareButton;

            TextReceived(View view) {
                super(view);
                message = view.findViewById(R.id.message);
                time = view.findViewById(R.id.time);
                container = view.findViewById(R.id.container);
                corner = view.findViewById(R.id.triangle);
                copyButton = view.findViewById(R.id.copy_button);
                replyButton = view.findViewById(R.id.reply_button);
                deleteButton = view.findViewById(R.id.delete_button);
                replyLayout = view.findViewById(R.id.reply_receive_layout);
                replyName = view.findViewById(R.id.name);
                replyMessage = view.findViewById(R.id.reply_text);
                lengthCorrector = view.findViewById(R.id.messag);
                bubble = view.findViewById(R.id.incoming_layout_bubble);
                replyThumbnail = view.findViewById(R.id.thumbnail);
                shareButton = view.findViewById(R.id.share_button);
            }
        }

         static class TextSent extends RecyclerView.ViewHolder {
             TextView message;
             TextView time;
             SwipeRevealLayout container;
             ImageView corner;
             ProgressBar pg;
             ImageView sent;
             ImageView received;
             ImageView seen;
             ImageButton copyButton;
             ImageButton replyButton;
             ImageButton deleteButton;
             ImageButton infoButton;
             RelativeLayout replyLayout;
             TextView replyName;
             TextView replyMessage;
             TextView lengthCorrector;
             LinearLayout bubble;
             ImageView replyThumbnail;
             ImageButton shareButton;

             TextSent(View view) {
                 super(view);
                 message = view.findViewById(R.id.message);
                 time = view.findViewById(R.id.time);
                 container = view.findViewById(R.id.container);
                 sent = view.findViewById(R.id.sent);
                 received = view.findViewById(R.id.received);
                 seen = view.findViewById(R.id.seen);
                 corner = view.findViewById(R.id.triangle);
                 pg = view.findViewById(R.id.waiting);
                 copyButton = view.findViewById(R.id.copy_button);
                 replyButton = view.findViewById(R.id.reply_button);
                 deleteButton = view.findViewById(R.id.delete_button);
                 infoButton = view.findViewById(R.id.info_button);
                 replyLayout = view.findViewById(R.id.reply_send_layout);
                 replyName = view.findViewById(R.id.name);
                 replyMessage = view.findViewById(R.id.reply_text);
                 lengthCorrector = view.findViewById(R.id.messag);
                 bubble = view.findViewById(R.id.outgoing_layout_bubble);
                 replyThumbnail = view.findViewById(R.id.thumbnail);
                 shareButton = view.findViewById(R.id.share_button);
             }
         }

         static class ImageSent extends RecyclerView.ViewHolder {

             SwipeRevealLayout container;
             ImageView corner;
             ProgressBar pg;
             ImageView sent;
             ImageView received;
             ImageView seen;
             ImageButton copyButton;
             ImageButton replyButton;
             ImageButton deleteButton;
             ImageButton infoButton;
             ImageView main;
             TextView time;
             RelativeLayout replyLayout;
             TextView replyName;
             TextView replyMessage;
             ImageView replyThumbnail;
             ImageButton shareButton;

            ImageSent(View view) {
                super(view);
                container = view.findViewById(R.id.container);
                sent = view.findViewById(R.id.sent);
                received = view.findViewById(R.id.received);
                seen = view.findViewById(R.id.seen);
                corner = view.findViewById(R.id.triangle);
                pg = view.findViewById(R.id.waiting);
                copyButton = view.findViewById(R.id.copy_button);
                replyButton = view.findViewById(R.id.reply_button);
                deleteButton = view.findViewById(R.id.delete_button);
                infoButton = view.findViewById(R.id.info_button);
                main = view.findViewById(R.id.main);
                time = view.findViewById(R.id.time);
                replyLayout = view.findViewById(R.id.reply_send_layout);
                replyName = view.findViewById(R.id.name);
                replyMessage = view.findViewById(R.id.reply_text);
                replyThumbnail = view.findViewById(R.id.thumbnail);
                shareButton = view.findViewById(R.id.share_button);
            }
         }

         static class ImageReceived extends RecyclerView.ViewHolder{

             SwipeRevealLayout container;
             ImageView corner;
             ImageButton copyButton;
             ImageButton replyButton;
             ImageButton deleteButton;
             ImageView main;
             TextView time;
             LinearLayout imageDownloadContainer;
             ImageView download;
             ProgressBar downloadProgressBar;
             RelativeLayout replyLayout;
             TextView replyName;
             TextView replyMessage;
             ImageView replyThumbnail;
             ImageButton shareButton;

            ImageReceived(View view) {
                super(view);
                container = view.findViewById(R.id.container);
                corner = view.findViewById(R.id.triangle);
                copyButton = view.findViewById(R.id.copy_button);
                replyButton = view.findViewById(R.id.reply_button);
                deleteButton = view.findViewById(R.id.delete_button);
                main = view.findViewById(R.id.main);
                time = view.findViewById(R.id.time);
                imageDownloadContainer = view.findViewById(R.id.image_overlay);
                download = view.findViewById(R.id.image_download);
                downloadProgressBar = view.findViewById(R.id.image_loading);
                replyLayout = view.findViewById(R.id.reply_receive_layout);
                replyName = view.findViewById(R.id.name);
                replyMessage = view.findViewById(R.id.reply_text);
                replyThumbnail = view.findViewById(R.id.thumbnail);
                shareButton = view.findViewById(R.id.share_button);
            }
         }

         static class FileSent extends RecyclerView.ViewHolder{

             SwipeRevealLayout container;
             ImageView corner;
             ProgressBar pg;
             ImageView sent;
             ImageView received;
             ImageView seen;
             ImageButton copyButton;
             ImageButton replyButton;
             ImageButton deleteButton;
             ImageButton infoButton;
             TextView time;
             TextView fileName;
             RelativeLayout replyLayout;
             TextView replyName;
             TextView replyMessage;
             ImageView replyThumbnail;
             ImageButton shareButton;

            FileSent(View view){
                super(view);
                container = view.findViewById(R.id.container);
                sent = view.findViewById(R.id.sent);
                received = view.findViewById(R.id.received);
                seen = view.findViewById(R.id.seen);
                corner = view.findViewById(R.id.triangle);
                pg = view.findViewById(R.id.waiting);
                copyButton = view.findViewById(R.id.copy_button);
                replyButton = view.findViewById(R.id.reply_button);
                deleteButton = view.findViewById(R.id.delete_button);
                infoButton = view.findViewById(R.id.info_button);
                fileName = view.findViewById(R.id.file_name);
                time = view.findViewById(R.id.time);
                replyLayout = view.findViewById(R.id.reply_send_layout);
                replyName = view.findViewById(R.id.name);
                replyMessage = view.findViewById(R.id.reply_text);
                replyThumbnail = view.findViewById(R.id.thumbnail);
                shareButton = view.findViewById(R.id.share_button);
            }
         }

         static class FileReceived extends RecyclerView.ViewHolder{

             SwipeRevealLayout container;
             ImageView corner;
             ImageButton copyButton;
             ImageButton replyButton;
             ImageButton deleteButton;
             TextView fileName;
             TextView time;
             ImageButton download;
             ProgressBar downloadProgressBar;
             TextView fileType;
             RelativeLayout replyLayout;
             TextView replyName;
             TextView replyMessage;
             ImageView replyThumbnail;
             ImageButton shareButton;

            FileReceived(View view){
                super(view);
                container = view.findViewById(R.id.container);
                corner = view.findViewById(R.id.triangle);
                copyButton = view.findViewById(R.id.copy_button);
                replyButton = view.findViewById(R.id.reply_button);
                deleteButton = view.findViewById(R.id.delete_button);
                fileName = view.findViewById(R.id.file_name);
                time = view.findViewById(R.id.time);
                download = view.findViewById(R.id.download_file);
                downloadProgressBar = view.findViewById(R.id.download_progress);
                fileType = view.findViewById(R.id.file_type);
                replyLayout = view.findViewById(R.id.reply_receive_layout);
                replyName = view.findViewById(R.id.name);
                replyMessage = view.findViewById(R.id.reply_text);
                replyThumbnail = view.findViewById(R.id.thumbnail);
                shareButton = view.findViewById(R.id.share_button);
            }
         }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

            switch (viewType)
            {
                case RECEIVE_TEXT:
                {
                    View itemView = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.receive_msg, parent, false);
                    return new TextReceived(itemView);
                }
                case RECEIVE_ERROR:
                {
                    View itemView = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.list_msg_error, parent, false);
                    return new TextReceivedError(itemView);
                }
                case SEND_TEXT:
                {
                    View itemView = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.send_msg, parent, false);
                    return new TextSent(itemView);
                }
                case SEND_FILE:
                {
                    View itemView = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.send_file, parent, false);
                    return new FileSent(itemView);
                }
                case RECEIVE_FILE:
                {
                    View itemView = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.receive_file, parent, false);
                    return new FileReceived(itemView);
                }
                case SEND_IMAGE:
                {
                    View itemView = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.send_image, parent, false);
                    return new ImageSent(itemView);
                }
                default:
                {
                    View itemView = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.receive_image, parent, false);
                    return new ImageReceived(itemView);
                }
            }
        }

         @Override
         public int getItemViewType(int position) {
             Message message = messages.get(position);
             if(message.getContent()==null)
                 return RECEIVE_ERROR;
             if(message.getType()==Message.MESSAGE_TYPE_ONLYTEXT) {
                 if (message.getFrom().equals(otherUserId)) {
                     return RECEIVE_TEXT;
                 } else
                     return SEND_TEXT;
             }
             else if(message.getType()==Message.MESSAGE_TYPE_IMAGE) {
                 if(message.getFrom().equals(otherUserId))
                     return RECEIVE_IMAGE;
                 else
                     return SEND_IMAGE;
             }
             else if(message.getType()==Message.MESSAGE_TYPE_FILE)
             {
                 if(message.getFrom().equals(otherUserId))
                     return RECEIVE_FILE;
                 else
                     return SEND_FILE;
             }
             else {
                 return -1;
             }
         }

         @SuppressLint("SetTextI18n")
         @Override
        public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int position) {

             final Message message = messages.get(position);
             boolean flag = false;
             if (position != 0) {
                 Message prev = messages.get(position - 1);
                 flag = check(message, prev);
             }

             if(message.getContent()==null)
             {
                 TextReceivedError h = (TextReceivedError) holder;
                 if(flag)
                     h.corner.setVisibility(View.INVISIBLE);
                 else
                     h.corner.setVisibility(View.INVISIBLE);
                 h.time.setText(formatTime(message.getTimeStamp()));
                 return;
             }

             String messageContent;
             String messageReply = null;
             String messageReplyId = null;
             boolean containsImageThumbnail = false;

             try {
                 JSONObject jsonObject = new JSONObject(message.getContent());
                 messageContent = jsonObject.getString(MESSAGE_CONTENT);

                 if(jsonObject.has(MESSAGE_REPLIED))
                 messageReply = jsonObject.getString(MESSAGE_REPLIED);
                 if(jsonObject.has(MESSAGE_REPLIED_ID)) {
                     messageReplyId = jsonObject.getString(MESSAGE_REPLIED_ID);
                     if (messageReplyId.equals(currentUserId))
                         messageReplyId = "You";
                     else
                         messageReplyId = name;
                 }
                 if(jsonObject.has(Util.MESSAGE_CONTAINS_IMAGE_THUMBNAIL))
                     containsImageThumbnail = true;
             } catch (JSONException e) {
                 e.printStackTrace();
                 messageContent = message.getContent();
             }

             if(message.getType()==Message.MESSAGE_TYPE_ONLYTEXT)
             {
                 if (message.getFrom().equals(otherUserId)) {

                     final TextReceived h = (TextReceived) holder;
                     h.message.setText(messageContent);
                     if(messageContent.length()<15){
                         h.lengthCorrector.setText(messageContent);
                     }
                     else{
                         h.lengthCorrector.setText(formatTime(message.getTimeStamp()));
                     }
                     if(messageReply!=null && messageReplyId!=null) {
                         h.replyLayout.setVisibility(View.VISIBLE);
                         if(!containsImageThumbnail) {
                             h.replyMessage.setText(messageReply);
                             h.replyThumbnail.setVisibility(GONE);
                         }
                         else{
                             h.replyMessage.setText("Photo");
                             h.replyThumbnail.setVisibility(View.VISIBLE);
                             Glide.with(context).load(Util.getBytes(messageReply)).override(LOAD_THUMBNAIL_SIZE).into(h.replyThumbnail);
                         }
                         h.replyName.setText(messageReplyId);
                     }
                     else {
                         h.replyLayout.setVisibility(GONE);
                     }
                     h.time.setText(formatTime(message
                             .getTimeStamp()));
                     h.container.close(false);
                     if (flag)
                         h.corner.setVisibility(View.INVISIBLE);
                     else
                         h.corner.setVisibility(View.VISIBLE);
                     h.deleteButton.setOnClickListener(new View.OnClickListener() {
                         @Override
                         public void onClick(View view) {
                             if(h.container.isOpen()) {
                                 messageOptionsCallback.onOptionsSelected(MESSAGE_DELETE, position);
                                 h.container.close(true);
                             }
                         }
                     });
                     h.replyButton.setOnClickListener(new View.OnClickListener() {
                         @Override
                         public void onClick(View view) {
                             if(h.container.isOpen()) {
                                 messageOptionsCallback.onOptionsSelected(MESSAGE_REPLY, position);
                                 h.container.close(true);
                             }
                         }
                     });
                     h.copyButton.setOnClickListener(new View.OnClickListener() {
                         @Override
                         public void onClick(View view) {
                             if(h.container.isOpen()) {
                                 messageOptionsCallback.onOptionsSelected(MESSAGE_COPY, position);
                                 h.container.close(true);
                             }
                         }
                     });
                     h.shareButton.setOnClickListener(new View.OnClickListener() {
                         @Override
                         public void onClick(View view) {
                             if(h.container.isOpen()) {
                                 messageOptionsCallback.onOptionsSelected(MESSAGE_SHARE, position);
                                 h.container.close(true);
                             }
                         }
                     });
                 }
                 if (message.getFrom().equals(currentUserId))
                 {
                     final TextSent h = (TextSent) holder;
                     h.message.setText(messageContent);
                     if(messageContent.length()<15){
                         h.lengthCorrector.setText(messageContent);
                     }
                     else{
                         h.lengthCorrector.setText(formatTime(message.getTimeStamp()));
                     }
                     if(messageReply!=null && messageReplyId!=null) {
                         h.replyLayout.setVisibility(View.VISIBLE);
                         if(!containsImageThumbnail) {
                             h.replyMessage.setText(messageReply);
                             h.replyThumbnail.setVisibility(GONE);
                         }
                         else{
                             h.replyMessage.setText("Photo");
                             h.replyThumbnail.setVisibility(View.VISIBLE);
                             Glide.with(context).load(Util.getBytes(messageReply)).override(LOAD_THUMBNAIL_SIZE).into(h.replyThumbnail);
                         }
                         h.replyName.setText(messageReplyId);
                     }
                     else {
                         h.replyLayout.setVisibility(GONE);
                     }
                     h.time.setText(formatTime(message
                             .getTimeStamp()));
                     if(message.getSeen()!=null)
                     {
                         h.received.setVisibility(GONE);
                         h.sent.setVisibility(GONE);
                         h.pg.setVisibility(GONE);
                         h.seen.setVisibility(View.VISIBLE);
                     }
                     else if(message.getReceived()!=null)
                     {
                         h.seen.setVisibility(GONE);
                         h.sent.setVisibility(GONE);
                         h.pg.setVisibility(GONE);
                         h.received.setVisibility(View.VISIBLE);
                     }
                     else if(message.getSent()!=null)
                     {
                         h.seen.setVisibility(GONE);
                         h.received.setVisibility(GONE);
                         h.pg.setVisibility(GONE);
                         h.sent.setVisibility(View.VISIBLE);
                     }
                     else
                     {
                         h.seen.setVisibility(GONE);
                         h.received.setVisibility(GONE);
                         h.sent.setVisibility(GONE);
                         h.pg.setVisibility(View.VISIBLE);
                     }
                     h.container.close(false);

                     if (flag)
                         h.corner.setVisibility(View.INVISIBLE);
                     else
                         h.corner.setVisibility(View.VISIBLE);
                     h.infoButton.setOnClickListener(new View.OnClickListener() {
                         @Override
                         public void onClick(View view) {
                             if(h.container.isOpen()) {
                                 messageOptionsCallback.onOptionsSelected(MESSAGE_INFO, position);
                                 h.container.close(true);
                             }
                         }
                     });
                     h.deleteButton.setOnClickListener(new View.OnClickListener() {
                         @Override
                         public void onClick(View view) {
                             if(h.container.isOpen()) {
                                 messageOptionsCallback.onOptionsSelected(MESSAGE_DELETE, position);
                                 h.container.close(true);
                             }
                         }
                     });
                     h.replyButton.setOnClickListener(new View.OnClickListener() {
                         @Override
                         public void onClick(View view) {
                             if(h.container.isOpen()) {
                                 messageOptionsCallback.onOptionsSelected(MESSAGE_REPLY, position);
                                 h.container.close(true);
                             }
                         }
                     });
                     h.copyButton.setOnClickListener(new View.OnClickListener() {
                         @Override
                         public void onClick(View view) {
                             if(h.container.isOpen()) {
                                 messageOptionsCallback.onOptionsSelected(MESSAGE_COPY, position);
                                 h.container.close(true);
                             }
                         }
                     });
                     h.shareButton.setOnClickListener(new View.OnClickListener() {
                         @Override
                         public void onClick(View view) {
                             if(h.container.isOpen()) {
                                 messageOptionsCallback.onOptionsSelected(MESSAGE_SHARE, position);
                                 h.container.close(true);
                             }
                         }
                     });
                 }
             }
            else if(message.getType()==Message.MESSAGE_TYPE_IMAGE)
            {
               if(message.getFrom().equals(otherUserId))
               {
                   final ImageReceived h = (ImageReceived) holder;
                   h.container.close(false);
                   if(messageReply!=null && messageReplyId!=null) {
                       h.replyLayout.setVisibility(View.VISIBLE);
                       if(!containsImageThumbnail) {
                           h.replyMessage.setText(messageReply);
                           h.replyThumbnail.setVisibility(GONE);
                       }
                       else{
                           h.replyMessage.setText("Photo");
                           h.replyThumbnail.setVisibility(View.VISIBLE);
                           Glide.with(context).load(Util.getBytes(messageReply)).override(LOAD_THUMBNAIL_SIZE).into(h.replyThumbnail);
                       }
                       h.replyName.setText(messageReplyId);
                   }
                   else {
                       h.replyLayout.setVisibility(GONE);
                   }
                   h.time.setText(formatTime(message.getTimeStamp()));
                   if (flag)
                       h.corner.setVisibility(View.INVISIBLE);
                   else
                       h.corner.setVisibility(View.VISIBLE);

                   h.deleteButton.setOnClickListener(new View.OnClickListener() {
                       @Override
                       public void onClick(View view) {
                           if(h.container.isOpen()) {
                               messageOptionsCallback.onOptionsSelected(MESSAGE_DELETE, position);
                               h.container.close(true);
                           }
                       }
                   });
                   h.replyButton.setOnClickListener(new View.OnClickListener() {
                       @Override
                       public void onClick(View view) {
                           if(h.container.isOpen()) {
                               messageOptionsCallback.onOptionsSelected(MESSAGE_REPLY, position);
                               h.container.close(true);
                           }
                       }
                   });
                   h.copyButton.setOnClickListener(new View.OnClickListener() {
                       @Override
                       public void onClick(View view) {
                           if(h.container.isOpen()) {
                               messageOptionsCallback.onOptionsSelected(MESSAGE_COPY, position);
                               h.container.close(true);
                           }
                       }
                   });
                   h.shareButton.setOnClickListener(new View.OnClickListener() {
                       @Override
                       public void onClick(View view) {
                           if(h.container.isOpen()) {
                               messageOptionsCallback.onOptionsSelected(MESSAGE_SHARE, position);
                               h.container.close(true);
                           }
                       }
                   });
                   String path = Util.imagesPath+messageContent;
                   File file = new File(path);
                   if(file.exists())
                   {
                       h.imageDownloadContainer.setVisibility(GONE);
                       Glide.with(context).load(file).into(h.main);
                       h.main.setOnClickListener(new View.OnClickListener() {
                           @Override
                           public void onClick(View view) {
                               Intent intent = new Intent(context,Images.class);
                               intent.putExtra(Util.userId,otherUserId);
                               intent.putExtra(Util.messageId,message.getMessage_id());
                               context.startActivity(intent);
                           }
                       });
                   }
                   else
                   {
                       if(message.getFilePath()!=null)
                       {
                           h.imageDownloadContainer.setVisibility(View.VISIBLE);
                           h.download.setVisibility(View.VISIBLE);
                           h.downloadProgressBar.setVisibility(GONE);
                           Glide.with(context).load(R.drawable.transparent).into(h.main);
                           h.download.setOnClickListener(new View.OnClickListener() {
                               @Override
                               public void onClick(View view) {
                                   h.download.setVisibility(GONE);
                                   h.downloadProgressBar.setVisibility(View.VISIBLE);
                                   Intent intent = new Intent(context, Downloader.class);
                                   intent.putExtra(Util.id, message.getMessage_id());
                                   intent.putExtra(Util.toUserId, otherUserId);
                                   intent.putExtra(Util.userId, currentUserId);
                                   if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                       context.startForegroundService(intent);
                                   } else
                                       context.startService(intent);
                               }
                           });
                       }
                       else {
                           h.imageDownloadContainer.setVisibility(GONE);
                           h.main.setOnClickListener(new View.OnClickListener() {
                               @Override
                               public void onClick(View view) {}
                           });
                           Glide.with(context).load(R.drawable.transparent).into(h.main);
                       }
                   }
               }
               else {
                   final ImageSent h = (ImageSent) holder;

                   if(messageReply!=null && messageReplyId!=null) {
                       h.replyLayout.setVisibility(View.VISIBLE);
                       if(!containsImageThumbnail) {
                           h.replyMessage.setText(messageReply);
                           h.replyThumbnail.setVisibility(GONE);
                       }
                       else{
                           h.replyMessage.setText("Photo");
                           h.replyThumbnail.setVisibility(View.VISIBLE);
                           Glide.with(context).load(Util.getBytes(messageReply)).override(LOAD_THUMBNAIL_SIZE).into(h.replyThumbnail);
                       }
                       h.replyName.setText(messageReplyId);
                   }
                   else {
                       h.replyLayout.setVisibility(GONE);
                   }
                   h.container.close(false);
                   h.time.setText(formatTime(message.getTimeStamp()));
                   if (flag)
                       h.corner.setVisibility(View.INVISIBLE);
                   else
                       h.corner.setVisibility(View.VISIBLE);

                   h.infoButton.setOnClickListener(new View.OnClickListener() {
                       @Override
                       public void onClick(View view) {
                           if(h.container.isOpen()) {
                               messageOptionsCallback.onOptionsSelected(MESSAGE_INFO, position);
                               h.container.close(true);
                           }
                       }
                   });
                   h.deleteButton.setOnClickListener(new View.OnClickListener() {
                       @Override
                       public void onClick(View view) {
                           if(h.container.isOpen()) {
                               messageOptionsCallback.onOptionsSelected(MESSAGE_DELETE, position);
                               h.container.close(true);
                           }
                       }
                   });
                   h.replyButton.setOnClickListener(new View.OnClickListener() {
                       @Override
                       public void onClick(View view) {
                           if(h.container.isOpen()) {
                               messageOptionsCallback.onOptionsSelected(MESSAGE_REPLY, position);
                               h.container.close(true);
                           }
                       }
                   });
                   h.copyButton.setOnClickListener(new View.OnClickListener() {
                       @Override
                       public void onClick(View view) {
                           if(h.container.isOpen()) {
                               messageOptionsCallback.onOptionsSelected(MESSAGE_COPY, position);
                               h.container.close(true);
                           }
                       }
                   });
                   h.shareButton.setOnClickListener(new View.OnClickListener() {
                       @Override
                       public void onClick(View view) {
                           if(h.container.isOpen()) {
                               messageOptionsCallback.onOptionsSelected(MESSAGE_SHARE, position);
                               h.container.close(true);
                           }
                       }
                   });
                   String path = Util.sentImagesPath+messageContent;
                   File file = new File(path);
                   if (file.exists()) {
                       Glide.with(context).load(file).fitCenter().into(h.main);
                       h.main.setOnClickListener(new View.OnClickListener() {
                           @Override
                           public void onClick(View view) {
                               Intent intent = new Intent(context, Images.class);
                               intent.putExtra(Util.userId, otherUserId);
                               intent.putExtra(Util.messageId, message.getMessage_id());
                               context.startActivity(intent);
                           }
                       });
                   }
                   else
                   {
                       Glide.with(context).load(R.drawable.transparent).fitCenter().into(h.main);
                       h.main.setOnClickListener(new View.OnClickListener() {
                           @Override
                           public void onClick(View view) {}
                       });
                   }
                   if(message.getSeen()!=null)
                   {
                       h.received.setVisibility(GONE);
                       h.sent.setVisibility(GONE);
                       h.pg.setVisibility(GONE);
                       h.seen.setVisibility(View.VISIBLE);
                   }
                   else if(message.getReceived()!=null)
                   {
                       h.seen.setVisibility(GONE);
                       h.sent.setVisibility(GONE);
                       h.pg.setVisibility(GONE);
                       h.received.setVisibility(View.VISIBLE);
                   }
                   else if(message.getSent()!=null)
                   {
                       h.seen.setVisibility(GONE);
                       h.received.setVisibility(GONE);
                       h.pg.setVisibility(GONE);
                       h.sent.setVisibility(View.VISIBLE);
                   }
                   else
                   {
                       h.seen.setVisibility(GONE);
                       h.received.setVisibility(GONE);
                       h.sent.setVisibility(GONE);
                       h.pg.setVisibility(View.VISIBLE);
                   }
               }
            }
            else
            {
                if(message.getFrom().equals(otherUserId))
                {
                    final FileReceived h = (FileReceived) holder;

                    if(messageReply!=null && messageReplyId!=null) {
                        h.replyLayout.setVisibility(View.VISIBLE);
                        if(!containsImageThumbnail) {
                            h.replyMessage.setText(messageReply);
                            h.replyThumbnail.setVisibility(GONE);
                        }
                        else{
                            h.replyMessage.setText("Photo");
                            h.replyThumbnail.setVisibility(View.VISIBLE);
                            Glide.with(context).load(Util.getBytes(messageReply)).override(LOAD_THUMBNAIL_SIZE).into(h.replyThumbnail);
                        }
                        h.replyName.setText(messageReplyId);
                    }
                    else {
                        h.replyLayout.setVisibility(GONE);
                    }
                    h.container.close(false);

                    h.time.setText(formatTime(message.getTimeStamp()));
                    if (flag)
                        h.corner.setVisibility(View.INVISIBLE);
                    else
                        h.corner.setVisibility(View.VISIBLE);

                    h.deleteButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if(h.container.isOpen()) {
                                messageOptionsCallback.onOptionsSelected(MESSAGE_DELETE, position);
                                h.container.close(true);
                            }
                        }
                    });
                    h.replyButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if(h.container.isOpen()) {
                                messageOptionsCallback.onOptionsSelected(MESSAGE_REPLY, position);
                                h.container.close(true);
                            }
                        }
                    });
                    h.copyButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if(h.container.isOpen()) {
                                messageOptionsCallback.onOptionsSelected(MESSAGE_COPY, position);
                                h.container.close(true);
                            }
                        }
                    });
                    h.shareButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if(h.container.isOpen()) {
                                messageOptionsCallback.onOptionsSelected(MESSAGE_SHARE, position);
                                h.container.close(true);
                            }
                        }
                    });
                    String path = Util.documentsPath+messageContent;
                    final File file = new File(path);
                    if(file.exists())
                    {
                        h.download.setVisibility(GONE);
                        h.downloadProgressBar.setVisibility(GONE);
                        String fileName = messageContent;
                        h.fileName.setText(formatName(fileName));
                        fileName = fileName.substring(fileName.lastIndexOf('.'));
                        h.fileType.setText(fileName);
                        final String finalFileName = fileName;
                        h.fileName.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                String mimeType = mimeTypeMap.getMimeTypeFromExtension(finalFileName);
                                intent.setDataAndType(FileProvider.getUriForFile(context,context.getApplicationContext().getPackageName()+".fileProvider"
                                        ,file),mimeType);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                try {
                                    context.startActivity(intent);
                                }catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                    else
                    {
                        String fileName = messageContent;
                        h.fileName.setText(formatName(fileName));
                        fileName = fileName.substring(fileName.lastIndexOf('.'));
                        h.fileType.setText(formatName(fileName));
                        if(message.getFilePath()!=null) {
                            h.download.setVisibility(View.VISIBLE);
                            h.downloadProgressBar.setVisibility(GONE);
                            h.download.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    h.download.setVisibility(GONE);
                                    h.downloadProgressBar.setVisibility(View.VISIBLE);
                                    Intent intent = new Intent(context, Downloader.class);
                                    intent.putExtra(Util.id, message.getMessage_id());
                                    intent.putExtra(Util.toUserId, otherUserId);
                                    intent.putExtra(Util.userId, currentUserId);
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        context.startForegroundService(intent);
                                    } else
                                        context.startService(intent);
                                }
                            });
                        }
                    }
                }
                else {
                    final FileSent h = (FileSent) holder;
                    if(messageReply!=null && messageReplyId!=null) {
                        h.replyLayout.setVisibility(View.VISIBLE);
                        if(!containsImageThumbnail) {
                            h.replyMessage.setText(messageReply);
                            h.replyThumbnail.setVisibility(GONE);
                        }
                        else{
                            h.replyMessage.setText("Photo");
                            h.replyThumbnail.setVisibility(View.VISIBLE);
                            Glide.with(context).load(Util.getBytes(messageReply)).override(LOAD_THUMBNAIL_SIZE).into(h.replyThumbnail);
                        }
                        h.replyName.setText(messageReplyId);
                    }
                    h.container.close(false);
                    h.time.setText(formatTime(message.getTimeStamp()));
                    if (flag)
                        h.corner.setVisibility(View.INVISIBLE);
                    else
                        h.corner.setVisibility(View.VISIBLE);

                    h.infoButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if(h.container.isOpen()) {
                                messageOptionsCallback.onOptionsSelected(MESSAGE_INFO, position);
                                h.container.close(true);
                            }
                        }
                    });
                    h.deleteButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if(h.container.isOpen()) {
                                messageOptionsCallback.onOptionsSelected(MESSAGE_DELETE, position);
                                h.container.close(true);
                            }
                        }
                    });
                    h.replyButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if(h.container.isOpen()) {
                                messageOptionsCallback.onOptionsSelected(MESSAGE_REPLY, position);
                                h.container.close(true);
                            }
                        }
                    });
                    h.copyButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if(h.container.isOpen()) {
                                messageOptionsCallback.onOptionsSelected(MESSAGE_COPY, position);
                                h.container.close(true);
                            }
                        }
                    });
                    h.shareButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if(h.container.isOpen()) {
                                messageOptionsCallback.onOptionsSelected(MESSAGE_SHARE, position);
                                h.container.close(true);
                            }
                        }
                    });

                    String fileName = messageContent;
                    h.fileName.setText(formatName(fileName));
                    fileName = fileName.substring(fileName.lastIndexOf('.'));
                    final String finalFileName = fileName;
                    final File file = new File(Util.sentDocumentsPath+messageContent);
                    h.fileName.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if(!file.exists()) {
                                Toast.makeText(context,"File appears to be deleted from storage",Toast.LENGTH_SHORT).show();
                                return;
                            }
                            MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            String mimeType = mimeTypeMap.getMimeTypeFromExtension(finalFileName);
                            intent.setDataAndType(FileProvider.getUriForFile(context,context.getPackageName()+".fileProvider",file),mimeType);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            try {
                                context.startActivity(intent);
                            }catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    if(message.getSeen()!=null)
                    {
                        h.received.setVisibility(GONE);
                        h.sent.setVisibility(GONE);
                        h.pg.setVisibility(GONE);
                        h.seen.setVisibility(View.VISIBLE);
                    }
                    else if(message.getReceived()!=null)
                    {
                        h.seen.setVisibility(GONE);
                        h.sent.setVisibility(GONE);
                        h.pg.setVisibility(GONE);
                        h.received.setVisibility(View.VISIBLE);
                    }
                    else if(message.getSent()!=null)
                    {
                        h.seen.setVisibility(GONE);
                        h.received.setVisibility(GONE);
                        h.pg.setVisibility(GONE);
                        h.sent.setVisibility(View.VISIBLE);
                    }
                    else
                    {
                        h.seen.setVisibility(GONE);
                        h.received.setVisibility(GONE);
                        h.sent.setVisibility(GONE);
                        h.pg.setVisibility(View.VISIBLE);
                    }
                }
            }
             if(position==0&&messages.size()>=Util.MESSAGE_CACHE) {
                 scrollEndCallback.scrollEndReached();
             }
        }

        private String formatName(String name){
             if(name.length()<20)
                 return name;
             else {
                 return name.substring(0,16)+"...";
             }
        }

         @Override
         public long getItemId(int position) {
             return messages.get(position).getId();
         }

         private boolean check(@NonNull Message message, @NonNull Message prev) {
             return message.getFrom().equals(prev.getFrom());
         }

         private String formatTime(@NonNull String received) {
            received =received.substring(4,16);
            return received;
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }
    }
}