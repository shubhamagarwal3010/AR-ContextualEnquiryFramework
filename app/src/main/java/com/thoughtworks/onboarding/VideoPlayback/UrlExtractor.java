package com.thoughtworks.onboarding.VideoPlayback;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.SparseArray;

import at.huber.youtubeExtractor.VideoMeta;
import at.huber.youtubeExtractor.YouTubeExtractor;
import at.huber.youtubeExtractor.YtFile;

public class UrlExtractor {

    private String url;
    private Context context;

    public UrlExtractor(String url, Context context) {
        this.url = url;
        this.context = context;
    }

    @SuppressLint("StaticFieldLeak")
    public String getVideoUrl()
    {
        final String[] downloadUrl = {""};
        if(url.contains("youtube.com")) {
            new YouTubeExtractor(context) {
                @Override
                public void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta vMeta) {
                    if (ytFiles != null) {
                        int itag = 22;
                        downloadUrl[0] = ytFiles.get(itag).getUrl();
                    }
                }
            }.extract(url, true, true);
            return downloadUrl[0];
        }
        return url;
    }
}

