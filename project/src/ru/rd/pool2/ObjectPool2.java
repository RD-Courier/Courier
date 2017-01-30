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
package ru.rd.pool2;

import ru.rd.courier.logging.CourierLogger;
import ru.rd.pool.ObjectPoolIntf;
import ru.rd.pool.PoolObjectFactory;
import ru.rd.thread.Condition;
import ru.rd.thread.ThreadHelper;
import ru.rd.utils.TimedDisposable;

import java.util.*;
import java.util.concurrent.Executor;
import java.text.MessageFormat;

/**
 * User: AStepochkin
 * Date: 04.08.2008
 * Time: 9:58:37
 */
public abstract class ObjectPool2 implements ObjectPoolIntf, TimedDisposable {
    protected static class ObjectWrapper {
        private Object m_obj;
        private byte m_flags;
        private long m_releaseTime;
        private long m_createTime;

        public ObjectWrapper(Object obj, boolean wasAllocated) {
            super();
            m_obj = obj;
            m_createTime = System.currentTimeMillis();
            m_releaseTime = m_createTime;
            m_flags = 0;
            setWasAllocated(wasAllocated);
            setIsReleased(false);
            setIsCaptured(false);
        }

        public ObjectWrapper(Object obj) {
            this(obj, true);
        }

        private void setFlag(boolean value, byte flag) {
            if (value) {
                m_flags |= flag;
            } else {
                m_flags &= ~flag;
            }
        }

        public Object getObject() {
            return m_obj;
        }

        public long getCreateTime() {
            return m_createTime;
        }

        public long getReleaseTime() {
            return m_releaseTime;
        }

        public void setReleaseTime(long value) {
            m_releaseTime = value;
            setIsReleased(true);
        }

        public boolean getWasAllocated() {
            return (m_flags & pofWasAllocated) > 0;
        }

        public void setWasAllocated(boolean value) {
            setFlag(value, pofWasAllocated);
        }

        public boolean hasFlags(byte flags) {
            return (m_flags & flags) > 0;
        }

        public boolean getIsCaptured() {
            return (m_flags & pofCaptured) > 0;
        }

        public void setIsCaptured(boolean value) {
            setFlag(value, pofCaptured);
        }

        public boolean getIsReleased() {
            return (m_flags & pofReleased) > 0;
        }

        public void setIsReleased(boolean value) {
            setFlag(value, pofReleased);
        }

        public boolean getInvalid() {
            return (m_flags & pofInvalid) > 0;
        }

        public void setInvalid(boolean value) {
            setFlag(value, pofInvalid);
        }

        public void release() {
            setReleaseTime(System.currentTimeMillis());
        }

        private static String boolToString(boolean value) {
            return value ? "true" : "false";
        }

        public String toString() {
            return "Captured = " + boolToString(getIsCaptured());
        }
    }

    protected interface BoolFunc{
        boolean isTrue(ObjectWrapper ow);
    }

    private static class ShrinkFunc implements BoolFunc {
        private long m_curTime;
        private long m_shrinkObjPeriod;

        public boolean isTrue(ObjectWrapper ow) {
            return (
                (m_shrinkObjPeriod > 0)
                && ((m_curTime - ow.getReleaseTime()) > m_shrinkObjPeriod)
            );
        }

        public ShrinkFunc(long shrinkObjPeriod) {
            m_curTime = System.currentTimeMillis();
            m_shrinkObjPeriod = shrinkObjPeriod;
        }
    }

    private interface Cancelable {
        void cancel();
    }

    private interface CancelListener {
        void setCanceller(Cancelable c);
    }

    private abstract class PoolWaitCondition implements Cancelable, Condition {
        private boolean m_cancelled;

        public PoolWaitCondition() {
            m_cancelled = false;
        }

        public void cancel() {
            synchronized(m_lock) {
                m_cancelled = true;
                m_lock.notifyAll();
            }
        }

        public final boolean isCancelled() {
            return m_cancelled;
        }

        public final boolean isTrue() {
            if (m_cancelled) return true;
            return finishWait();
        }

        protected abstract boolean finishWait();
    }

    private class WaitFreeCondition extends PoolWaitCondition {
        private Object m_obj;

        public WaitFreeCondition(Object obj) {
            m_obj = obj;
        }

        protected boolean finishWait() {
            if (m_state != StateStarted) return true;
            m_obj = findValidObject();
            return (m_obj != null);
        }

        public Object getObject() {
            return m_obj;
        }
    }

    private static final byte pofWasAllocated = 1;
    private static final byte pofInvalid = 2;
    private static final byte pofCaptured = 4;
    private static final byte pofReleased = 8;

