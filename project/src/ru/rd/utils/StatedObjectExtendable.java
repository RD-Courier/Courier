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
 * Date: 14.10.2008
 * Time: 16:45:13
 */
public class StatedObjectExtendable {
    protected CourierLogger m_logger;
    protected State m_state;
    public final Object lock = new Object();
    private boolean m_toLogState;

    private static final State c_stateCorrupted = new State("CORRUPTED");
    protected static final State c_stateCreated = new State("CREATED");
    protected static final State c_stateStarting = new State("STARTING");
    protected static final State c_stateStarted = new State("STARTED");
    protected static final State c_stateStopping = new State("STOPPING");
    protected static final State c_stateStopped = new State("STOPPED");
    protected static final State c_stateClosed = new State("CLOSED");

    public StatedObjectExtendable(CourierLogger logger, boolean toLogState) {
        m_logger = logger;
        m_toLogState = toLogState;
        m_state = c_stateCreated;
    }

    public StatedObjectExtendable(CourierLogger logger) {
        this(logger, true);
    }

    protected State getState() {
        synchronized(lock) {
            return m_state;
        }
    }

    public final void setToLogState(boolean value) {
        m_toLogState = value;
    }

    protected void setState(State state, boolean signal) {
        synchronized(lock) {
            if (m_state == state) return;
            if (m_toLogState) {
                debug(" --> <" + state + ">");
            }
            m_state = state;
            if (signal) lock.notifyAll();
        }
    }

    protected void setState(State state) {
        setState(state, true);
    }

    protected void ensureState(State expectedState, String error) {
        State state = getState();
        if (state == c_stateCorrupted) {
            throw new RuntimeException("Object is corrupted");
        }
        if (state != expectedState) {
            //if (stateIfError != null) setState(stateIfError);
            StringBuffer mes = new StringBuffer();
            if (error != null) {
                mes.append(error);
                mes.append(": ");
            }
            mes.append("Invalid state: expected '");
            mes.append(expectedState);
            mes.append("' actual '");
            mes.append(state);
            mes.append("'");
            throw new RuntimeException(mes.toString());
        }
    }

    protected void ensureState(State expectedState) {
        ensureState(expectedState, null);
    }

    protected void setCorrupted() {
        setState(c_stateCorrupted);
    }

    protected void ensureNotCorrupted() {
        if (getState() == c_stateCorrupted) {
            throw new RuntimeException("Object is corrupted");
        }
    }

    public String getDesc() {
        return null;
    }

    protected final String getLogDesc() {
        String desc = getDesc();
        StringBuffer sb = new StringBuffer();
        if (desc != null) {
            sb.append(desc).append(' ');
        }
        sb.append('<').append(m_state).append(">");
        return sb.toString();
    }

    protected void debug(String mes) {
        if (m_logger == null) return;
        m_logger.debug(getLogDesc() + ": " + mes);
    }

    protected void info(String mes) {
        if (m_logger == null) return;
        m_logger.info(getLogDesc() + ": " + mes);
    }

    protected boolean waitState(State state, long timeout) {
        synchronized(lock) {
            return syncWaitState(state, timeout);
        }
    }

    protected boolean waitStates(State[] states, final long timeout) {
        synchronized(lock) {
            return syncWaitStates(states, timeout);
        }
    }

    protected void debugWaitState(
        String message, State curState, State state, long timeLeft
    ) {
        if (m_logger == null) return;
        StringBuffer sb = new StringBuffer();
        sb.append(getLogDesc()).append(": waitState ").append(message).append(":");
        if (timeLeft >= 0) sb.append(" timeLeft = ").append(timeLeft);
        sb.append(" current ").append(curState);
        sb.append(" waiting ").append(state);
        m_logger.debug(sb.toString());
    }

    protected boolean syncWaitState(State state, final long timeout) {
        //debugWaitState("begin", m_state, state, -1);
        long begTime = System.currentTimeMillis() + timeout;
        try {
            while (true) {
                State curState = getState();
                if (curState == state) return true;
                if (curState == c_stateCorrupted) return false;
                if (timeout > 0) {
                    long timeLeft = begTime - System.currentTimeMillis();
                    if (timeLeft <= 0) return false;
                    //debugWaitState("wait", curState, state, timeLeft);
                    lock.wait(timeLeft);
                } else {
                    //debugWaitState("wait", curState, state, -1);
                    lock.wait();
                }
                //debugWaitState("wake up", curState, state, -1);
            }
        } catch (InterruptedException e) {
            return false;
        }
    }

    @SuppressWarnings({"UNUSED_SYMBOL"})
    private static String arrayToString(Object[] arr) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(arr[i]);
        }
        return sb.toString();
    }

    private static boolean objInArray(Object obj, Object[] objects) {
        for (Object o: objects) if (obj == o) return true;
        return false;
    }

    private boolean syncWaitStates(State[] states, final long timeout) {
        //String statesStr = arrayToString(states);
        //debugWaitState("begin", m_state, state, -1);
        long begTime = System.currentTimeMillis() + timeout;
        try {
            while (true) {
                State curState = getState();
                if (objInArray(curState, states)) return true;
                if (curState == c_stateCorrupted) return false;
                if (timeout > 0) {
                    long timeLeft = begTime - System.currentTimeMillis();
                    if (timeLeft <= 0) return false;
                    //debugWaitState("wait", curState, state, timeLeft);
                    lock.wait(timeLeft);
                } else {
                    //debugWaitState("wait", curState, state, -1);
                    lock.wait();
                }
                //debugWaitState("wake up", curState, state, -1);
            }
        } catch (InterruptedException e) {
            return false;
        }
    }
}
