package com.github.axet.vget;

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

    void download(String url, String sdirectory) {
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
