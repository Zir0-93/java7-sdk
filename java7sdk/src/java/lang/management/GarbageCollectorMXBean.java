/*
 * Licensed Materials - Property of IBM,
 * (c) Copyright IBM Corp. 2005, 2012  All Rights Reserved.
 */

package java.lang.management;

/**
 * The interface for managing and monitoring the virtual machine's garbage
 * collection functionality.
 * <p>
 * Multiple instances of this interface are available to clients. Each may be
 * distinguished by their separate <code>ObjectName</code> value.
 * </p>
 * <p>
 * Accessing this kind of <code>MXBean</code> can be done in one of three
 * ways. <br/>
 * <ol>
 * <li>Invoking the static
 * {@link ManagementFactory#getGarbageCollectorMXBeans()}method which returns a
 * {@link java.util.List}of all currently instantiated GarbageCollectorMXBeans.
 * </li>
 * <li>Using a {@link javax.management.MBeanServerConnection}.</li>
 * <li>Obtaining a proxy MXBean from the static
 * {@link ManagementFactory#newPlatformMXBeanProxy}method, passing in the
 * string &quot;java.lang:type=GarbageCollector,name= <i>unique collector's name
 * </i>&quot; for the value of the second parameter.</li>
 * </ol>
 * </p>
 * 
 * @since 1.5
 * @author gharley
 */
public interface GarbageCollectorMXBean extends MemoryManagerMXBean {

    /**
     * Returns in a long the number of garbage collections carried out by the
     * associated collector.
     * 
     * @return the total number of garbage collections that have been carried
     *         out by the associated garbage collector.
     */
    public long getCollectionCount();

    /**
     * For the associated garbage collector, returns the total amount of time in
     * milliseconds that it has spent carrying out garbage collection.
     * 
     * @return the number of milliseconds that have been spent in performing
     *         garbage collection. This is a cumulative figure.
     */
    public long getCollectionTime();

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
