/*
 * Licensed Materials - Property of IBM,
 * (c) Copyright IBM Corp. 2005, 2012  All Rights Reserved.
 */

package com.ibm.lang.management;

import java.lang.management.ManagementFactory;
import java.lang.management.ManagementPermission;
import java.lang.management.MemoryManagerMXBean;
import java.lang.management.MemoryUsage;
import java.util.LinkedList;
import java.util.List;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import com.ibm.lang.management.ManagementUtils;
import com.ibm.oti.shared.SharedClassStatistics;

/**
 * Runtime type for {@link MemoryMXBean}.
 * <p>
 * Implementation note. This type of bean is both dynamic and a notification
 * emitter. The dynamic behaviour comes courtesy of the
 * {@link com.ibm.lang.management.DynamicMXBeanImpl}superclass while the
 * notifying behaviour uses a delegation approach to a private member that
 * implements the {@link javax.management.NotificationEmitter}interface.
 * Because multiple inheritance is not supported in Java it was a toss up which
 * behaviour would be based on inheritence and which would use delegation. Every
 * other <code>*MXBeanImpl</code> class in this package inherits from the
 * abstract base class <code>DynamicMXBeanImpl</code> so that seemed to be the
 * natural approach for this class too. By choosing not to make this class a
 * subclass of {@link javax.management.NotificationBroadcasterSupport}, the
 * protected
 * <code>handleNotification(javax.management.NotificationListener, javax.management.Notification, java.lang.Object)</code>
 * method cannot be overridden for any custom notification behaviour. However,
 * taking the agile mantra of <b>YAGNI </b> to heart, it was decided that the
 * default implementation of that method will suffice until new requirements
 * prove otherwise.
 * </p>
 * 
 * @author gharley
 * @since 1.5
 */
