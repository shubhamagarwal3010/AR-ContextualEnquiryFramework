/*===============================================================================
Copyright (c) 2016-2017 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.
===============================================================================*/

package com.vuforia.samples.BackgroundTextureAccess;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;

import com.vuforia.CameraDevice;
import com.vuforia.DataSet;
import com.vuforia.ObjectTracker;
import com.vuforia.Renderer;
import com.vuforia.STORAGE_TYPE;
import com.vuforia.State;
import com.vuforia.Tracker;
import com.vuforia.TrackerManager;
import com.vuforia.VideoBackgroundConfig;
import com.vuforia.Vuforia;
import com.vuforia.samples.SampleApplication.ImageTrackerManager;
import com.vuforia.samples.SampleApplication.SampleApplicationException;
import com.vuforia.samples.SampleApplication.UpdateTargetCallback;
import com.vuforia.samples.SampleApplication.utils.LoadingDialogHandler;
import com.vuforia.samples.SampleApplication.utils.SampleApplicationGLView;
import com.vuforia.samples.SampleApplication.utils.Texture;
import com.vuforia.samples.VideoPlayback.R;
import com.vuforia.samples.VideoPlayback.ui.SampleAppMenu.SampleAppMenu;
import com.vuforia.samples.VideoPlayback.ui.SampleAppMenu.SampleAppMenuGroup;
import com.vuforia.samples.VideoPlayback.ui.SampleAppMenu.SampleAppMenuInterface;

import java.util.Vector;


