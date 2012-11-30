package com.github.axet.vget.info;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import com.github.axet.vget.info.VideoInfo.States;
import com.github.axet.vget.info.VideoInfo.VideoQuality;
import com.github.axet.wget.WGet;
import com.github.axet.wget.info.ex.DownloadError;

public class YouTubeParser extends VGetParser {

    public static class AgeException extends DownloadError {
        private static final long serialVersionUID = 1L;

        public AgeException() {
            super("Age restriction, account required");
        }
    }

    public static class PrivateVideoException extends DownloadError {
        private static final long serialVersionUID = 1L;

        public PrivateVideoException() {
            super("Private video");
        }

        public PrivateVideoException(String s) {
            super(s);
        }
    }

    public static class EmbeddingDisabled extends DownloadError {
        private static final long serialVersionUID = 1L;

        public EmbeddingDisabled(String msg) {
            super(msg);
        }
    }

    HashMap<VideoQuality, URL> sNextVideoURL = new HashMap<VideoQuality, URL>();

    URL source;

    public YouTubeParser(URL input) {
        this.source = input;
    }

    public static boolean probe(URL url) {
        return url.toString().contains("youtube.com");
    }

    void downloadone(VideoInfo info, AtomicBoolean stop, Runnable notify) throws Exception {
        try {
            extractEmbedded(info, stop, notify);
        } catch (EmbeddingDisabled e) {
            streamCpature(info, stop, notify);
        }
    }

    void streamCpature(final VideoInfo info, final AtomicBoolean stop, final Runnable notify) throws Exception {
        String html;
        html = WGet.getHtml(info.getWeb(), new WGet.HtmlLoader() {
            @Override
            public void notifyRetry(int delay, Throwable e) {
                info.setState(States.RETRYING, e);
                info.setDelay(delay);
                notify.run();
            }

            @Override
            public void notifyDownloading() {
                info.setState(States.DOWNLOADING);
                notify.run();
            }

            @Override
            public void notifyMoved() {
                info.setState(States.RETRYING);
                notify.run();
            }
        }, stop);
        extractHtmlInfo(info, html);
        extractIcon(info, html);
    }

    /**
     * Add resolution video for specific youtube link.
     * 
     * @param s
     *            download source url
     * @throws MalformedURLException
     */
    void addVideo(VideoQuality vd, String s) throws MalformedURLException {
        if (s != null)
            sNextVideoURL.put(vd, new URL(s));
    }

    void addVideo(Integer itag, String url) throws MalformedURLException {
        switch (itag) {
        case 38:
            // mp4
            addVideo(VideoQuality.p1080, url);
            break;
        case 37:
            // mp4
            addVideo(VideoQuality.p1080, url);
            break;
        case 46:
            // webm
            addVideo(VideoQuality.p1080, url);
            break;
        case 22:
            // mp4
            addVideo(VideoQuality.p720, url);
            break;
        case 45:
            // webm
            addVideo(VideoQuality.p720, url);
            break;
        case 35:
            // mp4
            addVideo(VideoQuality.p480, url);
            break;
        case 44:
            // webm
            addVideo(VideoQuality.p480, url);
            break;
        case 18:
            // mp4
            addVideo(VideoQuality.p360, url);
            break;
        case 34:
            // flv
            addVideo(VideoQuality.p360, url);
            break;
        case 43:
            // webm
            addVideo(VideoQuality.p360, url);
            break;
        case 6:
            // flv
            addVideo(VideoQuality.p270, url);
            break;
        case 5:
            // flv
            addVideo(VideoQuality.p224, url);
            break;
        }
    }

    void extractEmbedded(final VideoInfo info, final AtomicBoolean stop, final Runnable notify) throws Exception {
        Pattern u = Pattern.compile("youtube.com/.*v=([^&]*)");
        Matcher um = u.matcher(source.toString());
        if (!um.find()) {
            throw new RuntimeException("unknown url");
        }
        String id = um.group(1);

        info.setTitle(String.format("http://www.youtube.com/watch?v=%s", id));

        String get = String
                .format("http://www.youtube.com/get_video_info?video_id=%s&el=embedded&ps=default&eurl=", id);

        URL url = new URL(get);

        String qs = WGet.getHtml(url, new WGet.HtmlLoader() {
            @Override
            public void notifyRetry(int delay, Throwable e) {
                info.setState(States.RETRYING, e);
                info.setDelay(delay);
                notify.run();
            }

            @Override
            public void notifyDownloading() {
                info.setState(States.DOWNLOADING);
                notify.run();
            }

            @Override
            public void notifyMoved() {
                info.setState(States.RETRYING);
                notify.run();
            }
        }, stop);

        Map<String, String> map = getQueryMap(qs);

        if (map.get("status").equals("fail")) {
            String r = URLDecoder.decode(map.get("reason"), "UTF-8");
            if (map.get("errorcode").equals("150"))
                throw new PrivateVideoException(r);
            else
                throw new EmbeddingDisabled(r);
        }

        info.setTitle(URLDecoder.decode(map.get("title"), "UTF-8"));

        // String fmt_list = URLDecoder.decode(map.get("fmt_list"), "UTF-8");
        // String[] fmts = fmt_list.split(",");
        String url_encoded_fmt_stream_map = URLDecoder.decode(map.get("url_encoded_fmt_stream_map"), "UTF-8");

        extractUrlEncodedVideos(url_encoded_fmt_stream_map);

        // 'iurlmaxresæ or 'iurlsd' or 'thumbnail_url'
        String icon = map.get("thumbnail_url");
        icon = URLDecoder.decode(icon, "UTF-8");
        info.setIcon(new URL(icon));
    }

