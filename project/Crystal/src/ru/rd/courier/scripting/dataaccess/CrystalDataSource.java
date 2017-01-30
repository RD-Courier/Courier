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

import com.crystaldecisions.sdk.framework.CrystalEnterprise;
import com.crystaldecisions.sdk.framework.IEnterpriseSession;
import com.crystaldecisions.sdk.occa.infostore.*;
import com.crystaldecisions.sdk.plugin.desktop.common.IReportFormatOptions;
import com.crystaldecisions.sdk.plugin.desktop.common.IReportParameter;
import com.crystaldecisions.sdk.plugin.desktop.common.IReportParameterSingleValue;
import com.crystaldecisions.sdk.plugin.desktop.report.IReport;
import com.crystaldecisions.sdk.plugin.destination.diskunmanaged.IDiskUnmanagedOptions;
import com.crystaldecisions.sdk.properties.IProperties;
import com.crystaldecisions.sdk.properties.IProperty;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import ru.rd.courier.CourierException;
import ru.rd.courier.jdbc.databuffer.DataBufferResultSet;
import ru.rd.courier.jdbc.databuffer.IntegerColumnInfo;
import ru.rd.courier.jdbc.databuffer.StringColumnInfo;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.scripting.DataSource;
import ru.rd.courier.scripting.LinkWarning;
import ru.rd.courier.scripting.TimedStringReceiver;
import ru.rd.courier.utils.DomHelper;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

/**
 * User: Andrew A.Toschev
 * Date: Apr 11, 2006 Time: 3:55:06 PM
 * Description: Datasource for Crystal Reports Interface
 */
public class CrystalDataSource extends TimedStringReceiver implements DataSource {
    private CourierLogger m_logger;
    private IEnterpriseSession enterpriseSession = null;
    private IInfoStore infoStore = null;
    private IInfoObjects iInfoObjects = null;

    public CrystalDataSource(
        CourierLogger logger,
        String m_username, String m_password, String m_host, String m_authentication
    ) throws CourierException {
        try {
            m_logger = logger;
            this.enterpriseSession = CrystalEnterprise.getSessionMgr().logon(m_username, m_password, m_host, m_authentication);
            infoStore = (IInfoStore) enterpriseSession.getService("", "InfoStore");
        } catch (Exception e) {
            throw new CourierException(e);
        }
    }

    public void closeConnect() {
        enterpriseSession.logoff();
    }

    public boolean checkConnect() {
        try {
            return enterpriseSession.getUserInfo().getUserID() > 0;
        } catch (Exception e) {
            m_logger.warning(e);
        }
        return false;
    }

    protected List<LinkWarning> timedFlush() { return null; }
    protected void timedClose() throws CourierException { }
    public void setTimeout(int i) throws CourierException { }
    public void cancel() throws CourierException { }

    static final String[] cRequestElem = {"schedule", "status", "parameters", "sql", "cleanup"};
    static final String cReportElem = "report";
    static final String cTypeElem = "type";
    static final String cIdElem = "id";
    static final String cNameElem = "name";
    static final String cValueElem = "value";
    static final String cFileElem = "file";
    static final String cRetryElem = "retries";
    static final String cWipeElem = "wipe";

    protected List<LinkWarning> timedProcess(String s) throws CourierException {
        request(s);
        return null;
    }

    public ResultSet request(String s) throws CourierException {

        try {
            final Node root = parseXml(s, true);
            final String rs_type = DomHelper.getNodeAttr(root, cTypeElem);

            if (rs_type.equalsIgnoreCase(cRequestElem[0])) return getInstance(root);
            else if (rs_type.equalsIgnoreCase(cRequestElem[1])) return getStatus(root);
            else if (rs_type.equalsIgnoreCase(cRequestElem[2])) return getParameters(root);
            else if (rs_type.equalsIgnoreCase(cRequestElem[3])) return getISql(root);
            else if (rs_type.equalsIgnoreCase(cRequestElem[4])) return setCleanUp(root);

            throw new CourierException("Data source does not support the '" + rs_type + "' operation");

        } catch (Exception e) {
            throw new CourierException(e);
        }
    }

