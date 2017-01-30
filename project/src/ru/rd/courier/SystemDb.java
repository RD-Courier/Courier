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

import ru.rd.courier.utils.Storage;

import java.util.List;

public interface SystemDb {
    boolean isActive();
    void start() throws CourierException;
    void stop() throws CourierException;

    void setStorage(Storage factory);

    void registerPipeline(
        String code, String desc, int status,
        int checkpointInterval, char markType,
        int maxWorkCount
    ) throws CourierException;

    void registerSourceRule(
        String pipeName, String name, String desc, String type
    ) throws CourierException;

    boolean registerProcessRequest(TransferRequest request) throws CourierException;

    void processProgress(
        Integer dbId, int recordCount,
        int errorCount, boolean movedSinceLastCall
    ) throws CourierException;

    void processFinished(TransferProcess process) throws CourierException;
    void clearPipeVars(String pipeName);
    void addWarnings(List<ProcessWarnings> warnings) throws CourierException;
    void checkWaitingProcess();
}
