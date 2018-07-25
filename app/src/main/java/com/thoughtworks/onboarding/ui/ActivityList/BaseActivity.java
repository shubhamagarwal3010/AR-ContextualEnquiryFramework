/*===============================================================================
Copyright (c) 2016-2017 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.
===============================================================================*/

package com.thoughtworks.onboarding.ui.ActivityList;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;

import com.thoughtworks.onboarding.TargetAndResourceRepository;
import com.thoughtworks.onboarding.TargetWithResource;
import com.thoughtworks.onboarding.utils.LoadingDialogHandler;

import java.util.List;


// The AR activity for the AugmentedDisplay sample.
public class BaseActivity extends Activity {

    private static final String LOGTAG = BaseActivity.class.getSimpleName();
    public static int NUM_TARGETS = 0;
    public List<TargetWithResource> targetWithResources;
    public LoadingDialogHandler loadingDialogHandler;
    private boolean mExtendedTracking = false;

    private void init() {
        TargetAndResourceRepository targetAndResourceRepository = new TargetAndResourceRepository();
        targetWithResources = targetAndResourceRepository.getTargetsAndResoruces();
        NUM_TARGETS = targetAndResourceRepository.getTargetCount();
    }

    public boolean isExtendedTrackingActive() {
        return mExtendedTracking;
    }

    public Pair getIndexOfTargetFromTargetName(String name) {
        for (int i = 0; i < NUM_TARGETS; i++) {
            if (targetWithResources.get(i).getTargetName().equals(name))
                return new Pair(i, targetWithResources.get(i).getDisplayType());
        }
        return new Pair(-1,null);
    }

    // Called when the activity first starts or the user navigates back
    // to an activity.
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOGTAG, "onCreate");
        super.onCreate(savedInstanceState);
        init();

        loadingDialogHandler = new LoadingDialogHandler(this);

        init();
    }

}
