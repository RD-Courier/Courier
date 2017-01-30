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
import ru.rd.courier.jdbc.ResultSets.IterColumnInfo;
import ru.rd.courier.jdbc.csv.*;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.scripting.dataaccess.streamed.*;
import ru.rd.courier.utils.DomHelper;
import ru.rd.courier.utils.LineReader;
import ru.rd.courier.utils.StringHelper;
import ru.rd.courier.utils.StringSimpleParser;
import ru.rd.courier.CourierContext;
import ru.rd.pool.ObjectPoolIntf;

import java.io.*;
import java.util.Properties;

/**
 * User: AStepochkin
 * Date: 09.08.2006
 * Time: 16:56:23
 */
public class StreamSourceFactory extends ReceiverFactory {
    private final StreamConnectorFactory m_connectorFactory;
    private final StreamParserFactory m_parserFactory;
    private final boolean m_cacheData;

    private interface StreamConnectorFactory {
        StreamConnector createConnector();
    }

    public StreamSourceFactory(
        CourierLogger logger, String name, CourierContext ctx, Node conf
    ) {
        super(logger, ctx.getThreadPool());
        m_cacheData = DomHelper.getBoolYesNo(conf, "cache-data", true);
        m_connectorFactory = getStreamFactory(name, ctx, conf);
        final Node parserConf = DomHelper.getChild(conf, "parser", false);
        if (parserConf == null) {
            m_parserFactory = new StreamParserFactory() {
                public StreamParser createParser() {
                    return null;
                }
            };
        } else {
            m_parserFactory = getParserFactory(logger, parserConf);
        }
    }

    private static class NumberedHeaderReader implements CsvParser.HeaderReader {
        private final int m_colsCount;
        private final String m_colsPrefix;

        public NumberedHeaderReader(int colsCount, String colsPrefix) {
            m_colsCount = colsCount;
            m_colsPrefix = colsPrefix;
        }

        public String[] readHeader(Reader r) {
            String[] res = new String[m_colsCount];
            for (int i = 0; i < m_colsCount; i++) {
                res[i] = m_colsPrefix + (i + 1);
            }
            return res;
        }
    }

    private static class CsvHeaderReader implements CsvParser.HeaderReader {
        private final LineSplitter m_lineParser;

        public CsvHeaderReader(LineSplitter lineParser) {
            m_lineParser = lineParser;
        }

        public String[] readHeader(Reader r) throws IOException {
            LineReader lr = new LineReader(r);
            return m_lineParser.parse(new StringBuffer(lr.readLine()));
        }
    }

    private static class ConstHeaderReader implements CsvParser.HeaderReader {
        private final String[] m_cols;

        public ConstHeaderReader(String[] cols) {
            m_cols = cols;
        }

        public String[] readHeader(Reader r) throws IOException {
            return m_cols;
        }
    }

    private static class CsvParserFactory implements StreamParserFactory {
        private final CsvParser m_parser;

        public CsvParserFactory(CourierLogger logger, Node conf) {
            Node fields = DomHelper.getChild(conf, "fields", false);
            final LineSplitter ls;
            final String[] cols;
            CsvParser.HeaderReader headerParser = null;
            if (fields == null) {
                ls = new CsvLineParser(new CsvLineSplitterInfo(conf));
                cols = null;
                Node header = DomHelper.getChild(conf, "header", false);
                if (header != null) {
                    String headerType = DomHelper.getNodeAttr(header, "type", "none");
                    if (headerType.equals("numbered")) {
                        headerParser = new NumberedHeaderReader(
                            DomHelper.getIntNodeAttr(header, "columns-count"),
                            DomHelper.getNodeAttr(header, "columns-prefix", "")
                        );
                    } else if (headerType.equals("csv")) {
                        headerParser = new CsvHeaderReader(ls);
                    } else if (headerType.equals("list")) {
                        headerParser = new ConstHeaderReader(
                            StringHelper.splitStringAndTrim(
                                DomHelper.getNodeAttr(header, "names"), ','
                            )
                        );
                    } else if (headerType.equals("none")) {
                        throw new RuntimeException("Header type '" + headerType + "' not implemented yet");
                    } else {
                        throw new RuntimeException("Invalid header type '" + headerType + "'");
                    }
                }
            } else {
                FixedColumnsFileSource.FixedLineSplitter fls = new FixedColumnsFileSource.FixedLineSplitter(
                    FixedColumnsFileSourceFactory.fieldsFromXml(fields)
                );
                ls = fls;
                cols = fls.getColNames();
            }

            Node constFields = DomHelper.getChild(conf, "const-fields", false);
            ResultSetInfo rsi = new ResultSetInfo(
                DomHelper.getNodeAttr(conf, "line-number-title", "LineNumber"),
                cols,
                -1,
                null,
                ls,
                1,
                DomHelper.getBoolYesNo(conf, "absent-as-null", false),
                FixedColumnsFileSourceFactory.filterFromXml(conf),
                constFields == null ? new Properties() : DomHelper.getAllParams(conf)
            );

            m_parser = new CsvParser(
                logger,
                DomHelper.getNodeAttr(conf, "encoding", "cp1251"),
                DomHelper.getIntNodeAttr(conf, "skip-first-lines", 0),
                headerParser,
                rsi
            );
        }

        public StreamParser createParser() {
            return m_parser;
        }
    }

