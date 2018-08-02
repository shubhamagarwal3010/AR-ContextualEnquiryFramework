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
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.RelativeLayout;

import com.thoughtworks.onboarding.BuildConfig;
import com.thoughtworks.onboarding.R;
import com.thoughtworks.onboarding.VuforiaApplication.ImageTrackerManager;
import com.thoughtworks.onboarding.VuforiaApplication.VuforiaApplicationException;
import com.thoughtworks.onboarding.VuforiaApplication.UpdateTargetCallback;
import com.thoughtworks.onboarding.utils.DialogUtils;
import com.thoughtworks.onboarding.utils.LoadingDialogHandler;
import com.thoughtworks.onboarding.utils.SampleApplicationGLView;
import com.thoughtworks.onboarding.utils.Texture;
import com.vuforia.CameraDevice;
import com.vuforia.ObjectTracker;
import com.vuforia.State;
import com.vuforia.TargetFinder;
import com.vuforia.TargetSearchResult;
import com.vuforia.Trackable;
import com.vuforia.Tracker;
import com.vuforia.TrackerManager;
import com.vuforia.Vuforia;

import java.util.Vector;

import static com.vuforia.TargetFinder.UPDATE_ERROR_NO_NETWORK_CONNECTION;
import static com.vuforia.TargetFinder.UPDATE_ERROR_SERVICE_NOT_AVAILABLE;


// The AR activity for the VideoPlayback sample.
public class VideoPlayback extends Activity implements ImageTrackerManager {
    private static final String LOGTAG = "VideoPlayback";
    final private static int CMD_BACK = -1;
    final private static int CMD_FULLSCREEN_VIDEO = 1;
    // Movie for the Targets:
    UpdateTargetCallback updateTargetCallback;
    Activity mActivity;
    boolean mIsInitialized = false;
    boolean mFinderStarted = false;
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
    private String mMovieName;
    private int mInitErrorCode = 0;
    private boolean mExtendedTracking = false;

