package com.vuforia.samples;

import java.util.ArrayList;
import java.util.List;

public class VideoTargetAndResourceRepository {

    private List<VideoTargetWithResource> videoTargetsAndResoruces;

    public List<VideoTargetWithResource> getVideoTargetsAndResoruces() {
        videoTargetsAndResoruces = new ArrayList<>();
        videoTargetsAndResoruces.add(new VideoTargetWithResource("stones", "VideoPlayback/SampleVideo.mp4"));
        videoTargetsAndResoruces.add(new VideoTargetWithResource("chips", "VideoPlayback/Flag_Transparent.mp4"));
        videoTargetsAndResoruces.add(new VideoTargetWithResource("tarmac", "VideoPlayback/Sample_Tarmac_Video.mp4"));
        return videoTargetsAndResoruces;
    }

    public int getTargetCount() {
        return videoTargetsAndResoruces != null ? videoTargetsAndResoruces.size() : 0;
    }

}
