package com.thoughtworks.onboarding.model;

public class MainContent {

    private MediaType mediaType;
    private String url;
    private float x;
    private float y;
    private float z;
    private float height;
    private float width;

    public String getUrl() {
        return url;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public enum MediaType {
        IMAGE,
        VIDEO,
        HYPERLINK
    }
}


