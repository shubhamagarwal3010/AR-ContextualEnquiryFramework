/*===============================================================================
Copyright (c) 2015-2016 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other
countries.
===============================================================================*/
package com.vuforia.samples.ImageDisplay;


import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import com.vuforia.Device;
import com.vuforia.Matrix44F;
import com.vuforia.Renderer;
import com.vuforia.State;
import com.vuforia.Tool;
import com.vuforia.Trackable;
import com.vuforia.TrackableResult;
import com.vuforia.VIDEO_BACKGROUND_REFLECTION;
import com.vuforia.Vuforia;
import com.vuforia.samples.SampleApplication.SampleAppRenderer;
import com.vuforia.samples.SampleApplication.SampleAppRendererControl;
import com.vuforia.samples.SampleApplication.UpdateTargetCallback;
import com.vuforia.samples.SampleApplication.utils.CubeShaders;
import com.vuforia.samples.SampleApplication.utils.LoadingDialogHandler;
import com.vuforia.samples.SampleApplication.utils.SampleApplication3DModel;
import com.vuforia.samples.SampleApplication.utils.SampleUtils;
import com.vuforia.samples.SampleApplication.utils.Teapot2D;
import com.vuforia.samples.SampleApplication.utils.Texture;

import java.io.IOException;
import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


// The renderer class for the BackgroundTextureAccess sample.
public class ImageDisplayRenderer implements GLSurfaceView.Renderer, SampleAppRendererControl {
    private static final String LOGTAG = "IMDRenderer";
    private static final float OBJECT_SCALE_FLOAT = 0.003f;
    public ImageDisplay mActivity;
    UpdateTargetCallback vuforiaAppSession;
    SampleAppRenderer mSampleAppRenderer;
    private boolean mIsActive = false;
    private Vector<Texture> mTextures;

    private int shaderProgramID;
    private SampleApplication3DModel mBuildingsModel;
    private int normalHandle;

    private int vertexHandle;

    private int textureCoordHandle;

    private int mvpMatrixHandle;

    private int texSampler2DHandle;
    private Renderer mRenderer;

    private Teapot2D mTeapotImage;
    private float kBuildingScale = 12.0f;

    public ImageDisplayRenderer(ImageDisplay activity, UpdateTargetCallback appSession) {

        Log.d(LOGTAG, "ImageDisplayRenderer constructor");
        vuforiaAppSession = appSession;
        mActivity = activity;

        // SampleAppRenderer used to encapsulate the use of RenderingPrimitives setting
        // the device mode AR/VR and stereo mode
        mSampleAppRenderer = new SampleAppRenderer(this, mActivity, Device.MODE.MODE_AR, false, 0.01f, 5f);
    }

    /**
     * Called to draw the current frame.
     */
    public void onDrawFrame(GL10 gl) {
        if (!mIsActive)
            return;

//        renderFrame();
        // Call our function to render content from SampleAppRenderer class
        mSampleAppRenderer.render();
    }

    // Called when the surface is created or recreated.
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d(LOGTAG, "GLRenderer.onSurfaceCreated");

        // Call function to initialize rendering:
        initRendering();

//        vuforiaAppSession.onSurfaceCreated();

        // Call Vuforia function to (re)initialize rendering after first use
        // or after OpenGL ES context was lost (e.g. after onPause/onResume):
        Vuforia.onSurfaceCreated();

