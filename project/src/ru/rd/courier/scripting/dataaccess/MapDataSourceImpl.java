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

import ru.rd.courier.scripting.MapDataSource;
import ru.rd.courier.scripting.DataLink;
import ru.rd.courier.CourierException;
import ru.rd.utils.Base64;

import java.util.Map;
import java.io.UnsupportedEncodingException;

/**
 * User: AStepochkin
 * Date: 21.06.2006
 * Time: 15:53:18
 */
public abstract class MapDataSourceImpl implements MapDataSource, DataLink {
    static protected String getParam(
        Map<String, String> pars, String name, String defaultValue
    ) {
        if (pars.containsKey(name)) return pars.get(name);
        return defaultValue;
    }

    static protected void ensureParam(Map<String, String> pars, String name) throws CourierException {
        if (!pars.containsKey(name)) {
            throw new CourierException("Parameter '" + name + "' not found");
        }
    }

    static protected String getReqParam(Map<String, String> pars, String name) throws CourierException {
        ensureParam(pars, name);
        return pars.get(name);
    }

    static protected byte[] getBytesParam(
        Map<String, String> pars, String name, String charSet
    ) throws UnsupportedEncodingException {
        return pars.get(name).getBytes(charSet);
    }

    static protected byte[] getReqBytesParam(
            Map<String, String> pars, String name, String charSet
    ) throws UnsupportedEncodingException, CourierException {
        ensureParam(pars, name);
        return getBytesParam(pars, name, charSet);
    }

    static protected byte[] getBase64Param(Map<String, String> pars, String name) {
        return Base64.decode(pars.get(name));
    }

    static protected byte[] getReqBase64Param(Map<String, String> pars, String name) throws CourierException {
        ensureParam(pars, name);
        return getBase64Param(pars, name);
    }

    public void setTimeout(int timeout) throws CourierException {}
    public void cancel() throws CourierException {}
    public void close() throws CourierException {}    
}
