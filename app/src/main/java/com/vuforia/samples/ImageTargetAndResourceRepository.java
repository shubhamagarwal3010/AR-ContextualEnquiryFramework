package com.vuforia.samples;

import java.util.HashMap;

public class ImageTargetAndResourceRepository {

    private HashMap<String,Integer> targetResourceMap = new HashMap<>();

    public ImageTargetAndResourceRepository(){
        targetResourceMap.put("alluri",0);
        targetResourceMap.put("aaron",1);
    }

    public int getTextureIndexForDataset(String dataset){
        return targetResourceMap.get(dataset);
    }
}