        mSampleAppRenderer.onSurfaceCreated();

    }


    // Called when the surface changed size.
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d(LOGTAG, "GLRenderer.onSurfaceChanged");
//        vuforiaAppSession.onSurfaceChanged(width, height);

        // Call Vuforia function to handle render surface size changes:
        Vuforia.onSurfaceChanged(width, height);

        // RenderingPrimitives to be updated when some rendering change is done
        updateRenderingPrimitives();

        // Call function to initialize rendering:
        initRendering();
    }


    // Renderer initializing function.
    public void initRendering() {
        mTeapotImage = new Teapot2D();

        mRenderer = Renderer.getInstance();

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, Vuforia.requiresAlpha() ? 0.0f
                : 1.0f);

        for (Texture t : mTextures) {
            GLES20.glGenTextures(1, t.mTextureID, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t.mTextureID[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, t.mWidth, t.mHeight, 0, GLES20.GL_RGBA,
                    GLES20.GL_UNSIGNED_BYTE, t.mData);
        }

        shaderProgramID = SampleUtils.createProgramFromShaderSrc(
                CubeShaders.CUBE_MESH_VERTEX_SHADER,
                CubeShaders.CUBE_MESH_FRAGMENT_SHADER);

        vertexHandle = GLES20.glGetAttribLocation(shaderProgramID,
                "vertexPosition");
        normalHandle = GLES20.glGetAttribLocation(shaderProgramID,
                "vertexNormal");
        textureCoordHandle = GLES20.glGetAttribLocation(shaderProgramID,
                "vertexTexCoord");
        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgramID,
                "modelViewProjectionMatrix");
        texSampler2DHandle = GLES20.glGetUniformLocation(shaderProgramID,
                "texSampler2D");

        try {
            mBuildingsModel = new SampleApplication3DModel();
            mBuildingsModel.loadModel(mActivity.getResources().getAssets(),
                    "ImageTargets/Buildings.txt");
        } catch (IOException e) {
            Log.e(LOGTAG, "Unable to load buildings");
        }

        // Hide the Loading Dialog
        mActivity.loadingDialogHandler
                .sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG);

    }


   /* // The render function called from SampleAppRendering by using RenderingPrimitives views.
    // The state is owned by SampleAppRenderer which is controlling it's lifecycle.
    // State should not be cached outside this method.
    public void renderFrame() {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        State state = mRenderer.begin();
//        mRenderer.drawVideoBackground();
        mSampleAppRenderer.renderVideoBackground();

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

       *//* // Set the viewport
        int[] viewport = vuforiaAppSession.getViewport();
        GLES20.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
*//*
        // handle face culling, we need to detect if we are using reflection
        // to determine the direction of the culling
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);
        if (Renderer.getInstance().getVideoBackgroundConfig().getReflection() == VIDEO_BACKGROUND_REFLECTION.VIDEO_BACKGROUND_REFLECTION_ON)
            GLES20.glFrontFace(GLES20.GL_CW); // Front camera
        else
            GLES20.glFrontFace(GLES20.GL_CCW); // Back camera

        // did we find any trackables this frame?
        for (int tIdx = 0; tIdx < state.getNumTrackableResults(); tIdx++) {
            TrackableResult result = state.getTrackableResult(tIdx);
            Trackable trackable = result.getTrackable();
            printUserData(trackable);
            Matrix44F modelViewMatrix_Vuforia = Tool
                    .convertPose2GLMatrix(result.getPose());
            float[] modelViewMatrix = modelViewMatrix_Vuforia.getData();

            int textureIndex = trackable.getName().equalsIgnoreCase("stones") ? 0
                    : 1;
            textureIndex = trackable.getName().equalsIgnoreCase("tarmac") ? 2
                    : textureIndex;

            // deal with the modelview and projection matrices
            float[] modelViewProjection = new float[16];

            if (!mActivity.isExtendedTrackingActive()) {
                Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f,
                        OBJECT_SCALE_FLOAT);
                Matrix.scaleM(modelViewMatrix, 0, OBJECT_SCALE_FLOAT,
                        OBJECT_SCALE_FLOAT, OBJECT_SCALE_FLOAT);
            } else {
                Matrix.rotateM(modelViewMatrix, 0, 90.0f, 1.0f, 0, 0);
                Matrix.scaleM(modelViewMatrix, 0, kBuildingScale,
                        kBuildingScale, kBuildingScale);
            }

            Matrix.multiplyMM(modelViewProjection, 0, vuforiaAppSession
                    .getProjectionMatrix().getData(), 0, modelViewMatrix, 0);

            // activate the shader program and bind the vertex/normal/tex coords
            GLES20.glUseProgram(shaderProgramID);

            if (!mActivity.isExtendedTrackingActive()) {
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mTeapotImage.getVertices());
                GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mTeapotImage.getNormals());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                        GLES20.GL_FLOAT, false, 0, mTeapotImage.getTexCoords());

                GLES20.glEnableVertexAttribArray(vertexHandle);
                GLES20.glEnableVertexAttribArray(normalHandle);
                GLES20.glEnableVertexAttribArray(textureCoordHandle);

                // activate texture 0, bind it, and pass to shader
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                        mTextures.get(textureIndex).mTextureID[0]);
                GLES20.glUniform1i(texSampler2DHandle, 0);

                // pass the model view matrix to the shader
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                        modelViewProjection, 0);

                // finally draw the teapot
                GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                        mTeapotImage.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT,
                        mTeapotImage.getIndices());

                // disable the enabled arrays
                GLES20.glDisableVertexAttribArray(vertexHandle);
                GLES20.glDisableVertexAttribArray(normalHandle);
                GLES20.glDisableVertexAttribArray(textureCoordHandle);
            } else {
                GLES20.glDisable(GLES20.GL_CULL_FACE);
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mBuildingsModel.getVertices());
                GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mBuildingsModel.getNormals());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                        GLES20.GL_FLOAT, false, 0, mBuildingsModel.getTexCoords());

                GLES20.glEnableVertexAttribArray(vertexHandle);
                GLES20.glEnableVertexAttribArray(normalHandle);
                GLES20.glEnableVertexAttribArray(textureCoordHandle);

                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                        mTextures.get(3).mTextureID[0]);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                        modelViewProjection, 0);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0,
                        mBuildingsModel.getNumObjectVertex());

                SampleUtils.checkGLError("Renderer DrawBuildings");
            }

            SampleUtils.checkGLError("Render Frame");

        }

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        mRenderer.end();
    }*/


    public void setActive(boolean active) {
        mIsActive = active;

        if (mIsActive)
            mSampleAppRenderer.configureVideoBackground();
    }

    public void updateRenderingPrimitives() {
        mSampleAppRenderer.onConfigurationChanged(mIsActive);
    }


    @SuppressWarnings("unused")
    private void printUserData(Trackable trackable) {
        String userData = (String) trackable.getUserData();
        Log.d(LOGTAG, "UserData.Retreived User Data	\"" + userData + "\"");
    }


    public void setTextures(Vector<Texture> textures) {
        mTextures = textures;
    }


    public void onTouchEvent(float x, float y) {
        mSampleAppRenderer.onTouchEvent(x, y);
    }

    @Override
    public void renderFrame(State state, float[] projectionMatrix) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        state = mRenderer.begin();
