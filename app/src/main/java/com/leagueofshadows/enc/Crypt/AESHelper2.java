package com.leagueofshadows.enc.Crypt;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;
import android.util.Base64;

import com.leagueofshadows.enc.App;
import com.leagueofshadows.enc.Exceptions.DataCorruptedException;
import com.leagueofshadows.enc.Exceptions.MalFormedFileException;
import com.leagueofshadows.enc.Exceptions.RunningOnMainThreadException;
import com.leagueofshadows.enc.Items.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import androidx.annotation.NonNull;

import static com.leagueofshadows.enc.Util.CheckMessageIV;

public class AESHelper2 {


    private Context context;

    private KeyGenerator keyGenerator;
    private SecureRandom secureRandom;
    private SecretKeyFactory factory;
    private Cipher cipher;

    private static final int keySize = 256;
    private static final String algorithm = "AES";
    private static final int iterations = 1000;
    private static final String pbkdf = "PBKDF2WithHmacSHA1";
    private static final String cipherAlgorithm = "AES/CBC/PKCS5Padding";
    private static final String hashAlgorithm = "SHA-256";

    private static final String threadException = "Must not be invoked from Main Thread";
    private static final String applicationFlowException = "it appears that the data has been corrupted or the " +
            "flow of application data is wrong don't call this function before encrypt function";

    private static final String dataCorruptedException = "it appears that the data has been tampered with.\n " +
            "this could also result from incorrect use of keys";

    private static final String malFormedFile = "it seems that the file is not encrypted properly";

    public static final String MESSAGE_CONTENT = "M_C";
    public static final String MESSAGE_KEYS = "M_K";
    public static final String MESSAGE_HASH_BYTES = "M_H";
    public static final String MESSAGE_IV = "M_I";
    public static final String MESSAGE_FILE_HASH_BYTES = "MF_H";

    public AESHelper2 (Context context) throws NoSuchAlgorithmException, NoSuchPaddingException {
        this.context = context;
        init();
    }


    private void init() throws NoSuchAlgorithmException, NoSuchPaddingException {
        secureRandom = new SecureRandom();
        keyGenerator = KeyGenerator.getInstance(algorithm);
        keyGenerator.init(keySize,secureRandom);
        factory = SecretKeyFactory.getInstance(pbkdf);
        cipher = Cipher.getInstance(cipherAlgorithm);
    }

    private SecretKey getOneTimeKey() throws NoSuchAlgorithmException, NoSuchPaddingException {
        if(keyGenerator!=null) {
            return keyGenerator.generateKey();
        }
        else {
            init();
            return keyGenerator.generateKey();
        }
    }

