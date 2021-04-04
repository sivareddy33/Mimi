package com.leagueofshadows.enc;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.jsibbold.zoomage.ZoomageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

public class ImagePreview extends AppCompatActivity {

    ZoomageView zoomageView;
    boolean flag = false;
    String path;
    Uri uri;
    boolean isVisible = true;

    @Override
    @SuppressLint("ClickableViewAccessibility")
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        actionBar.setDisplayHomeAsUpEnabled(true);

        zoomageView = findViewById(R.id.imageView);
        final GestureDetector gestureDetector = new GestureDetector(this,new SingleTapConfirm());

        zoomageView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(gestureDetector.onTouchEvent(motionEvent))
                {
                    if(isVisible) {
                        isVisible = false;
                        actionBar.hide();
                    }
                    else {
                        isVisible = true;
                        actionBar.show();
                    }
                    return true;
                }
                else
                    return false;
            }
        });

        /*zoomageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });*/

        Intent intent = getIntent();
        String name = intent.getStringExtra(Util.name);

        getSupportActionBar().setTitle("Send image to "+name);

        String camera = intent.getStringExtra(Util.camera);
        if(camera!=null)
        {
            flag = true;
            path = intent.getStringExtra(Util.path);
            try {
                Bitmap bitmap = BitmapFactory.decodeFile(path);
                ExifInterface exifInterface = new ExifInterface(getApplicationContext().getFilesDir()+"/current.jpg");
                final Bitmap correctBitmap;

                int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_UNDEFINED);
                switch (orientation)
                {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        correctBitmap = rotateBitmap(bitmap,90);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        correctBitmap = rotateBitmap(bitmap,180);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        correctBitmap = rotateBitmap(bitmap,270);
                        break;
                    case ExifInterface.ORIENTATION_NORMAL:
                    default: correctBitmap = bitmap;
                }
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            FileOutputStream fileOutputStream = new FileOutputStream(path);
                            correctBitmap.compress(Bitmap.CompressFormat.JPEG,100,fileOutputStream);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                });
                zoomageView.setImageBitmap(correctBitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else
        {
            uri = Uri.parse(intent.getStringExtra(Util.uri));
            try {
                zoomageView.setImageBitmap(BitmapFactory.decodeStream(getContentResolver().openInputStream(uri)));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        findViewById(R.id.send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(flag)
                {
                    Intent data = new Intent();
                    data.putExtra(Util.uri,Uri.fromFile(new File(path)).toString());
                    setResult(RESULT_OK,data);
                    finish();
                }
                else
                {
                    Intent data = new Intent();
                    data.putExtra(Util.uri,uri.toString());
                    setResult(RESULT_OK,data);
                    finish();
                }
            }
        });
    }

    @Override
    public boolean onNavigateUp() {
        onBackPressed();
        return true;
    }

    private Bitmap rotateBitmap(Bitmap bitmap, float i) {
        Matrix matrix = new Matrix();
        matrix.setRotate(i);
        return Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);
    }

    private static class SingleTapConfirm extends GestureDetector.SimpleOnGestureListener{
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return true;
        }
    }

}
