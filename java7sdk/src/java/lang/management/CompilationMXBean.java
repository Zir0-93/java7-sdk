/*
 * Licensed Materials - Property of IBM,
 * (c) Copyright IBM Corp. 2005, 2012  All Rights Reserved.
 */

package java.lang.management;

/**
 * The management and monitoring interface for the virtual machine's compilation
 * functionality.
 * <p>
 * If the virtual machine has a compilation system enabled, precisely one
 * instance of this interface will be made available to management clients.
 * Otherwise, there will be no instances of this <code>MXBean</code> available.
 * </p>
 * <p>
 * Accessing this <code>MXBean</code> can be done in one of three ways. <br/>
 * <ol>
 * <li>Invoking the static ManagementFactory.getCompilationMXBean() method.
 * </li>
 * <li>Using a javax.management.MBeanServerConnection.</li>
 * <li>Obtaining a proxy MXBean from the static
 * {@link ManagementFactory#newPlatformMXBeanProxy}
 * method, passing in the string &quot;java.lang:type=Compilation&quot; for
 * the value of the second parameter.
 * </li>
 * </ol>
 * </p>
 * 
 * @author gharley
 */
public interface CompilationMXBean extends PlatformManagedObject {

    /**
     * Returns the name of the virtual machine's Just In Time (JIT) compiler.
     * 
     * @return the name of the JIT compiler
     */
    public String getName();

    /**
     * If supported (see {@link #isCompilationTimeMonitoringSupported()}),
     * returns the total number of <b>milliseconds </b> spent by the virtual
     * machine performing compilations. The figure is taken over the lifetime of
     * the virtual machine.
     * 
     * @return the compilation time in milliseconds
     * @throws java.lang.UnsupportedOperationException
     *             if the virtual machine does not support compilation
     *             monitoring. This can be tested by calling the
     *             {@link #isCompilationTimeMonitoringSupported()}method.
     */
    public long getTotalCompilationTime();

    /**
     * A boolean indication of whether or not the virtual machine supports the
     * timing of its compilation facilities.
     * 
     * @return <code>true</code> if compilation timing is supported, otherwise
     *         <code>false</code>.
     */
    public boolean isCompilationTimeMonitoringSupported();

}

/*
 * $Log$
 * Revision 1.2  2005/01/14 11:25:26  gharley
 * Slight comment enhancement.
 *
 * Revision 1.1  2005/01/11 10:56:10  gharley
 * Initial upload
 *
 * Revision 1.1  2005/01/07 10:05:53  gharley
 * Initial creation
 *
 */
