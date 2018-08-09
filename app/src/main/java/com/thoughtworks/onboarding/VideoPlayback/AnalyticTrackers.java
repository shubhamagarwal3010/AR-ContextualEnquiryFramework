package com.thoughtworks.onboarding.VideoPlayback;

import android.content.Context;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.thoughtworks.onboarding.utils.Constants;

public class AnalyticTrackers {


    private FirebaseAnalytics mFirebaseAnalytics;

    Context ctx;

    public AnalyticTrackers(Context ctx)
    {
        this.ctx = ctx;
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(ctx);
    }

    public void trackScanner()
    {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.VALUE, "Scanner page onboarding");
        mFirebaseAnalytics.logEvent(Constants.FIREBASE_EVENT_ONBOARD, bundle);

    }

    public void trackImageTargets()
    {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.VALUE, "Image Targets");
        mFirebaseAnalytics.logEvent(Constants.FIREBASE_EVENT_IMAGE_TARGET, bundle);

    }

    public void trackVideoTargets()
    {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.VALUE, "Video Targets");
        mFirebaseAnalytics.logEvent(Constants.FIREBASE_EVENT_VIDEO_TARGET, bundle);

    }

    public void trackHyperLinkTargets()
    {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.VALUE, "HyperLink Targets");
        mFirebaseAnalytics.logEvent(Constants.FIREBASE_EVENT_HYPERLINK_TARGET, bundle);

    }


    public void trackTargetNames(String targetName)
    {
        targetName = targetName.replaceAll("-","_").replaceAll(" ", "_");
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.VALUE, targetName);
        mFirebaseAnalytics.logEvent(targetName, bundle);

    }
}