public final class MemoryMXBeanImpl extends DynamicMXBeanImpl implements
        MemoryMXBean, NotificationEmitter {

    /**
     * The delegate for all notification management.
     */
    private NotificationBroadcasterSupport notifier = new NotificationBroadcasterSupport();

    private static MemoryMXBeanImpl instance = new MemoryMXBeanImpl();
    
    private static OperatingSystemMXBean osinstance = ExtendedOperatingSystem.getInstance();

    private List<MemoryManagerMXBean> memoryManagerList;

    /**
     * Constructor intentionally private to prevent instantiation by others.
     * Sets the metadata for this bean.
     */
    MemoryMXBeanImpl() {
        super(ManagementUtils.createObjectName(ManagementFactory.MEMORY_MXBEAN_NAME));
        memoryManagerList = new LinkedList<MemoryManagerMXBean>();
        createMemoryManagers();
    }

    @Override
    public javax.management.MBeanInfo getMBeanInfo() {
        if (info == null) {
            setMBeanInfo(ManagementUtils
                .getMBeanInfo(java.lang.management.MemoryMXBean.class.getName()));
        }
        return info;
    }
    
    /**
     * Singleton accessor method.
     * 
     * @return the <code>ClassLoadingMXBeanImpl</code> singleton.
     */
    static MemoryMXBeanImpl getInstance() {
        return instance;
    }

    /**
     * Instantiates MemoryManagerMXBean and GarbageCollectorMXBean instance(s)
     * for the current VM configuration and stores them in memoryManagerList.
     */
    private native void createMemoryManagers();

    private void createMemoryManagerHelper(String name, int internalID, boolean isGC) {
        if (isGC) {
            ObjectName objectName = ManagementUtils.createObjectName(
                ManagementFactory.GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE, name);
            memoryManagerList.add(new GarbageCollectorMXBeanImpl(objectName, name, internalID, this));
        } else {
            ObjectName objectName = ManagementUtils.createObjectName(
                ManagementFactory.MEMORY_MANAGER_MXBEAN_DOMAIN_TYPE, name);
            memoryManagerList.add(new MemoryManagerMXBeanImpl(objectName, name, internalID, this));
        }
    }
	
    /**
     * Retrieves the list of memory manager beans in the system.
     * 
     * @return the list of <code>MemoryManagerMXBean</code> instances
     */
    List<MemoryManagerMXBean> getMemoryManagerMXBeans() {
        return memoryManagerList;
    }
	
    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.MemoryMXBean#gc()
     */
    public void gc() {
        System.gc();
    }

    /**
     * @return an instance of {@link MemoryUsage}which can be interrogated by
     *         the caller.
     * @see #getHeapMemoryUsage()
     */
    private native MemoryUsage getHeapMemoryUsageImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.MemoryMXBean#getHeapMemoryUsage()
     */
    public MemoryUsage getHeapMemoryUsage() {
        return this.getHeapMemoryUsageImpl();
    }

    /**
     * @return an instance of {@link MemoryUsage}which can be interrogated by
     *         the caller.
     * @see #getNonHeapMemoryUsage()
     */
    private native MemoryUsage getNonHeapMemoryUsageImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.MemoryMXBean#getNonHeapMemoryUsage()
     */
    public MemoryUsage getNonHeapMemoryUsage() {
        return this.getNonHeapMemoryUsageImpl();
    }

    /**
     * @return the number of objects awaiting finalization.
     * @see #getObjectPendingFinalizationCount()
     */
    private native int getObjectPendingFinalizationCountImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.MemoryMXBean#getObjectPendingFinalizationCount()
     */
    public int getObjectPendingFinalizationCount() {
        return this.getObjectPendingFinalizationCountImpl();
    }

    /**
     * @return <code>true</code> if verbose output is being produced ;
     *         <code>false</code> otherwise.
     * @see #isVerbose()
     */
    private native boolean isVerboseImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.MemoryMXBean#isVerbose()
     */
    public boolean isVerbose() {
        return this.isVerboseImpl();
    }

    /**
     * @param value
     *            <code>true</code> enables verbose output ;
     *            <code>false</code> disables verbose output.
     * @see #setVerbose(boolean)
     */
    private native void setVerboseImpl(boolean value);

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.MemoryMXBean#setVerbose(boolean)
     */
    public void setVerbose(boolean value) {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(new ManagementPermission("control"));
        }
        this.setVerboseImpl(value);
    }

	/**
	 * @return value of -Xmx in bytes
	 */
	private native long getMaxHeapSizeLimitImpl();
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.lang.management.MemoryMXBean#getMaxHeapSizeLimit()
	 */
	public long getMaxHeapSizeLimit() {
		return this.getMaxHeapSizeLimitImpl();
	}

	/**
	 * @return current value of -Xsoftmx in bytes
	 */
	private native long getMaxHeapSizeImpl();

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.lang.management.MemoryMXBean#getMaxHeapSize()
	 */
	public long getMaxHeapSize() {
		return this.getMaxHeapSizeImpl();
	}

	/**
	 * @return value of -Xms in bytes
	 */
	private native long getMinHeapSizeImpl();

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.lang.management.MemoryMXBean#getMinHeapSize()
	 */
	public long getMinHeapSize() {
		return this.getMinHeapSizeImpl();
	}
	
	/**
	 * @param size new value for -Xsoftmx
	 */
	private native void setMaxHeapSizeImpl( long size );
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.lang.management.MemoryMXBean#setMaxHeapSize(long)
	 */
	public void setMaxHeapSize( long size ) {
		if( !this.isSetMaxHeapSizeSupported() ) {
			throw new UnsupportedOperationException();
		}
		if( size < this.getMinHeapSize() || size > this.getMaxHeapSizeLimit() ) {
			throw new IllegalArgumentException();
		}
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(new ManagementPermission("control"));
        }
		this.setMaxHeapSizeImpl( size );
	}

	/**
	 * @return true if setMaxHeapSize is supported by this VM
	 */
	private native boolean isSetMaxHeapSizeSupportedImpl();
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.lang.management.MemoryMXBean#isSetMaxHeapSizeSupported()
	 */
	public boolean isSetMaxHeapSizeSupported() {
		return this.isSetMaxHeapSizeSupportedImpl();
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
        MBeanNotificationInfo[] notifications = new MBeanNotificationInfo[1];
        String[] notifTypes = new String[2];
        notifTypes[0] = java.lang.management.MemoryNotificationInfo.MEMORY_THRESHOLD_EXCEEDED;
        notifTypes[1] = java.lang.management.MemoryNotificationInfo.MEMORY_COLLECTION_THRESHOLD_EXCEEDED;
        notifications[0] = new MBeanNotificationInfo(notifTypes,
                javax.management.Notification.class.getName(),
                "Memory Notification");
        return notifications;
    }

    /*
     * (non-Javadoc)
     * 
     * Send notifications to registered listeners. This will be called when
     * either of the following situations occur: <ol><li> With the method
     * {@link java.lang.management.MemoryPoolMXBean#isUsageThresholdSupported()}
     * returning <code> true </code> , a memory pool increases its size and, in
     * doing so, reaches or exceeds the usage threshold value. In this case the
     * notification type will be
     * {@link MemoryNotificationInfo#MEMORY_THRESHOLD_EXCEEDED}. <li> With the
     * method
     * {@link java.lang.management.MemoryPoolMXBean#isCollectionUsageThresholdSupported()}
     * returning <code> true </code> , a garbage-collected memory pool has
     * reached or surpassed the collection usage threshold value after a system
     * garbage collection has taken place. In this case the notification type
     * will be
     * {@link MemoryNotificationInfo#MEMORY_COLLECTION_THRESHOLD_EXCEEDED}.
     * </ol>
     * 
     * @param notification For this type of bean the user data will consist of a
     * {@link CompositeData}instance that represents a
     * {@link MemoryNotificationInfo}object.
     */
    public void sendNotification(Notification notification) {
        notifier.sendNotification(notification);
    }

    /* (non-Javadoc)
     * @see com.ibm.lang.management.MemoryMXBean#getMaxSharedClassCacheSize()
     */
    public long getSharedClassCacheSize() {
        return SharedClassStatistics.maxSizeBytes();
    }

    /* (non-Javadoc)
     * @see com.ibm.lang.management.MemoryMXBean#getSharedClassCacheFreeSpace()
     */
    public long getSharedClassCacheFreeSpace() {
        return SharedClassStatistics.freeSpaceBytes();
    }
	
    /* (non-Javadoc)
     * @see com.ibm.lang.management.MemoryMXBean#getGCMode()
     */
	public String getGCMode() {
		return getGCModeImpl();
	}
	
    public long getUsedPhysicalMemory() {
        return osinstance.getTotalPhysicalMemory()
                - osinstance.getFreePhysicalMemorySize();
    }
    
    public long getTotalPhysicalMemory() {
        return osinstance.getTotalPhysicalMemory();
    }
     
	private native String getGCModeImpl();

	/**
     * Returns the amount of CPU time spent in the GC by the master thread, in milliseconds.
     * 
     * @return CPU time used in milliseconds
     * @see #getGCMasterThreadCpuUsed()
     */
	private native long getGCMasterThreadCpuUsedImpl();

	/*
     * (non-Javadoc)
     * 
     * @see com.ibm.lang.management.MemoryMXBean#getGCMasterThreadCpuUsed()
     */
	public long getGCMasterThreadCpuUsed() 
	{
		return getGCMasterThreadCpuUsedImpl();
	}

	/**
     * Returns the amount of CPU time spent in the GC by all slave threads, in milliseconds.
     * 
     * @return CPU time used in milliseconds
     * @see #getGCSlaveThreadsCpuUsed()
     */
	private native long getGCSlaveThreadsCpuUsedImpl();

	/*
     * (non-Javadoc)
     * 
     * @see com.ibm.lang.management.MemoryMXBean#getGCSlaveThreadsCpuUsed()
     */
	public long getGCSlaveThreadsCpuUsed() 
	{
		return getGCSlaveThreadsCpuUsedImpl();
	}

	/**
     * Returns the maximum number of GC worker threads.
     * 
     * @return maximum number of GC worker threads
     * @see #getMaximumGCThreads()
     */
	private native int getMaximumGCThreadsImpl();

	/*
     * (non-Javadoc)
     * 
     * @see com.ibm.lang.management.MemoryMXBean#getMaximumGCThreads()
     */
	public int getMaximumGCThreads() 
	{
		return getMaximumGCThreadsImpl();
	}

	/**
     * Returns the number of GC slave threads that participated in the most recent collection.
     * 
     * @return number of active GC worker threads
     * @see #getCurrentGCThreads()
     */
	private native int getCurrentGCThreadsImpl();

	/*
     * (non-Javadoc)
     * 
     * @see com.ibm.lang.management.MemoryMXBean#getCurrentGCThreads()
     */
	public int getCurrentGCThreads() 
	{
		return getCurrentGCThreadsImpl();
	}
}

