package com.github.axet.vget;

import java.io.File;
import java.net.URL;

public class DirectDownload {

    public static void main(String[] args) {
        try {
            VGet v = new VGet(new URL("http://www.youtube.com/watch?v=ZIPxGEaB5OM&feature=youtube_gdata"), new File("/Users/axet/Downloads"));
            v.download();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}