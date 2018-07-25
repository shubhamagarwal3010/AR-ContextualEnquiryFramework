package com.example.stammana.dualaugmentationapp.AugmentedDisplay;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.example.stammana.dualaugmentationapp.SampleApplication.SampleAppRenderer;
import com.example.stammana.dualaugmentationapp.SampleApplication.SampleAppRendererControl;
import com.example.stammana.dualaugmentationapp.SampleApplication.UpdateTargetCallback;
import com.example.stammana.dualaugmentationapp.utils.Texture;
import com.vuforia.Device;
import com.vuforia.State;
import com.vuforia.Trackable;
import com.vuforia.TrackableResult;
import com.vuforia.Vec3F;
import com.vuforia.Vuforia;

import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

// The renderer class for the AugmentedDisplay sample.
public class AugmentedRenderer implements GLSurfaceView.Renderer, SampleAppRendererControl {

    private static final String LOGTAG = "AugmentedRenderer";
    public AugmentedDisplay mActivity;
    private ImageDisplayRenderer imageDisplayRenderer;
    private VideoPlaybackRenderer videoPlaybackRenderer;
    SampleAppRenderer mSampleAppRenderer;
    // Trackable dimensions
    Vec3F targetPositiveDimensions[] = new Vec3F[AugmentedDisplay.NUM_TARGETS];
    // Video Playback Textures for the two targets
    int videoPlaybackTextureID[] = new int[AugmentedDisplay.NUM_TARGETS];

    public AugmentedRenderer(AugmentedDisplay activity,
                             UpdateTargetCallback session) {
        // SampleAppRenderer used to encapsulate the use of RenderingPrimitives setting/
        // the device mode AR/VR and stereo mode
        mActivity = activity;
        mSampleAppRenderer =
                new SampleAppRenderer(this, mActivity, Device.MODE.MODE_AR, false, 0.01f, 5f);

        imageDisplayRenderer = new ImageDisplayRenderer(activity, session, mSampleAppRenderer);
        videoPlaybackRenderer = new VideoPlaybackRenderer(activity, session, targetPositiveDimensions, videoPlaybackTextureID);
    }

    // Store the Player Helper object passed from the main activity
    public void setVideoPlayerHelper(int target,
                                     VideoPlayerHelper newVideoPlayerHelper) {
        videoPlaybackRenderer.setVideoPlayerHelper(target, newVideoPlayerHelper);
    }


    public void requestLoad(int target, String movieName, int seekPosition,
                            boolean playImmediately) {
        videoPlaybackRenderer.requestLoad(target, movieName, seekPosition, playImmediately);
    }

    // Called when the surface is created or recreated.
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d(LOGTAG, "GLRenderer.onSurfaceCreated");
        imageDisplayRenderer.onSurfaceCreated(gl, config, mSampleAppRenderer);
        videoPlaybackRenderer.onSurfaceCreated(gl, config, mSampleAppRenderer);
    }

    // Called when the surface changed size.
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d(LOGTAG, "GLRenderer.onSurfaceChanged");
        // Call Vuforia function to handle render surface size changes:
        Vuforia.onSurfaceChanged(width, height);

        imageDisplayRenderer.onSurfaceChanged(gl, width, height, mSampleAppRenderer);
        videoPlaybackRenderer.onSurfaceChanged(gl, width, height, mSampleAppRenderer);


    }

    // Called to draw the current frame.
    public void onDrawFrame(GL10 gl) {
        imageDisplayRenderer.onDrawFrame(gl, mSampleAppRenderer);
        videoPlaybackRenderer.onDrawFrame(gl, mSampleAppRenderer);

    }

    public void setActive(boolean active) {
        imageDisplayRenderer.setActive(active, mSampleAppRenderer);
        videoPlaybackRenderer.setActive(active, mSampleAppRenderer);
    }

    public void updateRenderingPrimitives() {
        imageDisplayRenderer.updateRenderingPrimitives(mSampleAppRenderer);
        videoPlaybackRenderer.updateRenderingPrimitives(mSampleAppRenderer);
    }

    // The render function called from SampleAppRendering by using RenderingPrimitives views.
    // The state is owned by SampleAppRenderer which is controlling it's lifecycle.
    // State should not be cached outside this method.

    public void renderFrame(State state, float[] projectionMatrix) {

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        mSampleAppRenderer.renderVideoBackground();
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // handle face culling, we need to detect if we are using reflection
        // to determine the direction of the culling
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);
        for (int tIdx = 0; tIdx < state.getNumTrackableResults(); tIdx++) {
            System.out.println("Check****");
            TrackableResult result = state.getTrackableResult(tIdx);
            Trackable trackable = result.getTrackable();
            if (trackable.getName().equals("alluri") || trackable.getName().equals("aaron")) {
                imageDisplayRenderer.renderFrame(state, projectionMatrix, mSampleAppRenderer);
            } else {
                videoPlaybackRenderer.renderFrame(state, projectionMatrix, mSampleAppRenderer);
            }

        }
    }

    public void setTextures(Vector<Texture> textures) {
        imageDisplayRenderer.setTextures(textures);
        videoPlaybackRenderer.setTextures(textures);
    }


}