    private static final byte paExpand = 1;
    private static final byte paRinse = 2;
    private static final byte paCheck = 4;
    private static final byte paShrink = 8;

    private final CourierLogger m_logger;
    private final String m_desc;
    private Executor m_executor;
    private ExecutePolicy m_execPolicy;
    private int m_state;
    protected boolean m_closed;
    protected final Object m_lock;
    private PoolObjectFactory m_objFactory;
    private Collection<PoolListener> m_listeners;
    private boolean m_ownsTimer = false;
    private Timer m_timer;
    private byte m_activities;
    private boolean m_rinseRequested;
    private TimerTask m_rinseTask;
    private int m_waitObjCount;
    private int m_bulkExpand;

    private long m_closeTimeout;
    private int m_initialCapacity;
    private int m_minCapacity;
    private int m_maxCapacity;
    private int m_incCapacity;
    private long m_shrinkInterval;
    private int m_shrinkCapacity;
    private long m_shrinkObjPeriod;
    private TimerTask m_shrinkTask;
    private long m_checkInterval;
    private TimerTask m_checkTask;
    private long m_expirePeriod;
    private long m_rinseInterval;
    private boolean m_selfExpand;
    private long m_recreateInterval;

    //private void logOperation(aLogLevel: TLogLevel; const aOperation, aFunction: string); overload;
    private void logOperation(String operation, String function) {
        m_logger.debug("ObjectPool2 '" + m_desc + "' " + operation + " " + function);
    }

    private void logEnter(String function) {
        logOperation("Entering", function);
    }

    private void logExit(String function) {
        logOperation("Exiting", function);
    }
    //procedure LogInt(const aMessage: string; aInt: integer);

    private class CommonCommand implements Runnable {
        private final byte m_activity;

        public CommonCommand(byte activity) {
            m_activity = activity;
        }

        public void run() {
            try {
                switch (m_activity) {
                    case paCheck: check(); break;
                    case paExpand: expand(); break;
                    case paRinse: rinse(); break;
                    case paShrink: shrink(); break;
                }
            } finally {
                synchronized(m_lock) {
                    m_activities &= ~m_activity;
                    if (m_state == StateClosing) m_lock.notifyAll();
                }
            }
        }
    }

    private void tryToLaunchActivity(byte activity) {
        synchronized(m_lock) {
            if ((m_state != StateClosing && !isOpen_nl()) || (m_activities & activity) != 0) return;
            m_activities |= activity;
        }
        m_executor.execute(new CommonCommand(activity));
    }

    private void requestRinse() {
        synchronized(m_lock) {
            //Logger.Debug('ObjectPool2.requestRinse: RinseRequested = %s', [BoolToString(m_rinseRequested)]);
            if (!m_rinseRequested) {
                m_rinseRequested = true;
                if (m_rinseTask != null) m_rinseTask.cancel();
                m_rinseTask = scheduleTask(paRinse, m_rinseInterval);
            }
            if (m_state != StateClosing && (m_minCapacity > 0 && size_nl() < m_minCapacity)) {
                tryToLaunchActivity(paExpand);
            }
        }
    }

    private boolean reachedMaxCapacity_nl() {
        return (m_maxCapacity > 0) && (size_nl() + m_creatingCount >= m_maxCapacity);
    }

    private boolean reachedMaxCapacity() {
        synchronized(m_lock) {
            return reachedMaxCapacity_nl();
        }
    }

    private void checkMaxCapacity() {
        synchronized(m_lock) {
            if (reachedMaxCapacity_nl()) {
                throw new RuntimeException(
                    "Pool '" + m_desc + "' max capacity " + m_maxCapacity + " has been reached"
                );
            }
        }
    }

    private boolean canAddObject_nl() {
        return isOpen_nl() && (m_maxCapacity <= 0 || size_nl() < m_maxCapacity);
    }

    private boolean needToAddObject(boolean changeBulk) {
        synchronized(m_lock) {
            if (!canAddObject_nl()) return false;
            if (m_minCapacity > 0 && size_nl() < m_minCapacity) return true;
            if (m_bulkExpand > 0) {
                if (changeBulk) m_bulkExpand--;
                return true;
            }
            return m_waitObjCount > 0;
        }
    }

    private void scheduleCreate() {
        if (m_recreateInterval > 0 && needToAddObject(false)) {
            scheduleTask(paExpand, m_recreateInterval);
        }
    }

