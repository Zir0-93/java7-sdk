/*
 * Licensed Materials - Property of IBM,
 * (c) Copyright IBM Corp. 2005, 2012  All Rights Reserved.
 */

package com.ibm.lang.management;

/**
 * The IBM-specific interface for monitoring the virtual machine's memory
 * management system.
 * 
 * @since 1.5
 * @author pchurch
 */
public interface MemoryMXBean extends java.lang.management.MemoryMXBean {

	/**
	 * Get the maximum size in bytes to which the max heap size could be
	 * increased in the currently running VM. This may be larger than the
	 * current max heap size.
	 * 
	 * @return value of -Xmx in bytes
	 */
	public long getMaxHeapSizeLimit();
	
	/**
	 * Get the current maximum heap size in bytes.
	 * 
	 * @return current value of -Xsoftmx in bytes
	 */
	public long getMaxHeapSize();
	
	/**
	 * Get the minimum heap size in bytes.
	 * 
	 * @return value of -Xms in bytes
	 */
	public long getMinHeapSize();
	
	/**
	 * Set the current maximum heap size to <code>size</code>.
	 * The parameter specifies the max heap size in bytes and must be
	 * between getMinHeapSize() and getMaxHeapSizeLimit().
	 * See -Xsoftmx in the command line reference for additional
	 * details on the effect of setting softmx. 
	 * 
	 * @param size new -Xsoftmx value in bytes
     * @throws UnsupportedOperationException
     *             if this operation is not supported.
     * @throws IllegalArgumentException
     *             if input value <code>size</code> is either less than
     *             getMinHeapSize() or greater than getMaxHeapSizeLimit().
     * @throws SecurityException
     *             if a {@link SecurityManager}is being used and the caller
     *             does not have the <code>ManagementPermission</code> value
     *             of "control".
	 */
	public void setMaxHeapSize( long size );
	
	/**
	 * Query whether the VM supports runtime reconfiguration of the
	 * maximum heap size through the setMaxHeapSize() call.
	 * 
	 * @return true if setMaxHeapSize is supported, false otherwise
	 */
	public boolean isSetMaxHeapSizeSupported();
    
    /**
     * Returns the total size in bytes of the cache that the JVM is currently
     * connected to.
     * 
     * @return the number of bytes in the shared class cache.
     */
    public long getSharedClassCacheSize();
    
    
    /**
     * Returns the <b>free space</b> in bytes of the cache that the JVM is
     * currently connected to.
     * 
     * @return the number of bytes free in the shared class cache.
     */
    public long getSharedClassCacheFreeSpace();
	
	/**
	 * Returns the current GC mode as a human-readable string.  
	 * 
	 * @return a String describing the mode the GC is currently operating in
	 */
	public String getGCMode();

	/**
     * Returns the amount of CPU time spent in the GC by the master thread, in milliseconds.
     * 
     * @return CPU time used in milliseconds
     */
	public long getGCMasterThreadCpuUsed();

	/**
     * Returns the total amount of CPU time spent in the GC by all slave threads, in milliseconds.
     * 
     * @return CPU time used in milliseconds
     */
	public long getGCSlaveThreadsCpuUsed();

	/**
     * Returns the maximum number of GC worker threads.
     * 
     * @return maximum number of GC worker threads
     */
	public int getMaximumGCThreads();

	/**
     * Returns the number of GC worker threads that participated in the most recent collection.
     * 
     * @return number of active GC worker threads
     */
	public int getCurrentGCThreads();
}

/*
 * $Log$
 * Revision 1.4  2005/06/16 20:30:52  pschurch
 * add getGCMode API
 *
 * Revision 1.3  2005/05/31 10:04:30  gharley
 * New code for exposing shared cache information.
 *
 * Revision 1.2  2005/05/12 10:52:41  gharley
 * Pathetically pedantic tweak to the file header and footer comment blocks.
 *
 */
