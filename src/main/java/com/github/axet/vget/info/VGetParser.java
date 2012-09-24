package com.github.axet.vget.info;

import java.net.URL;
import java.util.Arrays;
import java.util.Map;

import com.github.axet.vget.info.VideoInfo.VideoQuality;
import com.github.axet.wget.info.DownloadError;
import com.github.axet.wget.info.DownloadInfo;
import com.github.axet.wget.info.DownloadRetry;

public abstract class VGetParser {

    public VideoInfo extract(VideoInfo vi) {
        return extract(vi, VideoQuality.p2304);
    }

    abstract public VideoInfo extract(VideoInfo vi, VideoQuality max);

    VideoInfo getVideo(VideoInfo vi, Map<VideoQuality, URL> sNextVideoURL, VideoQuality maxQuality, URL source,
            String title) {
        VideoQuality[] avail = new VideoQuality[] { VideoQuality.p2304, VideoQuality.p1080, VideoQuality.p720,
                VideoQuality.p480, VideoQuality.p360, VideoQuality.p270, VideoQuality.p224 };

        int i = Arrays.binarySearch(avail, maxQuality);
        if (i == -1)
            i = 0;

        for (; i < avail.length; i++) {
            if (sNextVideoURL.containsKey(avail[i])) {
                VideoInfo vvi = new VideoInfo(source);
                vvi.setEmpty(false);
                vvi.setTitle(title);
                vvi.setVq(avail[i]);
                DownloadInfo info = new DownloadInfo(sNextVideoURL.get(avail[i]));
                info.extract();
                vvi.setInfo(info);
                return vvi;
            }
        }

        // retry. since youtube may already rendrered propertly quality.
        throw new DownloadRetry("no video with required quality found");
    }
}
