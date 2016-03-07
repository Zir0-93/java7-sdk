/*
 * Licensed Materials - Property of IBM,
 * (c) Copyright IBM Corp. 2005, 2012  All Rights Reserved.
 */

package com.ibm.lang.management;


/**
 * The IBM-specific interface for managing and monitoring the virtual machine's
 * garbage collection functionality.
 * 
 * @author gharley
 * @since 1.5
 */
public interface GarbageCollectorMXBean extends
        java.lang.management.GarbageCollectorMXBean {

    /**
     * Returns the start time <em>in milliseconds</em> of the last garbage
     * collection that was carried out by this collector.
     * 
     * @return the start time of the most recent collection
     */
    public long getLastCollectionStartTime();

    /**
     * Returns the end time <em>in milliseconds</em> of the last garbage
     * collection that was carried out by this collector.
     * 
     * @return the end time of the most recent collection
     */
    public long getLastCollectionEndTime();

    /**
     * Returns the amount of heap memory used by objects that are managed
     * by the collector corresponding to this bean object.
     * 
     * @return memory used in bytes
     */
    public long getMemoryUsed();

    /**
     * Returns the cumulative total amount of memory freed, in bytes, by the
     * garbage collector corresponding to this bean object.
     * 
     * @return memory freed in bytes
     */
    public long getTotalMemoryFreed();

    /**
     * Returns the cumulative total number of compacts that was performed by
     * garbage collector corresponding to this bean object.
     * 
     * @return number of compacts performed
     */
    public long getTotalCompacts();
}

/*
 * $Log$
 * Revision 1.4  2005/04/18 10:36:13  gharley
 * Removed getLastCollectionNumber at request of Paul Church.
 *
 * Revision 1.3  2005/03/13 22:44:10  gharley
 * Removed a couple of IBM extension methods that are better off going on the MemoryPoolMXBean interface.
 *
 * Revision 1.2  2005/02/14 13:20:48  gharley
 * D'oh ! The extension interface ought to extend the standard interface.
 * Revision 1.1 2005/02/14 12:10:06
 * gharley Initial implementation.
 * 
 */
