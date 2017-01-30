// Copyright 2004 by Justin T. Sampson
//
// This file is part of JThreadUnit.
//
// JThreadUnit is free software; you can redistribute it and/or modify it under
// the terms of the GNU Lesser General Public License as published by the Free
// Software Foundation; either Version 2.1 of the License, or (at your option)
// any later Version.
//
// JThreadUnit is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
// for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with JThreadUnit; if not, write to the Free Software Foundation, Inc.,
// 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

package com.krasama.jthreadunit;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import junit.framework.AssertionFailedError;

/**
 * A thread that runs in an endless loop performing actions. A controlling
 * thread should call the various methods of this class to control the
 * actions performed, and finally call {@link #kill()} when there are no
 * more actions to be performed. Calling {@link #interrupt()} will not
 * stopTest the thread, but merely reset it to the state of waiting for a command.
 * That is, if the thread is blocked, {@link #interrupt()} will unblock it
 * and make it ready to respond to {@link #performAction(String)}.
 * <p>
 * Each action must be implemented in a subclass by a parameterless method
 * with void return type named by prepending <tt>do</tt> to the capitalized
 * action name. For example, <tt>performAction("foo")</tt> results in a call
 * to the method with signature <tt>public void doFoo()</tt>.
 */
public abstract class TestThread extends Thread
{
    private static final int ACTION_COMPLETE = 0;
    private static final int ACTION_GIVEN = 1;
    private static final int ACTION_TAKEN = 2;

    private String initiatedAction = null;
    private int status = ACTION_COMPLETE;
    private volatile boolean killed = false;

    TestThreadGroup m_group;
    private Throwable lastActionException = null;
    private String failedAction = null;
    private Throwable frameworkError = null;

    public TestThread(TestThreadGroup group, String name)
    {
        super(group, name);
        m_group = group;
    }

    /**
     * Called to indicate that this thread should perform the named action
     * completely without blocking.
     * <p>
     * @throws AssertionFailedError If this thread blocks before completing the
     *         action.
     */
    public void performAction(String actionName) throws InterruptedException
    {
        checkFrameworkErrors();
        initiateAction(actionName);
        completeBlockedAction();
    }

    /**
     * Perform several actions. Equivalent to calling
     * {@link #performAction(String)} once for each of the given actions.
     * This is convenient when the code being tested contains
     * {@link #checkpoint(String)} calls.
     */
    public void performActions(String... actionNames)
            throws InterruptedException
    {
        for (String actionName : actionNames)
        {
            performAction(actionName);
        }
    }

    /**
     * Called to indicate that this thread should initiate the named action
     * but expect to block before completing it.
     * <p>
     * @throws AssertionFailedError If this thread completes the action without
     *         blocking.
     */
    public void actionShouldBlock(String actionName) throws InterruptedException
    {
        initiateAction(actionName);
        assertStillBlocked();
    }

    /**
     * Called to indicate that this thread should finish a previously
     * blocked action.
     * <p>
     * @throws AssertionFailedError If this thread is still blocked without
     *         having completed the action.
     */
    public void completeBlockedAction()
    {
        ThreadInfo[] infos = letRun();
        assertComplete(infos);
    }

    /**
     * Complete one action and perform several more. Equivalent to calling
     * {@link #completeBlockedAction()} once followed by calling
     * {@link #performAction(String)} once for each of the given actions.
     * This is convenient when the code being tested contains
     * {@link #checkpoint(String)} calls.
     */
    public void completeBlockedActionWithActions(String... actionNames)
            throws InterruptedException
    {
        completeBlockedAction();
        performActions(actionNames);
    }

    /**
     * Called to indicate that a previously blocked action should still be
     * blocked.
     * <p>
     * @throws AssertionFailedError If this thread has completed the action
     *         without blocking.
     */
    public void assertStillBlocked()
    {
        letRun();
        assertNotComplete();
    }

    /**
     * Called from within an action method to mark the current action as
     * complete and wait for the next action without returning from the first
     * action method.
     */
    protected void expectAction(String actionName) throws InterruptedException
    {
        checkActionName(actionName);

        clearAction();
        String actualAction = waitForAction();

        if (!actualAction.equals(actionName))
        {
            throw new IllegalStateException(
                    "Expected action " + actionName
                    + " but was " + actualAction);
        }
    }

    /**
     * Called from arbitrary code to mark the current action as complete and
     * wait for the next action without returning from the first action method.
     * Equivalent to calling {@link #expectAction(String)} directly from an
     * action method, unless the current thread is not a <tt>TestThread</tt>,
     * in which case does nothing.
     */
    public static void checkpoint(String actionName) throws InterruptedException
    {
        if (Thread.currentThread() instanceof TestThread)
        {
            ((TestThread) Thread.currentThread()).expectAction(actionName);
        }
    }

    /**
     * Kill this thread. Sets a flag and interrupts the thread, as an
     * alternative to the deprecated {@link Thread#stop()}.
     */
    void kill()
    {
        killed = true;
        interrupt();
        checkFrameworkErrors();
    }

    public void start()
    {
        ThreadMXBean mbean = ManagementFactory.getThreadMXBean();
        mbean.setThreadContentionMonitoringEnabled(true);
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        this.setPriority(Thread.MAX_PRIORITY);
        super.start();
    }

    public void run()
    {
        try
        {
            while (!killed)
            {
                try
                {
                    action();
                }
                catch (InterruptedException okay)
                {
                }
            }
        }
        catch (Error error)
        {
            throw error;
        }
        catch (Throwable throwable)
        {
            frameworkError = throwable;
            getTestThreadGroup().addErrorThread(this);
            //throwable.printStackTrace();
        }
    }

