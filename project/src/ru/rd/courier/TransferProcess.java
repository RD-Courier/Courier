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
package ru.rd.courier;

import ru.rd.courier.logging.data.DataLogger;
import ru.rd.pool.PoolObjectFactory;

import java.util.Date;

public interface TransferProcess {
    int REQUESTED_STATE = 0;
    int WORKING_STATE = REQUESTED_STATE + 1;
    int COMPLETED_STATE = WORKING_STATE + 1;
    int INTERRUPTED_STATE = COMPLETED_STATE + 1;

    int BREAK_DEFAULT = 0;
    int BREAK_PROCESS = 1;

    String c_intervalTypeVarName = "$IntervalType";
    String c_intervalValueVarName = "$IntervalValue";
    String c_intervalValueCacheVarName = "$IntervalValueCache";
    String c_intervalColVarName = "$IntervalColumn";
    String c_intervalStepVarName = "$Step";
    String c_recordCountVarName = "$RecordCount";
    String c_processId = "$Id";
    String c_RuleValueVarName = "$RuleValue";
    String c_PipeValueVarName = "$PipeValue";
    String c_PipeNameVarName = "$pipeline";
    String c_SourceProfileVarName = "$source-profile";
    String c_SourceRuleVarName = "$source-rule";
    String c_TargetProfileVarName = "$target-profile";
    String c_FailCountObjectName = "fail-count";
    String c_IgnoreErrorNumberObjectName = "ignore-error-number";
    String c_IgnoreErrorCountObjectName = "ignore-error-count";

    Long c_intMarkDefault = (long)0;
    Date c_dateMarkDefault = new Date(0);

    Integer getId();
    String getTargetProfileName();
    DataLogger getDataLogger();
    int getErrorCount();
    int getRecordCount();
    String getErrorText();
    String getErrorStack();
    long getStartTime();
    long getTotalTime();
    long getTargetTime();
    long getSourceTime();
    String getSourceDbName();
    String getTargetDbName();
    PoolObjectFactory getPoolFactory(String name);
    String getIntervalValue();

    void  start();
    void stop() throws CourierException;
    void close() throws CourierException;    
    void syncProgress() throws CourierException;
    String getCurrentStateDesc();
    TransferProcessResult getResults();
}
