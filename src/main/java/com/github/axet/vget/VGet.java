package com.github.axet.vget;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.axet.vget.info.VGetParser;
import com.github.axet.vget.info.VideoInfo;
import com.github.axet.vget.info.VimeoParser;
import com.github.axet.vget.info.YouTubeParser;

public class VGet {

    ArrayList<Listener> list = new ArrayList<VGet.Listener>();
    VideoInfo source;
    File target;

    File targetForce;

    VGetDownload vget;

    public static interface Listener {
        public void changed();
    }

    void changed() {
        for (Listener l : list) {
            l.changed();
        }
    }

    public VGet(URL source, File target) {
        VideoInfo info = new VideoInfo(source);

        info = extract(source);

        create(info, target, new AtomicBoolean(false), new Runnable() {
            @Override
            public void run() {
            }
        });
    }

    public VGet(VideoInfo info, File target, AtomicBoolean stop, Runnable notify) {
        create(info, target, stop, notify);
    }

    public static VideoInfo extract(URL source) {
        VGetParser ei = null;

        if (YouTubeParser.probe(source))
            ei = new YouTubeParser(source);

        if (VimeoParser.probe(source))
            ei = new VimeoParser(source);

        if (ei == null)
            throw new RuntimeException("unsupported web site");

        return ei.extract();
    }

    void create(VideoInfo video, File target, AtomicBoolean stop, Runnable notify) {
        this.source = video;
        this.target = target;

        vget = new VGetDownload(video, target, stop, notify);
    }

    public void setTarget(File path) {
        targetForce = path;
    }

    /**
     * get output file on local file system
     * 
     * @return
     */
    public File getOutput() {
        return vget.target;
    }

    public VideoInfo getVideo() {
        return vget.url;
    }

    public void download() {
        if (targetForce != null)
            vget.target = targetForce;

        vget.download();
    }

    public static void main(String[] args) {
        try {
            VGet v = new VGet(new URL("http://vimeo.com/52716355"), new File("/Users/axet/Downloads"));
            v.download();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
