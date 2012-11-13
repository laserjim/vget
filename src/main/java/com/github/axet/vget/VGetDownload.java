package com.github.axet.vget;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;

import com.github.axet.vget.info.VideoInfo;
import com.github.axet.wget.Direct;
import com.github.axet.wget.DirectRange;
import com.github.axet.wget.DirectSingle;
import com.github.axet.wget.info.DownloadInfo;
import com.github.axet.wget.info.DownloadRetry;

class VGetDownload {

    File targetDir;

    File target = null;

    String input;

    Runnable notify;

    VideoInfo url;

    AtomicBoolean stop;

    public VGetDownload(VideoInfo url, File targetDir, AtomicBoolean stop, Runnable notify) {
        this.url = url;
        this.targetDir = targetDir;
        this.notify = notify;
        this.stop = stop;
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

    void download() {
        try {
            File f;

            DownloadInfo info = url.getInfo();

            if (target == null) {
                Integer idupcount = 0;

                String sfilename = replaceBadChars(url.getTitle());

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
            } else {
                f = target;
            }

            Direct direct;
            if (info.range())
                direct = new DirectRange(info, f, stop, notify);
            else
                direct = new DirectSingle(info, f, stop, notify);

            if (info.getContentType() == null || !info.getContentType().contains("video/")) {
                throw new DownloadRetry("unable to download video, bad content");
            }

            direct.download();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
