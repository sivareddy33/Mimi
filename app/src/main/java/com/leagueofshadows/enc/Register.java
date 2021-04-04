package com.leagueofshadows.enc;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class Register extends AppCompatActivity {

    EditText nameEditText;
    EditText numberEditText;
    EditText passwordEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        nameEditText = findViewById(R.id.name);
        numberEditText = findViewById(R.id.number);
        passwordEditText = findViewById(R.id.password);

        findViewById(R.id.sign_in_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = nameEditText.getText().toString();
                String number = numberEditText.getText().toString();
                String password = passwordEditText.getText().toString();
                if(!name.equals(""))
                {
                    if (isValidPhoneNumber(number))
                    {
                        if(!(password.length()<8))
                        {
                            signUp(name,number,password);
                        }
                        else {
                            passwordEditText.setError("password too small minimum 8 chars");
                        }
                    }
                    else {
                        numberEditText.setError("phone number not valid");
                    }
                }
                else {
                    nameEditText.setError("name required");
                }
            }
        });
        askPermission();
    }

    private void askPermission() {
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_CONTACTS,Manifest.permission.WRITE_EXTERNAL_STORAGE},0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode==0)
        {
            boolean x = true;
            for(int i=0;i<permissions.length;i++)
            {
                if(grantResults[i]!=PERMISSION_GRANTED) {
                    x = false;
                    break;
                }
            }
            if(!x)
                finish();
        }
    }

    private boolean isValidPhoneNumber(String number) {
        return number.startsWith("+91") && number.length() == 13;
    }

    private void signUp(String name, String number, String password) {

        Intent intent = new Intent(this,OTP.class);
        intent.putExtra(Util.name,name);
        intent.putExtra(Util.number,number);
        intent.putExtra(Util.password,password);
        startActivity(intent);

    }
}