    private void addNewObjects() {
        if (!isOpen() || !m_selfExpand) return;

        Object threadContext = createThreadContext();
        try {
            while (needToAddObject(true)) {
                ObjectWrapper ow = null;
                try {
                    ow = addNewObject(threadContext);
                } catch (Exception e) {
                    m_logger.error("Error adding new object pool '" + m_desc + "'", e);
                }
                if (ow == null) {
                    scheduleCreate();
                    break;
                }
            }
        } finally {
            closeThreadContext(threadContext);
        }
    }

    private void addNewObjectsAfterRemove(int count) {
        if (count <= 0) return;

        synchronized(m_lock) {
            if (m_state == StateClosing) return;
        }

        addNewObjects();
    }

    private ObjectWrapper addNewObject(Object threadContext) {
        return addNewObjectEx(false, threadContext);
    }

    private int m_creatingCount;

    private ObjectWrapper addNewObjectEx(boolean busyAtOnce, Object threadContext) {
        //logEnter("addNewObject");
        Object o;
        synchronized(m_lock) {
            m_creatingCount++;
        }
        try {
            final boolean contextCreated;
            if (threadContext == null) {
                threadContext = createThreadContext();
                contextCreated = true;
            } else {
                contextCreated = false;
            }
            try {
                o = allocateObject(threadContext, getObjectFactory());
            } finally {
                if (contextCreated) closeThreadContext(threadContext);
            }
        } catch (Exception e) {
            warn("Error while allocating new object", e);
            return null;
        }
        if (o == null) {
            synchronized(m_lock) {
                m_creatingCount--;
            }
            return null;
        }
        ObjectWrapper ow = new ObjectWrapper(o);
        addObjectWrapper(threadContext, ow, busyAtOnce, true);
        return ow;
    }

    private void addObjectWrapper(
        Object threadContext, ObjectWrapper ow, boolean busy, boolean decCreatingCount
    ) {
        try {
            synchronized(m_lock) {
                if (decCreatingCount) m_creatingCount--;
                if (busy) {
                    customAddBusyObject_nl(ow);
                } else {
                    customAddFreeObject_nl(ow);
                    m_lock.notifyAll();
                }
            }
            if (!busy && m_listeners != null) {
                for (PoolListener l: m_listeners) l.objectAdded();
            }
        } catch (Exception e) {
            returnObject(threadContext, ow);
            throw new RuntimeException(e);
        }
    }

    private void returnObject(Object threadContext, ObjectWrapper ow) {
        try {
            final boolean contextCreated;
            if (threadContext == null) {
                threadContext = createThreadContext();
                contextCreated = true;
            } else {
                contextCreated = false;
            }
            try {
                deallocateObject(threadContext, ow.getObject(), getObjectFactory());
            } finally {
                if (contextCreated) closeThreadContext(threadContext);
            }
        } catch (Exception e) {
            m_logger.error("Error returning object from pool '" + m_desc + "'");
        }
    }

    private int doReturnObjects(
        Object threadContext, BoolFunc cond, boolean checkObject, int maxCount
    ) {
        return doReturnObjects(threadContext, getFreeIterator(), cond, checkObject, maxCount);
    }

    private void debug(String message) {
        m_logger.debug("ObjectPool2 '" + m_desc + "': " + message);
    }

    private void warn(String message) {
        m_logger.warning("ObjectPool2 '" + m_desc + "': " + message);
    }

    private void warn(String message, Throwable e) {
        m_logger.warning("ObjectPool2 '" + m_desc + "': " + message, e);
    }

    private int doReturnObjects(
        Object threadContext, Iterator<ObjectWrapper> it, BoolFunc cond, boolean checkObject, int maxCount
    ) {
        int result = 0;
        while (true) {
            ObjectWrapper ow;
            synchronized(m_lock) {
                if (
                    m_closed || !it.hasNext()
                    || (m_state != StateClosing && (
                        (maxCount >= 0 && result >= maxCount) ||
                        (m_minCapacity > 0 && size_nl() <= m_minCapacity)
                    ))
                ) break;
                ow = it.next();
                if (ow == null) {
                    warn("doReturnObjects: Iterator returned null wrapper");
                    continue;
                }
                if (!objectCapture(ow, cond)) continue;
            }

            if (!checkObject || !objectValid(threadContext, ow)) {
                if (!ow.getIsReleased()) {
                    debug("returns object in use. Object: " + ow.getObject());
                }
                synchronized(m_lock) {
                    returnObject(threadContext, ow);
                    it.remove();
                    objectUncapture(ow);
                }
                result++;
            } else {
                objectUncapture(ow);
            }
        }
        return result;
    }

