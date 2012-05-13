package com.github.axet.vget;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import com.github.axet.vget.info.VGetInfo;
import com.github.axet.vget.info.VGetInfo.VideoQuality;
import com.github.axet.vget.info.VGetInfo.VideoURL;
import com.github.axet.vget.info.VimeoInfo;
import com.github.axet.vget.info.YouTubeInfo;
import com.github.axet.wget.info.DownloadError;
import com.github.axet.wget.info.DownloadInfo;

class VGetThread extends Thread {

    // is the main thread done working?
    boolean canJoin = false;

    // exception druning execution
    Exception e;

    Object statsLock = new Object();
    VGetInfo ei;
    DownloadInfo info;
    VGetDownload d;

    Runnable notify;

    public VGetThread(final VGetBase base, URL url, File target) {
        try {

            notify = new Runnable() {
                @Override
                public void run() {
                    base.changed();
                }
            };

            if (YouTubeInfo.probe(url))
                ei = new YouTubeInfo(base, url);

            if (VimeoInfo.probe(url))
                ei = new VimeoInfo(base, url);

            if (ei == null)
                throw new RuntimeException("unsupported web site");

            ei.extract();
            
            VideoURL max = getVideo();
            info = new DownloadInfo(new URL(max.url));

            info.extract();

            d = new VGetDownload(base, info, ei, target, notify);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public String getTitle() {
        synchronized (statsLock) {
            return ei.getTitle();
        }
    }

    public long getTotal() {
        synchronized (statsLock) {
            return info.getLength();
        }
    }

    public long getCount() {
        synchronized (statsLock) {
            return info.getCount();
        }
    }

    public VideoQuality getVideoQuality() {
        synchronized (statsLock) {
            return d.max != null ? d.max.vq : null;
        }
    }

    public String getFileName() {
        synchronized (statsLock) {
            return d.target;
        }
    }

    public String getInput() {
        synchronized (statsLock) {
            return ei.getSource();
        }
    }

    public void setFileName(String file) {
        synchronized (statsLock) {
            d.target = file;
        }
    }

    @Override
    public void run() {
        try {
            ei.extract();
            d.download();
        } catch (Exception e) {
            synchronized (statsLock) {
                this.e = e;
            }
            notify.run();
        }

        synchronized (statsLock) {
            canJoin = true;
        }
        notify.run();
    }

    public VideoURL getVideo() {
        Map<VideoQuality, VideoURL> sNextVideoURL = ei.getVideos();

        VideoQuality[] avail = new VideoQuality[] { VideoQuality.p2304, VideoQuality.p1080, VideoQuality.p720,
                VideoQuality.p480, VideoQuality.p360, VideoQuality.p270, VideoQuality.p224 };

        for (int i = 0; i < avail.length; i++) {
            if (sNextVideoURL.containsKey(avail[i]))
                return sNextVideoURL.get(avail[i]);
        }

        throw new DownloadError("no video with required quality found");
    }
}