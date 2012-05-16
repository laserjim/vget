package com.github.axet.vget.info;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import com.github.axet.vget.VGetBase;
import com.github.axet.vget.info.VideoInfo.VideoQuality;
import com.github.axet.wget.WGet;

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
        Pattern u = Pattern.compile("vimeo.com/(\\d+)");
        Matcher um = u.matcher(sURL.toString());
        if (!um.find()) {
            throw new RuntimeException("unknown url");
        }
        String id = um.group(1);
        String clip = "http://www.vimeo.com/moogaloop/load/clip:" + id;

        URL url = new URL(clip);

        String xml = WGet.getHtml(url);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new ByteArrayInputStream(xml.getBytes()));
        String sig = doc.getElementsByTagName("request_signature").item(0).getTextContent();
        String exp = doc.getElementsByTagName("request_signature_expires").item(0).getTextContent();

        sTitle = doc.getElementsByTagName("caption").item(0).getTextContent();

        String get = String.format("http://www.vimeo.com/moogaloop/play/clip:%s/%s/%s/?q=", id, sig, exp);
        String hd = get + "hd";
        String sd = get + "sd";

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

    public VideoInfo extract(VideoQuality max) {
        try {
            downloadone(source);

            return getVideo(sNextVideoURL, max, sTitle);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