    private int returnObjects(BoolFunc condition, boolean checkObject, int maxCount) {
        synchronized(m_lock) {
            if (m_state != StateClosing && !isOpen_nl()) return 0;
        }

        Object threadContext = createThreadContext();
        int delCount = 0;
        try {
            try {
                delCount = doReturnObjects(threadContext, condition, checkObject, maxCount);
            } catch (Exception e) {
                m_logger.error(e);
            }
        } finally {
            closeThreadContext(threadContext);
        }

        debug(
            "returned " + delCount + " objects: Busy = " + busySize() + " Free = " + freeSize()
        );

        return delCount;
    }

    private void rinse() {
        Object threadContext = createThreadContext();
        try {
            ObjectWrapper ow;
            Iterator<ObjectWrapper> it;
            synchronized(m_lock) {
                it = customGetInvalidIterator_nl();
            }
            while (true) {
                synchronized(m_lock) {
                    if (!it.hasNext()) break;
                    ow = it.next();
                    it.remove();
                }
                returnObject(threadContext, ow);
            }
        } finally {
            closeThreadContext(threadContext);
        }
    }

    private static final BoolFunc s_validFunc = new BoolFunc() {
        public boolean isTrue(ObjectWrapper ow) {
            return !ow.getInvalid();
        }
    };

    @SuppressWarnings({"UNUSED_SYMBOL"})
    private void plog(String m) {
        //System.out.println(
        //    System.currentTimeMillis() + " - " + Thread.currentThread().getId() + " - " + m
        //);
    }

    private Object findValidObject() {
        Object result = null;
        boolean needRinse = false;
        while (true) {
            ObjectWrapper ow = null;
            Iterator<ObjectWrapper> it;
            synchronized(m_lock) {
                if (m_closed) break;
                it = getFreeIterator();
                while (it.hasNext()) {
                    ObjectWrapper cow = it.next();
                    if (objectCapture(cow, s_validFunc)) {
                        ow = cow;
                        break;
                    }
                }
            }

            if (ow == null) break;
            if (objectValid(null, ow)) {
                synchronized(m_lock) {
                    customObjectBusy_nl(ow, it);
                    objectUncapture(ow);
                }
                result = ow.getObject();
                break;
            } else {
                synchronized(m_lock) {
                    customObjectInvalid_nl(ow, it);
                    objectUncapture(ow);
                }
                needRinse = true;
            }
        }
        if (needRinse) requestRinse();
        return result;
    }

    @SuppressWarnings({"UNUSED_SYMBOL"})
    private Object findValidObject2() {
        Object result = null;
        boolean needRinse = false;
        Iterator<ObjectWrapper> it;
        synchronized(m_lock) {
            it = getFreeIterator();
            while (it.hasNext()) {
                if (m_closed) break;
                ObjectWrapper ow = it.next();
                if (!ow.getInvalid() && (!ow.getIsCaptured())) {
                    if (objectValid(null, ow)) {
                        customObjectBusy_nl(ow, it);
                        result = ow.getObject();
                        break;
                    } else {
                        customObjectInvalid_nl(ow, it);
                        needRinse = true;
                    }
                }
            }
        }

        if (needRinse) requestRinse();
        return result;
    }


    public final Object getObject(
        long timeout, boolean useTimeout, CancelListener cancelListener
    ) {
        return getObject(timeout, useTimeout, cancelListener, false);
    }

    private void debugGet(String num) {
        m_logger.debug("ObjectPool2 '" + m_desc + "' getObject " + num + ": size = " + size_nl() + " max = " + m_maxCapacity);
    }

