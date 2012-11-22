package com.github.axet.vget;

import java.io.File;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;

import com.github.axet.vget.info.VideoInfo;
import com.github.axet.vget.info.VideoInfo.States;
import com.github.axet.wget.Direct;
import com.github.axet.wget.DirectRange;
import com.github.axet.wget.DirectSingle;
import com.github.axet.wget.RetryWrap;
import com.github.axet.wget.info.DownloadInfo;
import com.github.axet.wget.info.ex.DownloadIOError;
import com.github.axet.wget.info.ex.DownloadInterruptedError;
import com.github.axet.wget.info.ex.DownloadRetry;

public class VGet {

    VideoInfo info;
    File targetDir;

    File targetForce = null;

    File target = null;

    public VGet(URL source, File targetDir) {
        VideoInfo info = new VideoInfo(source);

        create(info, targetDir);
    }

    public VGet(VideoInfo info, File targetDir) {
        create(info, targetDir);
    }

    void create(VideoInfo video, File targetDir) {
        this.info = video;
        this.targetDir = targetDir;
    }

    public void setTarget(File file) {
        targetForce = file;
    }

    /**
     * get output file on local file system
     * 
     * @return
     */
    public File getTarget() {
        return target;
    }

    public VideoInfo getVideo() {
        return info;
    }

    public void download() {
        download(new AtomicBoolean(false), new Runnable() {
            @Override
            public void run() {
            }
        });
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

    boolean done(AtomicBoolean stop) {
        if (stop.get())
            throw new DownloadInterruptedError("stop");
        if (Thread.currentThread().isInterrupted())
            throw new DownloadInterruptedError("interrupted");

        return false;
    }

    void retry(AtomicBoolean stop, Runnable notify, Throwable e) {
        info.setState(States.RETRYING, e);
        notify.run();

        boolean retracted = false;

        while (!retracted) {
            for (int i = RetryWrap.RETRY_DELAY; i >= 0; i--) {
                if (stop.get())
                    throw new DownloadInterruptedError("stop");
                if (Thread.currentThread().isInterrupted())
                    throw new DownloadInterruptedError("interrupted");

                info.setDelay(i);
                notify.run();

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ee) {
                    throw new DownloadInterruptedError(ee);
                }
            }

            try {
                // if we continue to download from old source, and this proxy
                // server is
                // down we have to try to extract new info and try to resume
                // download

                DownloadInfo infoOld = info.getInfo();
                info.extract(stop, notify);
                DownloadInfo infoNew = info.getInfo();

                if (infoOld.resume(infoNew)) {
                    infoNew.copy(infoOld);
                } else {
                    if (target != null) {
                        target.delete();
                        target = null;
                    }
                }

                retracted = true;
            } catch (DownloadRetry ee) {
                info.setState(States.RETRYING, ee);
                notify.run();
            }
        }
    }

    void target(DownloadInfo dinfo) {
        if (targetForce != null)
            target = targetForce;

        File f;

        if (target == null) {
            Integer idupcount = 0;

            String sfilename = replaceBadChars(info.getTitle());

            String ct = dinfo.getContentType();
            if (ct == null)
                throw new DownloadRetry("null content type");

            String ext = ct.replaceFirst("video/", "").replaceAll("x-", "");

            do {
                String add = idupcount > 0 ? " (".concat(idupcount.toString()).concat(")") : "";

                f = new File(targetDir, sfilename + add + "." + ext);
                idupcount += 1;
            } while (f.exists());

            target = f;
        }
    }

    public void download(final AtomicBoolean stop, final Runnable notify) {
        try {
            if (info.empty())
                info.extract(stop, notify);

            info.setState(States.EXTRACTING_DONE);
            notify.run();

            while (!done(stop)) {
                try {
                    final DownloadInfo dinfo = info.getInfo();

                    target(dinfo);

                    Direct direct;
                    if (dinfo.range())
                        direct = new DirectRange(dinfo, target);
                    else
                        direct = new DirectSingle(dinfo, target);

                    if (dinfo.getContentType() == null || !dinfo.getContentType().contains("video/")) {
                        throw new DownloadRetry("unable to download video, bad content");
                    }

                    direct.download(stop, new Runnable() {
                        @Override
                        public void run() {
                            switch (dinfo.getState()) {
                            case DOWNLOADING:
                                info.setState(States.DOWNLOADING);
                                notify.run();
                                break;
                            case RETRYING:
                                info.setState(States.RETRYING, dinfo.getException());
                                info.setDelay(dinfo.getDelay());
                                notify.run();
                                break;
                            default:
                                // we can safely skip all statues. (extracting -
                                // already
                                // pased, STOP / ERROR / DONE i will catch up
                                // here
                            }
                        }
                    });

                    info.setState(States.DONE);
                    notify.run();
                } catch (DownloadRetry e) {
                    retry(stop, notify, e);
                } catch (DownloadIOError e) {
                    retry(stop, notify, e);
                }
            }
        } catch (DownloadInterruptedError e) {
            info.setState(VideoInfo.States.STOP);
            notify.run();

            throw e;
        } catch (RuntimeException e) {
            info.setState(VideoInfo.States.ERROR);
            notify.run();

            throw e;
        }
    }
}
