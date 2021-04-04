package com.leagueofshadows.enc;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.firebase.auth.FirebaseAuth;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String uid = FirebaseAuth.getInstance().getUid();
        String userId = getSharedPreferences(Util.preferences,MODE_PRIVATE).getString(Util.userId,null);

        final Intent intent;

        if(uid==null) {
            FirebaseAuth.getInstance().signOut();
            SharedPreferences sp = getSharedPreferences(Util.preferences,MODE_PRIVATE);
            sp.edit().clear().apply();
            intent = new Intent(this,Register.class);
        }
        else {
            if(userId == null) {
                FirebaseAuth.getInstance().signOut();
                SharedPreferences sp = getSharedPreferences(Util.preferences,MODE_PRIVATE);
                sp.edit().clear().apply();
                intent = new Intent(this,Register.class);
            }
            else {
                intent = new Intent(this, Login.class);
            }
        }

        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
        executor.schedule(new Runnable() {
            @Override
            public void run() {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        },1, TimeUnit.SECONDS);
    }
}