    public final Object getObject(
        long timeout, boolean useTimeout, CancelListener cancelListener, boolean peekMode
    ) {
        //debugGet("10");

        Object result = findValidObject();
        if (result != null) return result;

        //debugGet("20");

        if (m_selfExpand && !reachedMaxCapacity()) {
            //debugGet("30");
            boolean launchExpand;
            synchronized(m_lock) {
              if (m_incCapacity > 1 && m_bulkExpand == 0) {
                  m_bulkExpand = m_incCapacity - 1;
                  if (m_minCapacity > 0 && m_minCapacity < size_nl()) {
                      m_bulkExpand -= size_nl() - m_minCapacity;
                      if (m_bulkExpand < 0) m_bulkExpand = 0;
                  }
              }
              launchExpand = m_bulkExpand > 0;
            }
            if (launchExpand) tryToLaunchActivity(paExpand);
            ObjectWrapper ow = addNewObjectEx(true, null);
            if (ow != null) return ow.getObject();
            if (!useTimeout) {
                if (peekMode) return null;
                throw new RuntimeException(
                    "Object pool '" + m_desc + "' failed to allocate object");
            }
        }
        WaitFreeCondition condObj = new WaitFreeCondition(this);
        if (cancelListener != null) cancelListener.setCanceller(condObj);
        try {
            synchronized(m_lock) {
                m_waitObjCount++;
                try {
                    if (!ThreadHelper.waitEvent(m_lock, condObj, timeout)) {
                        if (peekMode) return null;
                        throw new RuntimeException(
                          "ObjectPool2 '" + m_desc + "': Get object timeout (" + timeout + ") expired");
                    }
                } finally {
                    m_waitObjCount--;
                }
                if(m_state != StateStarted) releaseObject(condObj.getObject());
            }
        } finally {
            if (cancelListener != null) cancelListener.setCanceller(null);
        }
        result = condObj.getObject();
        if (result == null && !peekMode) {
            if (condObj.isCancelled()) {
                throw new RuntimeException("Request cancelled");
            }
            throw new RuntimeException("ObjectPool2 '" + m_desc + "' failed to allocate object");
        }
        return result;
    }

    public final Object peekObject(
        long timeout, boolean useTimeout, CancelListener cancelListener
    ) {
        return getObject(timeout, useTimeout, cancelListener, true);
    }

    public final Object peekObject(long timeout) {
        return getObject(timeout, true, null, true);
    }

    public final Object peekObject() {
        return getObject(0, false, null, true);
    }

    void check() {
        logEnter("check");
        if (!isOpen()) return;
        addNewObjectsAfterRemove(returnObjects(null, true, -1));
        logExit("check");
    }

    private void expand() {
        addNewObjects();
    }

    void shrink() {
        logEnter("shrink");
        if (!isOpen()) return;
        returnObjects(new ShrinkFunc(m_shrinkObjPeriod), false, m_shrinkCapacity);
        logExit("shrink");
    }

    private void ensureTimer() {
        if (m_timer == null) {
            m_timer = new Timer("ObjectPool2-" + getDesc() + "-Timer");
            m_ownsTimer = true;
        }
    }

    private class ActivityTask extends TimerTask {
        private byte m_tactivity;

        public void run() {
            try {
                //m_logger.debug('ActivityTask.run: Activity = %d', [integer(fActivity)]);
                tryToLaunchActivity(m_tactivity);
            } catch (Exception e) {
                m_logger.error("Activity " + m_tactivity + " error for '" + m_desc + "'", e);
            }
        }

        public ActivityTask(byte activity) {
            m_tactivity = activity;
        }
    }

    protected TimerTask scheduleTask(byte activity, long delay, long period) {
        m_logger.debug("ObjectPool2.scheduleTask: Activity = " + activity + " period = " + period);

        ensureTimer();
        TimerTask tt = new ActivityTask(activity);
        m_timer.schedule(tt, delay, period);
        return tt;
    }

    protected TimerTask scheduleTask(byte activity, long delay) {
        m_logger.debug("ObjectPool2.scheduleTask: Activity = " + activity);

        ensureTimer();
        TimerTask tt = new ActivityTask(activity);
        m_timer.schedule(tt, delay);
        return tt;
    }

    private static final byte StateCorrupted = 0;
    private static final byte StateInitial = StateCorrupted + 1;
    private static final byte StateStarted = StateInitial + 1;
    private static final byte StateClosing = StateStarted + 1;
    private static final byte StateClosed = StateClosing + 1;

    protected boolean isOpen_nl() {
        if (m_closed) return false;
        return (m_state == StateStarted);
    }

    protected boolean isOpen() {
        synchronized(m_lock) {
            return isOpen_nl();
        }
    }

    protected interface PoolIterator {
        ObjectWrapper next();
    }

    protected abstract void customStart_nl();
    protected abstract void customAddFreeObject_nl(ObjectWrapper ow);
    protected abstract void customAddBusyObject_nl(ObjectWrapper ow);
    protected abstract void customObjectBusy_nl(ObjectWrapper ow, Iterator<ObjectWrapper> freeIterator);
    protected abstract void customObjectInvalid_nl(ObjectWrapper ow, Iterator<ObjectWrapper> freeIterator);
    protected abstract ObjectWrapper customFindReleaseObject_nl(Object o);
    protected abstract ObjectWrapper customReleaseObjectToRemove_nl(Object o);
    protected abstract Iterator<ObjectWrapper> customGetFreeIterator_nl();
    protected abstract Iterator<ObjectWrapper> customGetBusyIterator_nl();
    protected abstract Iterator<ObjectWrapper> customGetInvalidIterator_nl();
    protected abstract int customFreeSize();
    protected abstract int customBusySize();

