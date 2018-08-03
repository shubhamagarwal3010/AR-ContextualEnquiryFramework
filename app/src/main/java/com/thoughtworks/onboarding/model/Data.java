package com.thoughtworks.onboarding.model;

public class Data {

    private String augmentType;
    private MainContent mainContent;

    public Data(String augmentType, MainContent mainContent) {
        this.augmentType = augmentType;
        this.mainContent = mainContent;
    }

    public MainContent getMainContent() {
        return mainContent;
    }

}