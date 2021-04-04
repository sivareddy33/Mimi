package com.leagueofshadows.enc;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.widget.Toast;

import com.leagueofshadows.enc.Items.Group;
import com.leagueofshadows.enc.Items.Message;
import com.leagueofshadows.enc.Items.User;
import com.leagueofshadows.enc.storage.DatabaseManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class Util {

    static final int MESSAGE_CACHE = 1000;
    static final String TOKEN_SENT = "TOKEN_SENT";

    private static final String originalPath = Environment.getExternalStorageDirectory().getPath()+"/Mimi/";
    public static final String imagesPath = Environment.getExternalStorageDirectory().getPath()+"/Mimi/Images/";
    static final String sentImagesPath = Environment.getExternalStorageDirectory().getPath()+"/Mimi/Images/sent/";
    public static final String documentsPath = Environment.getExternalStorageDirectory().getPath()+"/Mimi/Documents/";
    static final String sentDocumentsPath = Environment.getExternalStorageDirectory().getPath()+"/Mimi/Documents/sent/";
    public static final String privatePath = Environment.getExternalStorageDirectory().getPath()+"/Mimi/private/";

    public static final String preferences = "preferences";
    static final String MESSAGE_CONTAINS_IMAGE_THUMBNAIL = "M_TB";
    public static String type = "type";
    public static String path = "path";
    public static String messageId = "messageId";
    public static String content = "content";
    public static String cipherText = "cipherText";
    public static String messageType = "messageType";
    public static String admins = "admins";
    public static String groupActive = "groupActive";
    static long timeOfTyping = 5;
    static String camera = "camera";

    public static String ServiceNotificationChannelID = "ServiceNotificationChannelID";
    private static String NewMessageNotificationChannelID = "NewMessageNotificationChannelID";
    private static String ServiceNotificationChannelTitle = "Service notifications channel";
    private static String NewMessageNotificationChannelTitle = "New Message notifications";

    static String CheckMessageEncrypted = "CheckMessageEncrypted";

    public static String key = "API_KEY";
    public static String uri = "uri";
    public static String timeStamp = "timeStamp";
    public static String fileName = "fileName";
    public static String id = "id";
    public static String toUserId = "toUserId";
    public static String name = "name";
    public static String number = "number";
    static String password = "password";
    public static final String CheckMessageIV = "CheckMessageIV";
    public static String userId = "userId";

    static final String PublicKeyString = "PublicKeyString";

    static final String  CheckMessage = "CheckMessage";
    public static final String base64EncodedPublicKey = "base64EncodedPublicKey";

    static final int FILE_ATTACHMENT_REQUEST = 1;
    static final int IMAGE_ATTACHMENT_REQUEST = 2;
    static final int OPEN_CAMERA_REQUEST = 3;
    static final int IMAGE_SELECTED = 4;

    static  final int RECEIVE_TEXT = 0;
    static  final int RECEIVE_IMAGE = 1;
    static  final int RECEIVE_FILE = 2;
    static  final int SEND_TEXT = 3;
    static  final int SEND_IMAGE = 4;
    static  final int SEND_FILE = 5;
    static final int RECEIVE_ERROR = 6;

    static final int MESSAGE_INFO = 1;
    static final int MESSAGE_DELETE = 2;
    static final int MESSAGE_COPY = 3;
    static final int MESSAGE_REPLY = 4;
    static final int MESSAGE_SHARE = 5;

    static final String MESSAGE_CONTENT = "M_C";
    static final String MESSAGE_REPLIED = "M_R";
    static final String MESSAGE_REPLIED_ID = "M_I";

    private static final int THUMBNAIL_SIZE = 64;
    static final int LOAD_THUMBNAIL_SIZE = 128;

    public static final int BACKGROUND_NOTIFICATION_ID = 15478;

    static String getMessageContent(String messageContent)
    {
        if(messageContent!=null) {
            try {
                JSONObject jsonObject = new JSONObject(messageContent);
                return jsonObject.getString(MESSAGE_CONTENT);
            } catch (Exception e) {
                e.printStackTrace();
                return "";
            }
        }else {
            return "";
        }
    }

    private static String getBase64String(byte[] array){
        return Base64.encodeToString(array,Base64.DEFAULT);
    }

    static byte[] getBytes(String Base64String){
        return Base64.decode(Base64String,Base64.DEFAULT);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    static void checkPath(int x) {
        File file = new File(Util.originalPath);
        if(!file.exists())
            file.mkdir();

        file = new File(Util.privatePath);
        if(!file.exists())
            file.mkdir();

        if(x == IMAGE_ATTACHMENT_REQUEST) {
            file = new File(Util.imagesPath);
            if(!file.exists())
                file.mkdir();
            file = new File(Util.sentImagesPath);
            if(!file.exists())
                file.mkdir();
        }
        if(x==FILE_ATTACHMENT_REQUEST) {
            file = new File(Util.documentsPath);
            if(!file.exists())
                file.mkdir();
            file = new File(Util.sentDocumentsPath);
            if(!file.exists())
                file.mkdir();
        }
    }

    static String getFileName(Uri uri, @NonNull Context context) {
        Cursor cursor = context.getContentResolver().query(uri,null,null,null,null);
        int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        cursor.moveToFirst();
        String name = cursor.getString(index);
        cursor.close();
        return name;
    }

    static int getCompressionFactor(int byteCount) {

        final int byteUpperLimit = 3145728;
        final int byteLowerLimit = 307200;
        final int upperCompression = 10;
        final int lowerCompression = 80;
        if(byteCount>byteUpperLimit)
            return upperCompression;
        else if(byteCount<byteLowerLimit)
            return lowerCompression;
        else {
            int factor = (byteUpperLimit - byteLowerLimit)/(lowerCompression-upperCompression);
            return 80 - (byteCount-byteLowerLimit)/factor;
        }
    }

    static String prepareMessage(String messageContent, Message replyMessage, String currentUserId)
    {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(MESSAGE_CONTENT,messageContent);
            if(replyMessage!=null) {
                if (replyMessage.getType() == Message.MESSAGE_TYPE_IMAGE) {
                    jsonObject.put(MESSAGE_REPLIED_ID, replyMessage.getFrom());
                    String base64Bitmap = getBase64String(getThumbnailString(replyMessage,currentUserId));
                    jsonObject.put(MESSAGE_REPLIED,base64Bitmap);
                    jsonObject.put(MESSAGE_CONTAINS_IMAGE_THUMBNAIL,1);
                }
                else {
                    jsonObject.put(MESSAGE_REPLIED, getMessageContent(replyMessage.getContent()));
                    jsonObject.put(MESSAGE_REPLIED_ID, replyMessage.getFrom());
                }
            }
        } catch (JSONException | FileNotFoundException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }

    static byte[] getThumbnailString(@NonNull Message replyMessage, String currentUserId) throws FileNotFoundException {

        String path;
        if(replyMessage.getFrom().equals(currentUserId))
            path = Util.sentImagesPath+getMessageContent(replyMessage.getContent());
        else
            path = Util.imagesPath+getMessageContent(replyMessage.getContent());

        FileInputStream fileInputStream = new FileInputStream(path);
        Bitmap bitmap = BitmapFactory.decodeStream(fileInputStream);

        int height = bitmap.getHeight();
        int width = bitmap.getWidth();

        if(height>width){
            width =  (THUMBNAIL_SIZE*width)/height;
            height = THUMBNAIL_SIZE;

        }else {
            height = (THUMBNAIL_SIZE*height)/width;
            width = THUMBNAIL_SIZE;
        }

        bitmap = Bitmap.createScaledBitmap(bitmap,width,height,false);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG,100,byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    static void messageCopy(Message message, Context context) {
        try {
            ClipboardManager clipboardManager = (ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData = ClipData.newPlainText("message text",getMessageContent(message.getContent()));
            assert clipboardManager != null;
            clipboardManager.setPrimaryClip(clipData);
            Toast.makeText(context,"text copied to clipboard",Toast.LENGTH_SHORT).show();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendNewMessageNotification(final Message message, final DatabaseManager databaseManager, final Context context, final Intent notificationIntent){
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try{
                    String text;
                    int notificationId;
                    User user = databaseManager.getUser(message.getFrom());
                    if(message.getIsGroupMessage()==Message.MESSAGE_TYPE_GROUP_MESSAGE) {
                        Group group = databaseManager.getGroup(message.getTo());
                        text = "New message from " + user.getName() + " in group - "+group.getName();
                        notificationId = group.getId().hashCode();
                    }else {
                        text = "New message from " + user.getName();
                        notificationId = user.getId().hashCode();
                    }
                    Util.createMessageNotificationChannel(context);
                    Util.showNewMessageNotification(context,text,notificationId,notificationIntent);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    public static void showNewMessageNotification(Context context,String notificationText, int notificationId,Intent notificationIntent){

        PendingIntent pendingIntent = PendingIntent.getActivity(context,1,notificationIntent,0);
        Notification notification = new NotificationCompat.Builder(context,Util.NewMessageNotificationChannelID).setContentTitle(context.getString(R.string.app_name))
                .setContentText(notificationText)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentIntent(pendingIntent)
                .setAutoCancel(false)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .build();
        NotificationManagerCompat.from(context).notify(notificationId,notification);
    }

    public static void createMessageNotificationChannel(Context context){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel serviceChannel = new NotificationChannel(Util.NewMessageNotificationChannelID,Util.NewMessageNotificationChannelTitle, NotificationManager.IMPORTANCE_HIGH);
            //serviceChannel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(serviceChannel);

        }
    }
    public static void createServiceNotificationChannel(Context context){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel serviceChannel = new NotificationChannel(ServiceNotificationChannelID,ServiceNotificationChannelTitle, NotificationManager.IMPORTANCE_LOW);
            //serviceChannel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(serviceChannel);

        }
    }

    static void clearNewMessageNotification(int id,Context context){
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
        notificationManagerCompat.cancel(id);
    }

    public static Intent getNotificationIntent(Message message,Context context) {
        Intent intent;
        if(message.getIsGroupMessage()==Message.MESSAGE_TYPE_SINGLE_USER) {
            intent = new Intent(context, ChatActivity.class);
            intent.putExtra(userId,message.getFrom());
        }
        else {
            intent = new Intent(context, GroupChatActivity.class);
            intent.putExtra(userId,message.getTo());
        }
        return intent;
    }
}
