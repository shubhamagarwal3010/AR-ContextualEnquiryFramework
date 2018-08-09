package com.thoughtworks.onboarding.AugmentedDisplay;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.SparseArray;

import com.thoughtworks.onboarding.SplashScreenActivity;

import java.util.ArrayList;

import at.huber.youtubeExtractor.VideoMeta;
import at.huber.youtubeExtractor.YouTubeExtractor;
import at.huber.youtubeExtractor.YtFile;

public abstract class UrlExtractor {

    private String url;
    private Context context;





    public void setVideoUrl(final String url, Context context) {

        this.context = context;
        if (url.contains("youtube.com")) {

            try {
                 //new ExtractAsyncTask().execute(url);

                new YouTubeExtractor(context) {
                    @Override
                    public void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta vMeta) {


                        if (ytFiles != null) {
                            int itag = 22;
                            String downloadUrl= ytFiles.get(itag).getUrl();

                            onUrlExtracted(downloadUrl);
                        }else
                        {
                            onUrlExtracted(url);
                        }
                    }
                }.extract(url, true, true);



            }catch (Exception e)
            {
                e.printStackTrace();
            }

        }else {
            onUrlExtracted(url);
        }

    }



    public abstract void onUrlExtracted(String image);
}