public class BackgroundTextureAccess extends Activity implements
        ImageTrackerManager, SampleAppMenuInterface {
    final public static int CMD_BACK = -1;
    // Constants for Hiding/Showing Loading dialog
    static final int HIDE_LOADING_DIALOG = 0;
    static final int SHOW_LOADING_DIALOG = 1;
    private static final String LOGTAG = "BackgroundTextureAccess";
    UpdateTargetCallback updateTargetCallback;
    // Our OpenGL view:
    private SampleApplicationGLView mGlView;
    // Our renderer:
    private BackgroundTextureAccessRenderer mRenderer;
    // The textures we will use for rendering:
    private Vector<Texture> mTextures;
    private RelativeLayout mUILayout;
    private SampleAppMenu mSampleAppMenu;
    private LoadingDialogHandler loadingDialogHandler = new LoadingDialogHandler(
            this);
    private DataSet mDataSetTarmac;
    private boolean isARInitialized = false;
    // Alert Dialog used to display SDK errors
    private AlertDialog mErrorDialog;

    // Called when the activity first starts or the user navigates back
    // to an activity.
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOGTAG, "onCreate");
        super.onCreate(savedInstanceState);

        updateTargetCallback = new UpdateTargetCallback(this);

        startLoadingAnimation();

        updateTargetCallback
                .initAR(this, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Load any sample specific textures:
        mTextures = new Vector<Texture>();
        loadTextures();
    }

    // We want to load specific textures from the APK, which we will later
    // use for rendering.
    private void loadTextures() {
        mTextures.add(Texture.loadTextureFromApk(
                "BgdTexture/TextureTeapotRed.png", getAssets()));
    }

    // Called when the activity will start interacting with the user.
    protected void onResume() {
        Log.d(LOGTAG, "onResume");
        super.onResume();

        showProgressIndicator(true);
        updateTargetCallback.onResume();

        if (isARInitialized) {
            VideoBackgroundConfig config = Renderer.getInstance()
                    .getVideoBackgroundConfig();

            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
        }
    }

    // Callback for configuration changes the activity handles itself
    public void onConfigurationChanged(Configuration config) {
        Log.d(LOGTAG, "onConfigurationChanged");
        super.onConfigurationChanged(config);

        updateTargetCallback.onConfigurationChanged();

        if (isARInitialized) {
            VideoBackgroundConfig vconfig = Renderer.getInstance()
                    .getVideoBackgroundConfig();

            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
        }
    }

    // Called when the system is about to start resuming a previous activity.
    protected void onPause() {
        Log.d(LOGTAG, "onPause");
        super.onPause();

        if (mGlView != null) {
            mGlView.setVisibility(View.INVISIBLE);
            mGlView.onPause();
        }

        try {
            updateTargetCallback.pauseAR();
        } catch (SampleApplicationException e) {
            Log.e(LOGTAG, e.getString());
        }
    }

    // The final call you receive before your activity is destroyed.
    protected void onDestroy() {
        Log.d(LOGTAG, "onDestroy");
        super.onDestroy();

        try {
            updateTargetCallback.stopAR();
        } catch (SampleApplicationException e) {
            Log.e(LOGTAG, e.getString());
        }

        // Unload texture:
        mTextures.clear();
        mTextures = null;

        System.gc();
    }

    private void startLoadingAnimation() {
        mUILayout = (RelativeLayout) View.inflate(this, R.layout.camera_overlay,
                null);

        mUILayout.setVisibility(View.VISIBLE);
        mUILayout.setBackgroundColor(Color.BLACK);

        // Gets a reference to the loading dialog
        loadingDialogHandler.mLoadingDialogContainer = mUILayout
                .findViewById(R.id.loading_indicator);

        // Shows the loading indicator at start
        loadingDialogHandler.sendEmptyMessage(SHOW_LOADING_DIALOG);

        // Adds the inflated layout to the view
        addContentView(mUILayout, new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
    }

    // Initializes AR application components.
    private void initApplicationAR() {
        // Create OpenGL ES view:
        int depthSize = 16;
        int stencilSize = 0;
        boolean translucent = Vuforia.requiresAlpha();

        mGlView = new SampleApplicationGLView(this);
        mGlView.init(translucent, depthSize, stencilSize);

        mRenderer = new BackgroundTextureAccessRenderer(this, updateTargetCallback);
        mRenderer.mActivity = this;
        mRenderer.setTextures(mTextures);
        mGlView.setRenderer(mRenderer);
    }

    public boolean onTouchEvent(MotionEvent event) {
        float x, y;

        // Process the Gestures
        if (mSampleAppMenu != null)
            mSampleAppMenu.processEvent(event);

        if (mRenderer == null)
            return false;

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_UP:
                mRenderer.onTouchEvent(-100.0f, -100.0f);
                break;

            case MotionEvent.ACTION_POINTER_UP:
                mRenderer.onTouchEvent(-100.0f, -100.0f);
                break;

            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                break;

            case MotionEvent.ACTION_MOVE:
                DisplayMetrics metrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(metrics);

                x = Math.abs(event.getX(0)) / metrics.widthPixels;
                y = Math.abs(event.getY(0)) / metrics.heightPixels;

                if (x > 1.0f)
                    x = 1.0f;
                if (y > 1.0f)
                    y = 1.0f;

                mRenderer.onTouchEvent(x, y);
                break;

            default:
                break;
        }

        // Indicate that event was handled:
        return true;
    }

    // Tracker initialization and deinitialization.
    @Override
    public boolean doInitTrackers() {
        // Indicate if the trackers were initialized correctly
        boolean result = true;

        TrackerManager tManager = TrackerManager.getInstance();
        Tracker tracker = tManager.initTracker(ObjectTracker.getClassType());
        if (tracker == null) {
            Log.e(
                    LOGTAG,
                    "Tracker not initialized. Tracker already initialized or the camera is already started");
            result = false;
        } else {
            Log.i(LOGTAG, "Tracker successfully initialized");
        }

        return result;
    }

    @Override
    public boolean doDeinitTrackers() {
        // Indicate if the trackers were deinitialized correctly
        boolean result = true;

        TrackerManager tManager = TrackerManager.getInstance();
        tManager.deinitTracker(ObjectTracker.getClassType());

        return result;
    }

    // Functions to load and destroy tracking data.
    @Override
    public boolean doLoadTrackersData() {
        TrackerManager tManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) tManager
                .getTracker(ObjectTracker.getClassType());
        if (objectTracker == null)
            return false;

        mDataSetTarmac = objectTracker.createDataSet();
        if (mDataSetTarmac == null)
            return false;

        if (!mDataSetTarmac.load("Tarmac.xml",
                STORAGE_TYPE.STORAGE_APPRESOURCE))
            return false;

        if (!objectTracker.activateDataSet(mDataSetTarmac))
            return false;

        int numTrackables = mDataSetTarmac.getNumTrackables();
        for (int count = 0; count < numTrackables; count++) {
            String name = "Tarmac : "
                    + mDataSetTarmac.getTrackable(count).getName();
            mDataSetTarmac.getTrackable(count).setUserData(name);
            Log.d(LOGTAG, "UserData:Set the following user data "
                    + (String) mDataSetTarmac.getTrackable(count).getUserData());
        }

        return true;
    }

    @Override
    public boolean doUnloadTrackersData() {
        // Indicate if the trackers were unloaded correctly
        boolean result = true;

        TrackerManager tManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) tManager
                .getTracker(ObjectTracker.getClassType());
        if (objectTracker == null)
            return false;

        if (mDataSetTarmac != null && mDataSetTarmac.isActive()) {
            if (objectTracker.getActiveDataSet(0).equals(mDataSetTarmac)
                    && !objectTracker.deactivateDataSet(mDataSetTarmac)) {
                result = false;
            } else if (!objectTracker.destroyDataSet(mDataSetTarmac)) {
                result = false;
            }

            mDataSetTarmac = null;
        }

        return result;
    }

    // Returns the number of registered textures.
    public int getTextureCount() {
        return mTextures.size();
    }

    // Returns the texture object at the specified index.
    public Texture getTexture(int i) {
        return mTextures.elementAt(i);
    }

    @Override
    public boolean doStartTrackers() {
        // Indicate if the trackers were started correctly
        boolean result = true;

        Tracker objectTracker = TrackerManager.getInstance().getTracker(
                ObjectTracker.getClassType());
        if (objectTracker != null)
            objectTracker.start();

        return result;
    }

    @Override
    public boolean doStopTrackers() {
        // Indicate if the trackers were stopped correctly
        boolean result = true;

        Tracker objectTracker = TrackerManager.getInstance().getTracker(
                ObjectTracker.getClassType());
        if (objectTracker != null)
            objectTracker.stop();

        return result;
    }

    @Override
    public void onInitARDone(SampleApplicationException exception) {

        if (exception == null) {
            initApplicationAR();

            mRenderer.setActive(true);
            isARInitialized = true;

            // Now add the GL surface view. It is important
            // that the OpenGL ES surface view gets added
            // BEFORE the camera is started and video
            // background is configured.
            addContentView(mGlView, new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT));

            // Sets the UILayout to be drawn in front of the camera
            mUILayout.bringToFront();

            updateTargetCallback.startAR(CameraDevice.CAMERA_DIRECTION.CAMERA_DIRECTION_DEFAULT);

            // Hides the Loading Dialog
            loadingDialogHandler
                    .sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG);

            // Sets the layout background to transparent
            mUILayout.setBackgroundColor(Color.TRANSPARENT);

            VideoBackgroundConfig config = Renderer.getInstance()
                    .getVideoBackgroundConfig();

            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);

            mSampleAppMenu = new SampleAppMenu(this, this, "Background Texture",
                    mGlView, mUILayout, null);
            setSampleAppMenuSettings();

        } else {
            Log.e(LOGTAG, exception.getString());
            showInitializationErrorMessage(exception.getString());
        }

    }

    // Shows initialization error messages as System dialogs
    public void showInitializationErrorMessage(String message) {
        final String errorMessage = message;
        runOnUiThread(new Runnable() {
            public void run() {
                if (mErrorDialog != null) {
                    mErrorDialog.dismiss();
                }

                // Generates an Alert Dialog to show the error message
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        BackgroundTextureAccess.this);
                builder
                        .setMessage(errorMessage)
                        .setTitle(getString(R.string.INIT_ERROR))
                        .setCancelable(false)
                        .setIcon(0)
                        .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        finish();
                                    }
                                });

                mErrorDialog = builder.create();
                mErrorDialog.show();
            }
        });
    }

    @Override
    public void onVuforiaUpdate(State state) {
    }

    @Override
    public void onVuforiaResumed() {
        if (mGlView != null) {
            mGlView.setVisibility(View.VISIBLE);
            mGlView.onResume();
        }
    }

    @Override
    public void onVuforiaStarted() {
        mRenderer.updateRenderingPrimitives();

        // Set camera focus mode
        if (!CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO)) {
            // If continuous autofocus mode fails, attempt to set to a different mode
            if (!CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_TRIGGERAUTO)) {
                CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_NORMAL);
            }
        }

        showProgressIndicator(false);
    }

    public void showProgressIndicator(boolean show) {
        if (loadingDialogHandler != null) {
            if (show) {
                loadingDialogHandler
                        .sendEmptyMessage(LoadingDialogHandler.SHOW_LOADING_DIALOG);
            } else {
                loadingDialogHandler
                        .sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG);
            }
        }
    }

    // This method sets the menu's settings
    private void setSampleAppMenuSettings() {
        SampleAppMenuGroup group;

        group = mSampleAppMenu.addGroup("", false);
        group.addTextItem(getString(R.string.menu_back), -1);

        mSampleAppMenu.attachMenu();
    }


    @Override
    public boolean menuProcess(int command) {

        boolean result = true;

        switch (command) {
            case CMD_BACK:
                finish();
                break;

        }

        return result;
    }

}