    private Element parseXml(String xml, boolean addHeader) throws SQLException {
        try {
            return DomHelper.parseString((addHeader ? "<?xml version=\"1.0\" encoding=\"windows-1251\" ?> " : "") + xml).getDocumentElement();
        } catch (Exception e) {
            SQLException te = new SQLException(e.getMessage());
            te.initCause(e);
            throw te;
        }
    }

    private int getIdElement(Node root) throws CourierException {
        final Element idElem = DomHelper.getChild(root, cReportElem, false);

        if (idElem == null || !idElem.hasAttribute(cIdElem))
            throw new CourierException("Incorrect '" + cIdElem + "' element item specified");

        return Integer.valueOf(idElem.getAttribute(cIdElem));
    }

    private ResultSet getInstance(Node root) throws CourierException {
        try {
            IInfoObject reportObj = getInfoObject(getIdElement(root));

            if (reportObj.isInstance())
                throw new CourierException("Rescheduling haven't implemented yet.");

            return getStatus(scheduleReports(reportObj, root), new DataBufferResultSet());

        } catch (Exception e) {
            throw new CourierException(e);
        }
    }

    private ResultSet getStatus(Node root) throws CourierException {
        try {
            return getStatus(getInfoObject(getIdElement(root)), new DataBufferResultSet());
        } catch (Exception e) {
            throw new CourierException(e);
        }
    }

    private ResultSet getStatus(IInfoObject reportObject, DataBufferResultSet rs) throws CourierException {
        try {
            if (rs.getMetaData().getColumnCount() == 0) {
                rs.addColumn(new IntegerColumnInfo(cIdElem, false));
                rs.addColumn(new StringColumnInfo(cNameElem, 255));
                rs.addColumn(new StringColumnInfo(cRequestElem[1], 16));
            }

            rs.addRecord();
            rs.updateInt(cIdElem, reportObject.getID());
            rs.updateString(cNameElem, reportObject.getTitle());
            rs.updateString(cRequestElem[1], getStatusType(reportObject.isInstance() ?
                    reportObject.getSchedulingInfo().getStatus() : -1));
            rs.beforeFirst();

        } catch (Exception e) {
            throw new CourierException(e);
        }
        return rs;
    }

    private ResultSet getParameters(Node root) throws CourierException {
        DataBufferResultSet rs = new DataBufferResultSet();

        try {
            rs.addColumn(new IntegerColumnInfo(cIdElem, false));
            rs.addColumn(new StringColumnInfo(cNameElem, 32));
            rs.addColumn(new StringColumnInfo(cTypeElem, 16));
            rs.addColumn(new StringColumnInfo(cValueElem, 120));

            IInfoObject scheduledObj = getInfoObject(getIdElement(root));

            if (scheduledObj.isInstance())
                return getParamsbyId(rs, scheduledObj.getID(), scheduledObj.getProcessingInfo().properties().getProperty("SI_PROMPTS"));
            else
                return getParamsbyId(rs, scheduledObj.getID(), ((IReport) scheduledObj).getReportParameters());
        } catch (Exception e) {
            throw new CourierException(e);
        }
    }

    private ResultSet getISql(Node root) throws Exception {
        IInfoObjects oiInfoObjects = infoStore.query(root.getTextContent());
        DataBufferResultSet rs = new DataBufferResultSet();

        for (Object oiInfoObject : oiInfoObjects) {
            rs = (DataBufferResultSet) getStatus((IInfoObject) oiInfoObject, rs);
        }
        return rs;
    }

