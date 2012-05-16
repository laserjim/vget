package com.github.axet.vget;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.axet.vget.info.VGetParser;
import com.github.axet.vget.info.VideoInfo;
import com.github.axet.vget.info.VideoInfo.VideoQuality;
import com.github.axet.vget.info.VimeoParser;
import com.github.axet.vget.info.YouTubeParser;
import com.github.axet.wget.info.DownloadInfo;

class VGetThread extends Thread {

    Object lock = new Object();

    // is the main thread done working?
    AtomicBoolean canJoin = new AtomicBoolean(false);

    // exception druning execution
    Exception e;

    VGetDownload d;
    VideoInfo max;

    Runnable notify;

    public VGetThread(final VGetBase base, VideoInfo video, File target) {
        notify = new Runnable() {
            @Override
            public void run() {
                base.changed();
            }
        };

        d = new VGetDownload(base, max, target, notify);
    }

    public VideoInfo getVideo() {
        return max;
    }

    public String getFileName() {
        synchronized (lock) {
            return d.target;

        }
    }

    public synchronized void setFileName(String file) {
        synchronized (lock) {
            d.target = file;
        }
    }

    @Override
    public void run() {
        try {
            d.download();
        } catch (Exception e) {
            synchronized (lock) {
                this.e = e;
            }
            notify.run();
        }

        canJoin.set(true);
        notify.run();
    }
}