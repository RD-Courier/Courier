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
package ru.rd.courier.scripting.dataaccess;

import ru.rd.courier.CourierException;
import ru.rd.courier.FileDataTestCase;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.logging.test.NullLogger;
import ru.rd.pool.ObjectPoolIntf;
import ru.rd.pool.PoolException;
import ru.rd.pool.SynchObjectPool;
import ru.rd.thread.ThreadFactory;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * User: AStepochkin
 * Date: 07.08.2006
 * Time: 15:02:20
 */
public class UrlSourceTest extends FileDataTestCase {
    public void test() throws CourierException, SQLException, PoolException {
        CourierLogger logger = new NullLogger();
        ObjectPoolIntf threadPool = new SynchObjectPool(
            "thread pool", logger, new ThreadFactory(logger, "Test")
        );
        threadPool.start();
        UrlSource s = new UrlSource(
            logger, threadPool,
            //"212.44.148.10",
            null, // without proxy
            0, 0, 0
        );
        File f = getTempFile("url data.bin");
        final ResultSet rs = s.request(
            "url=http://www.google.ru file='" + f.getAbsolutePath() + "'"
        );
        assertTrue(rs.next());
        System.out.println(rs.getString(1));
        setDeleteTempDir(false);
    }
}
