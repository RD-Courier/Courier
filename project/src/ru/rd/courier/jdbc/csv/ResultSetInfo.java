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
package ru.rd.courier.jdbc.csv;

import java.io.Reader;
import java.util.Properties;

/**
 * User: AStepochkin
 * Date: 03.02.2006
 * Time: 13:57:56
 */
public class ResultSetInfo {
    public String m_lineNumTitle;
    public String[] m_cols;
    public int m_linesToRead;
    public Reader m_reader;
    public LineSplitter m_parser;
    public int m_firstLineNum;
    public LineFilter m_filter;
    public boolean m_addAbsentAsNull;
    public Properties m_constFields;

    public void setLineNumTitle(String lineNumTitle) {
        m_lineNumTitle = lineNumTitle;
    }

    public void setCols(String[] cols) {
        m_cols = cols;
    }

    public void setLinesToRead(int linesToRead) {
        m_linesToRead = linesToRead;
    }

    public void setReader(Reader reader) {
        m_reader = reader;
    }

    public void setParser(LineSplitter parser) {
        m_parser = parser;
    }

    public void setFirstLineNum(int firstLineNum) {
        m_firstLineNum = firstLineNum;
    }

    public void setFilter(LineFilter filter) {
        m_filter = filter;
    }

    public void setAddAbsentAsNull(boolean addAbsentAsNull) {
        m_addAbsentAsNull = addAbsentAsNull;
    }

    public void setConstFields(Properties constFields) {
        m_constFields = constFields;
    }

    public ResultSetInfo(
        String lineNumTitle, String[] cols, int linesToRead, Reader reader,
        LineSplitter parser, int firstLineNum,
        boolean addAbsentAsNull, LineFilter filter,
        Properties constFields
    ) {
        m_lineNumTitle = lineNumTitle;
        m_cols = cols;
        m_linesToRead = linesToRead;
        m_reader = reader;
        m_parser = parser;
        m_firstLineNum = firstLineNum;
        m_addAbsentAsNull = addAbsentAsNull;
        m_filter = filter;
        m_constFields = constFields;
    }

    public ResultSetInfo(ResultSetInfo rsi) {
        this(
            rsi.m_lineNumTitle, rsi.m_cols, rsi.m_linesToRead, rsi.m_reader,
            rsi.m_parser, rsi.m_firstLineNum, rsi.m_addAbsentAsNull,
            rsi.m_filter, rsi.m_constFields
        );
    }
}
