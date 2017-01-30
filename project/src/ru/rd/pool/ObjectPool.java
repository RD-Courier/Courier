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
package ru.rd.pool;

import ru.rd.courier.logging.CourierLogger;
import ru.rd.utils.Disposable;

import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public abstract class ObjectPool implements ObjectPoolIntf {
    protected String m_desc;
    private boolean m_closed = true;
    protected PoolObjectFactory m_objFactory;
    protected Collection<ObjectWrapper> m_objs;
    private int m_initialCapacity;
    protected int m_incCapacity;
    private int m_maxCapacity;
    private long m_shrinkInterval;
    protected int m_shrinkCapacity;
    protected long m_shrinkObjPeriod;
    private long m_checkInterval;
    private long m_expirePeriod;
    protected CourierLogger m_logger;
    private Timer m_timer;

    public ObjectPool(
        final String desc,
        final CourierLogger logger,
        final PoolObjectFactory objFactory
    ) {
        m_desc = desc;
        m_logger = logger;
        m_objFactory = objFactory;
        setCapacityPars(0, 1, -1);
        setShrinkPars(-1, -1, -1);
        setCheckPars(-1);
        setExpirePars(-1);
    }

    public ObjectPool(
        final String desc,
        final CourierLogger logger,
        final PoolObjectFactory objFactory,
        final int initialCapacity, final int incCapacity, int maxCapacity,
        final long shrinkInterval, final int shrinkCapacity, final long shrinkObjPeriod,
        final long checkInterval,
        long expirePeriod
    ) {
        this(desc, logger, objFactory);
        setCapacityPars(initialCapacity, incCapacity, maxCapacity);
        setShrinkPars(shrinkInterval, shrinkCapacity, shrinkObjPeriod);
        setCheckPars(checkInterval);
        setExpirePars(expirePeriod);
    }

    public ObjectPool(
        final String desc,
        final CourierLogger logger,
        final PoolObjectFactory objFactory,
        final int initialCapacity, final int incCapacity, int maxCapacity,
        final long shrinkInterval, final int shrinkCapacity, final long shrinkObjPeriod,
        final long checkInterval
    ) {
        this(desc, logger, objFactory);
        setCapacityPars(initialCapacity, incCapacity, maxCapacity);
        setShrinkPars(shrinkInterval, shrinkCapacity, shrinkObjPeriod);
        setCheckPars(checkInterval);
    }

    public final void setCapacityPars(
        final int initialCapacity, final int incCapacity, int maxCapacity
    ) {
        m_initialCapacity = initialCapacity;
        m_incCapacity = incCapacity;
        m_maxCapacity = maxCapacity;
    }

    public final void setShrinkPars(
        final long shrinkInterval, final int shrinkCapacity,
        final long shrinkObjPeriod
    ) {
        m_shrinkInterval = shrinkInterval;
        m_shrinkCapacity = shrinkCapacity;
        m_shrinkObjPeriod = shrinkObjPeriod;
    }

    public final void setCheckPars(
        final long checkInterval
    ) {
        m_checkInterval = checkInterval;
    }

    public final void setExpirePars(long expirePeriod) {
        m_expirePeriod = expirePeriod;
    }

    public final synchronized Object getObject() {
        m_logger.debug(
            "Entering " + getClass().getName() + ".getObject of '" +
            m_desc + "' size = " + m_objs.size()
        );
        ensureOpen();
        Object o = innerGetObject();
        if (o instanceof PooledObject) {
            ((PooledObject)o).allocated();
        }
        return o;
    }

    private Object innerGetObject() {
        Object o;
        o = findFreeObject();
        if (o != null) return o;

        expire();
        o = findFreeObject();
        if (o != null) return o;
        if (reachedMaxCapacity()) {
            throw new RuntimeException(
                "Pool '" + m_desc + "' has reached max capacity " + m_maxCapacity);
        }

        expand();
        o = findFreeObject();
        if (o != null) return o;

        throw new RuntimeException(
            "Object pool '" + m_desc + "' failed to allocate object");
    }

    public final synchronized void releaseObject(final Object o) {
        boolean needToRemove = false;
        if (o instanceof PooledObject) {
            needToRemove = !((PooledObject)o).released();
        }
        releaseObject(o, needToRemove);
    }

    private void releaseObject(final Object o, boolean needToRemove) {
        m_logger.debug(
            "Entering ObjectPool.releaseObject of '" + m_desc + "'"
        );
        ensureOpen();

        final Iterator<ObjectWrapper> it = m_objs.iterator();
        while (it.hasNext()) {
            ObjectWrapper ow = it.next();
            if (ow.getObject() == o) {
                ow.release();
                if (needToRemove) {
                    it.remove();
                    returnObject(ow);
                }
                return;
            }
        }
        m_logger.warning(
            "ObjectPool '" + m_desc + "'" +
            ": there was no release for object: " + o.toString()
        );
    }

    private void removeSingleObject(final Object o) {
        returnObjects(
            new BoolFunc() {
                public boolean isTrue(ObjectWrapper ow) {
                    return ow.getObject() == o;
                }
            }
            , 1
        );
    }

    public final synchronized void releaseAndRemoveObject(final Object o) {
        releaseObject(o, true);
    }

    public synchronized String toString() {
        StringBuffer buf = new StringBuffer(
            "ObjectPool '" + m_desc + "' Size = " + m_objs.size()
        );
        int freeQnt = 0;
        StringBuffer obuf = new StringBuffer();
        int i = 0;
        DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS");
        for (final ObjectWrapper ow : m_objs) {
            obuf.append(i);
            obuf.append(" --> create time = ").append(df.format(ow.getCreateTime()));
            obuf.append(" free = ").append(ow.isFree());
            if (ow.isFree()) {
                obuf.append(" release time = ").append(df.format(ow.getReleaseTime()));
            }
            obuf.append('\n');
            obuf.append(ow.getObject().toString());
            obuf.append('\n');
            if (ow.isFree()) freeQnt++;
            i++;
        }
        buf.append("; Free count = ").append(freeQnt).append("\n");
        buf.append(obuf.toString());
        return buf.toString();
    }

    synchronized final int freeCount() {
        final Iterator<ObjectWrapper> it = m_objs.iterator();
        int freeQnt = 0;
        while (it.hasNext()) {
            final ObjectWrapper ow = it.next();
            if (ow.isFree()) freeQnt++;
        }
        return freeQnt;
    }

    protected final boolean reachedMaxCapacity() {
        return (m_maxCapacity > 0) && (size() >= m_maxCapacity);
    }

    protected final void addNewObject() {
        m_logger.debug("Getting new object for pool '" + m_desc + "'");
        if (reachedMaxCapacity()) {
            throw new RuntimeException(
                "Pool '" + m_desc + "' max capacity " +
                m_maxCapacity + " has been reached"
            );
        }
        try {
            Object o = allocateObject();
            m_logger.debug("Pool '" + m_desc + "' allocated new object: " + o.toString());
            m_objs.add(new ObjectWrapper(o));
        } catch (Exception e) {
            m_logger.error("Error while allocating new pool '" + m_desc + "' object", e);
        }
    }

    private Object findFreeObject() {
        final Iterator<ObjectWrapper> it = m_objs.iterator();
        while (it.hasNext()) {
            final ObjectWrapper ow = it.next();
            if (ow.isFree()) {
                if (!objectExpired(ow)) {
                    if (objectValid(ow)) return ow.capture();
                    else {
                        it.remove();
                        returnObject(ow);
                    }
                }
            }
        }
        return null;
    }

    protected void beforeActivities() {}
    protected void afterActivities() {}

    protected abstract Object allocateObject() throws Exception;
    protected abstract void deallocateObject(Object o);
    protected abstract boolean objectWrapperValid(Object o);

    private interface BoolFunc {
        boolean isTrue(ObjectWrapper ow);
    }

    private int returnObjects(BoolFunc condition, int maxCount) {
        ensureOpen();

        beforeActivities();
        int delCount = 0;
        try {
            final Iterator<ObjectWrapper> it = m_objs.iterator();
            while (it.hasNext() && (maxCount < 0 || delCount < maxCount)) {
                ObjectWrapper ow = it.next();
                if (condition.isTrue(ow)) {
                    it.remove();
                    returnObject(ow);
                    delCount++;
                }
            }
        } finally {
            afterActivities();
        }

        m_logger.debug("Pool '" + m_desc + "' returned " + delCount + " objects");
        return delCount;
    }

    private void addNewObjects(int count) {
        ensureOpen();

        beforeActivities();
        int i = 0;
        try {
            for (; !reachedMaxCapacity() && (i < count); i++) {
                addNewObject();
            }
        } finally {
            afterActivities();
        }

        m_logger.debug("Pool '" + m_desc + "' added " + i + " objects");
    }

    protected final synchronized void expand() {
        debugEnter("expand");
        addNewObjects(m_incCapacity);
    }

    protected final synchronized void shrink() {
        debugEnter("shrink");

        final long curTime = System.currentTimeMillis();
        returnObjects(
            new BoolFunc() {
                public boolean isTrue(ObjectWrapper ow) {
                    return (
                        ow.isFree()
                        && (
                          (m_shrinkObjPeriod > 0)
                          && ((curTime - ow.getReleaseTime()) > m_shrinkObjPeriod)
                        )
                    );
                }
            }, m_shrinkCapacity
        );

        expire();
    }

    private synchronized void expire() {
        debugEnter("expire");

        int delCount = returnObjects(
            new BoolFunc() {
                public boolean isTrue(ObjectWrapper ow) {
                    return ow.isFree() && objectExpired(ow);
                }
            }, -1
        );

        addNewObjects(delCount);
    }

    protected synchronized final void check() {
        debugEnter("check");

        int delCount = returnObjects(
            new BoolFunc() {
                public boolean isTrue(ObjectWrapper ow) {
                    return ow.isFree() && !objectValid(ow);
                }
            }, -1
        );

        addNewObjects(delCount);
    }

    protected final boolean objectExpired(ObjectWrapper ow) {
        return
            m_expirePeriod >= 0
            && (System.currentTimeMillis() - ow.getCreateTime()) > m_expirePeriod;
    }

    protected final boolean objectValid(ObjectWrapper ow) {
        boolean isGoodObject = false;
        Object o = ow.getObject();
        try {
            isGoodObject = objectWrapperValid(o);
            if (!isGoodObject) {
                m_logger.warning(
                    "Pool object " + o.getClass().getName() +
                    " (" + o + ") failed checking"
                );
            }
        } catch (Exception e) {
            m_logger.warning(
                "Error while checking object" +
                (o != null ? " " + o.getClass().getName() + ": " + o : "")
                , e
            );
        }
        return isGoodObject;
    }

    public final synchronized void start() throws PoolException {
        try {
            m_objs = new LinkedList<ObjectWrapper>();

            for (int i = 0; !reachedMaxCapacity() && (i < m_initialCapacity); i++) {
                try {
                    addNewObject();
                } catch(Throwable e) {
                    m_logger.error(e);
                }
            }

            if (m_shrinkInterval > 0) {
                ensureTimer();
                TimerTask shrinkTask = new TimerTask() {
                    public void run() {
                        try {
                            shrink();
                        }
                        catch (Exception e) {
                            m_logger.error(e);
                        }
                    }
                };
                m_timer.schedule(shrinkTask, m_shrinkInterval, m_shrinkInterval);
            }

            if (m_checkInterval > 0) {
                ensureTimer();
                TimerTask checkTask = new TimerTask() {
                    public void run() {
                        try {
                            check();
                        }
                        catch (Exception e) {
                            m_logger.error(e);
                        }
                    }
                };
                m_timer.schedule(checkTask, m_checkInterval, m_checkInterval);
            }
        } catch(Throwable e) {
            try { close(); }
            catch(Throwable e1){ m_logger.error(e1); }
            throw new PoolException(e);
        }
        m_closed = false;
    }

    private void ensureTimer() {
        if (m_timer == null) m_timer = new Timer(m_desc + "-Timer", true);
    }

    public synchronized void close() throws PoolException {
        if (m_closed) return;

        if(m_timer != null) {
            try { m_timer.cancel(); }
            catch(Throwable e) { m_logger.warning(e); }
        }

        for (Iterator<ObjectWrapper> it = m_objs.iterator(); it.hasNext(); ) {
            final ObjectWrapper ow = it.next();
            it.remove();
            if (ow.isFree()) {
                returnObject(ow);
            } else {
                m_logger.warning(
                    "Pool '" + m_desc + "': close cannot return object because its in use. " +
                    "Object: " + ow.getObject().toString()
                );
            }
        }

        if (m_objFactory instanceof Disposable) {
            ((Disposable)m_objFactory).dispose();
        }

        m_closed = true;
    }

    protected final void returnObject(ObjectWrapper ow) {
        m_logger.debug("Returning object from pool '" + m_desc + "'");        
        try {
            deallocateObject(ow.getObject());
        } catch (Throwable e) {
            m_logger.error("Error returning object from pool '" + m_desc + "'", e);
        }
    }

    public final synchronized int size() {
        return m_objs.size();
    }

    public final synchronized boolean isStarted() {
        return !m_closed;
    }

    protected final void ensureOpen() {
        if (m_closed) throw new IllegalStateException("Pool '" + m_desc + "' closed");
    }

    public final void setMaxCapacity(int maxCapacity) {
        m_maxCapacity = maxCapacity;
    }

    public String getDesc() {
        return m_desc;
    }

    public PoolObjectFactory getObjectFactory() {
        return m_objFactory;
    }

    private void debugEnter(String method) {
        m_logger.debug("Entering " + getClass().getSimpleName() + "." + method + " of '" + m_desc + "'");
    }
}
