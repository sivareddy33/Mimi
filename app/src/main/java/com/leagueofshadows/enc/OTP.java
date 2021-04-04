package com.leagueofshadows.enc;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.leagueofshadows.enc.Crypt.AESHelper;
import com.leagueofshadows.enc.Crypt.RSAHelper;
import com.leagueofshadows.enc.Exceptions.RunningOnMainThreadException;
import com.leagueofshadows.enc.Interfaces.UserCallback;
import com.leagueofshadows.enc.Items.User;
import com.leagueofshadows.enc.Background.ContactsWorker;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.TimeUnit;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import static com.leagueofshadows.enc.Background.ContactsWorker.FLAG;

public class OTP extends AppCompatActivity implements UserCallback {


    String verificationId;
    EditText  otp;
    ProgressDialog progressDialog;
    FirebaseHelper firebaseHelper;
    String name;
    String number;
    String password;
    String uid;
    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_o_t_p);

        firebaseHelper = new FirebaseHelper(this);
        if(!firebaseHelper.checkConnection()) {
            Toast.makeText(this,"no internet connectivity",Toast.LENGTH_SHORT).show();
            finish();
        }

        progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setTitle(R.string.app_name);

        Intent intent = getIntent();
        name = intent.getStringExtra(Util.name);
        number = intent.getStringExtra(Util.number);
        password = intent.getStringExtra(Util.password);

        otp = findViewById(R.id.otp);

        assert number != null;

        TextView numberTextView = findViewById(R.id.phone_number);
        numberTextView.append(number);

        sendOTP();

        findViewById(R.id.confirm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!firebaseHelper.checkConnection())
                {
                    if(!firebaseHelper.checkConnection()) {
                        Toast.makeText(OTP.this,"no internet connectivity",Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
                String o = otp.getText().toString();
                if(o.length()<6) {
                    otp.setError("invalid otp");
                    return;
                }
                otp.clearFocus();
                PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId,o);
                progressDialog.setMessage("verifying otp...");
                progressDialog.show();
                signInWithCredential(credential);
            }
        });
    }

    void signInWithCredential(PhoneAuthCredential credential)
    {
        FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener(OTP.this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(OTP.this, "Verification Success", Toast.LENGTH_SHORT).show();
                    startProcess();
                } else {
                    if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                        otp.setError("Invalid OTP");
                        progressDialog.dismiss();
                        //Toast.makeText(OTP.this, "Verification Failed", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }


    void sendOTP()
    {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(number,
                0,
                TimeUnit.SECONDS,
                OTP.this,
                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                        signInWithCredential(phoneAuthCredential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        Toast.makeText(OTP.this,"OTP sent",Toast.LENGTH_SHORT).show();
                        verificationId = s;
                    }
                });
    }

    private void startProcess() {

        //TODO : check uid
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        progressDialog.setMessage("creating key pair...");
        final RSAHelper rsaHelper = new RSAHelper(this);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    rsaHelper.generateKeyPair(password);
                    FirebaseHelper firebaseHelper = new FirebaseHelper(OTP.this);
                    String Base64PublicKeyString = getSharedPreferences(Util.preferences,MODE_PRIVATE).getString(Util.PublicKeyString,null);
                    if(firebaseHelper.checkConnection())
                    {
                        updateProgressDialog();
                        user = new User(uid,name,number,Base64PublicKeyString);
                        firebaseHelper.sendUserData(user, OTP.this);
                    }
                    else {
                        show("Something went wrong please try again");
                        finish();
                    }
                } catch (NoSuchAlgorithmException | NoSuchPaddingException | BadPaddingException |
                        InvalidKeyException | IllegalBlockSizeException |
                        InvalidAlgorithmParameterException | InvalidKeySpecException |
                        RunningOnMainThreadException e) {
                    show("Something went wrong please try again");
                    finish();
                    e.printStackTrace();
                }
            }
        });
    }

    private void updateProgressDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressDialog.setMessage("setting up user details...");
            }
        });
    }

    void setUpUser()
    {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {

                try {
                    AESHelper aesHelper = new AESHelper(OTP.this);

                    SharedPreferences.Editor editor = getSharedPreferences(Util.preferences,MODE_PRIVATE).edit();

                    SecureRandom secureRandom = new SecureRandom();
                    byte[] randomBytes = new byte[128];
                    secureRandom.nextBytes(randomBytes);

                    String Base64randomString = aesHelper.getBase64(randomBytes);

                    String encrypted = aesHelper.encryptCheckMessage(Base64randomString,password);
                    editor.putString(Util.CheckMessageEncrypted,encrypted).putString(Util.CheckMessage,Base64randomString).putString(Util.userId,uid).
                            putString(Util.name,name).putString(Util.number,number).apply();
                    password=null;
                    show("verification successful");
                    Intent intent = new Intent(OTP.this,Login.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);

                    Intent intent1 = new Intent(OTP.this, ContactsWorker.class);
                    intent1.putExtra(FLAG,0);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent1);
                    }
                    else {
                        startService(intent1);
                    }

                    startActivity(intent);
                    finish();
                } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException |
                        InvalidKeyException | BadPaddingException | IllegalBlockSizeException | InvalidKeySpecException |
                        RunningOnMainThreadException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void show(final String s) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressDialog.dismiss();
                Toast.makeText(OTP.this,s,Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void result(boolean x, String data) {
        progressDialog.dismiss();
        if(x) {
            setUpUser();
        }
        else {
            Log.e("data",data);
            Toast.makeText(this,data,Toast.LENGTH_LONG).show();
        }
    }
}
