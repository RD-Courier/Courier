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

import ru.rd.courier.utils.LineReader;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.CourierException;

import java.io.IOException;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;

/**
 * User: AStepochkin
 * Date: 03.02.2006
 * Time: 11:27:33
 */
public class FixedColumnsFileSource extends FileSource {
    public static final class Field {
        private final String m_name;
        private final int m_bpos, m_epos;

        public Field(String name, int bpos, int epos) {
            m_name = name;
            m_bpos = bpos;
            m_epos = epos;
        }

        public String getName() {
            return m_name;
        }

        public String extract(StringBuffer line) {
            return line.substring(m_bpos, m_epos);
        }

        public int getBeginPos() {
            return m_bpos;
        }

        public int getWidth() {
            return m_epos - m_bpos;
        }

        public boolean canExtract(StringBuffer line) {
            return (m_epos <= line.length());
        }
    }

    public FixedColumnsFileSource(
        CourierLogger logger,
        String dir, FileSelector fileSelector,
        boolean holdFileUntilNew,
        int fullReadEvery, long fullReadInterval,
        int headerRow, String titleVarName, boolean addAbsentAsNull,
        LineFilter filter, String charSet,
        List<Field> fields
    ) throws CourierException {
        super(
            logger, dir,
            fileSelector, holdFileUntilNew,
            fullReadEvery, fullReadInterval, headerRow,
            titleVarName, addAbsentAsNull, filter, charSet
        );
        sortFields(fields);
        m_lineSplitter = new FixedLineSplitter(fields);
    }

    private static void sortFields(final List<Field> fields) {
        Collections.sort(
            fields,
            new Comparator<Field>() {
                public int compare(Field f1, Field f2) {
                    return f1.m_bpos - f2.m_bpos;
                }
            }
        );
    }

    public static class FixedLineSplitter implements LineSplitter {
        private final List<Field> m_fields;

        public FixedLineSplitter(List<Field> fields) {
            Collections.sort(
                fields,
                new Comparator<FixedColumnsFileSource.Field>() {
                    public int compare(FixedColumnsFileSource.Field f1, FixedColumnsFileSource.Field f2) {
                        return f1.getBeginPos() - f2.getBeginPos();
                    }
                }
            );
            m_fields = fields;
        }

        public String[] parse(StringBuffer text) {
            String[] ret = new String[m_fields.size()];
            int i = 0;
            for (Field f: m_fields) {
                if (!f.canExtract(text)) break;
                ret[i] = f.extract(text);
                i++;
            }
            if (i == ret.length) return ret;
            String[] ret1 = new String[i];
            System.arraycopy(ret, 0, ret1, 0, i);
            return ret1;
        }

        public String[] getColNames() {
            return extractColumnNames(m_fields);
        }
    }

    private static String[] extractColumnNames(final List<Field> fields) {
        String[] cols = new String[fields.size()];
        int i = 0;
        for (Field f: fields) {
            cols[i] = f.getName();
            i++;
        }
        return cols;
    }

    protected void initColumns(LineReader lr) throws IOException {
        m_cols = ((FixedLineSplitter)m_lineSplitter).getColNames();
    }
}