    private ResultSet setCleanUp(Node root) throws CourierException {
        final Element wipeElem = DomHelper.getChild(root, cReportElem, false);
        try {
            IInfoObject reportObj = getInfoObject(getIdElement(root));

            if (!reportObj.isInstance())
                throw new CourierException("Incorrect Instanceid=" + reportObj.getID() + " '" + reportObj.getTitle() + "' specified");

            if (wipeElem != null && wipeElem.hasAttribute(cWipeElem)) {
                String sWipe = wipeElem.getAttribute(cWipeElem);
                if (sWipe.toLowerCase().matches("(1|true|yes)")) {
                    IProperties iDest = (IProperties)reportObj.getSchedulingInfo().properties().getProperty("SI_DESTINATION").getValue();
                    iDest = (IProperties)((IProperties)iDest.getProperty("SI_DEST_SCHEDULEOPTIONS").getValue()).getProperty("SI_OUTPUT_FILES").getValue();

                    File fileReport = new File(iDest.getProperty("1").getValue().toString());
                    if (fileReport.exists()) fileReport.delete();
                }
            }
            reportObj.deleteNow();
            return getStatus(reportObj, new DataBufferResultSet());

        } catch (Exception e) {
            throw new CourierException(e);
        }
    }

    /**
     * Other helper and internal functions
     */
    public IInfoObject scheduleReports(IInfoObject reportObj, Node root) throws Exception {
        final Element typeElem = DomHelper.getChild(root, cReportElem, false);

        if (typeElem == null) throw new NullPointerException("typeElem");
        if (!typeElem.hasAttribute(cTypeElem) || !typeElem.hasAttribute(cFileElem)) {
            throw new CourierException("Incorrect '" + (typeElem.hasAttribute(cTypeElem) ? cFileElem : cTypeElem) + "' element item specified");
        }

        int repType = getReportType(typeElem.getAttribute(cTypeElem).toUpperCase());

        //Grab the first object in the collection, this will be the object that will be scheduled.
        IReportFormatOptions reportFormat = ((IReport) reportObj).getReportFormatOptions();

        //Set report format.
        reportFormat.setFormat(repType);
        IDestinationPlugin destinationPlugin = getDestinationPlugin(typeElem.getAttribute(cFileElem));
        setReportParameters((IReport) reportObj, root);

        //Retrieve the ISchedulingInfo Interface for the Report object and set the schedule time (right now) and type (run once)
        ISchedulingInfo schedInfo = reportObj.getSchedulingInfo();
        schedInfo.setRightNow(true);
        schedInfo.setType(CeScheduleType.ONCE);
        if (typeElem.hasAttribute(cRetryElem)) {
            int iRetries = Integer.parseInt(typeElem.getAttribute(cRetryElem));
            if (iRetries > 0) {
                schedInfo.setRetriesAllowed(iRetries);
                schedInfo.setRetryInterval(30);
            }
        }

        IDestination destination = schedInfo.getDestination();
        destination.setFromPlugin(destinationPlugin);
        destination.setCleanup(false);

        infoStore.schedule(iInfoObjects);
        String newID = ((IReport)iInfoObjects.get(0)).properties().getProperty("SI_NEW_JOB_ID").getValue().toString();

        return getInfoObject(Integer.parseInt(newID));
    }

    private IDestinationPlugin getDestinationPlugin(String outFile) throws Exception {
        //Retrieve the UnmanagedDisk Destination plugin from the InfoStore
        final String queryStr =
            "SELECT TOP 1 * FROM CI_SYSTEMOBJECTS" +
            " WHERE SI_NAME='CrystalEnterprise.DiskUnmanaged'";
        IDestinationPlugin destinationPlugin = (IDestinationPlugin) infoStore.query(queryStr).get(0);

        //Retrieve the Scheduling Options and cast it as IDiskUnmanagedOptions.
        IDiskUnmanagedOptions diskUnmanagedOptions = (IDiskUnmanagedOptions) destinationPlugin.getScheduleOptions();
        List listDestination = diskUnmanagedOptions.getDestinationFiles();
        //noinspection unchecked
        listDestination.add(outFile);
        return destinationPlugin;
    }

