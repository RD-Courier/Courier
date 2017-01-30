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
package ru.rd.courier.scripting.test;

import ru.rd.courier.CourierException;
import ru.rd.courier.logging.ConsoleCourierLogger;
import ru.rd.courier.scripting.AbstractContext;
import ru.rd.courier.scripting.Context;
import ru.rd.courier.scripting.LinkWarning;
import ru.rd.courier.utils.StringExpression;
import ru.rd.courier.utils.templates.SimplePreparedTemplate;

import java.text.SimpleDateFormat;
import java.util.List;

public class TestPreparedTemplate {
    public static void main(String[] args) throws CourierException {
        StringExpression pt = new SimplePreparedTemplate(
            ">>[%!map testvar NULL 'var2=[%var2] ''null'' value' 'alex' 'alex value' ELSE 'else ''[%testvar]'' value']<<"
        );
        Context ctx = new AbstractContext(
            ConsoleCourierLogger.instance(), new SimpleDateFormat("yyyyMMdd hh:mm:ss.SSS")
        ) {
            protected void initDataSource(String dbName) {}
            protected void initDataReceiver(String dbName) {}
            protected void initPooledObject(String name) {}
            public void addDbWarning(List<LinkWarning> warnings) {}
        };
        ctx.setVar("var2", "value2");
        ctx.setVar("testvar", "alex");
        System.out.println(ctx.getVar("testvar") + " --> " + pt.calculate(ctx));
        ctx.setVar("testvar", (String)null);
        System.out.println(ctx.getVar("testvar") + " --> " + pt.calculate(ctx));
        ctx.setVar("testvar", "qwerty");
        System.out.println(ctx.getVar("testvar") + " --> " + pt.calculate(ctx));
    }
}
