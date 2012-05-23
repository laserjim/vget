package com.github.axet.vget;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;

import com.github.axet.vget.info.VGetParser;
import com.github.axet.vget.info.VideoInfo;
import com.github.axet.vget.info.VimeoParser;
import com.github.axet.vget.info.YouTubeParser;
import com.github.axet.wget.info.DownloadInfo;

public class VGet extends VGetBase {

    ArrayList<Listener> list = new ArrayList<VGet.Listener>();
    VideoInfo source;
    File target;

    File targetForce;

    public static interface Listener {
        public void changed();
    }

    void changed() {
        for (Listener l : list) {
            l.changed();
        }
    }

    public VGet(URL source, File target) {
        super();

        VideoInfo info = new VideoInfo(source);

        extract(info);

        create(info, target);
    }

    public VGet(VideoInfo info, File target) {
        super();

        create(info, target);
    }

    public static VideoInfo extract(VideoInfo vi) {
        VGetParser ei = null;

        if (YouTubeParser.probe(vi.getWeb()))
            ei = new YouTubeParser(vi.getWeb());

        if (VimeoParser.probe(vi.getWeb()))
            ei = new VimeoParser(vi.getWeb());

        if (ei == null)
            throw new RuntimeException("unsupported web site");

        return ei.extract(vi);
    }

    void create(VideoInfo video, File target) {
        this.source = video;
        this.target = target;
    }

    public void setTarget(File path) {
        targetForce = path;
    }

    /**
     * ask thread to start work
     */
    public void start() {
        if (t1 != null && isActive())
            throw new RuntimeException("already started");

        File oldpath = null;
        if (t1 != null)
            oldpath = t1.getFileName();

        if (targetForce != null)
            oldpath = targetForce;

        download(source, target);

        t1.setFileName(oldpath);

        stop(false);
        t1.start();
    }

    /**
     * ask thread to stop working. and wait for change event.
     * 
     */
    public void stop() {
        stop(true);
    }

    /**
     * if working thread is active.
     * 
     * @return
     */
    public boolean isActive() {
        if (t1 == null)
            return false;
        return t1.isAlive();
    }

    /**
     * check if working thread has send the last possible event. so we can join.
     * 
     * @return true - we can join
     */
    public boolean isJoin() {
        if (t1 == null)
            return false;

        return t1.canJoin.get();
    }

    /**
     * Join to working thread and wait until it done
     */
    public void join() {
        try {
            t1.join();
        } catch (InterruptedException e) {
        }
    }

    /**
     * get exception.
     * 
     * @return
     */
    public Exception getException() {
        synchronized (t1.lock) {
            return t1.e;
        }
    }

    /**
     * wait until thread ends and close it. do before you exit app.
     */
    public void close() {
        shutdownAppl();
    }

    /**
     * get output file on local file system
     * 
     * @return
     */
    public File getOutput() {
        return t1.getFileName();
    }

    public VideoInfo getVideo() {
        return t1.max;
    }

    public boolean canceled() {
        return getStop().get();
    }

    /**
     * Please not by using listener you agree to handle multithread calls. I
     * suggest if you do SwingUtils.invokeLater (or your current thread manager)
     * for each 'Listener.changed' event.
     * 
     * @param l
     *            listenrer
     */
    public void addListener(Listener l) {
        list.add(l);
    }

    public void removeListener(Listener l) {
        list.remove(l);
    }

    public static void main(String[] args) {
        try {
            // 120p test
            // YTD2 y = new YTD2("http://www.youtube.com/watch?v=OY7fmYkpsRs",
            // "/Users/axet/Downloads");

            // age restriction test
            VideoInfo video = new VideoInfo(new URL("http://www.youtube.com/watch?v=QoTWRHheshw&feature=youtube_gdata"));
            VGet.extract(video);
            VGet y = new VGet(video, new File("/Users/axet/Downloads"));

            // user page test
            // YTD2 y = new YTD2(
            // "http://www.youtube.com/user/cubert01?v=gidumziw4JE&feature=pyv&ad=8307058643&kw=youtube%20download",
            // "/Users/axet/Downloads");

            // hd test
            // VGet y = new VGet("http://www.youtube.com/watch?v=rRS6xL1B8ig",
            // "/Users/axet/Downloads");

            // VGet y = new VGet("http://vimeo.com/39289096",
            // "/Users/axet/Downloads");

            y.start();

            while (y.isActive()) {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                }

                DownloadInfo info = video.getInfo();

                System.out.println("title: " + video.getTitle() + ", Quality: " + video.getVq() + ", bytes: "
                        + info.getCount() + ", total: " + info.getLength());
            }

            if (y.isJoin())
                y.join();

            y.close();

            if (y.getException() != null)
                y.getException().printStackTrace();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
