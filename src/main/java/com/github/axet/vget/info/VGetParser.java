package com.github.axet.vget.info;

import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.axet.vget.info.VideoInfo.VideoQuality;
import com.github.axet.wget.info.DownloadInfo;
import com.github.axet.wget.info.ex.DownloadRetry;

public abstract class VGetParser {

    public void extract(VideoInfo info, AtomicBoolean stop, Runnable notify) {
        extract(info, VideoQuality.p2304, stop, notify);
    }

    abstract public void extract(VideoInfo info, VideoQuality max, AtomicBoolean stop, Runnable notify);

    void getVideo(VideoInfo vvi, Map<VideoQuality, URL> sNextVideoURL, VideoQuality maxQuality) {
        VideoQuality[] avail = new VideoQuality[] { VideoQuality.p2304, VideoQuality.p1080, VideoQuality.p720,
                VideoQuality.p480, VideoQuality.p360, VideoQuality.p270, VideoQuality.p224 };

        int i = Arrays.binarySearch(avail, maxQuality);
        if (i == -1)
            i = 0;

        for (; i < avail.length; i++) {
            if (sNextVideoURL.containsKey(avail[i])) {
                vvi.setEmpty(false);
                vvi.setVq(avail[i]);
                DownloadInfo info = new DownloadInfo(sNextVideoURL.get(avail[i]));
                vvi.setInfo(info);
                return;
            }
        }

        // retry. since youtube may already rendrered propertly quality.
        throw new DownloadRetry("no video with required quality found");
    }
}
