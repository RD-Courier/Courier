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
package ru.rd.utils;

import ru.rd.courier.logging.CourierLogger;

/**
 * User: AStepochkin
 * Date: 17.08.2006
 * Time: 10:48:34
 */
public class StatedObject extends StatedObjectExtendable {
    public StatedObject(CourierLogger logger, boolean toLogState) {
        super(logger, toLogState);
    }

    public StatedObject(CourierLogger logger) {
        super(logger);
    }

    public final State getState() {
        return super.getState();
    }

    public final void setState(State state, boolean signal) {
        super.setState(state, signal);
    }

    public final void setState(State state) {
        super.setState(state);
    }

    public final void ensureState(State expectedState, String error) {
        super.ensureState(expectedState, error);
    }

    public final void ensureState(State expectedState) {
        super.ensureState(expectedState);
    }

    public final void setCorrupted() {
        super.setCorrupted();
    }

    public final void ensureNotCorrupted() {
        super.ensureNotCorrupted();
    }

    public final boolean waitState(State state, long timeout) {
        return super.waitState(state, timeout);
    }

    public final boolean waitStates(State[] states, final long timeout) {
        return super.waitStates(states, timeout);
    }    
}
