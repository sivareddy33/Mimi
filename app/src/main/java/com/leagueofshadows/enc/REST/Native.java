package com.leagueofshadows.enc.REST;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.leagueofshadows.enc.Items.EncryptedMessage;
import com.leagueofshadows.enc.Items.User;
import com.leagueofshadows.enc.Util;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;

import static com.leagueofshadows.enc.FirebaseHelper.Users;

public class Native {

    public static final String USER_ID = "USER_ID";
    private static final String RESEND_MESSAGE = "RESEND_MESSAGE";
    public static final String TEMP_USER_ID = "TEMP_USER_ID";
    private static final String MESSAGE_ID = "MESSAGE_ID";
    private static final String NEW_MESSAGE = "NEW_MESSAGE";
    public static final String NEW_GROUP = "NEW_GROUP";
    public static final String GROUP_DELETE = "GROUP_DELETE";
    public static final String NOTIFICATION_TEXT = "N_T";
    public static final String GROUP_UPDATE = "GROUP_UPDATE";

    private Context context;
    private DatabaseReference databaseReference;
    private final String TOKEN = "TOKEN";

    public Native(Context context) {
        this.context = context;
        databaseReference = FirebaseDatabase.getInstance().getReference().child(Users);
    }

    public void sendNewMessageNotification(final String userId, final String groupId)
    {
        databaseReference.child(userId).child(TOKEN).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    String token = (String) dataSnapshot.getValue();
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(NEW_MESSAGE, NEW_MESSAGE);
                    if(groupId!=null)
                        jsonObject.put(USER_ID,groupId);
                    update(jsonObject,token);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    public void sendResendMessageNotification(final EncryptedMessage message)
    {
        final String userId = message.getFrom();
        databaseReference.child(userId).child(TOKEN).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    String token = (String) dataSnapshot.getValue();
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(RESEND_MESSAGE, message.getId());
                    jsonObject.put(USER_ID, userId);
                    jsonObject.put(MESSAGE_ID,message.getId());
                    jsonObject.put(TEMP_USER_ID,message.getTo());
                    update(jsonObject, token);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    private void update(final JSONObject j, final String token) {

        String url = "https://fcm.googleapis.com/fcm/send";
        try {
            JSONObject f = new JSONObject();
            f.put("data", j);
            f.put("to", token);
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST,url, f, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    //Log.e("response",response.toString());
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e("error",error.toString());
                }
            }){
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "key=" + getKey());
                    //headers.put("Content-Type","application/json");
                    return headers;
                }
            };
            VolleyHelper.getInstance(context).addToRequestQueue(jsonObjectRequest);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getKey() {
        //TODO : more secure management of api key
        return Util.key;
    }

    public void updateToken(String s) {
        String userId = context.getSharedPreferences(Util.preferences,Context.MODE_PRIVATE).getString(Util.userId,null);
        if(userId!=null) {
            databaseReference.child(userId).child(TOKEN).setValue(s);
        }
    }

    public void sendNewGroupNotification(ArrayList<User> users, final User currentUser,String groupName)
    {
        final String text = currentUser.getName()+" has added you to group - \""+groupName+"\"";
        for (User u:users) {
            if (!u.getId().equals(currentUser.getId())) {
                DatabaseReference dr = databaseReference.child(u.getId()).child(TOKEN);
                dr.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        String token = (String) dataSnapshot.getValue();

                        try {
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put(NEW_GROUP, text);
                            update(jsonObject, token);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                    }
                });
            }
        }
    }

    public void sendGroupDeleteNotification(String groupId,ArrayList<User> users,String text,String currentUserId){
        try{
            final JSONObject jsonObject = new JSONObject();
            jsonObject.put(NOTIFICATION_TEXT,text);
            jsonObject.put(GROUP_DELETE,groupId);
            for (User u:users) {
                if(!u.getId().equals(currentUserId)){
                    DatabaseReference dr = databaseReference.child(u.getId()).child(TOKEN);
                    dr.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            update(jsonObject,(String) dataSnapshot.getValue());
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {}
                    });
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void sendGroupUpdatedNotification(String groupId,ArrayList<User> users, User currentUser) {
        try {
            final JSONObject jsonObject = new JSONObject();
            jsonObject.put(GROUP_UPDATE, groupId);
            for (User u :users) {
                if(!currentUser.getId().equals(u.getId())) {
                    DatabaseReference dr = databaseReference.child(u.getId()).child(TOKEN);
                    dr.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            update(jsonObject, (String) dataSnapshot.getValue());
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                        }
                    });
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