    private void setReportParameters(IReport oReport, Node root) throws Exception {
        List paramList = oReport.getReportParameters();
        IReportParameterSingleValue currentValue;
        Properties prop = DomHelper.getElementParams(root);

        //For each parameter in the report, set a parameter value appropriate for the parameter type prior to scheduling.
        for (Object aParamList : paramList) {
            IReportParameter oReportParameter = (IReportParameter) aParamList;

            String sParName = oReportParameter.getParameterName();
            Enumeration<?> em = prop.propertyNames();

            while (em.hasMoreElements()) {
                String objName = em.nextElement().toString();
                if (sParName.replace('@', ' ').trim().equalsIgnoreCase(objName.replace('@', ' ').trim())) {
                    oReportParameter.getCurrentValues().clear();
                    currentValue = oReportParameter.getCurrentValues().addSingleValue();
                    currentValue.setValue(getFormattedValue(prop.getProperty(objName), oReportParameter.getValueType()));
                    break;
                }
            }
        }
    }

    private DataBufferResultSet getParamsbyId(DataBufferResultSet rs, int id, List paramList) throws SQLException {
        for (Object aParamList : paramList) {
            IReportParameter oReportParameter = (IReportParameter) aParamList;

            rs.addRecord();
            rs.updateInt(cIdElem, id);
            rs.updateString(cNameElem, oReportParameter.getParameterName());
            rs.updateString(cTypeElem, getValueType(oReportParameter.getValueType()));
            rs.updateString(cValueElem, oReportParameter.getCurrentValues().isEmpty() ? "" :
                    getFormattedValue(((IReportParameterSingleValue) oReportParameter.getCurrentValues().get(0)).getValue(),
                            oReportParameter.getValueType()));

        }
        rs.beforeFirst();
        return rs;

    }

    private DataBufferResultSet getParamsbyId(DataBufferResultSet rs, int id, IProperty paramProp) throws SQLException {
        String pstr = ((IProperties) paramProp.getValue()).getProperty("SI_NUM_PROMPTS").getValue().toString();
        for (int i = 0; i < Integer.parseInt(pstr); i++) {
            IProperties iPrompt = ((IProperties) ((IProperties) paramProp.getValue()).getProperty("SI_PROMPT" + (i + 1)).getValue());
            String s_name = iPrompt.getProperty("SI_NAME").getValue().toString();
            String s_type = iPrompt.getProperty("SI_PROMPT_TYPE").getValue().toString();
            int i_type = Integer.parseInt(s_type);
            String s_value = ((IProperties) ((IProperties) iPrompt.getProperty("SI_CURRENT_VALUES").getValue()).getProperty("SI_VALUE1").getValue()).getProperty("SI_DATA").getValue().toString();

            rs.addRecord();
            rs.updateInt(cIdElem, id);
            rs.updateString(cNameElem, s_name);
            rs.updateString(cTypeElem, getValueType(i_type));
            rs.updateString(cValueElem, getFormattedValue(s_value, i_type));
        }
        rs.beforeFirst();
        return rs;
    }

    private String getValueType(int type) {
        switch (type) {
            case IReportParameter.ReportVariableValueType.DATE:
                return "date";
            case IReportParameter.ReportVariableValueType.DATE_TIME:
                return "datetime";
            case IReportParameter.ReportVariableValueType.TIME:
                return "time";
            case IReportParameter.ReportVariableValueType.BOOLEAN:
                return "boolean";
            case IReportParameter.ReportVariableValueType.NUMBER:
            case IReportParameter.ReportVariableValueType.CURRENCY:
                return "number";
        }
        return "string";
    }

