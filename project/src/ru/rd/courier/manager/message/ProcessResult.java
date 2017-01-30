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
package ru.rd.courier.manager.message;

/**
 * User: STEPOCHKIN
 * Date: 29.07.2008
 * Time: 18:41:54
 */
public class ProcessResult {
    private long m_id = 0;
    private String m_pipe = null;
    private String m_sourceDbName = null;
    private String m_sourceDbType = null;
    private String m_sourceDbUrl = null;
    private String m_targetDbName = null;
    private String m_targetDbType = null;
    private String m_targetDbUrl = null;
    private long m_startTime = 0;
    private long m_totalTime = 0;
    private long m_sourceTime = 0;
    private long m_targetTime = 0;
    private int m_recordCount = 0;
    private int m_errorCount = 0;
    private String m_error = null;
    private String m_errorStack = null;

    public final long getId() {
        return m_id;
    }

    public final void setId(long value) {
        m_id = value;
    }

    public final String getPipe() {
        return m_pipe;
    }

    public final void setPipe(String pipe) {
        m_pipe = pipe;
    }

    public final String getSourceDbName() {
        return m_sourceDbName;
    }

    public final void setSourceDbName(String value) {
        m_sourceDbName = value;
    }

    public final String getSourceDbType() {
        return m_sourceDbType;
    }

    public final void setSourceDbType(String value) {
        m_sourceDbType = value;
    }

    public final String getSourceDbUrl() {
        return m_sourceDbUrl;
    }

    public final void setSourceDbUrl(String value) {
        m_sourceDbUrl = value;
    }

    public final String getTargetDbName() {
        return m_targetDbName;
    }

    public final void setTargetDbName(String value) {
        m_targetDbName = value;
    }

    public final String getTargetDbType() {
        return m_targetDbType;
    }

    public final void setTargetDbType(String value) {
        m_targetDbType = value;
    }

    public final String getTargetDbUrl() {
        return m_targetDbUrl;
    }

    public final void setTargetDbUrl(String value) {
        m_targetDbUrl = value;
    }

    public final long getStartTime() {
        return m_startTime;
    }

    public final void setStartTime(long value) {
        m_startTime = value;
    }

    public final long getTotalTime() {
        return m_totalTime;
    }

    public final void setTotalTime(long value) {
        m_totalTime = value;
    }

    public final long getSourceTime() {
        return m_sourceTime;
    }

    public final void setSourceTime(long sourceTime) {
        m_sourceTime = sourceTime;
    }

    public final long getTargetTime() {
        return m_targetTime;
    }

    public final void setTargetTime(long targetTime) {
        m_targetTime = targetTime;
    }

    public final int getRecordCount() {
        return m_recordCount;
    }

    public final void setRecordCount(int recordCount) {
        m_recordCount = recordCount;
    }

    public final int getErrorCount() {
        return m_errorCount;
    }

    public final void setErrorCount(int errorCount) {
        m_errorCount = errorCount;
    }

    public String getError() {
        return m_error;
    }

    public void setError(String value) {
        m_error = value;
    }

    public String getErrorStack() {
        return m_errorStack;
    }

    public void setErrorStack(String errorStack) {
        this.m_errorStack = errorStack;
    }

    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ProcessResult)) return false;
        ProcessResult pr = (ProcessResult)obj;
        return (
            m_id == pr.m_id
            /*
            &&
            SysUtils.objEquals(m_pipe, pr.m_pipe) &&
            SysUtils.objEquals(m_sourceDbName, pr.m_sourceDbName) &&
            SysUtils.objEquals(m_sourceDbType, pr.m_sourceDbType) &&
            SysUtils.objEquals(m_sourceDbType, pr.m_sourceDbUrl) &&
            SysUtils.objEquals(m_targetDbName, pr.m_targetDbName) &&
            SysUtils.objEquals(m_targetDbType, pr.m_targetDbType) &&
            SysUtils.objEquals(m_targetDbType, pr.m_targetDbUrl) &&
            m_recordCount == pr.m_recordCount &&
            m_errorCount == pr.m_errorCount &&
            SysUtils.objEquals(m_error, pr.m_error) &&
            m_startTime == pr.m_startTime &&
            m_totalTime == pr.m_totalTime &&
            m_sourceTime == pr.m_sourceTime &&
            m_targetTime == pr.m_targetTime
            */
        );
    }

    public String toString() {
        return "ID = " + m_id + " Pipe = " + m_pipe
            + " SourceDb = " + m_sourceDbName
            + " SourceDbType = " + m_sourceDbType
            + " SourceDbUrl = " + m_sourceDbUrl
            + " TargetDb = " + m_sourceDbName
            + " TargetDbType = " + m_sourceDbType
            + " TargetDbUrl = " + m_sourceDbUrl
            + " StartTime = " + m_startTime
            + " TotalTime = " + m_totalTime
            + " SourceTime = " + m_sourceTime
            + " TargetTime = " + m_targetTime
            + " RecordCount = " + m_recordCount
            + " ErrorCount = " + m_errorCount
            + " Error = " + m_error
            + " ErrorStack = " + m_errorStack;
    }
}
