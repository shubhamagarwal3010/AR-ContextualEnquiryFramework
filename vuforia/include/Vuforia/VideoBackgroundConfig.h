/*===============================================================================
Copyright (c) 2015-2016,2018 PTC Inc. All Rights Reserved.

Copyright (c) 2010-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.

\file
    VideoBackgroundConfig.h

\brief
    Header file for VideoBackgroundConfig struct.
===============================================================================*/

#ifndef _VUFORIA_VIDEOBACKGROUNDCONFIG_H_
#define _VUFORIA_VIDEOBACKGROUNDCONFIG_H_

// Include files
#include <Vuforia/Vectors.h>

namespace Vuforia
{

/// Configuration options for the video background (DEPRECATED)
/**
 * \deprecated This enum is deprecated. Front camera support will be removed in
 * a future %Vuforia release.
 * \public
 */
enum VIDEO_BACKGROUND_REFLECTION
{
    VIDEO_BACKGROUND_REFLECTION_DEFAULT,  ///< Allows the SDK to set the recommended reflection settings for the current camera (DEPRECATED)
    VIDEO_BACKGROUND_REFLECTION_ON,       ///< Overrides the SDK recommendation to force a reflection (DEPRECATED)
    VIDEO_BACKGROUND_REFLECTION_OFF       ///< Overrides the SDK recommendation to disable reflection (DEPRECATED)
};

/// Defines how the video background should be rendered.
struct VideoBackgroundConfig
{
    /// Constructor to provide basic initalization. 
    VideoBackgroundConfig()
    {
        mEnabled = true;
        mPosition.data[0] = 0;
        mPosition.data[1] = 0;
        mSize.data[0] = 0;
        mSize.data[1] = 0;
        mReflection = VIDEO_BACKGROUND_REFLECTION_DEFAULT;
    }

    /// Enables/disables rendering of the video background (DEPRECATED).
    /**
     * \deprecated This class member has been deprecated. It will be removed in an
     * upcoming %Vuforia release. Enabling / disabling video background rendering using
     * this setting is no longer necessary and will have no impact on video background
     * rendering.
     */
    bool mEnabled;

    /// Relative position of the video background in the render target, in pixels.
    /**
     * Describes the offset of the center of video background to the
     * center of the screen (viewport) in pixels. A value of (0,0) centers the
     * video background, whereas a value of (-10,15) moves the video background
     * 10 pixels to the left and 15 pixels upwards.
     */
    Vec2I mPosition;

    /// Width and height of the video background in pixels.
    /**
     * Using the device's screen size for this parameter scales the image to
     * fullscreen. Notice that if the camera's aspect ratio is different than
     * the screen's aspect ratio this will create a non-uniform stretched
     * image.
     */
    Vec2I mSize;

    /// Reflection parameter to control how the video background is rendered (DEPRECATED)
    /**
     * If you set this to VIDEO_BACKGROUND_REFLECTION_DEFAULT, the SDK will
     * update the projection matrix and video background automatically to provide
     * the best AR mode possible for the given camera on your specific device.
     * For the BACK camera, this will generally result in no reflection at all.
     * For the FRONT camera, this will generally result in a reflection to provide
     * an "AR Mirror" effect.
     *
     * For advanced use cases, you can override the default behaviour by setting
     * this to VIDEO_BACKGROUND_REFLECTION_ON or VIDEO_BACKGROUND_REFLECTION_OFF.
     *
     * \deprecated Front camera support will be removed in a future %Vuforia release.
     */
    VIDEO_BACKGROUND_REFLECTION mReflection;
};

} // namespace Vuforia

#endif //_VUFORIA_RENDERER_H_
