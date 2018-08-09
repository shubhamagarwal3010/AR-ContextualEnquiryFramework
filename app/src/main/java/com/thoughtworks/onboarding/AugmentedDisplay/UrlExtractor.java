package com.thoughtworks.onboarding.AugmentedDisplay;

import android.content.Context;
import android.util.SparseArray;

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
                            String downloadUrl = ytFiles.get(itag).getUrl();

                            onUrlExtracted(downloadUrl);
                        } else {
                            onUrlExtracted(url);
                        }
                    }
                }.extract(url, true, true);


            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            onUrlExtracted(url);
        }

    }


    public abstract void onUrlExtracted(String image);
}

