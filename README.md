# vget

Inspired by http://sourceforge.net/projects/ytd2/.

Code taken from ytd2 and completely rewritten to support more web sites.

Good examples here:
  https://github.com/pculture/miro/blob/master/tv/lib/flashscraper.py

## Exceptions

Here is a three kind of exceptions.

1) Fatal exception. all RuntimeException's
  We shall stop application

2) DownloadError (extends RuntimeException)
  We unable to process following url and shall stop to download it
  
3) DownloadRetry (caused by IOException)
  We're having temporary problems. Shall retry download after a delay.

## Example Direct Download

    package com.github.axet.vget;
    
    import java.io.File;
    import java.net.URL;
    
    public class Example {
    
        public static void main(String[] args) {
            try {
                VGet v = new VGet(new URL("http://vimeo.com/52716355"), new File("/Users/axet/Downloads"));
                v.download();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    
    }

## Example Application Managed Download


## Cetranal Maven Repo

    <dependency>
      <groupId>com.github.axet</groupId>
      <artifactId>vget</artifactId>
      <version>0.1.8</version>
    </dependency>
