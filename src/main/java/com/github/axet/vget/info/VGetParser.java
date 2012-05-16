package com.github.axet.vget.info;

import java.net.URL;
import java.util.Arrays;
import java.util.Map;

import com.github.axet.vget.info.VideoInfo.VideoQuality;
import com.github.axet.wget.info.DownloadError;

public abstract class VGetParser {

    public VideoInfo extract() {
        return extract(VideoQuality.p2304);
    }

    abstract public VideoInfo extract(VideoQuality max);
    
    VideoInfo getVideo(Map<VideoQuality, URL> sNextVideoURL, VideoQuality maxQuality, String title) {
        VideoQuality[] avail = new VideoQuality[] { VideoQuality.p2304, VideoQuality.p1080, VideoQuality.p720,
                VideoQuality.p480, VideoQuality.p360, VideoQuality.p270, VideoQuality.p224 };

        int i = Arrays.binarySearch(avail, maxQuality);
        if (i == -1)
            i = 0;

        for (; i < avail.length; i++) {
            if (sNextVideoURL.containsKey(avail[i]))
                return new VideoInfo(avail[i], sNextVideoURL.get(avail[i]), title);
        }

        throw new DownloadError("no video with required quality found");
    }
}