    void extractIcon(VideoInfo info, String html) {
        try {
            Pattern title = Pattern.compile("itemprop=\"thumbnailUrl\" href=\"(.*)\"");
            Matcher titleMatch = title.matcher(html);
            if (titleMatch.find()) {
                String sline = titleMatch.group(1);
                sline = StringEscapeUtils.unescapeHtml4(sline);
                info.setIcon(new URL(sline));
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, String> getQueryMap(String qs) {
        try {
            qs = qs.trim();
            List<NameValuePair> list;
            list = URLEncodedUtils.parse(new URI(null, null, null, 0, null, qs, null), "UTF-8");
            HashMap<String, String> map = new HashMap<String, String>();
            for (NameValuePair p : list) {
                map.put(p.getName(), p.getValue());
            }
            return map;
        } catch (URISyntaxException e) {
            throw new RuntimeException(qs, e);
        }
    }

    void extractHtmlInfo(VideoInfo info, String html) throws Exception {
        Pattern age = Pattern.compile("(verify_age)");
        Matcher ageMatch = age.matcher(html);
        if (ageMatch.find())
            throw new AgeException();

        Pattern gen = Pattern.compile("\"(http(.*)generate_204(.*))\"");
        Matcher genMatch = gen.matcher(html);
        if (genMatch.find()) {
            String sline = genMatch.group(1);

            sline = StringEscapeUtils.unescapeJava(sline);
        }

        Pattern urlencod = Pattern.compile("\"url_encoded_fmt_stream_map\": \"([^\"]*)\"");
        Matcher urlencodMatch = urlencod.matcher(html);
        if (urlencodMatch.find()) {
            String url_encoded_fmt_stream_map;
            url_encoded_fmt_stream_map = urlencodMatch.group(1);

            // normal embedded video, unable to grab age restricted videos
            Pattern encod = Pattern.compile("url=(.*)");
            Matcher encodMatch = encod.matcher(url_encoded_fmt_stream_map);
            if (encodMatch.find()) {
                String sline = encodMatch.group(1);

                extractUrlEncodedVideos(sline);
            }

            // stream video
            Pattern encodStream = Pattern.compile("stream=(.*)");
            Matcher encodStreamMatch = encodStream.matcher(url_encoded_fmt_stream_map);
            if (encodStreamMatch.find()) {
                String sline = encodStreamMatch.group(1);

                String[] urlStrings = sline.split("stream=");

                for (String urlString : urlStrings) {
                    urlString = StringEscapeUtils.unescapeJava(urlString);

                    Pattern link = Pattern.compile("(sparams.*)&itag=(\\d+)&.*&conn=rtmpe(.*),");
                    Matcher linkMatch = link.matcher(urlString);
                    if (linkMatch.find()) {

                        String sparams = linkMatch.group(1);
                        String itag = linkMatch.group(2);
                        String url = linkMatch.group(3);

                        url = "http" + url + "?" + sparams;

                        url = URLDecoder.decode(url, "UTF-8");

                        addVideo(Integer.decode(itag), url);
                    }
                }
            }
        }

        Pattern title = Pattern.compile("<meta name=\"title\" content=(.*)");
        Matcher titleMatch = title.matcher(html);
        if (titleMatch.find()) {
            String sline = titleMatch.group(1);
            String name = sline.replaceFirst("<meta name=\"title\" content=", "").trim();
            name = StringUtils.strip(name, "\">");
            name = StringEscapeUtils.unescapeHtml4(name);
            info.setTitle(name);
        }
    }

    void extractUrlEncodedVideos(String sline) throws Exception {
        String[] urlStrings = sline.split("url=");

        for (String urlString : urlStrings) {
            urlString = StringEscapeUtils.unescapeJava(urlString);

            {
                Pattern link = Pattern.compile("(.*)&quality=(.*)&fallback_host=(.*)&type=(.*)itag=(\\d+)");
                Matcher linkMatch = link.matcher(urlString);
                if (linkMatch.find()) {
                    String url = linkMatch.group(1);
                    String itag = linkMatch.group(5);

                    url = URLDecoder.decode(url, "UTF-8");

                    addVideo(Integer.decode(itag), url);
                }
            }

            // youtube after 2012/09/27
            {
                Pattern link = Pattern.compile("(.*)&type=(.*)&fallback_host=(.*)&sig=(.*)&quality=(.*),itag=(\\d+)");
                Matcher linkMatch = link.matcher(urlString);
                if (linkMatch.find()) {
                    String url = linkMatch.group(1);

                    String type = linkMatch.group(2);
                    String fallback_host = linkMatch.group(3);
                    String sig = linkMatch.group(4);
                    String quality = linkMatch.group(5);
                    String itag = linkMatch.group(6);

                    url = URLDecoder.decode(url, "UTF-8");
                    type = URLDecoder.decode(type, "UTF-8");

                    url += "&signature=" + sig;

                    addVideo(Integer.decode(itag), url);
                }
            }
        }
    }

    @Override
    public void extract(VideoInfo info, VideoQuality max, AtomicBoolean stop, Runnable notify) {
        try {
            downloadone(info, stop, notify);

            getVideo(info, sNextVideoURL, max);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
