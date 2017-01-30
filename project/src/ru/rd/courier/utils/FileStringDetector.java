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
package ru.rd.courier.utils;

import ru.rd.utils.FileChangeDetector;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Timer;
import java.io.File;

/**
 * User: AStepochkin
 * Date: 06.04.2009
 * Time: 10:35:31
 */
public abstract class FileStringDetector {
    protected final Logger m_logger;
    protected FileChangeDetector m_notifier;

    public FileStringDetector(Logger logger, File file, Timer timer) {
        m_logger = logger;
        m_notifier = new FileChangeDetector(file);
        m_notifier.setTimer(timer);
        m_notifier.addListener(new FileChangeDetector.Listener() {
            public void dataChanged(File file) {
                try {
                    dataChanged_(file);
                } catch (Exception e) {
                    m_logger.log(Level.WARNING, e.getMessage(), e);
                }
            }
        });
    }

    private void dataChanged_(File file) throws Exception {
        String text;
        if (file.exists()) {
            text = FileHelper.fileToString(file, "cp1251");
        } else {
            text = "";
        }
        StringSimpleParser p = new StringSimpleParser(text);
        parse(p);
    }

    public void start() {
        m_notifier.start();
    }

    public void stop() {
        m_notifier.stop();
    }

    public Logger getLogger() {
        return m_logger;
    }

    public FileChangeDetector getDetector() {
        return m_notifier;
    }

    protected abstract void parse(StringSimpleParser p);
}
