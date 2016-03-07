/*
 * Licensed Materials - Property of IBM,
 * (c) Copyright IBM Corp. 2005, 2012  All Rights Reserved.
 */

package com.ibm.lang.management;

import javax.management.ObjectName;

/**
 * Runtime type for {@link java.lang.management.GarbageCollectorMXBean}. In
 * addition to implementing the &quot;standard&quot; management interface
 * <code>java.lang.management.GarbageCollectorMXBean</code>, this class also
 * provides an implementation of the IBM extension interface
 * <code>com.ibm.lang.management.GarbageCollectorMXBean</code>.
 * 
 * @author gharley
 * @since 1.5
 */
public final class GarbageCollectorMXBeanImpl extends MemoryManagerMXBeanImpl
        implements GarbageCollectorMXBean {

    /**
     * @param objectName The ObjectName of this bean
     * @param name The name of this collector
     * @param id An internal id number representing this collector
     * @param memBean The memory bean that receives notification events from pools managed by this collector
     */
    GarbageCollectorMXBeanImpl(ObjectName objectName, String name, int id, MemoryMXBeanImpl memBean) {
        super(objectName, name, id, memBean);
    }

    //This instance is only used for ManagementUtils
    private static GarbageCollectorMXBeanImpl tempInstance = new GarbageCollectorMXBeanImpl();
    
    // Only used by management utils
    GarbageCollectorMXBeanImpl() {
        super(null, null, -1, null);
    }
    
   //This method is only used for ManagementUtils
    static GarbageCollectorMXBeanImpl getInstanceFromMgmtUtils(){
    	return tempInstance;
    }
    /**
     * Sets the metadata for this bean.
     */
    protected void initializeInfo() {
        setMBeanInfo(ManagementUtils
                .getMBeanInfo(java.lang.management.GarbageCollectorMXBean.class
                        .getName()));
    }

    /**
     * @return the total number of garbage collections that have been carried
     *         out by the associated garbage collector.
     * @see #getCollectionCount()
     */
    private native long getCollectionCountImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.GarbageCollectorMXBean#getCollectionCount()
     */
    public long getCollectionCount() {
        return this.getCollectionCountImpl();
    }

    /**
     * @return the number of milliseconds that have been spent in performing
     *         garbage collection. This is a cumulative figure.
     * @see #getCollectionTime()
     */
    private native long getCollectionTimeImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.GarbageCollectorMXBean#getCollectionTime()
     */
    public long getCollectionTime() {
        return this.getCollectionTimeImpl();
    }

    /**
     * @return the start time of the most recent collection
     * @see #getLastCollectionStartTime()
     */
    private native long getLastCollectionStartTimeImpl();

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.lang.management.GarbageCollectorMXBean#getLastCollectionStartTime()
     */
    public long getLastCollectionStartTime() {
        return this.getLastCollectionStartTimeImpl();
    }

    /**
     * @return the end time of the most recent collection
     * @see #getLastCollectionEndTime()
     */
    private native long getLastCollectionEndTimeImpl();

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.lang.management.GarbageCollectorMXBean#getLastCollectionEndTime()
     */
    public long getLastCollectionEndTime() {
        return this.getLastCollectionEndTimeImpl();
    }

    /**
     * Returns the amount of heap memory used by objects that are managed
     * by the collector corresponding to this bean object.
     * 
     * @return memory used in bytes
     * @see #getMemoryUsed()
     */
    private native long getMemoryUsedImpl();

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.lang.management.GarbageCollectorMXBean#getMemoryUsed()
     */
    public long getMemoryUsed() {
        return this.getMemoryUsedImpl();
    }

    /**
     * Returns the cumulative total amount of memory freed, in bytes, by the
     * garbage collector corresponding to this bean object.
     *
     * @return memory freed in bytes
     * @see #getTotalMemoryFreed()
     */
    private native long getTotalMemoryFreedImpl();

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.lang.management.GarbageCollectorMXBean#getTotalMemoryFreed()
     */
    public long getTotalMemoryFreed() {
        return this.getTotalMemoryFreedImpl();
    }

    /**
     * Returns the cumulative total number of compacts that was performed by
     * garbage collector corresponding to this bean object.
     *
     * @return number of compacts performed
     * @see #getTotalCompacts()
     */
    private native long getTotalCompactsImpl();

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.lang.management.GarbageCollectorMXBean#getTotalCompacts()
     */
    public long getTotalCompacts() {
        return this.getTotalCompactsImpl();
    }
    
    public long getAllocatedHeapSizeTarget(){
        return MemoryMXBeanImpl.getInstance().getMaxHeapSizeLimit();
    }
    
    public void setAllocatedHeapSizeTarget(long size){
        MemoryMXBeanImpl.getInstance().setMaxHeapSize(size);
    }
    
    public String getStrategy(){
        return MemoryMXBeanImpl.getInstance().getGCMode();
    }
}

/*
 * $Log$
 * Revision 1.8  2005/04/29 23:58:49  pschurch
 * Revised and extended memory management beans to support the native implementation.
 *
 * Revision 1.7  2005/04/18 10:36:29  gharley
 * Removed getLastCollectionNumber at request of Paul Church.
 *
 * Revision 1.6  2005/03/13 22:44:18  gharley
 * Removed a couple of IBM extension methods that are better off going on the MemoryPoolMXBean interface.
 *
 * Revision 1.5  2005/02/14 13:22:55  gharley
 * Now implements com.ibm.lang.management.GarbageCollectorMXBean which, in turn, extends the standard java.lang.GarbageCollectorMXBean interface.
 * Revision 1.4 2005/02/14 12:12:03
 * gharley Now implements IBM platform interface
 * com.ibm.lang.management.GarbageCollectorMXBean. All new methods result in
 * native calls which have been declared here.
 * 
 * Revision 1.3 2005/02/13 18:18:35 gharley Initial implementation.
 * 
 * Revision 1.2 2005/01/12 21:56:26 gharley Now implements DynamicMBean (as an
 * MXBean should be a dynamic MBean). Revision 1.1 2005/01/11 10:56:10 gharley
 * Initial upload
 * 
 * Revision 1.1 2005/01/07 10:05:53 gharley Initial creation
 * 
 */
