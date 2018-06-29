package com.vuforia.samples;

public class VideoTargetWithResource {

    private String TargetName;
    private String Resource;

    public VideoTargetWithResource(String targetName, String resource) {
        TargetName = targetName;
        Resource = resource;
    }

    public String getTargetName() {
        return TargetName;
    }

    public String getResource() {
        return Resource;
    }


}