//        mRenderer.drawVideoBackground();
        mSampleAppRenderer.renderVideoBackground();

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

       /* // Set the viewport
        int[] viewport = vuforiaAppSession.getViewport();
        GLES20.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
*/
        // handle face culling, we need to detect if we are using reflection
        // to determine the direction of the culling
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);
        if (Renderer.getInstance().getVideoBackgroundConfig().getReflection() == VIDEO_BACKGROUND_REFLECTION.VIDEO_BACKGROUND_REFLECTION_ON)
            GLES20.glFrontFace(GLES20.GL_CW); // Front camera
        else
            GLES20.glFrontFace(GLES20.GL_CCW); // Back camera

        // did we find any trackables this frame?
        for (int tIdx = 0; tIdx < state.getNumTrackableResults(); tIdx++) {
            TrackableResult result = state.getTrackableResult(tIdx);
            Trackable trackable = result.getTrackable();
            printUserData(trackable);
            Matrix44F modelViewMatrix_Vuforia = Tool
                    .convertPose2GLMatrix(result.getPose());
            float[] modelViewMatrix = modelViewMatrix_Vuforia.getData();

            int textureIndex = trackable.getName().equalsIgnoreCase("stones") ? 0
                    : 1;
            textureIndex = trackable.getName().equalsIgnoreCase("tarmac") ? 2
                    : textureIndex;

            // deal with the modelview and projection matrices
            float[] modelViewProjection = new float[16];

            if (!mActivity.isExtendedTrackingActive()) {
                Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f,
                        OBJECT_SCALE_FLOAT);
                Matrix.scaleM(modelViewMatrix, 0, OBJECT_SCALE_FLOAT,
                        OBJECT_SCALE_FLOAT, OBJECT_SCALE_FLOAT);
            } else {
                Matrix.rotateM(modelViewMatrix, 0, 90.0f, 1.0f, 0, 0);
                Matrix.scaleM(modelViewMatrix, 0, kBuildingScale,
                        kBuildingScale, kBuildingScale);
            }

            Matrix.multiplyMM(modelViewProjection, 0,
                    projectionMatrix, 0, modelViewMatrix, 0);

            // activate the shader program and bind the vertex/normal/tex coords
            GLES20.glUseProgram(shaderProgramID);

            if (!mActivity.isExtendedTrackingActive()) {
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mTeapotImage.getVertices());
                GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mTeapotImage.getNormals());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                        GLES20.GL_FLOAT, false, 0, mTeapotImage.getTexCoords());

                GLES20.glEnableVertexAttribArray(vertexHandle);
                GLES20.glEnableVertexAttribArray(normalHandle);
                GLES20.glEnableVertexAttribArray(textureCoordHandle);

                // activate texture 0, bind it, and pass to shader
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                        mTextures.get(textureIndex).mTextureID[0]);
                GLES20.glUniform1i(texSampler2DHandle, 0);

                // pass the model view matrix to the shader
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                        modelViewProjection, 0);

                // finally draw the teapot
                GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                        mTeapotImage.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT,
                        mTeapotImage.getIndices());

                // disable the enabled arrays
                GLES20.glDisableVertexAttribArray(vertexHandle);
                GLES20.glDisableVertexAttribArray(normalHandle);
                GLES20.glDisableVertexAttribArray(textureCoordHandle);
            } else {
                GLES20.glDisable(GLES20.GL_CULL_FACE);
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mBuildingsModel.getVertices());
                GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mBuildingsModel.getNormals());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                        GLES20.GL_FLOAT, false, 0, mBuildingsModel.getTexCoords());

                GLES20.glEnableVertexAttribArray(vertexHandle);
                GLES20.glEnableVertexAttribArray(normalHandle);
                GLES20.glEnableVertexAttribArray(textureCoordHandle);

                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                        mTextures.get(3).mTextureID[0]);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                        modelViewProjection, 0);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0,
                        mBuildingsModel.getNumObjectVertex());

                SampleUtils.checkGLError("Renderer DrawBuildings");
            }

            SampleUtils.checkGLError("Render Frame");

        }

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        mRenderer.end();
    }
}
