package com.github.axet.vget.info;

import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.axet.wget.info.DownloadInfo;
import com.github.axet.wget.info.ex.DownloadInterruptedError;

public class VideoInfo {

    public enum VideoQuality {
        p2304, p1080, p720, p480, p360, p270, p224
    }

    public enum States {
        QUEUE, EXTRACTING, EXTRACTING_DONE, DOWNLOADING, RETRYING, DONE, ERROR, STOP
    }

    private VideoQuality vq;
    private DownloadInfo info;
    private String title;
    // user friendly url (not direct video stream url)
    private URL web;
    private URL icon;

    // states, three variables
    private States state;
    private Throwable exception;
    private int delay;

    /**
     * 
     * @param vq
     *            max video quality to download
     * @param web
     *            user firendly url
     * @param video
     *            video stream url
     * @param title
     *            video title
     */
    public VideoInfo(URL web) {
        this.setWeb(web);
        setState(States.QUEUE);
    }

    public boolean empty() {
        return info == null;
    }

    public void reset() {
        info = null;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public DownloadInfo getInfo() {
        return info;
    }

    public void setInfo(DownloadInfo info) {
        this.info = info;
    }

    public VideoQuality getVq() {
        return vq;
    }

    public void setVq(VideoQuality vq) {
        this.vq = vq;
    }

    public URL getWeb() {
        return web;
    }

    public void setWeb(URL source) {
        this.web = source;
    }

    public void extract(AtomicBoolean stop, Runnable notify) {
        VGetParser ei = null;

        if (YouTubeParser.probe(web))
            ei = new YouTubeParser(web);

        if (VimeoParser.probe(web))
            ei = new VimeoParser(web);

        if (ei == null)
            throw new RuntimeException("unsupported web site");

        try {
            ei.extract(this, stop, notify);

            info.extract(stop, notify);
        } catch (DownloadInterruptedError e) {
            setState(States.STOP, e);

            throw e;
        } catch (RuntimeException e) {
            setState(States.ERROR, e);

            throw e;
        }
    }

    public States getState() {
        return state;
    }

    public void setState(States state) {
        this.state = state;
        this.exception = null;
        this.delay = 0;
    }

    public void setState(States state, Throwable e) {
        this.state = state;
        this.exception = e;
        this.delay = 0;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay, Throwable e) {
        this.delay = delay;
        this.exception = e;
        this.state = States.RETRYING;
    }

    public Throwable getException() {
        return exception;
    }

    public void setException(Throwable exception) {
        this.exception = exception;
    }

    public URL getIcon() {
        return icon;
    }

    public void setIcon(URL icon) {
        this.icon = icon;
    }
}