package com.github.axet.vget;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.axet.vget.info.VideoInfo;
import com.github.axet.wget.info.DownloadInfo;
import com.github.axet.wget.info.DownloadInfo.Part;
import com.github.axet.wget.info.DownloadInfo.Part.States;

public class AppManagedDownload {

    VideoInfo info;
    long last;

    public void run(String url) {
        try {
            AtomicBoolean stop = new AtomicBoolean(false);
            Runnable notify = new Runnable() {
                @Override
                public void run() {
                    VideoInfo i1 = info;
                    DownloadInfo i2 = i1.getInfo();

                    // notify app or save download state
                    // you can extract information from DownloadInfo info;
                    switch (i1.getState()) {
                    case EXTRACTING:
                    case EXTRACTING_DONE:
                    case DONE:
                        System.out.println(i1.getState());
                        break;
                    case RETRYING:
                        System.out.println(i1.getState() + " " + i1.getDelay());
                        break;
                    case DOWNLOADING:
                        long now = System.currentTimeMillis();
                        if (now - 1000 > last) {
                            last = now;

                            String parts = "";

                            List<Part> pp = i2.getParts();
                            if (pp != null) {
                                // multipart download
                                for (Part p : pp) {
                                    if (p.getState().equals(States.DOWNLOADING)) {
                                        parts += String.format("Part#%d(%.2f) ", p.getNumber(), p.getCount()
                                                / (float) p.getLength());
                                    }
                                }
                            }

                            System.out.println(String.format("%s %.2f %s", i1.getState(),
                                    i2.getCount() / (float) i2.getLength(), parts));
                        }
                        break;
                    default:
                        break;
                    }
                }
            };

            info = new VideoInfo(new URL(url));

            VGet v = new VGet(info, new File("/Users/axet/Downloads"));

            // optional. only if you dlike to get video title before start
            // download
            v.extract(stop, notify);
            System.out.println(info.getTitle());

            v.download(stop, notify);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        AppManagedDownload e = new AppManagedDownload();
        e.run(args[0]);
    }
}