    private int size_nl() {
        return customFreeSize() + customBusySize();
    }

    private Iterator<ObjectWrapper> getFreeIterator() {
        synchronized(m_lock) {
            return customGetFreeIterator_nl();
        }
    }

    private Object createThreadContext() {
        return m_execPolicy.createThreadContext();
    }

    private void closeThreadContext(Object threadContext) {
        m_execPolicy.closeThreadContext(threadContext);
    }

    private Object allocateObject(Object threadContext, PoolObjectFactory factory) throws Exception {
        return m_execPolicy.allocateObject(this, threadContext, factory);
    }

    private void deallocateObject(Object threadContext, Object o, PoolObjectFactory factory) {
        m_execPolicy.deallocateObject(threadContext, o, factory);
    }

    private boolean objectWrapperValid(Object bulkContext, Object o, PoolObjectFactory factory) {
        return m_execPolicy.objectWrapperValid(bulkContext, o, factory);
    }

    protected boolean objectExpired(ObjectWrapper ow) {
        return
          (m_expirePeriod > 0)
          && ((System.currentTimeMillis() - ow.getCreateTime()) > m_expirePeriod);
    }

    protected boolean objectValidHelper(Object threadContext, ObjectWrapper ow) {
        if (objectExpired(ow) || ow.getInvalid()) return false;

        Object o = ow.getObject();

        boolean result = false;
        final boolean contextCreated;
        if (threadContext == null) {
            threadContext = createThreadContext();
            contextCreated = true;
        } else {
            contextCreated = false;
        }
        try {
            result = objectWrapperValid(threadContext, o, getObjectFactory());
        } finally {
            if (contextCreated) closeThreadContext(threadContext);
        }

        //if (!result) {
        //    m_logger.warning("Pool '" + m_desc + "' object (" + o + ") failed checking");
        //}
        return result;
    }

    protected boolean objectValid(Object threadContext, ObjectWrapper ow) {
        boolean result = false;
        try {
            result = objectValidHelper(threadContext, ow);
        } catch (Exception e) {
            Object o = ow.getObject();
            String mes = "Pool '" + m_desc + "' : error while checking object";
            if (o != null) {
                mes += " " + o.getClass().getName() + ": " + o;
            }
            m_logger.warning(mes, e);
        }
        ow.setInvalid(!result);
        return result;
    }

    protected boolean objectCapture(ObjectWrapper ow, BoolFunc condition) {
        synchronized(m_lock) {
            if ((condition == null || condition.isTrue(ow)) && (!ow.getIsCaptured())) {
                ow.setIsCaptured(true);
                return true;
            } else {
                return false;
            }
        }
    }

    protected void objectUncapture(ObjectWrapper ow) {
        synchronized(m_lock) {
            ow.setIsCaptured(false);
            if (m_state == StateClosing) {
                m_lock.notifyAll();
            }
        }
    }

    private void ensureState(int aActualState, int aExpectedState) {
        if (aActualState == StateCorrupted) {
            throw new RuntimeException("Object is corrupted");
        }
        if (aActualState != aExpectedState) {
            throw new RuntimeException(
                "ObjectPool2 '" + m_desc + "' state: expected '" + aExpectedState + "' actual '" + aActualState + "'");
        }
    }

    private void prohibitState(
        int actualState, int prohibitedState, String message, Object ... arguments
    ) {
      if (actualState == StateCorrupted) {
          throw new RuntimeException("ObjectPool2 '" + m_desc + "' is corrupted");
      }
      if (actualState == prohibitedState) {
          StringBuffer buf = new StringBuffer("ObjectPool2 '" + m_desc + "' prohibited state: '" + prohibitedState + "'");
          String mes = MessageFormat.format(message, arguments);
          if (mes.length() > 0) buf.append(": ").append(mes);
          throw new RuntimeException(buf.toString());
      }
    }

    private void prohibitState(int actualState, int prohibitedState, String message) {
      if (actualState == StateCorrupted) {
          throw new RuntimeException("ObjectPool2 '" + m_desc + "' is corrupted");
      }
      if (actualState == prohibitedState) {
          StringBuffer buf = new StringBuffer("ObjectPool2 '" + m_desc + "' prohibited state: '" + prohibitedState + "'");
          if (message.length() > 0) buf.append(": ").append(message);
          throw new RuntimeException(buf.toString());
      }
    }

