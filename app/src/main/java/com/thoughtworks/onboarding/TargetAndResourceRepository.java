package com.thoughtworks.onboarding;

import java.util.ArrayList;
import java.util.List;

public class TargetAndResourceRepository {

    private List<TargetWithResource> targetsAndResoruces;

    public List<TargetWithResource> getTargetsAndResoruces() {
        targetsAndResoruces = new ArrayList<>();
        targetsAndResoruces.add(new TargetWithResource("stones", "VideoPlayback/SampleVideo.mp4", DisplayType.VIDEO));
        targetsAndResoruces.add(new TargetWithResource("chips", "VideoPlayback/Flag_Transparent.mp4", DisplayType.VIDEO));
        targetsAndResoruces.add(new TargetWithResource("alluri", "alluri.png", DisplayType.IMAGE));
        targetsAndResoruces.add(new TargetWithResource("aaron", "aaron.png", DisplayType.IMAGE));

        return targetsAndResoruces;
    }

    public int getTargetCount() {
        return targetsAndResoruces != null ? targetsAndResoruces.size() : 0;
    }

}
