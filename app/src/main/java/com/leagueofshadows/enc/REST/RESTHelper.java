package com.leagueofshadows.enc.REST;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.leagueofshadows.enc.Items.EncryptedMessage;
import com.leagueofshadows.enc.Items.Message;
import com.leagueofshadows.enc.Util;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import androidx.annotation.NonNull;
import static com.android.volley.Request.Method.POST;
import static com.leagueofshadows.enc.FirebaseReceiver.RECEIVED_STATUS;
import static com.leagueofshadows.enc.FirebaseReceiver.SEEN_STATUS;
@Deprecated
public class RESTHelper {


    private static final String TOKEN_UPDATE_ENDPOINT = "https://loschat.000webhostapp.com/update_users.php";
    private static final String SEND_NOTIFICATION_ENDPOINT = "https://loschat.000webhostapp.com/send_message.php";
    private static final String SEND_STATUS_ENDPOINT = "https://loschat.000webhostapp.com/message_status.php";
    private static final String RESEND_MESSAGE_ENDPOINT = "https://loschat.000webhostapp.com/message_status.php";

    public static final String USER_ID = "USER_ID";
    public static final String RESEND_MESSAGE = "RESEND_MESSAGE";
    public static final String TEMP_USER_ID = "TEMP_USER_ID";
    public static final String MESSAGE_ID = "MESSAGE_ID";
    private static final String NEW_MESSAGE = "NEW_MESSAGE";
    private String userId;

    private Response.Listener<String> responseListener;
    private Response.ErrorListener errorListener;

    private Context context;
    private String logKey = "logKey";
    private String TOKEN = "TOKEN";
    private String accessToken;
    private SharedPreferences sp;

    @Deprecated
    public RESTHelper(Context context) {
        this.context = context;

        sp = context.getSharedPreferences(Util.preferences,Context.MODE_PRIVATE);
        userId = sp.getString(Util.userId,null);
        accessToken = sp.getString(Util.accessToken,null);

         responseListener = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.e(logKey,response);
            }
        };
        errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(logKey,error.toString());
            }
        };
    }


    public void test(String logKey,final Map<String, String> params, final String endpoint, Response.Listener<String> listener, Response.ErrorListener error)
    {
        this.logKey = logKey;
        if(listener==null)
            listener = responseListener;
        if(error == null)
            error = errorListener;

        StringRequest stringRequest = new StringRequest(POST, endpoint,listener,error){
            @Override
            protected Map<String, String> getParams() {
                return params;
            }
        };
        VolleyHelper.getInstance(context).addToRequestQueue(stringRequest);
    }


    public void sendResendMessage(@NonNull EncryptedMessage encryptedMessage)
    {
        HashMap<String,String> params = new HashMap<>();
        params.put(USER_ID,encryptedMessage.getFrom());
        params.put(RESEND_MESSAGE, encryptedMessage.getId());
        params.put(TEMP_USER_ID,encryptedMessage.getTo());
        test("Resend notification",params,RESEND_MESSAGE_ENDPOINT,null,null);
    }

    public void sendReceivedStatus(@NonNull EncryptedMessage e)
    {
        Map<String,String> params = new HashMap<>();
        params.put(MESSAGE_ID,e.getId());
        String timeStamp = Calendar.getInstance().getTime().toString();
        params.put(RECEIVED_STATUS,timeStamp);
        params.put(USER_ID,e.getFrom());
        params.put(TEMP_USER_ID,e.getTo());
        test("messageReceivedStatus "+e.getId(),params, RESTHelper.SEND_STATUS_ENDPOINT,null,null);
    }

    public void sendSeenStatus(@NonNull Message e)
    {

        String timeStamp = Calendar.getInstance().getTime().toString();
        HashMap<String, String> params = new HashMap<>();
        params.put(SEEN_STATUS, timeStamp);
        params.put(MESSAGE_ID, e.getMessage_id());
        params.put(USER_ID,e.getFrom());
        params.put(TEMP_USER_ID,e.getTo());
        test("messageSeenStatus", params, RESTHelper.SEND_STATUS_ENDPOINT, null, null);
    }

    public void sendNewMessageNotification(String toUserId) {

        HashMap<String,String> params = new HashMap<>();
        params.put(USER_ID,toUserId);
        params.put(NEW_MESSAGE,NEW_MESSAGE);
        test("sendNewMessageNotification",params,SEND_NOTIFICATION_ENDPOINT,null,null);
    }

    public void updateToken(final String token)
    {
        HashMap<String, String> params = new HashMap<>();
        params.put(TOKEN, token);
        params.put(USER_ID, userId);
        test("token sending", params, RESTHelper.TOKEN_UPDATE_ENDPOINT, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                sp.edit().putString(Util.TOKEN_SENT,Util.TOKEN_SENT).apply();
            }
        }, null);
    }

}
