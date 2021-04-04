package com.leagueofshadows.enc.Crypt;

import android.content.Context;
import android.os.Looper;
import android.util.Base64;

import com.leagueofshadows.enc.Exceptions.RunningOnMainThreadException;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class RSAHelper {


    private Context context;
    private static String threadException = "Must not be invoked from Main Thread";
    private static String algorithm = "RSA";
    private static int keySize = 2048;
    private static String cipherAlgorithm = "RSA/ECB/OAEPWithSHA1AndMGF1Padding";

    public RSAHelper(Context context) {
        this.context = context;
        /*Cipher cipher = Cipher.getInstance(cipherAlgorithm);
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(algorithm);
        Log.e("Cipher RSA details - ",cipher.getAlgorithm()+"  "+cipher.getProvider()+"  "+cipher.getParameters());
        Log.e("Keypair g Rsa details-",keyPairGenerator.getAlgorithm()+" "+keyPairGenerator.getProvider());*/
    }


    public void generateKeyPair(String Password) throws NoSuchAlgorithmException,
            NoSuchPaddingException, BadPaddingException, InvalidKeyException, IllegalBlockSizeException,
            InvalidAlgorithmParameterException, InvalidKeySpecException, RunningOnMainThreadException {

        if(Looper.getMainLooper()==Looper.myLooper()){
            throw new IllegalStateException(threadException);
        }

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(algorithm);
        keyPairGenerator.initialize(keySize);
        KeyPair keyPair = keyPairGenerator.genKeyPair();

        PublicKey publicKey = keyPair.getPublic();
        String publicKeyString = getBase64(publicKey.getEncoded());
        context.getSharedPreferences(com.leagueofshadows.enc.Util.preferences,Context.MODE_PRIVATE).edit().putString(Util.PublicKeyString,publicKeyString).apply();
        AESHelper aesHelper = new AESHelper(context);
        aesHelper.encryptPrivateKey(keyPair.getPrivate().getEncoded(),Password);

    }

   private PublicKey getPublicKey(String Base64String) throws NoSuchAlgorithmException, InvalidKeySpecException {

        byte[] encodedBytes = getbytes(Base64String);
        KeyFactory keyFactory = KeyFactory.getInstance(algorithm);

        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(encodedBytes);
        return keyFactory.generatePublic(x509EncodedKeySpec);
    }

    public PrivateKey getPrivateKey(String Password) throws NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException,
            InvalidKeyException, IllegalBlockSizeException,
            InvalidAlgorithmParameterException, InvalidKeySpecException, RunningOnMainThreadException {

        if(Looper.getMainLooper() == Looper.myLooper()) {
            throw new IllegalStateException(threadException);
        }

        AESHelper aesHelper = new AESHelper(context);
        byte[] encodedBytes = aesHelper.decryptPrivateKey(Password);
        KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
        PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(encodedBytes);
        return keyFactory.generatePrivate(pkcs8EncodedKeySpec);
    }

    byte[] encryptKey(byte[] encodedBytes,String Base64String) throws InvalidKeySpecException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {

        PublicKey publicKey = getPublicKey(Base64String);
        Cipher cipher = Cipher.getInstance(cipherAlgorithm);
        cipher.init(Cipher.ENCRYPT_MODE,publicKey);
        return cipher.doFinal(encodedBytes);
    }

    byte[] decryptKey(byte[] encryptedBytes,PrivateKey privateKey) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException {

        Cipher cipher = Cipher.getInstance(cipherAlgorithm);
        cipher.init(Cipher.DECRYPT_MODE,privateKey);
        return cipher.doFinal(encryptedBytes);
    }

    byte[] signHash(byte[] hashBytes,PrivateKey privateKey) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException {

        Cipher cipher = Cipher.getInstance(cipherAlgorithm);
        cipher.init(Cipher.ENCRYPT_MODE,privateKey);
        return cipher.doFinal(hashBytes);
    }

    byte[] unSignHash(byte[] signedHash,String Base64String) throws InvalidKeySpecException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {

        PublicKey publicKey = getPublicKey(Base64String);
        Cipher cipher = Cipher.getInstance(cipherAlgorithm);
        cipher.init(Cipher.DECRYPT_MODE,publicKey);
        return cipher.doFinal(signedHash);
    }

    private String getBase64(byte[] bytes) {
        return Base64.encodeToString(bytes,Base64.DEFAULT);
    }

    private byte[] getbytes(String Base64String) {
        return Base64.decode(Base64String,Base64.DEFAULT);
    }
}