/*
 * $Log$
 * Revision 1.18  2005/08/19 22:41:19  pchurch
 * Tidy up comments and add some javadoc.
 *
 * Revision 1.17  2005/06/16 20:30:52  pschurch
 * add getGCMode API
 *
 * Revision 1.16  2005/05/31 10:04:30  gharley
 * New code for exposing shared cache information.
 *
 * Revision 1.15  2005/05/12 10:54:24  gharley
 * Belt and braces approach to ensuring the MBean metadata gets set correctly at construction time.
 *
 * Revision 1.14  2005/05/12 00:33:26  pschurch
 * initial release of dynamic heap resizing extensions
 *
 * Revision 1.13  2005/04/29 23:58:49  pschurch
 * Revised and extended memory management beans to support the native implementation.
 *
 * Revision 1.12  2005/02/11 17:24:36  gharley
 * Removed stubs with bogus values. Now making native calls...
 * Revision 1.11 2005/02/10 12:15:22 gharley
 * Moved invoke method into superclass
 * 
 * Revision 1.10 2005/02/09 22:24:16 gharley Moved getAttribute and setAttribute
 * into superclass
 * 
 * Revision 1.9 2005/02/04 23:13:55 gharley Added in security permission code :
 * either explicitly or added a comment where an invoked method carries out the
 * check on our behalf.
 * 
 * Revision 1.8 2005/02/02 14:09:58 gharley Moved MBeanInfo setup into
 * ManagementUtils on the assumption that the metadata is going to be useful for
 * the proxy bean support.
 * 
 * Revision 1.7 2005/01/24 14:28:54 gharley Some more notifications management
 * code.
 * 
 * Revision 1.6 2005/01/21 14:57:59 gharley Fixed a couple of bugs flushed out
 * by unit tests. DymanicMBean support code (again!)
 * 
 * Revision 1.5 2005/01/19 12:45:34 gharley Implementation of DynamicMBean and
 * notification emitter behaviours.
 * 
 * Revision 1.4 2005/01/16 22:33:12 gharley Added creation of MBeanInfo to the
 * constructor and returned it in the getMBeanInfo method. Seems to be a lot of
 * duplication here : some refactoring (common base class ?) required soon,
 * methinks.
 * 
 * Revision 1.3 2005/01/14 11:23:33 gharley Added singleton enforcement code.
 * 
 * Revision 1.2 2005/01/12 21:55:57 gharley Now extends
 * NotificationBroadcasterSupport (as this type of MXBean is an emitter) plus
 * implements DynamicMBean (as an MXBean should be a dynamic MBean).
 * 
 * Revision 1.1 2005/01/11 10:56:10 gharley Initial upload
 * 
 * Revision 1.1 2005/01/07 10:05:53 gharley Initial creation
 * 
 */
