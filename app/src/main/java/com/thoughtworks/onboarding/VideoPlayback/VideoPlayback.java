package com.thoughtworks.onboarding.VideoPlayback;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;

import com.thoughtworks.onboarding.R;
import com.thoughtworks.onboarding.SampleApplication.ImageTrackerManager;
import com.thoughtworks.onboarding.SampleApplication.SampleApplicationException;
import com.thoughtworks.onboarding.SampleApplication.UpdateTargetCallback;
import com.thoughtworks.onboarding.utils.LoadingDialogHandler;
import com.thoughtworks.onboarding.utils.SampleApplicationGLView;
import com.thoughtworks.onboarding.utils.Texture;
import com.vuforia.CameraDevice;
import com.vuforia.DataSet;
import com.vuforia.HINT;
import com.vuforia.ObjectTracker;
import com.vuforia.STORAGE_TYPE;
import com.vuforia.State;
import com.vuforia.Tracker;
import com.vuforia.TrackerManager;
import com.vuforia.Vuforia;

import java.util.Vector;


// The AR activity for the VideoPlayback sample.
public class VideoPlayback extends Activity implements
        ImageTrackerManager {
    private static final String LOGTAG = "VideoPlayback";
    final private static int CMD_BACK = -1;
    final private static int CMD_FULLSCREEN_VIDEO = 1;
    // Movie for the Targets:
    UpdateTargetCallback updateTargetCallback;
    Activity mActivity;
    DataSet dataSetStonesAndChips = null;
    boolean mIsInitialized = false;
    // Helpers to detect events such as double tapping:
    private GestureDetector videoGestureDetector = null;
    private SimpleOnGestureListener videoGestureListner = null;
    private VideoPlayerHelper mVideoPlayerHelper = null;
    private int videoSeekPosition;
    private boolean mWasPlaying;
    // A boolean to indicate whether we come from full screen:
    private boolean mReturningFromFullScreen = false;
    // Our OpenGL view:
    private SampleApplicationGLView mGlView;
    // Our renderer:
    private VideoPlaybackRenderer mRenderer;
    // The textures we will use for rendering:
    private Vector<Texture> mTextures;
    private RelativeLayout mUILayout;
    private boolean mPlayFullscreenVideo = false;
    private LoadingDialogHandler loadingDialogHandler = new LoadingDialogHandler(
            this);
    // Alert Dialog used to display SDK errors
    private AlertDialog mErrorDialog;

    private String mMovieName = "VideoPlayback/SampleVideo.mp4";

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

        mActivity = this;

        // Create the gesture detector that will handle the single and
        // double taps:
        videoGestureListner = new SimpleOnGestureListener();
        videoGestureDetector = new GestureDetector(getApplicationContext(),
                videoGestureListner);
        mVideoPlayerHelper = null;
        videoSeekPosition = 0;
        mWasPlaying = false;

        // Create the video player helper that handles the playback of the movie
        // for the targets:
        mVideoPlayerHelper = new VideoPlayerHelper(this);
    }

    // We want to load specific textures from the APK, which we will later
    // use for rendering.
    private void loadTextures() {
        mTextures.add(Texture.loadTextureFromApk(
                "VideoPlayback/TextureTransparent.png", getAssets()));
        mTextures.add(Texture.loadTextureFromApk(
                "VideoPlayback/TextureTransparent.png", getAssets()));
    }

    // Called when the activity will start interacting with the user.
    protected void onResume() {
        Log.d(LOGTAG, "onResume");
        super.onResume();

        showProgressIndicator(true);
        updateTargetCallback.onResume();

        // Reload all the movies
        if (mRenderer != null) {
            if (!mReturningFromFullScreen) {
                mRenderer.requestLoad(mMovieName, videoSeekPosition,
                        false);
            } else {
                mRenderer.requestLoad(mMovieName, videoSeekPosition,
                        mWasPlaying);
            }
        }

        mReturningFromFullScreen = false;
    }

    // Called when returning from the full screen player
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {

            mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

            if (resultCode == RESULT_OK) {
                // The following values are used to indicate the position in
                // which the video was being played and whether it was being
                // played or not:
                String movieBeingPlayed = data.getStringExtra("movieName");
                mReturningFromFullScreen = true;

                // Find the movie that was being played full screen
                if (movieBeingPlayed.compareTo(mMovieName) == 0) {
                    videoSeekPosition = data.getIntExtra(
                            "currentSeekPosition", 0);
                    mWasPlaying = false;
                }
            }
        }
    }

    public void onConfigurationChanged(Configuration config) {
        Log.d(LOGTAG, "onConfigurationChanged");
        super.onConfigurationChanged(config);

        updateTargetCallback.onConfigurationChanged();
    }

    // Called when the system is about to start resuming a previous activity.
    protected void onPause() {
        Log.d(LOGTAG, "onPause");
        super.onPause();

        if (mGlView != null) {
            mGlView.setVisibility(View.INVISIBLE);
            mGlView.onPause();
        }

        // Store the playback state of the movies and unload them:
        // If the activity is paused we need to store the position in which
        // this was currently playing:
        if (mVideoPlayerHelper.isPlayableOnTexture()) {
            videoSeekPosition = mVideoPlayerHelper.getCurrentPosition();
            mWasPlaying = mVideoPlayerHelper.getStatus() == VideoPlayerHelper.MEDIA_STATE.PLAYING;
        }

        // We also need to release the resources used by the helper, though
        // we don't need to destroy it:
        if (mVideoPlayerHelper != null)
            mVideoPlayerHelper.unload();

        mReturningFromFullScreen = false;

        updateTargetCallback.pauseAR();
    }

    // The final call you receive before your activity is destroyed.
    protected void onDestroy() {
        Log.d(LOGTAG, "onDestroy");
        super.onDestroy();

        // If the activity is destroyed we need to release all resources:
        if (mVideoPlayerHelper != null)
            mVideoPlayerHelper.deinit();
        mVideoPlayerHelper = null;

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

    // Pause all movies except one
    // if the value of 'except' is -1 then
    // do a blanket pause
    private void pauseAll() {
        // And pause all the playing videos:
        // We can make one exception to the pause all calls:
        // Check if the video is playable on texture
        if (mVideoPlayerHelper.isPlayableOnTexture()) {
            // If it is playing then we pause it
            mVideoPlayerHelper.pause();
        }
    }

    // Do not exit immediately and instead show the startup screen
    public void onBackPressed() {
        pauseAll();
        super.onBackPressed();
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
        loadingDialogHandler
                .sendEmptyMessage(LoadingDialogHandler.SHOW_LOADING_DIALOG);

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

        mRenderer = new VideoPlaybackRenderer(this, updateTargetCallback);
        mRenderer.setTextures(mTextures);

        // The renderer comes has the OpenGL context, thus, loading to texture
        // must happen when the surface has been created. This means that we
        // can't load the movie from this thread (GUI) but instead we must
        // tell the GL thread to load it once the surface has been created.
        mRenderer.setVideoPlayerHelper(0, mVideoPlayerHelper);
        mRenderer.requestLoad(mMovieName, 0, false);

        mGlView.setRenderer(mRenderer);

        float[] temp = {0f, 0f, 0f};
        mRenderer.targetPositiveDimensions.setData(temp);
        mRenderer.videoPlaybackTextureID[0] = -1;

    }

    // We do not handle the touch event here, we just forward it to the
    // gesture detector
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = false;
        // Process the Gestures
        if (!result)
            videoGestureDetector.onTouchEvent(event);

        return result;
    }

    @Override
    public boolean doInitTrackers() {
        // Indicate if the trackers were initialized correctly
        boolean result = true;

        // Initialize the image tracker:
        TrackerManager trackerManager = TrackerManager.getInstance();
        Tracker tracker = trackerManager.initTracker(ObjectTracker
                .getClassType());
        if (tracker == null) {
            Log.d(LOGTAG, "Failed to initialize ObjectTracker.");
            result = false;
        }

        return result;
    }

    @Override
    public boolean doLoadTrackersData() {
        // Get the image tracker:
        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) trackerManager
                .getTracker(ObjectTracker.getClassType());
        if (objectTracker == null) {
            Log.d(
                    LOGTAG,
                    "Failed to load tracking data set because the ObjectTracker has not been initialized.");
            return false;
        }

        // Create the data sets:
        dataSetStonesAndChips = objectTracker.createDataSet();
        if (dataSetStonesAndChips == null) {
            Log.d(LOGTAG, "Failed to create a new tracking data.");
            return false;
        }

        // Load the data sets:
        if (!dataSetStonesAndChips.load("StonesAndChips.xml",
                STORAGE_TYPE.STORAGE_APPRESOURCE)) {
            Log.d(LOGTAG, "Failed to load data set.");
            return false;
        }

        // Activate the data set:
        if (!objectTracker.activateDataSet(dataSetStonesAndChips)) {
            Log.d(LOGTAG, "Failed to activate data set.");
            return false;
        }

        Log.d(LOGTAG, "Successfully loaded and activated data set.");
        return true;
    }

    @Override
    public boolean doStartTrackers() {
        // Indicate if the trackers were started correctly
        boolean result = true;

        Tracker objectTracker = TrackerManager.getInstance().getTracker(
                ObjectTracker.getClassType());
        if (objectTracker != null) {
            objectTracker.start();
            Vuforia.setHint(HINT.HINT_MAX_SIMULTANEOUS_IMAGE_TARGETS, 2);
        } else
            result = false;

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
        else
            result = false;

        return result;
    }

    @Override
    public boolean doUnloadTrackersData() {
        // Indicate if the trackers were unloaded correctly
        boolean result = true;

        // Get the image tracker:
        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) trackerManager
                .getTracker(ObjectTracker.getClassType());
        if (objectTracker == null) {
            Log.d(
                    LOGTAG,
                    "Failed to destroy the tracking data set because the ObjectTracker has not been initialized.");
            return false;
        }

        if (dataSetStonesAndChips != null) {
            if (objectTracker.getActiveDataSet(0) == dataSetStonesAndChips
                    && !objectTracker.deactivateDataSet(dataSetStonesAndChips)) {
                Log.d(
                        LOGTAG,
                        "Failed to destroy the tracking data set StonesAndChips because the data set could not be deactivated.");
                result = false;
            } else if (!objectTracker.destroyDataSet(dataSetStonesAndChips)) {
                Log.d(LOGTAG,
                        "Failed to destroy the tracking data set StonesAndChips.");
                result = false;
            }

            dataSetStonesAndChips = null;
        }

        return result;
    }

    @Override
    public boolean doDeinitTrackers() {
        // Indicate if the trackers were deinitialized correctly
        boolean result = true;

        // Deinit the image tracker:
        TrackerManager trackerManager = TrackerManager.getInstance();
        trackerManager.deinitTracker(ObjectTracker.getClassType());

        return result;
    }

    @Override
    public void onInitARDone(SampleApplicationException exception) {

        if (exception == null) {
            initApplicationAR();

            mRenderer.setActive(true);

            // Now add the GL surface view. It is important
            // that the OpenGL ES surface view gets added
            // BEFORE the camera is started and video
            // background is configured.
            addContentView(mGlView, new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT));

            // Sets the UILayout to be drawn in front of the camera
            mUILayout.bringToFront();

            // Hides the Loading Dialog
            loadingDialogHandler
                    .sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG);

            // Sets the layout background to transparent
            mUILayout.setBackgroundColor(Color.TRANSPARENT);

            updateTargetCallback.startAR(CameraDevice.CAMERA_DIRECTION.CAMERA_DIRECTION_DEFAULT);

            mIsInitialized = true;

        } else {
            Log.e(LOGTAG, exception.getString());
            showInitializationErrorMessage(exception.getString());
        }

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
                        VideoPlayback.this);
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


    public boolean menuProcess(int command) {

        boolean result = true;

        switch (command) {
            case CMD_BACK:
                finish();
                break;

            case CMD_FULLSCREEN_VIDEO:
                mPlayFullscreenVideo = !mPlayFullscreenVideo;

                if (mVideoPlayerHelper.getStatus() == VideoPlayerHelper.MEDIA_STATE.PLAYING) {
                    // If it is playing then we pause it
                    mVideoPlayerHelper.pause();

                    mVideoPlayerHelper.play(true,
                            videoSeekPosition);
                }
                break;

        }

        return result;
    }
}