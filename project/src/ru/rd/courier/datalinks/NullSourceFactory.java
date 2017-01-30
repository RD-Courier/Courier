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
package ru.rd.courier.datalinks;

import org.w3c.dom.Node;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.scripting.LinkWarning;
import ru.rd.courier.scripting.TimedStringReceiver;
import ru.rd.courier.scripting.DataSource;
import ru.rd.courier.CourierException;
import ru.rd.courier.jdbc.EmptyResultSet;
import ru.rd.pool.ObjectPoolIntf;

import java.util.List;
import java.sql.ResultSet;

/**
 * User: AStepochkin
 * Date: 03.10.2006
 * Time: 10:28:18
 */
public class NullSourceFactory extends ReceiverFactory {
    public NullSourceFactory(CourierLogger logger, Node conf) {
        super(logger, null);
    }

    public String getDesc() {
        return "NULL";
    }

    private static class NullSource extends TimedStringReceiver implements DataSource {
        protected List<LinkWarning> timedProcess(String operation) { return null; }
        protected List<LinkWarning> timedFlush() { return null; }
        protected void timedClose() {}
        public void setTimeout(int timeout) {}
        public void cancel() {}
        public ResultSet request(String query) throws CourierException {
            return new EmptyResultSet();
        }
    }

    public Object getObject(ObjectPoolIntf pool) {
        return new NullSource();
    }
}
