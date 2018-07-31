package com.thoughtworks.onboarding.ui.ActivityList;

import android.app.Application;

import com.thoughtworks.onboarding.utils.Prefs;

public class MyApplication extends Application{


    @Override
    public void onCreate() {
        super.onCreate();

        Prefs.initPrefs(this);

    }
}
