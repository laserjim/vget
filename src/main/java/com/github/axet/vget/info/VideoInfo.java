package com.github.axet.vget.info;

import java.net.URL;

import com.github.axet.wget.info.DownloadInfo;

public class VideoInfo {

    public enum VideoQuality {
        p2304, p1080, p720, p480, p360, p270, p224
    }

    private VideoQuality vq;
    private DownloadInfo info;
    private String title;
    // user friendly url (not direct video stream url)
    private URL source;

    /**
     * 
     * @param vq
     *            max video quality to download
     * @param source
     *            user firendly url
     * @param video
     *            video stream url
     * @param title
     *            video title
     */
    public VideoInfo(VideoQuality vq, URL source, URL video, String title) {
        this.setSource(source);

        DownloadInfo info = new DownloadInfo(video);
        info.extract();

        this.setVq(vq);
        this.setInfo(info);
        this.setTitle(title);
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

    public URL getSource() {
        return source;
    }

    public void setSource(URL source) {
        this.source = source;
    }
}