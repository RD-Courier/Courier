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

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.Timer;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * User: AStepochkin
 * Date: 13.03.2009
 * Time: 14:21:12
 */
public abstract class FileLinesDetector {
    protected final Logger m_logger;
    protected FileChangeDetector m_notifier;
    protected Timer m_timer;
    protected int m_changeCount;

    public FileLinesDetector(Logger logger, File file, Timer timer) {
        m_logger = logger;
        m_timer = timer;
        m_changeCount = 0;
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

    public interface LineParser {
        public void startParse();
        public void parse() throws Exception;
        public void finishParse();
    }

    public static abstract class LineParserImpl implements LineParser {
        protected final StringSimpleParser p;

        public LineParserImpl(StringSimpleParser p) {
            this.p = p;
        }

        public void startParse() {}
        public void finishParse() {}
    }

    protected abstract LineParser createParser(StringSimpleParser p);

    protected void dataChanged_(File file) throws Exception {
        m_changeCount++;
        StringSimpleParser p = new StringSimpleParser();
        LineParser parser = createParser(p);
        if (!file.exists()) {
            p.setPos(0);
            p.setText("");
            parser.parse();
            return;
        }
        Reader data = new FileReader(file);
        LineReader lr = new LineReader(data);
        String line;
        while((line = lr.readLineOrNull()) != null) {
            p.setPos(0);
            p.setText(line);
            try {
                parser.parse();
            } catch (Exception e) {
                m_logger.log(Level.WARNING, e.getMessage(), e);
            }
        }
        parser.finishParse();
    }

    public FileChangeDetector getDetector() {
        return m_notifier;
    }

    public Logger getLogger() {
        return m_logger;
    }

    public Timer getTimer() {
        return m_timer;
    }
}
