/*===============================================================================
Copyright (c) 2015-2016 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other
countries.
===============================================================================*/
package com.thoughtworks.onboarding.AugmentedDisplay;


import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;
import android.util.Pair;

import com.thoughtworks.onboarding.DisplayType;
import com.thoughtworks.onboarding.SampleApplication.SampleAppRenderer;
import com.thoughtworks.onboarding.SampleApplication.UpdateTargetCallback;
import com.thoughtworks.onboarding.utils.CubeShaders;
import com.thoughtworks.onboarding.utils.Image;
import com.thoughtworks.onboarding.utils.LoadingDialogHandler;
import com.thoughtworks.onboarding.utils.SampleApplication3DModel;
import com.thoughtworks.onboarding.utils.SampleUtils;
import com.thoughtworks.onboarding.utils.Texture;
import com.vuforia.Matrix44F;
import com.vuforia.Renderer;
import com.vuforia.State;
import com.vuforia.Tool;
import com.vuforia.Trackable;
import com.vuforia.TrackableResult;
import com.vuforia.VIDEO_BACKGROUND_REFLECTION;
import com.vuforia.Vuforia;

import java.io.IOException;
import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


// The renderer class for the BackgroundTextureAccess sample.
public class ImageDisplayRenderer {
    private static final String LOGTAG = "IMDRenderer";
    private static final float OBJECT_SCALE_FLOAT = 0.01f;
    public AugmentedDisplay mActivity;
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

    private Image projectingImage = new Image();
    private float kBuildingScale = 12.0f;

    public ImageDisplayRenderer(AugmentedDisplay activity, UpdateTargetCallback appSession, SampleAppRenderer mSampleAppRenderer) {

        this.vuforiaAppSession = appSession;
        this.mActivity = activity;
        //Image target resource repository will be used to map image targets and image resources
        mRenderer = Renderer.getInstance();
        this.mSampleAppRenderer = mSampleAppRenderer;
    }

    /**
     * Called to draw the current frame.
     */
    public void onDrawFrame(GL10 gl, SampleAppRenderer mSampleAppRenderer) {
        if (!mIsActive)
            return;

//        renderFrame();
        // Call our function to render content from SampleAppRenderer class
        mSampleAppRenderer.render();
    }

    // Called when the surface is created or recreated.
    public void onSurfaceCreated(GL10 gl, EGLConfig config, SampleAppRenderer mSampleAppRenderer) {

        // Call function to initialize rendering:
        initRendering();

//        vuforiaAppSession.onSurfaceCreated();

        // Call Vuforia function to (re)initialize rendering after first use
        // or after OpenGL ES context was lost (e.g. after onPause/onResume):
        Vuforia.onSurfaceCreated();

        mSampleAppRenderer.onSurfaceCreated();

    }


    // Called when the surface changed size.
    public void onSurfaceChanged(GL10 gl, int width, int height, SampleAppRenderer mSampleAppRenderer) {


        // RenderingPrimitives to be updated when some rendering change is done
        updateRenderingPrimitives(mSampleAppRenderer);

        // Call function to initialize rendering:
        initRendering();
    }


    // Renderer initializing function.
    public void initRendering() {
        projectingImage = new Image();

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

    public void setActive(boolean active, SampleAppRenderer mSampleAppRenderer) {
        mIsActive = active;

        if (mIsActive)
            mSampleAppRenderer.configureVideoBackground();
    }

    public void updateRenderingPrimitives(SampleAppRenderer mSampleAppRenderer) {
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

    public void renderFrame(State state, float[] projectionMatrix, SampleAppRenderer mSampleAppRenderer) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        state = mRenderer.begin();
        mSampleAppRenderer.renderVideoBackground();

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // handle face culling, we need to detect if we are using reflection
        // to determine the direction of the culling
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);
//        state = mRenderer.begin();
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

            Pair indexAndType;
            indexAndType = mActivity.getIndexOfTargetFromTargetName(trackable.getName());
            int currentTarget = Integer.parseInt(indexAndType.first.toString());
            if (indexAndType.second == DisplayType.VIDEO)
                continue;

            // deal with the modelview and projection matrices
            float[] modelViewProjection = new float[16];

            Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f,
                    OBJECT_SCALE_FLOAT);
            Matrix.scaleM(modelViewMatrix, 0, OBJECT_SCALE_FLOAT,
                    OBJECT_SCALE_FLOAT, OBJECT_SCALE_FLOAT);


            Matrix.multiplyMM(modelViewProjection, 0,
                    projectionMatrix, 0, modelViewMatrix, 0);

            // activate the shader program and bind the vertex/normal/tex coords
            GLES20.glUseProgram(shaderProgramID);

            GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                    false, 0, projectingImage.getVertices());
            GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
                    false, 0, projectingImage.getNormals());
            GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                    GLES20.GL_FLOAT, false, 0, projectingImage.getTexCoords());

            GLES20.glEnableVertexAttribArray(vertexHandle);
            GLES20.glEnableVertexAttribArray(normalHandle);
            GLES20.glEnableVertexAttribArray(textureCoordHandle);

            // activate texture from repository, bind it, and pass to shader
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                    mTextures.get(currentTarget).mTextureID[0]);
            GLES20.glUniform1i(texSampler2DHandle, 0);

            // pass the model view matrix to the shader
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                    modelViewProjection, 0);

            // finally draw the teapot
            GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                    projectingImage.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT,
                    projectingImage.getIndices());

            // disable the enabled arrays
            GLES20.glDisableVertexAttribArray(vertexHandle);
            GLES20.glDisableVertexAttribArray(normalHandle);
            GLES20.glDisableVertexAttribArray(textureCoordHandle);

            SampleUtils.checkGLError("Render Frame");

        }

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        mRenderer.end();
    }
}