    private SecretKey getMasterKey(@NonNull String Password) throws InvalidKeySpecException {

        SharedPreferences sp = context.getSharedPreferences(com.leagueofshadows.enc.Util.preferences,Context.MODE_PRIVATE);
        String salt = sp.getString(Util.saltString,null);
        if(salt == null)
        {
            byte[] saltBytes = new byte[32];
            secureRandom.nextBytes(saltBytes);
            salt = getBase64(saltBytes);
            sp.edit().putString(Util.saltString,salt).apply();
        }
        byte[] saltBytes = getbytes(salt);
        PBEKeySpec spec = new PBEKeySpec(Password.toCharArray(), saltBytes,iterations,keySize);
        SecretKey secretKey = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), algorithm);
        App app = (App) context.getApplicationContext();
        app.setMasterKey(secretKey);
        return secretKey;
    }

    public String encryptCheckMessage(@NonNull String message,String password) throws
            InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException,
            IllegalBlockSizeException, InvalidKeySpecException, RunningOnMainThreadException {

        if(Looper.myLooper() == Looper.getMainLooper()) {
            throw new RunningOnMainThreadException(threadException);
        }

        String Base64IV = context.getSharedPreferences(com.leagueofshadows.enc.Util.preferences,Context.MODE_PRIVATE).getString(CheckMessageIV,null);
        byte[] iv;
        if(Base64IV == null) {
            iv = getNewIV();
            Base64IV = getBase64(iv);
            context.getSharedPreferences(com.leagueofshadows.enc.Util.preferences,Context.MODE_PRIVATE).edit().putString(CheckMessageIV,Base64IV).apply();
        }

        iv = getbytes(Base64IV);

        SecretKey secretKey = getMasterKey(password);
        byte[] messageBytes = getbytes(message);


        cipher.init(Cipher.ENCRYPT_MODE,secretKey,new IvParameterSpec(iv));
        byte[] encryptedMessageBytes =  cipher.doFinal(messageBytes);
        return getBase64(encryptedMessageBytes);
    }

    void encryptPrivateKey(@NonNull byte[] encodedPrivateKey ,@NonNull String Password) throws InvalidKeySpecException,
            InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException,
            RunningOnMainThreadException {

        if(Looper.myLooper() == Looper.getMainLooper()) {
            throw new RunningOnMainThreadException(threadException);
        }

        SecretKey masterKey = getMasterKey(Password);
        byte[] iv = getNewIV();
        SharedPreferences.Editor editor = context.getSharedPreferences(com.leagueofshadows.enc.Util.preferences,Context.MODE_PRIVATE).edit();
        editor.putString(Util.PrivateKeyIV,getBase64(iv)).apply();
        cipher.init(Cipher.ENCRYPT_MODE,masterKey,new IvParameterSpec(iv));
        byte[] out = cipher.doFinal(encodedPrivateKey);
        String privateKeyString = getBase64(out);
        editor.putString(Util.PrivateKeyString,privateKeyString);
        editor.apply();
        //masterKey.destroy();
    }

    byte[] decryptPrivateKey(@NonNull String Password) throws InvalidKeySpecException, InvalidAlgorithmParameterException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException, RunningOnMainThreadException {

        if(Looper.myLooper() == Looper.getMainLooper()) {
            throw new RunningOnMainThreadException(threadException);
        }

        SecretKey masterKey = getMasterKey(Password);
        SharedPreferences sp = context.getSharedPreferences(com.leagueofshadows.enc.Util.preferences,Context.MODE_PRIVATE);
        byte[] iv = getbytes(sp.getString(Util.PrivateKeyIV,null));
        byte[] in = getbytes(sp.getString(Util.PrivateKeyString,null));

        if( iv != null && in != null) {
            cipher.init(Cipher.DECRYPT_MODE,masterKey,new IvParameterSpec(iv));
            return cipher.doFinal(in);
        }
        else {
            throw new IllegalStateException(applicationFlowException);
        }
    }

    public String encryptMessage(@NonNull String messageText, @NonNull User[] users, @NonNull PrivateKey privateKey) throws RunningOnMainThreadException {

        if(Looper.myLooper() == Looper.getMainLooper()) {
            throw new RunningOnMainThreadException(threadException);
        }

        try {
            SecretKey secretKey = getOneTimeKey();
            byte[] messageBytes = messageText.getBytes();
            byte[] encodedKeyBytes = secretKey.getEncoded();

            JSONObject messageJsonObject = new JSONObject();

            RSAHelper rsaHelper = new RSAHelper(context);

            byte[] hashBytes = getHash(messageBytes, encodedKeyBytes);
            JSONObject keysJsonObject = new JSONObject();
            for (User user : users) {
                byte[] encryptedKeyBytes = rsaHelper.encryptKey(encodedKeyBytes, user.getBase64EncodedPublicKey());
                keysJsonObject.put(user.getId(), getBase64(encryptedKeyBytes));
            }

            hashBytes = rsaHelper.signHash(hashBytes, privateKey);

            byte[] iv = getNewIV();
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
            byte[] encryptedMessageBytes = cipher.doFinal(messageBytes);

            messageJsonObject.put(MESSAGE_CONTENT,getBase64(encryptedMessageBytes));
            messageJsonObject.put(MESSAGE_KEYS,keysJsonObject);
            messageJsonObject.put(MESSAGE_IV,getBase64(iv));
            messageJsonObject.put(MESSAGE_HASH_BYTES,getBase64(hashBytes));

            return messageJsonObject.toString();
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String DecryptMessage(@NonNull String message,@NonNull PrivateKey privateKey,@NonNull User otherUser)
            throws NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException,
            InvalidKeyException, InvalidKeySpecException, InvalidAlgorithmParameterException,
            DataCorruptedException, RunningOnMainThreadException {

        if(Looper.myLooper() == Looper.getMainLooper()) {
            throw new RunningOnMainThreadException(threadException);
        }
        try {
            JSONObject messageJsonObject = new JSONObject(message);
            byte[] hashBytes = getbytes(messageJsonObject.getString(MESSAGE_HASH_BYTES));
            byte[] encryptedKeyBytes = getbytes(messageJsonObject.getJSONObject(MESSAGE_KEYS).getString(otherUser.getId()));
            byte[] iv = getbytes(messageJsonObject.getString(MESSAGE_IV));
            byte[] messageBytes = getbytes(messageJsonObject.getString(MESSAGE_CONTENT));

            RSAHelper rsaHelper = new RSAHelper(context);
            hashBytes = rsaHelper.unSignHash(hashBytes, otherUser.getBase64EncodedPublicKey());

            byte[] encodedKeyByes = rsaHelper.decryptKey(encryptedKeyBytes, privateKey);

            SecretKeySpec secretKeySpec = new SecretKeySpec(encodedKeyByes, algorithm);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(iv));
            messageBytes = cipher.doFinal(messageBytes);

            byte[] newHahBytes = getHash(messageBytes, encodedKeyByes);
            boolean x = Arrays.equals(hashBytes, newHahBytes);

            if (x)
                return new String(messageBytes);
            else {
                throw new DataCorruptedException(dataCorruptedException + "\n"
                        + "original Hash - " + getBase64(hashBytes)
                        + "received Hash - " + getBase64(newHahBytes));
            }
        }
        catch (JSONException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public String encryptFile(@NonNull FileInputStream fileInputStream,@NonNull FileInputStream fileInputStream1,
                              @NonNull FileOutputStream fileOutputStream, @NonNull PrivateKey privateKey, @NonNull User[] users,String messageText)
            throws RunningOnMainThreadException, NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException,
            InvalidKeySpecException, InvalidKeyException, IOException, InvalidAlgorithmParameterException {

        if(Looper.myLooper()== Looper.getMainLooper()) {
            throw new RunningOnMainThreadException(threadException);
        }
        SecretKey secretKey = getOneTimeKey();

        try{
            byte[] messageBytes = messageText.getBytes();
            byte[] encodedKeyBytes = secretKey.getEncoded();

            JSONObject messageJsonObject = new JSONObject();

            RSAHelper rsaHelper = new RSAHelper(context);

            byte[] hashBytes = getHash(messageBytes, encodedKeyBytes);
            JSONObject keysJsonObject = new JSONObject();
            for (User user : users) {
                byte[] encryptedKeyBytes = rsaHelper.encryptKey(encodedKeyBytes, user.getBase64EncodedPublicKey());
                keysJsonObject.put(user.getId(), getBase64(encryptedKeyBytes));
            }

            hashBytes = rsaHelper.signHash(hashBytes, privateKey);

            byte[] iv = getNewIV();
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
            byte[] encryptedMessageBytes = cipher.doFinal(messageBytes);

            messageJsonObject.put(MESSAGE_CONTENT,getBase64(encryptedMessageBytes));
            messageJsonObject.put(MESSAGE_KEYS,keysJsonObject);
            messageJsonObject.put(MESSAGE_IV,getBase64(iv));
            messageJsonObject.put(MESSAGE_HASH_BYTES,getBase64(hashBytes));
            hashBytes = getHashFromFile(fileInputStream,encodedKeyBytes);
            hashBytes = rsaHelper.signHash(hashBytes,privateKey);

            messageJsonObject.put(MESSAGE_FILE_HASH_BYTES,getBase64(hashBytes));

            fileOutputStream.write(hashBytes);
            cipher.init(Cipher.ENCRYPT_MODE,secretKey,new IvParameterSpec(iv));
            convertFile(fileInputStream1,fileOutputStream,cipher);

            return messageJsonObject.toString();
        }
        catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void decryptFile(@NonNull FileInputStream fileInputStream, @NonNull  FileOutputStream fileOutputStream,
                            @NonNull PrivateKey privateKey, @NonNull User otherUser,@NonNull File outFile,@NonNull String cipherText)

            throws IOException, MalFormedFileException, NoSuchPaddingException, NoSuchAlgorithmException,
            IllegalBlockSizeException, BadPaddingException, InvalidKeyException, InvalidKeySpecException,
            InvalidAlgorithmParameterException, RunningOnMainThreadException {

        if(Looper.myLooper()== Looper.getMainLooper()) {
            throw new RunningOnMainThreadException(threadException);
        }

        try{
            JSONObject messageJsonObject = new JSONObject(cipherText);
            byte[] hashBytes = getbytes(messageJsonObject.getString(MESSAGE_FILE_HASH_BYTES));
            byte[] encryptedKeyBytes = getbytes(messageJsonObject.getJSONObject(MESSAGE_KEYS).getString(otherUser.getId()));
            byte[] iv = getbytes(messageJsonObject.getString(MESSAGE_IV));
            RSAHelper rsaHelper = new RSAHelper(context);
            hashBytes = rsaHelper.unSignHash(hashBytes,otherUser.getBase64EncodedPublicKey());

            encryptedKeyBytes = rsaHelper.decryptKey(encryptedKeyBytes,privateKey);
            SecretKeySpec secretKeySpec = new SecretKeySpec(encryptedKeyBytes,algorithm);
            cipher.init(Cipher.DECRYPT_MODE,secretKeySpec,new IvParameterSpec(iv));

            convertFile(fileInputStream,fileOutputStream,cipher);

            byte[] newHashBytes = getHashFromFile(new FileInputStream(outFile),encryptedKeyBytes);
            if(!(Arrays.equals(hashBytes,newHashBytes))) {
                throw new MalFormedFileException(malFormedFile
                        +"\n"
                        +"file deleted - ?"
                        +" original Hash - "+getBase64(hashBytes)
                        +" received Hash - "+getBase64(newHashBytes));
            }
        }catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void convertFile(@NonNull FileInputStream fileInputStream,@NonNull FileOutputStream fileOutputStream,@NonNull Cipher cipher)
            throws IOException, BadPaddingException, IllegalBlockSizeException {

        byte[] buffer = new byte[4096];
        int len;
        while((len = fileInputStream.read(buffer))>0)
        {
            byte[] output = cipher.update(buffer,0,len);
            fileOutputStream.write(output);
        }
        byte[] output = cipher.doFinal();
        if(output!=null) {
            fileOutputStream.write(output);
        }
        fileOutputStream.flush();
        fileInputStream.close();
        fileOutputStream.close();
    }


    private byte[] getHash(@NonNull byte[] messageBytes, @NonNull byte[] encodedKeyBytes) throws NoSuchAlgorithmException {

        byte[] bytes = new byte[messageBytes.length+encodedKeyBytes.length];

        System.arraycopy(messageBytes,0,bytes,0,messageBytes.length);
        System.arraycopy(encodedKeyBytes,0,bytes,messageBytes.length,encodedKeyBytes.length);

        MessageDigest messageDigest = MessageDigest.getInstance(hashAlgorithm);
        return messageDigest.digest(bytes);
    }

    private byte[] getHashFromFile(FileInputStream fileInputStream,byte[] encodedKeyBytes) throws IOException, NoSuchAlgorithmException {

        MessageDigest messageDigest = MessageDigest.getInstance(hashAlgorithm);
        byte[] buffer = new byte[8192];
        int count;
        while ((count = fileInputStream.read(buffer))>0) {
            messageDigest.update(buffer,0,count);
        }
        return messageDigest.digest(encodedKeyBytes);
    }

    @NonNull
    private byte[] getNewIV() {
        byte[] iv = new byte[16];
        secureRandom.nextBytes(iv);
        return iv;
    }

    public String getBase64(byte[] bytes) {
        String s = Base64.encodeToString(bytes,Base64.DEFAULT);
        return s.trim();
    }

    private byte[] getbytes(String Base64String) {
        return Base64.decode(Base64String,Base64.DEFAULT);
    }
}
