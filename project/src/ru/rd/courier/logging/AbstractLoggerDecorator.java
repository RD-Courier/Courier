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
package ru.rd.courier.logging;

import java.util.logging.Logger;

public abstract class AbstractLoggerDecorator implements CourierLogger, StdLoggerAdapter {
    private final CourierLogger m_logger;

    public AbstractLoggerDecorator(CourierLogger logger) {
        if (logger == null) throw new IllegalArgumentException("Logger cannot be null");
        m_logger = logger;
    }

    protected abstract String getDecoratedMessage(String msg);

    public String getLoggerName() {
        return m_logger.getLoggerName();
    }

    public CourierLogger getParentLogger() {
        return m_logger.getParentLogger();
    }

    public CourierLogger getChild(String name) {
        return m_logger.getChild(name);
    }

    public void debug(String msg) {
        m_logger.debug(getDecoratedMessage(msg));
    }

    public void info(String msg) {
        m_logger.info(getDecoratedMessage(msg));
    }

    public void warning(String msg) {
        m_logger.warning(getDecoratedMessage(msg));
    }

    public void warning(Throwable e) {
        m_logger.warning(getDecoratedMessage(e.getMessage()), e);
    }

    public void warning(String msg, Throwable e) {
        m_logger.warning(getDecoratedMessage(msg), e);
    }

    public void error(String msg) {
        m_logger.error(getDecoratedMessage(msg));
    }

    public void error(Throwable e) {
        m_logger.error(getDecoratedMessage(e.getMessage()), e);
    }

    public void error(String msg, Throwable e) {
        m_logger.error(getDecoratedMessage(msg), e);
    }

    public Logger getInnerLogger() {
        if (m_logger instanceof StdLoggerAdapter) return ((StdLoggerAdapter)m_logger).getInnerLogger();
        return null;
    }
}
