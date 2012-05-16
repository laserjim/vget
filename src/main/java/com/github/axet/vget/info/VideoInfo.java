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

    public VideoInfo(VideoQuality vq, URL url, String title) {

        DownloadInfo info = new DownloadInfo(url);
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
}