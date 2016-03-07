/*
 * Licensed Materials - Property of IBM,
 * (c) Copyright IBM Corp. 2005, 2012  All Rights Reserved.
 */

package com.ibm.lang.management;

import java.lang.management.MemoryUsage;

/**
 * The IBM-specific interface for managing and monitoring the virtual machine's
 * memory pools.
 * 
 * @author gharley
 * @since 1.5
 */
public interface MemoryPoolMXBean extends java.lang.management.MemoryPoolMXBean {
    
    /**
     * If supported by the virtual machine, returns a {@link MemoryUsage}which
     * encapsulates this memory pool's memory usage <em>before</em> the most
     * recent run of the garbage collector. No garbage collection will be
     * actually occur as a result of this method getting called.
     * <p>
     * The method will return a <code>null</code> if the virtual machine does
     * not support this type of functionality.
     * </p>
     * <h4>MBeanServer access:</h4>
     * The return value will be mapped to a
     * {@link javax.management.openmbean.CompositeData} with attributes as
     * specified in {@link MemoryUsage}.
     * 
     * @return a {@link MemoryUsage} containing the usage details for the memory
     *         pool just before the most recent collection occurred.
     */
    public MemoryUsage getPreCollectionUsage();
}

/*
 * $Log$
 * Revision 1.1  2005/03/13 22:42:20  gharley
 * New IBM extension interface on MemoryPoolMXBean. Initial version.
 *
 */
