package com.thoughtworks.onboarding.ui.ActivityList;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.thoughtworks.onboarding.AugmentedDisplay.AugmentedDisplay;
import com.thoughtworks.onboarding.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void dualAugmentation(View view) {
        Intent intent = new Intent(MainActivity.this, AugmentedDisplay.class);
        startActivity(intent);
    }
}