    private void prohibitState(int actualState, int prohibitedState) {
      if (actualState == StateCorrupted) {
          throw new RuntimeException("ObjectPool2 '" + m_desc + "' is corrupted");
      }
      if (actualState == prohibitedState) {
          throw new RuntimeException("ObjectPool2 '" + m_desc + "' prohibited state: '" + prohibitedState + "'");
      }
    }

    protected final void ensureCommon(int state) {
        ensureState(m_state, state);
    }

    protected final void ensureOpen() {
        ensureCommon(StateStarted);
    }

    protected final void ensureInitial() {
        ensureCommon(StateInitial);
    }

    protected final void prohibitCommon(byte state) {
        prohibitState(m_state, state);
    }

    protected final void prohibitClosed() {
        prohibitCommon(StateClosed);
    }

    protected final void prohibitStarted() {
        prohibitCommon(StateStarted);
    }

    public ObjectPool2(
        CourierLogger logger, String desc, PoolObjectFactory objFactory
    ) {
        super();

        m_logger = logger;
        m_closed = false;
        m_state = StateInitial;
        m_desc = desc;
        m_executor = null;
        m_execPolicy = null;
        m_objFactory = objFactory;
        m_listeners = new java.util.LinkedList<PoolListener>();
        m_lock = new Object();
        m_closeTimeout = 0;
        m_rinseInterval = 5000;
        m_selfExpand = true;
        setCapacityPars(0, 1, 0, 0);
        setShrinkPars(0, -1, 0);
        setCheckPars(0);
        setExpirePars(0);
        setRecreateInterval(0);
    }

    public void setExecPolicy(ExecutePolicy execPolicy) {
        m_execPolicy = execPolicy;
    }

    public void setRecreateInterval(long recreateInterval) {
        m_recreateInterval = recreateInterval;
    }

    public PoolObjectFactory getObjectFactory() {
        return m_objFactory;
    }

    public void setExecutor(Executor executor) {
        m_executor = executor;
    }

    public void setTimer(Timer timer) {
        Timer oldTimer;
        boolean oldOwnsTimer;
        synchronized(m_lock) {
            oldTimer = m_timer;
            oldOwnsTimer = m_ownsTimer;
            m_timer = timer;
            m_ownsTimer = false;
        }
        if (oldTimer != null && oldOwnsTimer) {
            try {
                oldTimer.cancel();
            } catch (Exception e) {
                m_logger.warning("Error cancelling timer for pool '" + m_desc + "'", e);
            }
        }
    }

    public void addListener(PoolListener listener) {
        m_listeners.add(listener);
    }

    public void removeListener(PoolListener listener) {
        m_listeners.remove(listener);
    }

    public void setInitialCapacity(int value) {
        m_initialCapacity = value;
    }

    public int getIncCapacity() {
        return m_incCapacity;
    }

    public void setIncCapacity(int value) {
        m_incCapacity = value;
    }

    public void setMinCapacity(int value) {
        m_minCapacity = value;
    }

    public void setMaxCapacity(int value) {
        m_maxCapacity = value;
    }

    public void setCheckInterval(long value) {
        m_checkInterval = value;
    }

    public void setExpirePeriod(long value) {
        m_expirePeriod = value;
    }

    public void setShrinkInterval(long value) {
        m_shrinkInterval = value;
    }

    public void setShrinkCapacity(int value) {
        m_shrinkCapacity = value;
    }

    public void setShrinkObjPeriod(long value) {
        m_shrinkObjPeriod = value;
    }

    public void setCloseTimeout(long value) {
        m_closeTimeout = value;
    }

    public void setSelfExpand(boolean value) {
        ensureInitial();
        m_selfExpand = value;
    }

    public void setRinseInterval(long value) {
        ensureInitial();
        m_rinseInterval = value;
    }

    public void setCapacityPars(
        int initialCapacity, int incCapacity, int minCapacity, int maxCapacity
    ) {
        setInitialCapacity(initialCapacity);
        setIncCapacity(incCapacity);
        setMinCapacity(minCapacity);
        setMaxCapacity(maxCapacity);
    }

    public void setCheckPars(long checkInterval) {
        setCheckInterval(checkInterval);
    }

    public void setExpirePars(long expirePeriod) {
        setExpirePeriod(expirePeriod);
    }

    public void setShrinkPars(
      long shrinkInterval, int shrinkCapacity, long shrinkObjPeriod
    ) {
        setShrinkInterval(shrinkInterval);
        setShrinkCapacity(shrinkCapacity);
        setShrinkObjPeriod(shrinkObjPeriod);
    }

    public void addObject(Object o) {
        checkMaxCapacity();
        addObjectWrapper(null, new ObjectWrapper(o, false), false, false);
    }

