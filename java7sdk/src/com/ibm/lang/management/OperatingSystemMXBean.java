/*
 * Licensed Materials - Property of IBM,
 * (c) Copyright IBM Corp. 2005, 2013  All Rights Reserved.
 */

package com.ibm.lang.management;

/**
 * IBM specific platform management interface for the Operating System on which the Java Virtual Machine is running. 
 * <p>
 * Usage example for the {@link com.ibm.lang.management.OperatingSystemMXBean}
 * <table border="1">
 * <tr> <td> <pre>
 * <small>
 * {@code
 *  ...
 *     com.ibm.lang.management.OperatingSystemMXBean osmxbean = null;
 *    osmxbean = (com.ibm.lang.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
 *   ...
 * }
 * </small>
 * </pre></td></tr></table>
 * <p>
 *
 * @since 1.5
 */
public interface OperatingSystemMXBean extends
        java.lang.management.OperatingSystemMXBean {

    /**
     * Returns the total available physical memory on the system in bytes.
     * 
     * @return the total available physical memory on the system in bytes.
     */
    public long getTotalPhysicalMemory();

    /**
     * Returns the collective capacity of the virtual processors in
     * the partition the VM is running in. The value returned is in
     * units of 1% of a physical processor's capacity, so a value of
     * 100 is equal to 1 physical processor. In environments without
     * such partitioning support, this call will return
     * getAvailableProcessors() * 100.
     * 
     * @return the collective capacity of the virtual processors available
     * to the VM.
     */
    public int getProcessingCapacity();

    /**
     * Returns total amount of time the process has been scheduled or
     * executed so far in both kernel and user modes. Returns -1 if the
     * value is unavailable on this platform or in the case of an error.
     *
     * @return process cpu time in 100 ns units.
     */
    public long getProcessCpuTime();
    
    /**
     * Returns total amount of time the process has been scheduled or executed 
     * so far in both kernel and user modes (in nano seconds). Returns -1 if the
     * value is unavailable on this platform or in the case of
     * an error.
     *
     * @return process cpu time in ns units.
     */
    public long getProcessCpuTimeByNS();

    /**
     * Returns the "recent cpu usage" for the whole system. This value is a double in
     * the [0.0,1.0] interval. A value of 0.0 means all CPUs were idle in the recent
     * period of time observed, while a value of 1.0 means that all CPUs were actively
     * running 100% of the time during the recent period of time observed. All values
     * between 0.0 and 1.0 are possible. The first call to the method always 
     * returns {@link com.ibm.lang.management.CpuLoadCalculationConstants}.ERROR_VALUE
     * (-1.0), which marks the starting point. If the Java Virtual Machine's recent CPU
     * usage is not available, the method returns a negative error code from
     * {@link com.ibm.lang.management.CpuLoadCalculationConstants}.
     * <p>getSystemCpuLoad might not return the same value that is reported by operating system
     * utilities such as Unix "top" or Windows task manager.</p>
     *  
     * @return A value between 0 and 1.0, or a negative error code from
     *         {@link com.ibm.lang.management.CpuLoadCalculationConstants} in case
     *         of an error. On the first call to the API,
     *         {@link com.ibm.lang.management.CpuLoadCalculationConstants}.ERROR_VALUE
     *         (-1.0) shall be returned.
     * <ul>
     * <li>Because this information is not available on z/OS, the call returns
     * {@link com.ibm.lang.management.CpuLoadCalculationConstants}.UNSUPPORTED_VALUE
     * (-3.0).
     * </ul>
     * @see CpuLoadCalculationConstants
     */

    public double getSystemCpuLoad();

    /**
     * Returns the amount of physical memory that is available on the system in bytes. 
     * Returns -1 if the value is unavailable on this platform or in the case of an error.
     * <ul>
     * <li>This information is not available on the z/OS platform.
     * </ul>
     *
     * @return amount of physical memory available in bytes.
     */
    public long getFreePhysicalMemorySize();

    /**
     * Returns the amount of virtual memory used by the process in bytes.
     * Returns -1 if the value is unavailable on this platform or in the
     * case of an error.
     * <ul>
     * <li>This information is not available on the z/OS platform.
     * </ul>
     *
     * @return amount of virtual memory used by the process in bytes.
     */
    public long getProcessVirtualMemorySize();

    /**
     * Returns the amount of private memory used by the process in bytes.
     * Returns -1 if the value is unavailable on this platform or in the
     * case of an error.
     * <ul>
     * <li>This information is not available on the z/OS platform.
     * </ul>
     *
     * @return amount of private memory used by the process in bytes.
     */
    public long getProcessPrivateMemorySize();

    /**
     * Returns the amount of physical memory being used by the process
     * in bytes. Returns -1 if the value is unavailable on this platform
     * or in the case of an error.
     * <ul>
     * <li>This information is not available on the AIX and z/OS platforms.
     * </ul>
     *
     * @return amount of physical memory being used by the process in bytes.
     */
    public long getProcessPhysicalMemorySize();

    /**
     * Returns the total amount of swap space in bytes.
     *
     * @return the total amount of swap space in bytes.
     * @since   1.7
     */
    public long getTotalSwapSpaceSize();

    /**
     * Returns the amount of free swap space in bytes.
     *
     * @return the amount of free swap space in bytes.
     * @since   1.7
     */
    public long getFreeSwapSpaceSize();

    /**
     * Returns the "recent cpu usage" for the Java Virtual Machine process.
     * This value is a double in the [0.0,1.0] interval. A value of 0.0 means
     * that none of the CPUs were running threads from the JVM process during
     * the recent period of time observed, while a value of 1.0 means that all
     * CPUs were actively running threads from the JVM 100% of the time
     * during the recent period of time observed. Threads from the JVM include
     * application threads as well as JVM internal threads. All values
     * between 0.0 and 1.0 are possible. The first call to the method always 
     * returns {@link com.ibm.lang.management.CpuLoadCalculationConstants}.ERROR_VALUE
     * (-1.0), which marks the starting point. If the Java Virtual Machine's recent CPU
     * usage is not available, the method returns a negative error code from
     * {@link com.ibm.lang.management.CpuLoadCalculationConstants}.
     *
     * @return A value between 0 and 1.0, or a negative error code from
     *         {@link com.ibm.lang.management.CpuLoadCalculationConstants} in case
     *         of an error. On the first call to the API,
     *         {@link com.ibm.lang.management.CpuLoadCalculationConstants}.ERROR_VALUE
     *         (-1.0) shall be returned.
     * <ul>
     * <li>Because this information is not available on z/OS, the call returns
     * {@link com.ibm.lang.management.CpuLoadCalculationConstants}.UNSUPPORTED_VALUE
     * (-3.0).
     * </ul>
     * @see CpuLoadCalculationConstants
     * @since   1.7
     */
    public double getProcessCpuLoad();

}

/*
 * $Log$
 * Revision 1.5  2005/05/12 00:34:50  pschurch
 * add getProcessingCapacity
 *
 * Revision 1.4  2005/04/18 10:39:42  gharley
 * Removal of all attributes bar PhysicalMemory from the proprietary interface. Agreed with Paul Church and Tim Preece.
 *
 * Revision 1.3  2005/02/24 16:47:35  gharley
 * Incorrect method name. Replaced getCommittedPhysicalMemory with the
 * method called getCommittedVirtualMemory
 *
 * Revision 1.2  2005/02/14 13:21:05  gharley
 * D'oh ! The extension interface ought to extend the standard interface.
 * Revision 1.1 2005/02/14 12:10:18 gharley
 * Initial implementation.
 * 
 */
