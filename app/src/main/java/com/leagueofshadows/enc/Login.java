package com.leagueofshadows.enc;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.leagueofshadows.enc.Background.DecryptMessageWorker;
import com.leagueofshadows.enc.Background.GroupsWorker;
import com.leagueofshadows.enc.Background.MessageStatusWorker;
import com.leagueofshadows.enc.Background.ResendMessageWorker;
import com.leagueofshadows.enc.Crypt.AESHelper;
import com.leagueofshadows.enc.Crypt.RSAHelper;
import com.leagueofshadows.enc.Exceptions.RunningOnMainThreadException;
import com.leagueofshadows.enc.REST.Native;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import androidx.appcompat.app.AppCompatActivity;

public class Login extends AppCompatActivity {

    private EditText editText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        App app = (App) getApplication();
        if(!app.isnull())
            start();

        setContentView(R.layout.activity_login);

        editText = findViewById(R.id.password);

        findViewById(R.id.login).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String p = editText.getText().toString();
                if(!p.equals(""))
                {
                    checkPassword(p);
                }
                else
                {
                    editText.setError("password Empty");
                }
            }
        });

        sendFirebaseToken();

    }

    private void sendFirebaseToken() {

        final SharedPreferences sp = getSharedPreferences(Util.preferences,MODE_PRIVATE);
        boolean tokenSent = sp.contains(Util.TOKEN_SENT);
        if(!tokenSent) {
            FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(this, new OnSuccessListener<InstanceIdResult>() {
                @Override
                public void onSuccess(InstanceIdResult instanceIdResult) {
                    String token = instanceIdResult.getToken();
                    new Native(Login.this).updateToken(token);
                }
            });
        }
    }

    private void checkPassword(final String p) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    SharedPreferences sp = getSharedPreferences(Util.preferences,MODE_PRIVATE);
                    AESHelper aesHelper = new AESHelper(Login.this);

                    String checkMessage = sp.getString(Util.CheckMessage,null);
                    String encryptedCheckMessage = sp.getString(Util.CheckMessageEncrypted,null);

                    assert checkMessage != null;
                    String x = aesHelper.encryptCheckMessage(checkMessage,p);
                    if(x.equals(encryptedCheckMessage)) {
                        show();
                        setUp(p);
                        start();
                    }
                    else {
                        setError();
                    }
                } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException |
                        InvalidKeyException | BadPaddingException | IllegalBlockSizeException | InvalidKeySpecException |
                        RunningOnMainThreadException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    void start(){
        Intent intent = getIntent();
        if(intent.getAction()!=null)
        {
            if (intent.getAction().equals(Intent.ACTION_SEND))
            {
                Intent intent4 = new Intent(Login.this,ShareActivity.class);
                intent4.setAction(Intent.ACTION_SEND);
                intent4.setType(intent.getType());
                intent4.putExtra(Intent.EXTRA_STREAM,intent.getParcelableExtra(Intent.EXTRA_STREAM));
                intent4.putExtra(Intent.EXTRA_TEXT,intent.getStringExtra(Intent.EXTRA_TEXT));
                intent4.putExtra(Intent.EXTRA_SUBJECT,intent.getStringExtra(Intent.EXTRA_SUBJECT));
                startActivity(intent4);
                finish();
            }
        }
        else {

            Intent intent2 = new Intent(Login.this, ResendMessageWorker.class);
            startService(intent2);

            Intent intent1 = new Intent(Login.this, DecryptMessageWorker.class);
            startService(intent1);

            Intent intent3 = new Intent(Login.this, GroupsWorker.class);
            startService(intent3);

            Intent intent5 = new Intent(Login.this, MessageStatusWorker.class);
            startService(intent5);

            Intent intent4 = new Intent(Login.this, MainActivity.class);
            startActivity(intent4);
            finish();
        }
    }

    private void setUp(String p) {
        RSAHelper rsaHelper = new RSAHelper(this);
        try {
            PrivateKey privateKey = rsaHelper.getPrivateKey(p);
            App app = (App) getApplication();
            app.setPrivateKey(privateKey);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | BadPaddingException | InvalidKeyException | IllegalBlockSizeException | InvalidAlgorithmParameterException | InvalidKeySpecException | RunningOnMainThreadException e) {
            e.printStackTrace();
        }
    }

    /*private boolean check(String x, String encryptedCheckMessage) {
        if(x.length()!=encryptedCheckMessage.length())
        {
            Log.e("lengths","not equal "+x.length()+" "+encryptedCheckMessage.length());
            return false;
        }
        boolean y = true;
        for(int i=0;i<x.length();i++)
        {
            if(x.charAt(i)!=encryptedCheckMessage.charAt(i))
            {
                y = false;
                Log.e("pos - "+i, String.valueOf(x.charAt(i)+encryptedCheckMessage.charAt(i)));
            }
        }
        return y;
    }*/

    private void show() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(Login.this,"Verification successful",Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setError() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                editText.setError("Incorrect password");
            }
        });
    }
}
