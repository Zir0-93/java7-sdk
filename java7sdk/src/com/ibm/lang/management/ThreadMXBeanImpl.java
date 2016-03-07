/*
 * Licensed Materials - Property of IBM,
 * (c) Copyright IBM Corp. 2007, 2012  All Rights Reserved.
 */

package com.ibm.lang.management;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.ManagementPermission;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

/**
 * Runtime type for {@link java.lang.management.ThreadMXBean}
 * 
 * @author gharley
 * @since 1.5
 */
public final class ThreadMXBeanImpl extends DynamicMXBeanImpl implements
        ThreadMXBean {

    private static ThreadMXBeanImpl instance = new ThreadMXBeanImpl();
    private static Boolean isThreadCpuTimeEnabled = null;
    private static Boolean isThreadCpuTimeSupported = null;
    

    /**
     * Constructor intentionally private to prevent instantiation by others.
     * Sets the metadata for this bean.
     */
    ThreadMXBeanImpl() {
        super(ManagementUtils.createObjectName(ManagementFactory.THREAD_MXBEAN_NAME));
        setMBeanInfo(null);
    }
    
    public javax.management.MBeanInfo getMBeanInfo() {
        if (info == null) {
            setMBeanInfo(ManagementUtils.getMBeanInfo(ThreadMXBean.class.getName()));
        }
        return info;
    }

    /**
     * Singleton accessor method.
     * 
     * @return the <code>ThreadMXBeanImpl</code> singleton.
     */
    static ThreadMXBeanImpl getInstance() {
        return instance;
    }

    /**
     * @return an array of the identifiers of every thread in the virtual
     *         machine that has been detected as currently being in a deadlock
     *         situation over an object monitor. The return will not include the
     *         id of any threads blocked on ownable synchronizers. The
     *         information returned from this method will be a subset of that
     *         returned from {@link #findDeadlockedThreadsImpl()}.
     * @see #findMonitorDeadlockedThreads()
     */
    private native long[] findMonitorDeadlockedThreadsImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#findMonitorDeadlockedThreads()
     */
    public long[] findMonitorDeadlockedThreads() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(new ManagementPermission("monitor"));
        }
        return this.findMonitorDeadlockedThreadsImpl();
    }

    /**
     * @return the identifiers of all of the threads currently alive in the
     *         virtual machine.
     * @see #getAllThreadIds()
     */
    private native long[] getAllThreadIdsImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#getAllThreadIds()
     */
    public long[] getAllThreadIds() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(new ManagementPermission("monitor"));
        }
        return this.getAllThreadIdsImpl();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#getCurrentThreadCpuTime()
     */
    public long getCurrentThreadCpuTime() {
        long result = -1;
        if (isCurrentThreadCpuTimeSupported()) {
            if (isThreadCpuTimeEnabled()) {
                result = this.getThreadCpuTimeImpl(Thread.currentThread().getId());
            }
        } else {
            throw new UnsupportedOperationException(
                    "CPU time measurement is not supported on this virtual machine.");
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#getCurrentThreadUserTime()
     */
    public long getCurrentThreadUserTime() {
        long result = -1;
        if (isCurrentThreadCpuTimeSupported()) {
            if (isThreadCpuTimeEnabled()) {
                result = this.getThreadUserTimeImpl(Thread.currentThread().getId());
            }
        } else {
            throw new UnsupportedOperationException(
                    "CPU time measurement is not supported on this virtual machine.");
        }
        return result;
    }

    /**
     * @return the number of currently alive daemon threads.
     * @see #getDaemonThreadCount()
     */
    private native int getDaemonThreadCountImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#getDaemonThreadCount()
     */
    public int getDaemonThreadCount() {
        return this.getDaemonThreadCountImpl();
    }

    /**
     * @return the peak number of live threads
     * @see #getPeakThreadCount()
     */
    private native int getPeakThreadCountImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#getPeakThreadCount()
     */
    public int getPeakThreadCount() {
        return this.getPeakThreadCountImpl();
    }

    /**
     * @return the number of currently alive threads.
     * @see #getThreadCount()
     */
    private native int getThreadCountImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#getThreadCount()
     */
    public int getThreadCount() {
        return this.getThreadCountImpl();
    }

    /**
     * @param id
     *            the identifier for a thread. Must be a positive number greater
     *            than zero.
     * @return on virtual machines where thread CPU timing is supported and
     *         enabled, and there is a living thread with identifier
     *         <code>id</code>, the number of nanoseconds CPU time used by
     *         the thread. On virtual machines where thread CPU timing is
     *         supported but not enabled, or where there is no living thread
     *         with identifier <code>id</code> present in the virtual machine,
     *         a value of <code>-1</code> is returned.
     * @see #getThreadCpuTime(long)
     */
    private native long getThreadCpuTimeImpl(long id);

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#getThreadCpuTime(long)
     */
    public long getThreadCpuTime(long id) {
        // Validate input.
        if (id <= 0) {
            throw new IllegalArgumentException(
                    "Thread id must be greater than 0.");
        }

        long result = -1;
        if (isThreadCpuTimeSupported()) {
            if (isThreadCpuTimeEnabled()) {
                result = this.getThreadCpuTimeImpl(id);
            }
        } else {
            throw new UnsupportedOperationException(
                    "CPU time measurement is not supported on this virtual machine.");
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#getThreadInfo(long)
     */
    public ThreadInfo getThreadInfo(long id) {
        return getThreadInfo(id, 0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#getThreadInfo(long[])
     */
    public ThreadInfo[] getThreadInfo(long[] ids) {
        return getThreadInfo(ids, 0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#getThreadInfo(long[], int)
     */
    public ThreadInfo[] getThreadInfo(long[] ids, int maxDepth) {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(new ManagementPermission("monitor"));
        }

        // Validate inputs
        for (int i = 0; i < ids.length; i++) {
            if (ids[i] <= 0) {
                throw new IllegalArgumentException(
                        "Thread id must be greater than 0.");
            }
        }

        if (maxDepth < 0) {
            throw new IllegalArgumentException(
                    "maxDepth value cannot be negative.");
        }

        // Create an array and populate with individual ThreadInfos
        return this.getMultiThreadInfoImpl(ids, maxDepth, false, false);
    }

    /**
     * Get together information for threads and create instances of the
     * ThreadInfo class.
     * <p>
     * Assumes that caller has already carried out error checking on the
     * <code>id</code> and <code>maxDepth</code> arguments.
     * </p>
     * 
     * @param ids
     *            thread ids
     * @param lockedMonitors
     *            if <code>true</code> attempt to set the returned
     *            <code>ThreadInfo</code> with details of object monitors
     *            locked by the specified thread
     * @param lockedSynchronizers
     *            if <code>true</code> attempt to set the returned
     *            <code>ThreadInfo</code> with details of ownable
     *            synchronizers locked by the specified thread
     * @return information for threads
     */
    public ThreadInfo[] getThreadInfo(long[] ids, boolean lockedMonitors,
            boolean lockedSynchronizers) {
        // Verify inputs
        if (lockedMonitors && !isObjectMonitorUsageSupported()) {
            throw new UnsupportedOperationException(
                    "Monitoring of object monitors is not supported on this virtual machine");
        }
        if (lockedSynchronizers && !isSynchronizerUsageSupported()) {
            throw new UnsupportedOperationException(
                    "Monitoring of ownable synchronizer usage is not supported on this virtual machine");
        }

        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(new ManagementPermission("monitor"));
        }

        // Validate thread ids
        for (int i = 0; i < ids.length; i++) {
            if (ids[i] <= 0) {
                throw new IllegalArgumentException(
                        "Thread id must be greater than 0.");
            }
        }// end for each supplied thread id

        // Create an array and populate with individual ThreadInfos
        return this.getMultiThreadInfoImpl(ids, Integer.MAX_VALUE,
                lockedMonitors, lockedSynchronizers);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#getThreadInfo(long, int)
     */
    public ThreadInfo getThreadInfo(long id, int maxDepth) {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(new ManagementPermission("monitor"));
        }

        // Validate inputs
        if (id <= 0) {
            throw new IllegalArgumentException(
                    "Thread id must be greater than 0.");
        }
        if (maxDepth < 0) {
            throw new IllegalArgumentException(
                    "maxDepth value cannot be negative.");
        }
        return this.getThreadInfoImpl(id, maxDepth);
    }

    /*
     * Returns the corresponding Thread instance for a given thread id
     * 
     * @param id id of the thread (must be > 0) @return null if thread with the
     * id specified does not exist
     */
    private native Thread getThreadByIdImpl(long id);
    
    /**
     * Get together information for a thread and create an instance of the
     * ThreadInfo class.
     * <p>
     * Assumes that caller has already carried out error checking on the
     * <code>id</code> and <code>maxDepth</code> arguments.
     * </p>
     * 
     * @param id
     *            thread id
     * @param maxStackDepth
     *            maximum depth of the stack trace
     * @return ThreadInfo of the give thread
     */
    private native ThreadInfo getThreadInfoImpl(long id, int maxStackDepth);

    /**
     * Returns an array of <code>LockInfo</code> objects, one for each ownable
     * synchronizer locked by the thread with id <code>threadId</code>. If
     * the identified thread has no such locks then the returned array will have
     * a length of zero.
     * <p>
     * <b>Implementation Note:</b> this method will only be called on threads
     * that are known to exist in the virtual machine.
     * </p>
     * 
     * @param threadId
     *            the id of a thread currently alive in the virtual machine
     * @return an array of <code>LockInfo</code> objects, each element
     *         representing an ownable synchronizer locked by the identified
     *         thread. If no synchronizers are locked by the identified thread
     *         the array will have a length of zero
     */
    private native LockInfo[] getLockedSynchronizers(long threadId);

    /**
     * Returns an array of <code>MonitorInfo</code> objects, one for each
     * monitor object locked by the thread with id <code>threadId</code>. If
     * the identified thread has no such locks then the returned array will have
     * a length of zero.
     * <p>
     * <b>Implementation Note:</b> this method will only be called on threads
     * that are known to exist in the virtual machine.
     * </p>
     * 
     * @param threadId
     *            the id of a thread currently alive in the virtual machine
     * @return an array of <code>MonitorInfo</code> objects, each element
     *         representing a monitor object locked by the identified thread. If
     *         no monitor objects are locked by the identified thread the array
     *         will have a length of zero
     */
    private native MonitorInfo[] getLockedMonitors(long threadId);

    /**
     * @param id
     *            the identifier for a thread. Must be a positive number greater
     *            than zero.
     * @return on virtual machines where thread CPU timing is supported and
     *         enabled, and there is a living thread with identifier
     *         <code>id</code>, the number of nanoseconds CPU time used by
     *         the thread running in user mode. On virtual machines where thread
     *         CPU timing is supported but not enabled, or where there is no
     *         living thread with identifier <code>id</code> present in the
     *         virtual machine, a value of <code>-1</code> is returned.
     *         <p>
     *         If thread CPU timing was disabled when the thread was started
     *         then the virtual machine is free to choose any measurement start
     *         time between when the virtual machine started up and when thread
     *         CPU timing was enabled with a call to
     *         {@link #setThreadCpuTimeEnabled(boolean)}.
     *         </p>
     * @see #getThreadUserTime(long)
     */
    private native long getThreadUserTimeImpl(long id);

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#getThreadUserTime(long)
     */
    public long getThreadUserTime(long id) {
        // Validate input.
        if (id <= 0) {
            throw new IllegalArgumentException(
                    "Thread id must be greater than 0.");
        }

        long result = -1;
        if (isThreadCpuTimeSupported()) {
            if (isThreadCpuTimeEnabled()) {
                result = this.getThreadUserTimeImpl(id);
            }
        } else {
            throw new UnsupportedOperationException(
                    "CPU time measurement is not supported on this virtual machine.");
        }
        return result;
    }

    /**
     * @return the total number of started threads.
     * @see #getTotalStartedThreadCount()
     */
    private native long getTotalStartedThreadCountImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#getTotalStartedThreadCount()
     */
    public long getTotalStartedThreadCount() {
        return this.getTotalStartedThreadCountImpl();
    }

    /**
     * @return <code>true</code> if CPU timing of the current thread is
     *         supported, otherwise <code>false</code>.
     * @see #isCurrentThreadCpuTimeSupported()
     */
    private native boolean isCurrentThreadCpuTimeSupportedImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#isCurrentThreadCpuTimeSupported()
     */
    public boolean isCurrentThreadCpuTimeSupported() {
        return this.isCurrentThreadCpuTimeSupportedImpl();
    }

    /**
     * @return <code>true</code> if thread contention monitoring is enabled,
     *         <code>false</code> otherwise.
     * @see #isThreadContentionMonitoringEnabled()
     */
    private native boolean isThreadContentionMonitoringEnabledImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#isThreadContentionMonitoringEnabled()
     */
    public boolean isThreadContentionMonitoringEnabled() {
        if (!isThreadContentionMonitoringSupported()) {
            throw new UnsupportedOperationException(
                    "Thread contention monitoring is not supported on this virtual machine.");
        }
        return this.isThreadContentionMonitoringEnabledImpl();
    }

    /**
     * @return <code>true</code> if thread contention monitoring is supported,
     *         <code>false</code> otherwise.
     * @see #isThreadContentionMonitoringSupported()
     */
    private native boolean isThreadContentionMonitoringSupportedImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#isThreadContentionMonitoringSupported()
     */
    public boolean isThreadContentionMonitoringSupported() {
        return this.isThreadContentionMonitoringSupportedImpl();
    }

    /**
     * @return <code>true</code> if thread CPU timing is enabled,
     *         <code>false</code> otherwise.
     * @see #isThreadCpuTimeEnabled()
     */
    private native boolean isThreadCpuTimeEnabledImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#isThreadCpuTimeEnabled()
     */
    public boolean isThreadCpuTimeEnabled() {
        if (!isThreadCpuTimeSupported() && !isCurrentThreadCpuTimeSupported()) {
            throw new UnsupportedOperationException(
                    "Thread CPU timing is not supported on this virtual machine.");
        }
        if(isThreadCpuTimeEnabled == null){
            isThreadCpuTimeEnabled = this.isThreadCpuTimeEnabledImpl();
        }
        return isThreadCpuTimeEnabled;
    }

    /**
     * @return <code>true</code> if the virtual machine supports the CPU
     *         timing of threads, <code>false</code> otherwise.
     * @see #isThreadCpuTimeSupported()
     */
    private native boolean isThreadCpuTimeSupportedImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#isThreadCpuTimeSupported()
     */
    public boolean isThreadCpuTimeSupported() {
        if(isThreadCpuTimeSupported == null){
            isThreadCpuTimeSupported = this.isThreadCpuTimeSupportedImpl();
        }
        return isThreadCpuTimeSupported;
    }

    /**
     * @see #resetPeakThreadCount()
     */
    private native void resetPeakThreadCountImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#resetPeakThreadCount()
     */
    public void resetPeakThreadCount() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(new ManagementPermission("control"));
        }
        this.resetPeakThreadCountImpl();
    }

    /**
     * @param enable
     *            enable thread contention monitoring if <code>true</code>,
     *            otherwise disable thread contention monitoring.
     */
    private native void setThreadContentionMonitoringEnabledImpl(boolean enable);

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#setThreadContentionMonitoringEnabled(boolean)
     */
    public void setThreadContentionMonitoringEnabled(boolean enable) {
        if (!isThreadContentionMonitoringSupported()) {
            throw new UnsupportedOperationException(
                    "Thread contention monitoring is not supported on this virtual machine.");
        }

        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(new ManagementPermission("control"));
        }
        this.setThreadContentionMonitoringEnabledImpl(enable);
    }

    /**
     * @param enable
     *            enable thread CPU timing if <code>true</code>, otherwise
     *            disable thread CPU timing
     */
    private native void setThreadCpuTimeEnabledImpl(boolean enable);

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#setThreadCpuTimeEnabled(boolean)
     */
    public void setThreadCpuTimeEnabled(boolean enable) {
        if (!isThreadCpuTimeSupported() && !isCurrentThreadCpuTimeSupported()) {
            throw new UnsupportedOperationException(
                    "Thread CPU timing is not supported on this virtual machine.");
        }

        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(new ManagementPermission("control"));
        }
        this.setThreadCpuTimeEnabledImpl(enable);
        isThreadCpuTimeEnabled = enable;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#isObjectMonitorUsageSupported()
     */
    public boolean isObjectMonitorUsageSupported() {
        return this.isObjectMonitorUsageSupportedImpl();
    }

    /**
     * @return <code>true</code> if the VM supports monitoring of object
     *         monitors, <code>false</code> otherwise
     */
    private native boolean isObjectMonitorUsageSupportedImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#isSynchronizerUsageSupported()
     */
    public boolean isSynchronizerUsageSupported() {
        return this.isSynchronizerUsageSupportedImpl();
    }

    /**
     * @return <code>true</code> if the VM supports the monitoring of ownable
     *         synchronizers, <code>false</code> otherwise
     */
    private native boolean isSynchronizerUsageSupportedImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#findDeadlockedThreads()
     */
    public long[] findDeadlockedThreads() {
        if (!isSynchronizerUsageSupported()) {
            throw new UnsupportedOperationException(
                    "Monitoring of ownable synchronizer usage is not supported on this virtual machine");
        }

        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(new ManagementPermission("monitor"));
        }
        return this.findDeadlockedThreadsImpl();
    }

    /**
     * @return an array of the identifiers of every thread in the virtual
     *         machine that has been detected as currently being in a deadlock
     *         situation over an object monitor or an ownable synchronizer.
     * @see #findDeadlockedThreads()
     */
    private native long[] findDeadlockedThreadsImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#dumpAllThreads(boolean, boolean)
     */
    public ThreadInfo[] dumpAllThreads(boolean lockedMonitors,
            boolean lockedSynchronizers) {
        if (lockedMonitors && !isObjectMonitorUsageSupported()) {
            throw new UnsupportedOperationException(
                    "Monitoring of object monitors is not supported on this virtual machine");
        }
        if (lockedSynchronizers && !isSynchronizerUsageSupported()) {
            throw new UnsupportedOperationException(
                    "Monitoring of ownable synchronizer usage is not supported on this virtual machine");
        }
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(new ManagementPermission("monitor"));
        }
        return dumpAllThreadsImpl(lockedMonitors, lockedSynchronizers);
    }

    /**
     * @param lockedMonitors
     *            if <code>true</code> then include details of locked object
     *            monitors in returned array
     * @param lockedSynchronizers
     *            if <code>true</code> then include details of locked ownable
     *            synchronizers in returned array
     * @return an array of <code>ThreadInfo</code> objects&nbsp;-&nbsp;one for
     *         each thread running in the virtual machine
     */
    private native ThreadInfo[] dumpAllThreadsImpl(boolean lockedMonitors,
            boolean lockedSynchronizers);

    /**
     * Answers an array of instances of the ThreadInfo class according to ids
     * 
     * @param ids
     *            thread ids
     * @param maxStackDepth 
     *            the max stsck depth
     * @param getLockedMonitors
     *            if <code>true</code> attempt to set the returned
     *            <code>ThreadInfo</code> with details of object monitors
     *            locked by the specified thread
     * @param getLockedSynchronizers
     *            if <code>true</code> attempt to set the returned
     *            <code>ThreadInfo</code> with details of ownable
     *            synchronizers locked by the specified thread
     * @return new {@link ThreadInfo} instance
     */   
    private native ThreadInfo[] getMultiThreadInfoImpl(long[] ids, int maxStackDepth,
            boolean getLockedMonitors, boolean getLockedSynchronizers);
}