    public static StreamParserFactory getParserFactory(
        final CourierLogger logger, Node conf
    ) {
        String type = DomHelper.getNodeAttr(conf, "type").toLowerCase();
        if (type.equals("xml")) {
            return new XPathParserFactory(conf);
        } else if (type.equals("csv")) {
            return new CsvParserFactory(logger, conf);
        } else if (type.equals("json")) {
            return new JSONPathParserFactory(conf);
        } else if (type.equals("excel")) {
            return new ExcelParserFactory(conf);
        } else if (type.equals("dbf")) {
            final String encoding = DomHelper.getNodeAttr(conf, "encoding", null);
            final boolean trim = !DomHelper.getBoolYesNo(conf, "cancel-trim", false);
            return new StreamParserFactory() {
                public StreamParser createParser() {
                    return new DbfParser(encoding, trim);
                }
            };
        } else if (type.equals("null")) {
            final IterColumnInfo[] cols;
            cols = new IterColumnInfo[] {
                new IterColumnInfo(
                    DomHelper.getNodeAttr(conf, "field-name"), 0
                )
            };

            final String encoding;
            encoding = DomHelper.getNodeAttr(conf, "encoding", "cp1251");

            return new StreamParserFactory() {
                public StreamParser createParser() {
                    return new NullParser(encoding, cols);
                }
            };
        } else if (type.equals("file")) {
            final String file = DomHelper.getNodeAttr(conf, "file", null);
            final String fn = DomHelper.getNodeAttr(conf, "field-name", null);
            final String fieldName = DomHelper.getNodeAttr(conf, "fieldName", fn);
            return new StreamParserFactory() {
                public StreamParser createParser() {
                    return new MoveToFileParser(logger, fieldName, file);
                }
            };
        } else {
            throw new RuntimeException("Invalid parser type '" + type + "'");
        }
    }

    private StreamConnectorFactory getStreamFactory(final String name, final CourierContext ctx, Node rootConf) {
        final Node conf = DomHelper.getChild(rootConf, "stream", false);
        if (conf == null) return new StreamConnectorFactory() {
            public StreamConnector createConnector() { return null; }
        };

        abstract class PropFactory implements StreamConnectorFactory {
            protected Properties extractProperties() {
                return extractProperties(null, null);
            }

            protected Properties extractProperties(String hostparam, String type) {
                Properties ret = DomHelper.getAttrParams(conf);
                AccountUtils.accountToProps(hostparam, type, name, ctx.getAccountProvider(), ret);
                return ret;
            }
        }

        String type = DomHelper.getNodeAttr(conf, "type").toLowerCase();
        if (type.equals("url")) {
            return new PropFactory() {
                private final UrlStreamProperties m_props =
                    new UrlStreamProperties(extractProperties());

                public StreamConnector createConnector() {
                    return new UrlStreamConnector(m_props);
                }
            };
        } else if (type.equals("exec")) {
            final String wd = DomHelper.getNodeAttr(conf, "work-dir", null);
            Node envConf = DomHelper.getChild(conf, "env", false);
            final String[] env;
            if (envConf == null) {
                env = null;
            } else {
                Node[] vars = DomHelper.getChildrenByTagName(conf, "var", false);
                env = new String[vars.length];
                for (int i = 0; i < vars.length; i++) {
                    env[i] = DomHelper.getNodeValue(vars[i]);
                }
            }
            return new StreamConnectorFactory() {
                public StreamConnector createConnector() {
                    return new ExecStreamConnector(
                        m_logger, m_threadPool, null, env,
                        wd == null ? null : new File(wd)
                    );
                }
            };
        } else if (type.equals("ssh")) {
            return new StreamConnectorFactory() {
                public StreamConnector createConnector() {
                    String host = DomHelper.getNodeAttr(conf, "host", null);
                    Account account = AccountUtils.confAccountDef(host, "SSH", name, ctx.getAccountProvider(), conf, new Account(null, null));
                    return new SshStreamConnector(
                        m_logger, m_threadPool,
                        host,
                        DomHelper.getIntNodeAttr(conf, "port", -1),
                        account.name, account.password,
                        DomHelper.getNodeAttr(conf, "command", null)
                    );
                }
            };
        } else if (type.equals("const")) {
            Node dataConf = DomHelper.getChild(conf, "data", false);
            final String data;
            if (dataConf != null) {
                data = DomHelper.getNodeValue(dataConf);
            } else {
                data = null;
            }
            return new StreamConnectorFactory() {
                public StreamConnector createConnector() {
                    return new ConstStreamConnector(data);
                }
            };
        } else if (type.equals("ftp")) {
            return new PropFactory() {
                private final Properties m_props = extractProperties(FtpProperties.cHostProp, "FTP");

                public StreamConnector createConnector() {
                    return new FtpConnector(m_props);
                }
            };
        } else if (type.equals("file")) {
            return new PropFactory() {
                private final Properties m_props = extractProperties();

                public StreamConnector createConnector() {
                    return new StreamConnector() {
                        public void parseProperties(StringSimpleParser p) {
                            p.getProperties(m_props, '\'', "|");
                        }

                        public InputStream createStream() throws IOException {
                            return new BufferedInputStream(new FileInputStream(
                                StringHelper.stringParam(m_props, "file")
                            ));
                        }

                        public void cancel() {}
                    };
                }
            };
        } else {
            throw new RuntimeException("Invalid source type '" + type + "'");
        }
    }

    public Object getObject(ObjectPoolIntf pool) {
        return new StreamSource(
            m_logger, m_threadPool,
            m_connectorFactory.createConnector(),
            m_parserFactory.createParser(),
            m_cacheData
        );
    }
}
