package com.thoughtworks.onboarding.cloud;

public class Data {

    private int augmentType;
    private MainContent mainContent;

    public Data(int augmentType, MainContent mainContent) {
        this.augmentType = augmentType;
        this.mainContent = mainContent;
    }

    public MainContent getMainContent() {
        return mainContent;
    }

}