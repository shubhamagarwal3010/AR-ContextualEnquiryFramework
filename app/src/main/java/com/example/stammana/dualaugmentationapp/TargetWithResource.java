package com.example.stammana.dualaugmentationapp;

public class TargetWithResource {

    private String TargetName;
    private String Resource;
    private DisplayType displayType;

    public TargetWithResource(String targetName, String resource, DisplayType displayType) {
        TargetName = targetName;
        Resource = resource;
        this.displayType = displayType;
    }

    public String getTargetName() {
        return TargetName;
    }

    public String getResource() {
        return Resource;
    }

    public DisplayType getDisplayType() {
        return displayType;
    }
}
