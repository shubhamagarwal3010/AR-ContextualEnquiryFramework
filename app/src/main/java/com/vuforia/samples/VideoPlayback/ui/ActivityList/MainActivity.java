package com.vuforia.samples.VideoPlayback.ui.ActivityList;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.vuforia.samples.BackgroundTextureAccess.BackgroundTextureAccess;
import com.vuforia.samples.ImageDisplay.ImageDisplay;
import com.vuforia.samples.VideoPlayback.R;
import com.vuforia.samples.VideoPlayback.app.VideoPlayback.VideoPlayback;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }

    }

    public void viewARImage(View view) {

        Intent intent = new Intent(MainActivity.this, ImageDisplay.class);
        startActivity(intent);
    }

    public void viewARVideo(View view) {
        Intent intent = new Intent(MainActivity.this, VideoPlayback.class);
        startActivity(intent);

    }

    public void viewAR3DModel(View view) {
        Intent intent = new Intent(MainActivity.this, BackgroundTextureAccess.class);
        startActivity(intent);
    }
}
