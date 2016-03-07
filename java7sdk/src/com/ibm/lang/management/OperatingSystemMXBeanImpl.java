/*
 * Licensed Materials - Property of IBM,
 * (c) Copyright IBM Corp. 2007, 2012  All Rights Reserved.
 */

package com.ibm.lang.management;

import java.lang.management.ManagementFactory;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;

/**
 * Runtime type for {@link java.lang.management.OperatingSystemMXBean}. In
 * addition to implementing the &quot;standard&quot; management interface
 * <code>java.lang.management.GarbageCollectorMXBean</code>, this class also
 * provides an implementation of the IBM extension interface
 * <code>com.ibm.lang.management.OperatingSystemMXBean</code>.
 * 
 * @author gharley
 * @since 1.5
 */
public abstract class OperatingSystemMXBeanImpl extends DynamicMXBeanImpl
        implements OperatingSystemMXBean, NotificationEmitter {

    /**
     * The delegate for all notification management.
     */
    private NotificationBroadcasterSupport notifier = new NotificationBroadcasterSupport();

    private final CpuUtilizationHelper cpuUtilizationHelper;

    /**
     * Constructor intentionally private to prevent instantiation by others.
     * Sets the metadata for this bean.
     */
    OperatingSystemMXBeanImpl() {
        super(ManagementUtils.createObjectName(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME));
        // only launch the notification thread if the environment could change
        if (isDLPAREnabled()) {
            OperatingSystemNotificationThread t = new OperatingSystemNotificationThread(
                    this);
            t.setDaemon(true);
            t.setName("OperatingSystemMXBean notification dispatcher");
            t.setPriority(Thread.NORM_PRIORITY + 1);
            t.start();
        }
	cpuUtilizationHelper = new CpuUtilizationHelper();
    }
    
    public javax.management.MBeanInfo getMBeanInfo() {
        if (info == null) {
            setMBeanInfo(ManagementUtils
                .getMBeanInfo(java.lang.management.OperatingSystemMXBean.class
                        .getName()));
        }

        return info;
    }

    /**
     * @return true if we are executing in a DLPAR-enabled environment where
     *         #cpus / capacity / phys mem size change notifications might be
     *         emitted
     */
    private native boolean isDLPAREnabled();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.OperatingSystemMXBean#getArch()
     */
    public String getArch() {
        return System.getProperty("os.arch");
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.OperatingSystemMXBean#getAvailableProcessors()
     */
    public int getAvailableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.OperatingSystemMXBean#getName()
     */
    public String getName() {
        return System.getProperty("os.name");
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.OperatingSystemMXBean#getVersion()
     */
    public String getVersion() {
        return System.getProperty("os.version");
    }

    /**
     * @return the number of bytes used for physical memory
     * @see #getTotalPhysicalMemory()
     */
    private native long getTotalPhysicalMemoryImpl();

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.lang.management.OperatingSystemMXBean#getTotalPhysicalMemory()
     */
    public long getTotalPhysicalMemory() {
        return this.getTotalPhysicalMemoryImpl();
    }

    /**
     * @return the collective capacity of the virtual processors available to
     *         the VM
     * @see #getProcessingCapacity()
     */
    private native int getProcessingCapacityImpl();

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.lang.management.OperatingSystemMXBean#getProcessingCapacity()
     */
    public int getProcessingCapacity() {
        return this.getProcessingCapacityImpl();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.management.NotificationEmitter#removeNotificationListener(javax.management.NotificationListener,
     *      javax.management.NotificationFilter, java.lang.Object)
     */
    public void removeNotificationListener(NotificationListener listener,
            NotificationFilter filter, Object handback)
            throws ListenerNotFoundException {
        notifier.removeNotificationListener(listener, filter, handback);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.management.NotificationBroadcaster#addNotificationListener(javax.management.NotificationListener,
     *      javax.management.NotificationFilter, java.lang.Object)
     */
    public void addNotificationListener(NotificationListener listener,
            NotificationFilter filter, Object handback)
            throws IllegalArgumentException {
        notifier.addNotificationListener(listener, filter, handback);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.management.NotificationBroadcaster#removeNotificationListener(javax.management.NotificationListener)
     */
    public void removeNotificationListener(NotificationListener listener)
            throws ListenerNotFoundException {
        notifier.removeNotificationListener(listener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.management.NotificationBroadcaster#getNotificationInfo()
     */
    public MBeanNotificationInfo[] getNotificationInfo() {
        // We know what kinds of notifications we can emit whereas the
        // notifier delegate does not. So, for this method, no delegating.
        // Instead respond using our own metadata.
        MBeanNotificationInfo[] notifications = new MBeanNotificationInfo[3];
        // -- Processing capacity notification type
        String[] notifTypes = new String[1];
        notifTypes[0] = ProcessingCapacityNotificationInfo.PROCESSING_CAPACITY_CHANGE;
        notifications[0] = new MBeanNotificationInfo(notifTypes,
                                                     javax.management.Notification.class.getName(),
                                                     "Processing Capacity Notification");

        // -- Total physical memory notification type
        notifTypes = new String[1];
        notifTypes[0] = TotalPhysicalMemoryNotificationInfo.TOTAL_PHYSICAL_MEMORY_CHANGE;
        notifications[1] = new MBeanNotificationInfo(notifTypes,
                                                     javax.management.Notification.class.getName(),
                                                     "Total Physical Memory Notification");

        // -- Available processors notification type
        notifTypes = new String[1];
        notifTypes[0] = AvailableProcessorsNotificationInfo.AVAILABLE_PROCESSORS_CHANGE;
        notifications[2] = new MBeanNotificationInfo(notifTypes,
                                                     javax.management.Notification.class.getName(),
                                                     "Available Processors Notification");
        return notifications;
    }

    /*
     * (non-Javadoc)
     * 
     * @param notification For this type of bean the user data will consist of a
     * {@link CompositeData}instance that represents one of: <ul> <li>{@link ProcessingCapacityNotificationInfo}
     * <li>{@link TotalPhysicalMemoryNotificationInfo} <li>{@link AvailableProcessorsNotificationInfo}
     * </ul>
     */
    public void sendNotification(Notification notification) {
        notifier.sendNotification(notification);
    }

    /**
     * @return the time-averaged value of the sum of the number of runnable
     *         entities running on the available processors together with the
     *         number of runnable entities ready and queued to run on the
     *         available processors.
     * @see #getSystemLoadAverage()
     */
    private native double getSystemLoadAverageImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.OperatingSystemMXBean#getSystemLoadAverage()
     */
    public double getSystemLoadAverage() {
        return this.getSystemLoadAverageImpl();
    }

    /**
     * Returns total amount of time the process has been scheduled or
     * executed so far in both kernel and user modes. Returns -1 if the
     * value is unavailable on this platform or in the case of an error.
     * 
     * @return process cpu ime in 100 ns units
     * @see #getProcessCpuTime()
     */
    private native long getProcessCpuTimeImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.OperatingSystemMXBean#getProcessCpuTimeImpl()
     */
    public long getProcessCpuTime() {
        return this.getProcessCpuTimeImpl();
    }
    
     /**
     * Returns total amount of time the process has been scheduled or executed 
     * so far in both kernel and user modes (in nano seconds). Returns -1 if the
     * value is unavailable on this platform or in the case of
     * an error.
     *
     * @return process cpu time in ns units
     */
    public long getProcessCpuTimeByNS(){
    	return this.getProcessCpuTime()*100;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.lang.management.OperatingSystemMXBean#getSystemCpuLoad()
     */
    public double getSystemCpuLoad() {
        return cpuUtilizationHelper.getSystemCpuLoad();
    }

    /**
     * Returns the amount of free physical memory at current instance on the
     * system in bytes. Returns -1 if the value is unavailable on this
     * platform or in the case of an error.
     *
     * @return amount of physical memory available in bytes
     */
    private native long getFreePhysicalMemorySizeImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.OperatingSystemMXBean#getFreePhysicalMemorySize()
     */
    public long getFreePhysicalMemorySize() {
        return this.getFreePhysicalMemorySizeImpl();
    }

    /**
     * Returns the amount of virtual memory used by the process in bytes,
     * including physical memory and swap space. Returns -1 if the value
     * is unavailable on this platform or in the case of an error.
     * 
     * @return amount of virtual memory used by the process in bytes
     * @see #getProcessVirtualMemorySize()
     */
    private native long getProcessVirtualMemorySizeImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.OperatingSystemMXBean#getProcessVirtualMemorySize()
     */
    public long getProcessVirtualMemorySize() {
        return this.getProcessVirtualMemorySizeImpl();
    }

    /**
     * Returns the amount of private memory used by the process in bytes.
     * Returns -1 if the value is unavailable on this platform or in the
     * case of an error.
     * 
     * @return amount of private memory used by the process in bytes
     * @see #getProcessPrivateMemorySize()
     */
    private native long getProcessPrivateMemorySizeImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.OperatingSystemMXBean#getProcessPrivateMemorySize()
     */
    public long getProcessPrivateMemorySize() {
        return this.getProcessPrivateMemorySizeImpl();
    }

    /**
     * Returns the amount of physical memory being used by the process
     * in bytes. Returns -1 if the value is unavailable on this platform
     * or in the case of an error.
     * 
     * @return amount of physical memory being used by the process in bytes
     * @see #getProcessPrivateMemorySize()
     */
    private native long getProcessPhysicalMemorySizeImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.OperatingSystemMXBean#getProcessPrivateMemorySize()
     */
    public long getProcessPhysicalMemorySize() {
        return this.getProcessPhysicalMemorySizeImpl();
    }

    /**
     * Returns the "recent cpu usage" for the Java Virtual Machine process.
     * This value is a double in the [0.0,1.0] interval. A value of 0.0 means
     * that none of the CPUs were running threads from the JVM process during
     * the recent period of time observed, while a value of 1.0 means that all
     * CPUs were actively running threads from the JVM 100% of the time
     * during the recent period being observed. Threads from the JVM include
     * the application threads as well as the JVM internal threads. All values
     * betweens 0.0 and 1.0 are possible depending of the activities going on
     * in the JVM process and the whole system. If the Java Virtual Machine
     * recent CPU usage is not available, the method returns a negative value.
     *
     * @return the "recent cpu usage" for the Java Virtual Machine process;
     * a negative value if not available.
     * @since   1.7
     */
    abstract public double getProcessCpuLoad();

    /**
     * Returns the amount of free swap space in bytes.
     *
     * @return the amount of free swap space in bytes.
     */
    abstract public long getFreeSwapSpaceSize();

    /**
     * Returns the total amount of swap space in bytes.
     *
     * @return the total amount of swap space in bytes.
     */	
    abstract public long getTotalSwapSpaceSize(); 

}

/*
 * $Log$
 * Revision 1.21  2006/05/31 12:14:56  gharley
 * For 6.0, added in new LockInfo and MonitorInfo types. Added in new getSystemLoadAverage() method to OperatingSystemMXBean. Test cases added and some minor refactorings carried out. Work still in progress. Next step will entail changes to ThreadInfo and ThreadMXBean.
 * Revision 1.20 2005/08/19 21:19:52
 * pchurch Give the notification dispatcher threads names and set them to
 * priority NORMAL+1 so notifications are more likely to be delivered in a
 * timely manner.
 * 
 * Revision 1.19 2005/07/07 15:44:35 gharley Remove unnecessary PrivilegedAction
 * blocks
 * 
 * Revision 1.18 2005/06/30 15:49:26 gharley Wrapped all System.getProperty()
 * calls inside new java.security.PrivilegedAction. Note : all of the properties
 * read in this class are, by default, made readable to all by the java.policy
 * file shipped with the Java SDK so perhaps these new security checks are
 * unnecessary ?
 * 
 * Revision 1.17 2005/05/20 21:43:31 pschurch implementation of notification
 * support for DLPAR events
 * 
 * Revision 1.16 2005/05/12 10:51:45 gharley Minor Javadoc change for
 * sendNotification(). Basically lists the different types of notification.
 * 
 * Revision 1.15 2005/05/12 00:36:16 pschurch add getProcessingCapacity and
 * partial implementation of notification support
 * 
 * Revision 1.14 2005/04/18 10:40:28 gharley Removal of all proprietary
 * attributes bar PhysicalMemory. Agreed with Paul Church and Tim Preece.
 * 
 * Revision 1.13 2005/02/24 16:47:35 gharley Incorrect method name. Replaced
 * getCommittedPhysicalMemory with the method called getCommittedVirtualMemory
 * 
 * Revision 1.12 2005/02/14 13:23:29 gharley Now implements
 * com.ibm.lang.management.OperatingSystemMXBean which, in turn, extends the
 * standard java.lang.OperatingSystemMXBean interface. Revision 1.11 2005/02/14
 * 12:12:28 gharley Now implements IBM platform interface
 * com.ibm.lang.management.OperatingSystemMXBean. All new methods result in
 * native calls which have been declared here. Revision 1.10 2005/02/09 22:25:18
 * gharley Moved getAttribute into superclass
 * 
 * Revision 1.9 2005/02/04 23:13:55 gharley Added in security permission code :
 * either explicitly or added a comment where an invoked method carries out the
 * check on our behalf.
 * 
 * Revision 1.8 2005/02/02 14:09:58 gharley Moved MBeanInfo setup into
 * ManagementUtils on the assumption that the metadata is going to be useful for
 * the proxy bean support.
 * 
 * Revision 1.7 2005/01/30 22:26:22 gharley Added some implementation to get
 * properties from system.
 * 
 * Revision 1.6 2005/01/19 15:08:19 gharley Updated temporary hard-coded return
 * values for methods that will (eventually) call into the VM for the correct
 * answer.
 * 
 * Revision 1.5 2005/01/19 12:46:03 gharley Implementation of DynamicMBean
 * behaviour. Extends new base class. Revision 1.4 2005/01/16 22:33:12 gharley
 * Added creation of MBeanInfo to the constructor and returned it in the
 * getMBeanInfo method. Seems to be a lot of duplication here : some refactoring
 * (common base class ?) required soon, methinks.
 * 
 * Revision 1.3 2005/01/14 11:23:33 gharley Added singleton enforcement code.
 * 
 * Revision 1.2 2005/01/12 21:56:26 gharley Now implements DynamicMBean (as an
 * MXBean should be a dynamic MBean). Revision 1.1 2005/01/11 10:56:10 gharley
 * Initial upload
 * 
 * Revision 1.1 2005/01/07 10:05:53 gharley Initial creation
 * 
 */
