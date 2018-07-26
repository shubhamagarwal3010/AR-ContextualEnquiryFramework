/*===============================================================================
Copyright (c) 2016-2017 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.
===============================================================================*/

package com.thoughtworks.onboarding.cloud;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import com.google.gson.Gson;
import com.thoughtworks.onboarding.SampleApplication.SampleAppRenderer;
import com.thoughtworks.onboarding.SampleApplication.SampleAppRendererControl;
import com.thoughtworks.onboarding.SampleApplication.UpdateTargetCallback;
import com.thoughtworks.onboarding.utils.CubeShaders;
import com.thoughtworks.onboarding.utils.Image;
import com.thoughtworks.onboarding.utils.SampleUtils;
import com.thoughtworks.onboarding.utils.Texture;
import com.vuforia.Device;
import com.vuforia.ImageTarget;
import com.vuforia.Matrix44F;
import com.vuforia.Renderer;
import com.vuforia.State;
import com.vuforia.Tool;
import com.vuforia.Trackable;
import com.vuforia.TrackableResult;
import com.vuforia.Vuforia;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


// The renderer class for the CloudReco sample. 
public class CloudRecoRenderer implements GLSurfaceView.Renderer, SampleAppRendererControl {
    private static final float OBJECT_SCALE_FLOAT = 0.003f;
    private UpdateTargetCallback vuforiaAppSession;
    private SampleAppRenderer mSampleAppRenderer;
    private int shaderProgramID;
    private int vertexHandle;
    private int textureCoordHandle;
    private int mvpMatrixHandle;
    private int texSampler2DHandle;

    private Image mImage = new Image();

    private CloudReco mActivity;

    private boolean mIsActive = false;

    public CloudRecoRenderer(UpdateTargetCallback session, CloudReco activity) {
        vuforiaAppSession = session;
        mActivity = activity;

        // SampleAppRenderer used to encapsulate the use of RenderingPrimitives setting
        // the device mode AR/VR and stereo mode
        mSampleAppRenderer = new SampleAppRenderer(this, mActivity, Device.MODE.MODE_AR, false, 0.010f, 5f);
    }


    // Called when the surface is created or recreated.
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Call Vuforia function to (re)initialize rendering after first use
        // or after OpenGL ES context was lost (e.g. after onPause/onResume):
        vuforiaAppSession.onSurfaceCreated();

        mSampleAppRenderer.onSurfaceCreated();
    }


    // Called when the surface changed size.
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        // Call Vuforia function to handle render surface size changes:
        vuforiaAppSession.onSurfaceChanged(width, height);

        // RenderingPrimitives to be updated when some rendering change is done
        mSampleAppRenderer.onConfigurationChanged(mIsActive);

        // Call function to initialize rendering:
        initRendering();
    }


    // Called to draw the current frame.
    @Override
    public void onDrawFrame(GL10 gl) {
        // Call our function to render content from SampleAppRenderer class
        mSampleAppRenderer.render();
    }


    public void setActive(boolean active) {
        mIsActive = active;

        if (mIsActive)
            mSampleAppRenderer.configureVideoBackground();
    }


    // Function for initializing the renderer.
    private void initRendering() {
        // Define clear color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, Vuforia.requiresAlpha() ? 0.0f
                : 1.0f);

        shaderProgramID = SampleUtils.createProgramFromShaderSrc(
                CubeShaders.CUBE_MESH_VERTEX_SHADER,
                CubeShaders.CUBE_MESH_FRAGMENT_SHADER);

        vertexHandle = GLES20.glGetAttribLocation(shaderProgramID,
                "vertexPosition");
        textureCoordHandle = GLES20.glGetAttribLocation(shaderProgramID,
                "vertexTexCoord");
        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgramID,
                "modelViewProjectionMatrix");
        texSampler2DHandle = GLES20.glGetUniformLocation(shaderProgramID,
                "texSampler2D");
        // mTeapot = new Teapot();
    }


    public void updateRenderingPrimitives() {
        mSampleAppRenderer.updateRenderingPrimitives();
    }


    // The render function.
    // The render function called from SampleAppRendering by using RenderingPrimitives views.
    // The state is owned by SampleAppRenderer which is controlling it's lifecycle.
    // State should not be cached outside this method.
    public void renderFrame(State state, float[] projectionMatrix) {
        // Renders video background replacing Renderer.DrawVideoBackground()
        mSampleAppRenderer.renderVideoBackground();

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        // Did we find any trackables this frame?
        if (state.getNumTrackableResults() > 0) {
            // Gets current trackable result
            TrackableResult trackableResult = state.getTrackableResult(0);

            if (trackableResult == null) {
                return;
            }

            mActivity.stopFinderIfStarted();

            for (int tIdx = 0; tIdx < state.getNumTrackableResults(); tIdx++) {
                System.out.println("Check****");
                TrackableResult result = state.getTrackableResult(tIdx);
                Trackable trackable = result.getTrackable();
                // The assumption is that we always scan images / static content for AR.
                // TODO: This might fail if we intend to go with video scanning, change accordingly
                ImageTarget imageTarget = (ImageTarget) trackable;
                TargetMetadata targetMetadata = new Gson().fromJson(imageTarget.getMetaData(), TargetMetadata.class);

                // Renders the Augmentation View with the 3D Book data Panel
                renderAugmentation(trackableResult, projectionMatrix, targetMetadata);
            }


        } else {
            mActivity.startFinderIfStopped();
        }

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        Renderer.getInstance().end();
    }


    private void renderAugmentation(TrackableResult trackableResult, float[] projectionMatrix, TargetMetadata metadata) {


        Texture t = Texture.loadTextureFromUrl(mActivity, metadata.url);


        GLES20.glGenTextures(1, t.mTextureID, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t.mTextureID[0]);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                t.mWidth, t.mHeight, 0, GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE, t.mData);


        Matrix44F modelViewMatrix_Vuforia = Tool
                .convertPose2GLMatrix(trackableResult.getPose());
        float[] modelViewMatrix = modelViewMatrix_Vuforia.getData();

        int textureIndex = 0;

        // deal with the modelview and projection matrices
        float[] modelViewProjection = new float[16];
        Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f, OBJECT_SCALE_FLOAT);
        Matrix.scaleM(modelViewMatrix, 0, OBJECT_SCALE_FLOAT,
                OBJECT_SCALE_FLOAT, OBJECT_SCALE_FLOAT);
        Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelViewMatrix, 0);

        // activate the shader program and bind the vertex/normal/tex coords
        GLES20.glUseProgram(shaderProgramID);
        GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false,
                0, mImage.getVertices());
        GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT,
                false, 0, mImage.getTexCoords());

        GLES20.glEnableVertexAttribArray(vertexHandle);
        GLES20.glEnableVertexAttribArray(textureCoordHandle);

        // activate texture 0, bind it, and pass to shader
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                t.mTextureID[0]);
        GLES20.glUniform1i(texSampler2DHandle, 0);

        // pass the model view matrix to the shader
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                modelViewProjection, 0);

        // finally draw the teapot
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, mImage.getNumObjectIndex(),
                GLES20.GL_UNSIGNED_SHORT, mImage.getIndices());

        // disable the enabled arrays
        GLES20.glDisableVertexAttribArray(vertexHandle);
        GLES20.glDisableVertexAttribArray(textureCoordHandle);

        SampleUtils.checkGLError("CloudReco renderFrame");
    }

}