    private String getFormattedValue(String strValue, int valueType) {
        final boolean isIntFormat = strValue.toLowerCase().contains("date(") || strValue.toLowerCase().contains("time(");
        final String prefix = isIntFormat ? ".*?\\(" : "";
        final String postfix = isIntFormat ? "\\)" : "";
        final String dtComma = "[ .,/\\-:;]";
        final String dtPattern = "(\\d+)" + dtComma + "(\\d+)" + dtComma + "(\\d+)";

        switch (valueType) {
            case IReportParameter.ReportVariableValueType.DATE:
                return strValue.replaceAll(prefix + dtPattern + postfix, isIntFormat ? "$1-$2-$3" : "Date($1,$2,$3)");
            case IReportParameter.ReportVariableValueType.DATE_TIME:
                return strValue.replaceAll(prefix + dtPattern + dtComma + dtPattern + postfix, isIntFormat ? "$1-$2-$3 $4:$5:$6" : "DateTime($1,$2,$3,$4,$5,$6)");
            case IReportParameter.ReportVariableValueType.TIME:
                return strValue.replaceAll(prefix + dtPattern + postfix, isIntFormat ? "$1:$2:$3" : "Time($1,$2,$3)");
            case IReportParameter.ReportVariableValueType.BOOLEAN:
                return strValue.toLowerCase().matches("(1|true|yes)") ? "true" : "false";

        }
        return strValue;
    }

    private String getStatusType(int type) {
        switch (type) {
            case ISchedulingInfo.ScheduleStatus.COMPLETE:
                return "COMPLETE";
            case ISchedulingInfo.ScheduleStatus.FAILURE:
                return "FAILURE";
            case ISchedulingInfo.ScheduleStatus.PAUSED:
                return "PAUSED";
            case ISchedulingInfo.ScheduleStatus.PENDING:
                return "PENDING";
            case ISchedulingInfo.ScheduleStatus.RUNNING:
                return "RUNNING";
        }
        return "UNKNOWN";
    }

    private IInfoObject getInfoObject(int idElement) throws Exception {
        iInfoObjects = infoStore.query("SELECT * FROM CI_INFOOBJECTS WHERE SI_ID = " + idElement);

        if (iInfoObjects.size() > 0) return (IInfoObject) iInfoObjects.get(0);

        throw new CourierException("Incorrect '" + cIdElem + "=" + idElement + "' element item specified");
    }

    private int getReportType(String Type) {
        if (Type.equals("CRYSTAL_REPORT")) return IReportFormatOptions.CeReportFormat.CRYSTAL_REPORT;
        if (Type.equals("EXCEL")) return IReportFormatOptions.CeReportFormat.EXCEL;
        if (Type.equals("EXCEL_DATA_ONLY")) return IReportFormatOptions.CeReportFormat.EXCEL_DATA_ONLY;
        if (Type.equals("PDF")) return IReportFormatOptions.CeReportFormat.PDF;
        if (Type.equals("RTF")) return IReportFormatOptions.CeReportFormat.RTF;
        if (Type.equals("RTF_EDITABLE")) return IReportFormatOptions.CeReportFormat.RTF_EDITABLE;
        if (Type.equals("TEXT_CHARACTER_SEPARATED")) return IReportFormatOptions.CeReportFormat.TEXT_CHARACTER_SEPARATED;
        if (Type.equals("TEXT_PAGINATED")) return IReportFormatOptions.CeReportFormat.TEXT_PAGINATED;
        if (Type.equals("TEXT_PLAIN")) return IReportFormatOptions.CeReportFormat.TEXT_PLAIN;
        if (Type.equals("TEXT_TAB_SEPARATED")) return IReportFormatOptions.CeReportFormat.TEXT_TAB_SEPARATED;
        if (Type.equals("TEXT_TAB_SEPARATED_TEXT")) return IReportFormatOptions.CeReportFormat.TEXT_TAB_SEPARATED_TEXT;
        if (Type.equals("USER_DEFINED")) return IReportFormatOptions.CeReportFormat.USER_DEFINED;
        if (Type.equals("WORD")) return IReportFormatOptions.CeReportFormat.WORD;

        return IReportFormatOptions.CeReportFormat.CRYSTAL_REPORT;
    }

}
