package com.github.axet.vget.info;

import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import com.github.axet.vget.info.VideoInfo.VideoQuality;
import com.github.axet.wget.WGet;
import com.github.axet.wget.info.DownloadError;

public class VimeoParser extends VGetParser {

    HashMap<VideoQuality, URL> sNextVideoURL = new HashMap<VideoQuality, URL>();
    String sTitle = null;

    URL source;

    public VimeoParser(URL input) {
        this.source = input;
    }

    public static boolean probe(URL url) {
        return url.toString().contains("vimeo.com");
    }

    void downloadone(URL sURL) throws Exception {

        String id;
        String clip;
        {
            Pattern u = Pattern.compile("vimeo.com/(\\d+)");
            Matcher um = u.matcher(sURL.toString());
            if (!um.find()) {
                throw new DownloadError("unknown url");
            }
            id = um.group(1);
            clip = "http://vimeo.com/" + id;
        }

        URL url = new URL(clip);

        String html = WGet.getHtml(url);

        String sig;
        {
            Pattern u = Pattern.compile("\"signature\":\"([0-9a-f]+)\"");
            Matcher um = u.matcher(html);
            if (!um.find()) {
                throw new DownloadError("unknown vimeo respond");
            }
            sig = um.group(1);
        }

        String exp;
        {
            Pattern u = Pattern.compile("\"timestamp\":(\\d+)");
            Matcher um = u.matcher(html);
            if (!um.find()) {
                throw new DownloadError("unknown vimeo respond");
            }
            exp = um.group(1);
        }

        {
            Pattern u = Pattern.compile("\"title\":\"([^\"]+)\"");
            Matcher um = u.matcher(html);
            if (!um.find()) {
                throw new DownloadError("unknown vimeo respond");
            }
            sTitle = um.group(1);
        }

        System.out.println(html);

        String get = "http://player.vimeo.com/play_redirect?clip_id=%s&sig=%s&time=%s&quality=%s&codecs=H264,VP8,VP6&type=moogaloop_local&embed_location=&seek=0";

        String hd = String.format(get, id, sig, exp, "hd");
        String sd = String.format(get, id, sig, exp, "sd");

        sNextVideoURL.put(VideoQuality.p1080, new URL(hd));
        sNextVideoURL.put(VideoQuality.p480, new URL(sd));
    }

    /**
     * Add resolution video for specific youtube link.
     * 
     * @param s
     *            download source url
     * @throws MalformedURLException
     */
    void addVideo(VideoQuality vd, String s) throws MalformedURLException {
        sNextVideoURL.put(vd, new URL(s));
    }

    public VideoInfo extract(VideoInfo vi, VideoQuality max) {
        try {
            downloadone(source);

            return getVideo(vi, sNextVideoURL, max, source, sTitle);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
