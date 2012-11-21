package com.github.axet.vget;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;

import com.github.axet.vget.info.VideoInfo;
import com.github.axet.vget.info.VideoInfo.States;
import com.github.axet.vget.info.VideoInfo.VideoQuality;
import com.github.axet.wget.Direct;
import com.github.axet.wget.DirectRange;
import com.github.axet.wget.DirectSingle;
import com.github.axet.wget.info.DownloadInfo;
import com.github.axet.wget.info.URLInfo;
import com.github.axet.wget.info.ex.DownloadError;
import com.github.axet.wget.info.ex.DownloadRetry;

public class VGet {

    ArrayList<Listener> list = new ArrayList<VGet.Listener>();
    VideoInfo source;
    File targetDir;

    File target = null;

    public static interface Listener {
        public void changed();
    }

    void changed() {
        for (Listener l : list) {
            l.changed();
        }
    }

    public VGet(URL source, File targetDir) {
        VideoInfo info = new VideoInfo(source);

        create(info, targetDir);
    }

    public VGet(VideoInfo info, File targetDir) {
        create(info, targetDir);
    }

    void create(VideoInfo video, File targetDir) {
        this.source = video;
        this.targetDir = targetDir;
    }

    public void setTarget(File file) {
        target = file;
    }

    /**
     * get output file on local file system
     * 
     * @return
     */
    public File getOutput() {
        return target;
    }

    public VideoInfo getVideo() {
        return source;
    }

    public void download() {
        try {
            download(new AtomicBoolean(false), new Runnable() {
                @Override
                public void run() {
                }
            });
        } catch (InterruptedException e) {
            throw new DownloadError(e);
        }
    }

    /**
     * Drop all foribiden characters from filename
     * 
     * @param f
     *            input file name
     * @return normalized file name
     */
    static String replaceBadChars(String f) {
        String replace = " ";
        f = f.replaceAll("/", replace);
        f = f.replaceAll("\\\\", replace);
        f = f.replaceAll(":", replace);
        f = f.replaceAll("\\?", replace);
        f = f.replaceAll("\\\"", replace);
        f = f.replaceAll("\\*", replace);
        f = f.replaceAll("<", replace);
        f = f.replaceAll(">", replace);
        f = f.replaceAll("\\|", replace);
        f = f.trim();
        f = StringUtils.removeEnd(f, ".");
        f = f.trim();

        String ff;
        while (!(ff = f.replaceAll("  ", " ")).equals(f)) {
            f = ff;
        }

        return f;
    }

    public void download(final AtomicBoolean stop, final Runnable notify) throws InterruptedException {
        if (source.empty())
            source.extract(stop, notify);

        source.setState(States.EXTRACTING_DONE);
        notify.run();

        final DownloadInfo info = source.getInfo();

        try {
            File f;

            if (target == null) {
                Integer idupcount = 0;

                String sfilename = replaceBadChars(source.getTitle());

                String ct = info.getContentType();
                if (ct == null)
                    throw new DownloadRetry("null content type");

                String ext = ct.replaceFirst("video/", "").replaceAll("x-", "");

                do {
                    String add = idupcount > 0 ? " (".concat(idupcount.toString()).concat(")") : "";

                    f = new File(targetDir, sfilename + add + "." + ext);
                    idupcount += 1;
                } while (f.exists());
                this.target = f;
            }

            Direct direct;
            if (info.range())
                direct = new DirectRange(info, target);
            else
                direct = new DirectSingle(info, target);

            if (info.getContentType() == null || !info.getContentType().contains("video/")) {
                throw new DownloadRetry("unable to download video, bad content");
            }

            source.setState(States.DOWNLOADING);
            notify.run();

            direct.download(stop, new Runnable() {
                @Override
                public void run() {
                    switch (info.getState()) {
                    case DOWNLOADING:
                        source.setState(States.DOWNLOADING);
                        notify.run();
                        break;
                    case RETRYING:
                        source.setState(States.RETRYING, info.getException());
                        source.setDelay(info.getDelay());
                        notify.run();
                        break;
                    default:
                        // we can safely skip all statues. (extracting - already
                        // pased, STOP / ERROR / DONE i will catch up here
                    }
                }
            });

            source.setState(States.DONE);
            notify.run();
        } catch (InterruptedException e) {
            info.setState(URLInfo.States.STOPPED);
            notify.run();
            throw e;
        } catch (RuntimeException e) {
            info.setState(URLInfo.States.ERROR);
            notify.run();
            throw e;
        }
    }

}
