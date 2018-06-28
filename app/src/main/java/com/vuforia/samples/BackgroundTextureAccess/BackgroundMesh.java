/*===============================================================================
Copyright (c) 2016-2017 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2015 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.
===============================================================================*/


package com.vuforia.samples.BackgroundTextureAccess;

import android.util.Log;

import com.vuforia.Renderer;
import com.vuforia.VideoBackgroundTextureInfo;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class BackgroundMesh {

    public static final int BUFFER_TYPE_VERTEX = 0;
    public static final int BUFFER_TYPE_TEXTURE_COORD = 1;
    public static final int BUFFER_TYPE_NORMALS = 2;
    public static final int BUFFER_TYPE_INDICES = 3;
    private static final String LOGTAG = "BackgroundMesh";
    private static final int SIZE_OF_FLOAT = 4;
    private ByteBuffer mOrthoQuadVertices = null;
    private ByteBuffer mOrthoQuadTexCoords = null;
    private ByteBuffer mOrthoQuadIndices = null;
    private int mNumVertexRows;
    private int mNumVertexCols;


    public BackgroundMesh(int vbNumVertexRows, int vbNumVertexCols) {
        mNumVertexRows = vbNumVertexRows;
        mNumVertexCols = vbNumVertexCols;

        // Get the texture and image dimensions from Vuforia
        VideoBackgroundTextureInfo texInfo = Renderer.getInstance()
                .getVideoBackgroundTextureInfo();

        // If there is no image data yet then return;
        if ((texInfo.getImageSize().getData()[0] == 0)
                || (texInfo.getImageSize().getData()[1] == 0)) {
            Log.e(LOGTAG, "Invalid Image Size!! "
                    + texInfo.getImageSize().getData()[0] + " x "
                    + texInfo.getImageSize().getData()[1]);
            return;
        }

        // These calculate a slope for the texture coords
        float uRatio = ((float) texInfo.getImageSize().getData()[0] / (float) texInfo
                .getTextureSize().getData()[0]);
        float vRatio = ((float) texInfo.getImageSize().getData()[1] / (float) texInfo
                .getTextureSize().getData()[1]);
        float uSlope = uRatio / ((float) vbNumVertexCols - 1.0f);
        float vSlope = vRatio / ((float) vbNumVertexRows - 1.0f);

        // These calculate a slope for the vertex values in this case we have a
        // span of 2, from -1 to 1
        // Note: the pan is set a bit bigger (2.01 instead of 2.0) to avoid having a white line on some screens
        float totalSpan = 2.01f;
        float colSlope = totalSpan / ((float) vbNumVertexCols - 1.0f);
        float rowSlope = totalSpan / ((float) vbNumVertexRows - 1.0f);

        // Some helper variables
        short currentVertexIndex = 0;
        mOrthoQuadVertices = ByteBuffer.allocateDirect(
                vbNumVertexRows * vbNumVertexCols * 3 * SIZE_OF_FLOAT).order(
                ByteOrder.nativeOrder());
        mOrthoQuadTexCoords = ByteBuffer.allocateDirect(
                vbNumVertexRows * vbNumVertexCols * 2 * SIZE_OF_FLOAT).order(
                ByteOrder.nativeOrder());
        mOrthoQuadIndices = ByteBuffer.allocateDirect(getNumObjectIndex())
                .order(ByteOrder.nativeOrder());

        for (int j = 0; j < vbNumVertexRows; j++) {
            for (int i = 0; i < vbNumVertexCols; i++) {
                // We populate the mesh with a regular grid
                mOrthoQuadVertices
                        .putFloat(((colSlope * i) - (totalSpan / 2.0f)));
                // We subtract this because the values range from -totalSpan/2 to totalSpan/2
                mOrthoQuadVertices
                        .putFloat(((rowSlope * j) - (totalSpan / 2.0f)));
                // It is all a flat polygon orthogonal to the view vector
                mOrthoQuadVertices.putFloat(0.0f);

                float u = 0.0f, v = 0.0f;

                // We also populate its associated texture coordinate
                u = uSlope * i;
                v = vRatio - (vSlope * j);

                mOrthoQuadTexCoords.putFloat(u);
                mOrthoQuadTexCoords.putFloat(v);

                // Log.d(LOGTAG, "Vert (Text): " +
                // ((colSlope*i)-(totalSpan/2.0f)) + "," +
                // ((rowSlope*j)-(totalSpan/2.0f)) + " (" + u + "," + v +")");

                // Now we populate the triangles that compose the mesh
                // First triangle is the upper right of the vertex
                if (j < vbNumVertexRows - 1) {
                    // In the example above this would make triangles ABD and BCE
                    if (i < vbNumVertexCols - 1) {
                        mOrthoQuadIndices.put((byte) currentVertexIndex);
                        mOrthoQuadIndices.put((byte) (currentVertexIndex + 1));
                        mOrthoQuadIndices
                                .put((byte) (currentVertexIndex + vbNumVertexCols));
                    }

                    // In the example above this would make triangles BED and CFE
                    if (i > 0) {
                        mOrthoQuadIndices.put((byte) currentVertexIndex);
                        mOrthoQuadIndices
                                .put((byte) (currentVertexIndex + vbNumVertexCols));
                        mOrthoQuadIndices.put((byte) (currentVertexIndex
                                + vbNumVertexCols - 1));
                    }
                }
                currentVertexIndex += 1; // Vertex index increased by one
            }
        }

        mOrthoQuadVertices.rewind();
        mOrthoQuadTexCoords.rewind();
        mOrthoQuadIndices.rewind();

    }


    public boolean isValid() {
        return mOrthoQuadVertices != null && mOrthoQuadTexCoords != null
                && mOrthoQuadIndices != null;
    }


    public Buffer getBuffer(int bufferType) {
        Buffer result = null;
        switch (bufferType) {
            case BUFFER_TYPE_VERTEX:
                result = mOrthoQuadVertices;
                break;
            case BUFFER_TYPE_TEXTURE_COORD:
                result = mOrthoQuadTexCoords;
                break;
            case BUFFER_TYPE_INDICES:
                result = mOrthoQuadIndices;

            case BUFFER_TYPE_NORMALS:// no normals for this object
            default:
                break;

        }

        return result;
    }


    public int getNumObjectVertex() {
        return mNumVertexRows * mNumVertexCols * 3;
    }


    public int getNumObjectIndex() {
        return (mNumVertexCols - 1) * (mNumVertexRows - 1) * 6;
    }

}