    public Object getObject() {
        return getObject(0, false, null);
    }

    public Object getObject(long timeout) {
        return getObject(timeout, true, null);
    }

    public Object getObject(long timeout, CancelListener cancelListener) {
        return getObject(timeout, true, cancelListener);
    }

    public void releaseAndRemoveObject(Object o) {
        synchronized(m_lock) {
            prohibitClosed();
            ObjectWrapper ow = customReleaseObjectToRemove_nl(o);
            if (ow != null) {
                if (m_state == StateClosing) m_lock.notifyAll();
                else requestRinse();
            }
        }
    }

    public void releaseObject(Object o) {
        boolean wasReleased = false;
        synchronized(m_lock) {
            prohibitClosed();
            ObjectWrapper ow = customFindReleaseObject_nl(o);
            if (ow != null) {
                ow.release();
                m_lock.notifyAll();
                wasReleased = true;
            }
        }
        if (!wasReleased) {
            m_logger.warning("ObjectPool2 '" + m_desc + "' no release for object: " + o);
        }
    }

    public void start() {
        synchronized(m_lock) {
            try {
                customStart_nl();
                m_state = StateStarted;

                if (m_executor == null) {
                    m_executor = new Executor() {
                        public void execute(Runnable command) {
                            command.run();
                        }
                    };
                }

                if (m_execPolicy == null) {
                    m_execPolicy = new SynchExecPolicy();
                }

                if (m_initialCapacity > 0) {
                    m_bulkExpand = m_initialCapacity;
                    if (m_minCapacity > 0) m_bulkExpand -= m_minCapacity;
                    m_bulkExpand = Math.max(m_bulkExpand, 0);
                    if (m_maxCapacity > 0 && m_bulkExpand > m_maxCapacity) {
                        m_bulkExpand = m_maxCapacity;
                    }
                }
                if (m_bulkExpand > 0 || m_minCapacity > 0) tryToLaunchActivity(paExpand);

                if (m_shrinkInterval > 0) {
                    m_shrinkTask = scheduleTask(paShrink, m_shrinkInterval, m_shrinkInterval);
                }

                if (m_checkInterval > 0) {
                    m_checkTask = scheduleTask(paCheck, m_checkInterval, m_checkInterval);
                }

                m_rinseRequested = false;
                m_rinseTask = null;
            } catch (Exception e) {
                try {
                    close();
                } catch (Exception e1) {
                    m_logger.error("Error closing pool '" + m_desc + "'", e1);
                }
                throw new RuntimeException(e);
            }
        }
    }

    public boolean isStarted() {
        synchronized(m_lock) {
            if (m_closed) return false;
            return (m_state != StateCorrupted && m_state != StateClosed);
        }
    }

    private class CloseCondition extends PoolWaitCondition {
        protected boolean finishWait() {
            synchronized(m_lock) {
                //m_logger.info("m_activities = " + m_activities + " bsize = " + customBusySize());
                return m_activities == 0 && customBusySize() == 0;
            }
        }
    }

    public void close() {
        close(m_closeTimeout);
    }

    public boolean dispose(long timeout) {
        return close(timeout);
    }

    public boolean close(long timeout) {
        synchronized(m_lock) {
            if (m_state != StateStarted) return true;
            m_state = StateClosing;

            if (m_checkTask != null) {
                try {
                    m_checkTask.cancel();
                } catch (Exception e) {
                    m_logger.warning("ObjectPool2 '" + m_desc + "': Error cancelling check task");
                }
            }

            if (m_shrinkTask != null) {
                try {
                    m_shrinkTask.cancel();
                } catch (Exception e) {
                    m_logger.warning("ObjectPool2 '" + m_desc + "': Error cancelling shrink task");
                }
            }

            if (m_rinseTask != null) m_rinseTask.cancel();
            m_rinseRequested = false;

            setTimer(null);

            boolean stopOK = ThreadHelper.waitEvent(m_lock, new CloseCondition(), timeout);
            returnObjects(null, false, -1);
            doReturnObjects(null, customGetBusyIterator_nl(), null, false, -1);
            rinse();

            //SafeClose(m_objFactory);

            m_state = StateClosed;
            m_closed = true;
            return stopOK;
        }
    }

    public String getDesc() {
        return m_desc;
    }

    public int size() {
        synchronized(m_lock) {
            return size_nl();
        }
    }

    public int freeSize() {
        synchronized(m_lock) {
            return customFreeSize();
        }
    }

    public int busySize() {
        synchronized(m_lock) {
            return customBusySize();
        }
    }
}
