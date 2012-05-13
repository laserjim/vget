package com.github.axet.vget;

import java.io.File;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

public class VGetBase {
    private AtomicBoolean stop = new AtomicBoolean(false);

    VGetThread t1;

    void shutdownAppl() {
        synchronized (getStop()) {
            stop(true);
        }
        try {
            try {
                t1.interrupt();
            } catch (NullPointerException npe) {
            }
            try {
                t1.join();
            } catch (NullPointerException npe) {
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void download(URL url, File sdirectory) {
        t1 = new VGetThread(this, url, sdirectory);
    }

    void changed() {
    }

    public AtomicBoolean getStop() {
        return stop;
    }

    public void stop(boolean b) {
        stop.set(b);
    }
}