    private synchronized void initiateAction(String actionName)
            throws InterruptedException
    {
        checkActionName(actionName);

        if (status != ACTION_COMPLETE)
        {
            throw new IllegalStateException("Previous action not complete");
        }

        initiatedAction = actionName;
        status = ACTION_GIVEN;
        notifyAll();

        while (status == ACTION_GIVEN)
        {
            wait();
        }
    }

    private ThreadInfo[] letRun()
    {
        checkFrameworkErrors();
        ThreadMXBean mbean = ManagementFactory.getThreadMXBean();
        assert mbean.isThreadContentionMonitoringEnabled();

        ThreadGroup group = this.getThreadGroup();
        Thread[] threads = new TestThread[group.activeCount()];
        group.enumerate(threads);

        ThreadInfo[] infos = new ThreadInfo[threads.length];

        do
        {
            Thread.yield();
        }
        while (anyRunning(mbean, threads, infos));

        return infos;
    }

    private boolean anyRunning(
            ThreadMXBean mbean, Thread[] threads, ThreadInfo[] infos)
    {
        for (int i = 0; i < threads.length; i++)
        {
            Thread thread = threads[i];
            if (thread != null)
            {
                ThreadInfo info = mbean.getThreadInfo(thread.getId());
                infos[i] = info;
                if (info != null)
                {
                    if (info.getThreadState() == Thread.State.NEW
                            || info.getThreadState() == Thread.State.RUNNABLE)
                    {
                        return true;
                    }
                    else if (info.getLockOwnerId() != -1)
                    {
                        if (!lockOwnerAmongThreads(info, threads))
                        {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private boolean lockOwnerAmongThreads(ThreadInfo info, Thread[] threads)
    {
        for (Thread thread : threads)
        {
            if (thread != null && thread.getId() == info.getLockOwnerId())
            {
                return true;
            }
        }
        return false;
    }

    private synchronized void assertComplete(ThreadInfo[] infos)
    {
        if (status != ACTION_COMPLETE)
        {
            StringBuilder message = new StringBuilder();

            message.append(nameWithId(this.getName(), this.getId()));
            message.append(" should not be blocked during \"");
            message.append(initiatedAction);
            message.append("\"");

            for (ThreadInfo info : infos)
            {
                if (info != null)
                {
                    message.append(" <");
                    message.append(nameWithId(info.getThreadName(),
                                              info.getThreadId()));
                    message.append(": ");
                    message.append(info.getThreadState());

                    if (info.getLockName() != null)
                    {
                        message.append(" on ");

                        if (info.getLockName().startsWith(
                                this.getClass().getName() + "@"))
                        {
                            message.append("itself");
                        }
                        else
                        {
                            message.append(info.getLockName());
                        }
                    }

                    if (info.getLockOwnerName() != null)
                    {
                        message.append(" held by ");
                        message.append(nameWithId(info.getLockOwnerName(),
                                                  info.getLockOwnerId()));
                    }

                    if (info.getStackTrace().length != 0)
                    {
                        message.append(" at ");
                        message.append(info.getStackTrace()[0].toString());
                    }

                    message.append(">");
                }
            }

            throw new AssertionFailedError(message.toString());
        }
    }

    private synchronized void assertNotComplete()
    {
        if (status == ACTION_COMPLETE)
        {
            StringBuilder message = new StringBuilder();

            message.append(nameWithId(this.getName(), this.getId()));
            message.append(" should be blocked during \"");
            message.append(initiatedAction);
            message.append("\"");

            throw new AssertionFailedError(message.toString());
        }
    }

    private static String nameWithId(String name, long id)
    {
        return name + "[" + id + "]";
    }

    private void action() throws Throwable
    {
        String actionName = waitForAction();

        lastActionException = null;
        try
        {
            getActionMethod(actionName).invoke(this, new Object[0]);
        }
        catch (InvocationTargetException exception)
        {
            lastActionException = exception.getTargetException();
            failedAction = actionName;
        }
        finally
        {
            clearAction();
        }
    }

    private synchronized String waitForAction() throws InterruptedException
    {
        while (status != ACTION_GIVEN)
        {
            wait();
        }
        status = ACTION_TAKEN;
        notifyAll();
        return initiatedAction;
    }

    private synchronized void clearAction()
    {
        status = ACTION_COMPLETE;
        notifyAll();
    }

    private void checkActionName(String actionName)
    {
        if (actionName == null || actionName.equals(""))
        {
            throw new IllegalArgumentException(
                    "Action name must be non-empty string");
        }
    }

    private Method getActionMethod(String actionName)
            throws NoSuchMethodException
    {
        String methodName = "do" + capitalize(actionName);

        Method method = this.getClass().getMethod(methodName, new Class[0]);

        if (method.getReturnType() != Void.TYPE)
        {
            throw new NoSuchMethodException(
                "Action method " + methodName + " should be void");
        }

        return method;
    }

    private static String capitalize(String string)
    {
        return Character.toUpperCase(string.charAt(0)) + string.substring(1);
    }

    private void checkFrameworkErrors() {
        getTestThreadGroup().checkErrors();
    }

    private TestThreadGroup getTestThreadGroup() {
        return m_group;
    }

    public Throwable getLastActionException() {
        return lastActionException;
    }

    public Throwable getFrameworkError() {
        return frameworkError;
    }

    public synchronized void assertSuccessful() {
        if (getLastActionException() != null) {
            AssertionFailedError e = new AssertionFailedError(
                "Last action '" + failedAction + "' failed in thread: " + getDescription()
            );
            e.initCause(getLastActionException());
            throw e;
        }
    }

    private String getDescription() {
        return nameWithId(getName(), getId());
    }
}