    private TranslateAnimation scanAnimation;
    private View scanLine;

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
                "loading.png", getAssets()));
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
        } catch (VuforiaApplicationException e) {
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


        new DialogUtils() {
            @Override
            public void onPositiveButtonClick() {
                // pauseAll();
                finish();
            }

            @Override
            public void onNegativeButtonClick() {

            }
        }.showAlert(this, getString(R.string.exit_alert));

        // super.onBackPressed();
    }

    private void startLoadingAnimation() {
        // Inflates the Overlay Layout to be displayed above the Camera View
        LayoutInflater inflater = LayoutInflater.from(this);
        mUILayout = (RelativeLayout) inflater.inflate(R.layout.camera_overlay_with_scanline,
                null, false);

        mUILayout.setVisibility(View.VISIBLE);
        mUILayout.setBackgroundColor(Color.BLACK);

        // By default
        loadingDialogHandler.mLoadingDialogContainer = mUILayout
                .findViewById(R.id.loading_indicator);
        loadingDialogHandler.mLoadingDialogContainer
                .setVisibility(View.VISIBLE);

        scanLine = mUILayout.findViewById(R.id.scan_line);
        scanLine.setVisibility(View.GONE);
        scanAnimation = new TranslateAnimation(
                TranslateAnimation.ABSOLUTE, 0f,
                TranslateAnimation.ABSOLUTE, 0f,
                TranslateAnimation.RELATIVE_TO_PARENT, 0f,
                TranslateAnimation.RELATIVE_TO_PARENT, 1.0f);
        scanAnimation.setDuration(4000);
        scanAnimation.setRepeatCount(-1);
        scanAnimation.setRepeatMode(Animation.REVERSE);
        scanAnimation.setInterpolator(new LinearInterpolator());

        addContentView(mUILayout, new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
    }


    private void scanlineStart() {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                scanLine.setVisibility(View.VISIBLE);
                scanLine.setAnimation(scanAnimation);
            }
        });
    }

    private void scanlineStop() {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                scanLine.setVisibility(View.GONE);
                scanLine.clearAnimation();
            }
        });
    }


    public void startFinderIfStopped() {
        if (!mFinderStarted) {
            mFinderStarted = true;

            // Get the object tracker:
            TrackerManager trackerManager = TrackerManager.getInstance();
            ObjectTracker objectTracker = (ObjectTracker) trackerManager
                    .getTracker(ObjectTracker.getClassType());

            // Initialize target finder:
            TargetFinder targetFinder = objectTracker.getTargetFinder();

            targetFinder.clearTrackables();
            targetFinder.startRecognition();
            scanlineStart();
        }
    }

    public void stopFinderIfStarted() {
        if (mFinderStarted) {
            mFinderStarted = false;

            // Get the object tracker:
            TrackerManager trackerManager = TrackerManager.getInstance();
            ObjectTracker objectTracker = (ObjectTracker) trackerManager
                    .getTracker(ObjectTracker.getClassType());

            // Initialize target finder:
            TargetFinder targetFinder = objectTracker.getTargetFinder();

            targetFinder.stop();
            scanlineStop();
        }
    }

    private void starScanner() {
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
        //mRenderer.requestLoad(mMovieName, 0, false);

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

        // Initialize target finder:
        TargetFinder targetFinder = objectTracker.getTargetFinder();

        // Start initialization:
        if (targetFinder.startInit(BuildConfig.VUFORIA_ACCESS_KEY, BuildConfig.VUFORIA_SECRET_KEY)) {
            targetFinder.waitUntilInitFinished();
        }

        int resultCode = targetFinder.getInitState();
        if (resultCode != TargetFinder.INIT_SUCCESS) {
            if (resultCode == TargetFinder.INIT_ERROR_NO_NETWORK_CONNECTION) {
                mInitErrorCode = UPDATE_ERROR_NO_NETWORK_CONNECTION;
            } else {
                mInitErrorCode = UPDATE_ERROR_SERVICE_NOT_AVAILABLE;
            }

            Log.e(LOGTAG, "Failed to initialize target finder.");
            return false;
        }

        Log.d(LOGTAG, "Successfully loaded and activated data set.");
        return true;
    }

    @Override
    public boolean doStartTrackers() {
        // Indicate if the trackers were started correctly
        boolean result = true;

        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) trackerManager
                .getTracker(ObjectTracker.getClassType());
        objectTracker.start();

        // Start cloud based recognition if we are in scanning mode:
        TargetFinder targetFinder = objectTracker.getTargetFinder();
        targetFinder.startRecognition();
        mFinderStarted = true;


        return result;
    }

    @Override
    public boolean doStopTrackers() {
        // Indicate if the trackers were stopped correctly
        boolean result = true;

        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) trackerManager
                .getTracker(ObjectTracker.getClassType());

        if (objectTracker != null) {
            objectTracker.stop();

            // Stop cloud based recognition:
            TargetFinder targetFinder = objectTracker.getTargetFinder();
            targetFinder.stop();

            mFinderStarted = false;

            // Clears the trackables
            targetFinder.clearTrackables();
        } else {
            result = false;
        }

        return result;
    }

    @Override
    public boolean doUnloadTrackersData() {
        return true;
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
    public void onInitARDone(VuforiaApplicationException exception) {

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
    public void onVuforiaUpdate(State state) {// Get the tracker manager:
        TrackerManager trackerManager = TrackerManager.getInstance();

        // Get the object tracker:
        ObjectTracker objectTracker = (ObjectTracker) trackerManager
                .getTracker(ObjectTracker.getClassType());

        // Get the target finder:
        TargetFinder finder = objectTracker.getTargetFinder();

        // Check if there are new results available:
        final int statusCode = finder.updateSearchResults();

        // Show a message if we encountered an error:
        if (statusCode < 0) {

            boolean closeAppAfterError = (
                    statusCode == UPDATE_ERROR_NO_NETWORK_CONNECTION ||
                            statusCode == UPDATE_ERROR_SERVICE_NOT_AVAILABLE);
            Log.e(LOGTAG, "ci sono problemi!");
            //showErrorMessage(statusCode, state.getFrame().getTimeStamp(), closeAppAfterError);

        } else if (statusCode == TargetFinder.UPDATE_RESULTS_AVAILABLE) {
            // Process new search results
            if (finder.getResultCount() > 0) {
                TargetSearchResult result = finder.getResult(0);

                // Check if this target is suitable for tracking:
                if (result.getTrackingRating() > 0) {
                    Trackable trackable = finder.enableTracking(result);

                    if (mExtendedTracking)
                        trackable.startExtendedTracking();
                }
            }
        }
    }

    public void setUrl(String url) {
        if (mMovieName != url) {
            mMovieName = url;
//            mRenderer.requestLoad(mMovieName, mSeekPosition,
//                    false);
        }
    }
}
