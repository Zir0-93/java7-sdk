/*
 * Licensed Materials - Property of IBM,
 * (c) Copyright IBM Corp. 2005, 2012  All Rights Reserved.
 */

package java.lang.management;

/**
 * The management and monitoring interface for a virtual machine's memory
 * system.
 * <p>
 * Precisely one instance of this interface will be made available to management
 * clients.
 * </p>
 * <p>
 * Accessing this <code>MXBean</code> can be done in one of three ways. <br/>
 * <ol>
 * <li>Invoking the static {@link ManagementFactory#getMemoryMXBean}method.
 * </li>
 * <li>Using a {@link javax.management.MBeanServerConnection}.</li>
 * <li>Obtaining a proxy MXBean from the static
 * {@link ManagementFactory#newPlatformMXBeanProxy}method, passing in the
 * string &quot;java.lang:type=ClassLoading&quot; for the value of the second
 * parameter.</li>
 * </ol>
 * </p>
 * 
 * @author gharley
 */
public interface MemoryMXBean extends PlatformManagedObject {

    /**
     * Requests the virtual machine to run the system garbage collector.
     */
    public void gc();

    /**
     * Returns the current memory usage of the heap for both live objects and
     * for objects no longer in use which are awaiting garbage collection.
     * 
     * @return an instance of {@link MemoryUsage}which can be interrogated by
     *         the caller.
     */
    public MemoryUsage getHeapMemoryUsage();

    /**
     * Returns the current non-heap memory usage for the virtual machine.
     * 
     * @return an instance of {@link MemoryUsage}which can be interrogated by
     *         the caller.
     */
    public MemoryUsage getNonHeapMemoryUsage();

    /**
     * Returns the number of objects in the virtual machine that are awaiting
     * finalization. The returned value should only be used as an approximate
     * guide.
     * 
     * @return the number of objects awaiting finalization.
     */
    public int getObjectPendingFinalizationCount();

    /**
     * Returns a boolean indication of whether or not the memory system is
     * producing verbose output.
     * 
     * @return <code>true</code> if verbose output is being produced ;
     *         <code>false</code> otherwise.
     */
    public boolean isVerbose();

    /**
     * Updates the verbose output setting of the memory system.
     * 
     * @param value
     *            <code>true</code> enables verbose output ;
     *            <code>false</code> disables verbose output.
     * @throws SecurityException
     *             if a {@link SecurityManager}is being used and the caller
     *             does not have the <code>ManagementPermission</code> value
     *             of &quot;control&quot;.
     * @see ManagementPermission
     */
    public void setVerbose(boolean value);

}

/*
 * $Log$
 * Revision 1.1  2005/01/11 10:56:10  gharley
 * Initial upload
 *
 * Revision 1.1  2005/01/07 10:05:53  gharley
 * Initial creation
 *
 */
