/*
 * Copyright 2005-2017 Courier AUTHORS: please see AUTHORS file.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * 1. Redistributions of source code must retain the above
 *    copyright notice, this list of conditions and the
 *    following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer in the documentation and/or other materials
 *    provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY AUTHORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * <COPYRIGHT HOLDER> OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
package ru.rd.utils;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: AStepochkin
 * Date: 12.03.2009
 * Time: 9:17:12
 */
public class FileChangeDetector {
    private final File file;
    private boolean active;
    private long lastUpdate;
    private Timer timer;
    private boolean ownsTimer;
    private TimerTask timerTask;
    private long checkInterval;
    private Executor exec;
    private Logger logger;
    private Collection<Listener> listeners = new LinkedList<Listener>();

    public interface Listener {
        void dataChanged(File file);
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public FileChangeDetector(File file) {
        this.file = file;
        this.checkInterval = 5*1000;
        this.active = false;
    }

    public void setExecutor(Executor exec) {
        this.exec = exec;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    private Timer getTimer() {
        if (timer == null) {
            timer = new Timer();
            ownsTimer = true;
        }
        return timer;
    }

    public void setTimer(Timer timer) {
        this.timer = timer;
        ownsTimer = false;
    }

    public void setCheckInterval(long interval) {
        checkInterval = interval;
    }

    private final Runnable execRun = new Runnable() {
        public void run() {
            checkFile();
        }
    };

    public synchronized void start() {
        if (active) return;
        this.active = true;
        timerTask = new TimerTask() {
            public void run() {
                try {
                    if (exec == null) {
                        checkFile();
                    } else {
                        exec.execute(execRun);
                    }
                } catch (Exception e) {
                    if (logger != null) logger.log(Level.WARNING, e.getMessage(), e);
                }
            }
        };
        getTimer().schedule(timerTask, 0, checkInterval);
    }

    public synchronized void stop() {
        if (!active) return;
        this.active = false;
        if (timerTask != null) {
            timerTask.cancel();
        }
        if (timer != null && ownsTimer) {
            timer.cancel();
            timer = null;
        }
    }

    private boolean checking = false;

    private void checkFile() {
        synchronized(this) {
            if (!active || checking) return;
            checking = true;
        }
        try {
            if (!file.exists()) {
                if (lastUpdate >= 0) {
                    fileChanged();
                }
                lastUpdate = -1;
                return;
            }
            long updated = file.lastModified();
            if (lastUpdate < 0) {
                fileChanged();
            } else {
                if (updated != lastUpdate) {
                    fileChanged();
                }
            }
            lastUpdate = updated;
        } finally {
            synchronized(this) {
                checking = false;
            }
        }
    }

    protected void fileChanged() {
        for (Listener l: listeners) {
            l.dataChanged(file);
        }
    }
}
