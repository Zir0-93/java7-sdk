/*
 * Licensed Materials - Property of IBM,
 * (c) Copyright IBM Corp. 2005, 2012  All Rights Reserved.
 */

package com.ibm.lang.management;

import javax.management.openmbean.CompositeData;

/**
 * Encapsulates the details of a DLPAR notification emitted by a
 * {@link com.ibm.lang.management.OperatingSystemMXBean} when the total
 * physical memory changes.
 * Specifically, this notifiation indicates that the value returned by
 * {@link com.ibm.lang.management.OperatingSystemMXBean#getTotalPhysicalMemory()}
 * has changed.
 * 
 * @author gharley
 */
public class TotalPhysicalMemoryNotificationInfo {

    public static final String TOTAL_PHYSICAL_MEMORY_CHANGE = "com.ibm.management.total.physical.memory.change"; //$NON-NLS-1$

    private long newTotalPhysicalMemory;

    /**
     * Constructs a new instance of this object.
     * 
     * @param newTotalPhysicalMemory
     *            the new total bytes of physical memory
     */
    public TotalPhysicalMemoryNotificationInfo(long newTotalPhysicalMemory) {
        this.newTotalPhysicalMemory = newTotalPhysicalMemory;
    }

    /**
     * Returns the new value of bytes for the total physical memory after
     * the change that this notification corresponds to.
     * @return the new physical memory total in bytes
     */
    public long getNewTotalPhysicalMemory() {
        return this.newTotalPhysicalMemory;
    }

    /**
     * Receives a {@link CompositeData}representing a
     * <code>TotalPhysicalMemoryNotificationInfo</code> object and attempts to
     * return the root <code>TotalPhysicalMemoryNotificationInfo</code>
     * instance.
     * 
     * @param cd
     *            a <code>CompositeDate</code> that represents a
     *            <code>TotalPhysicalMemoryNotificationInfo</code>.
     * @return if <code>cd</code> is non- <code>null</code>, returns a new
     *         instance of <code>TotalPhysicalMemoryNotificationInfo</code>.
     *         If <code>cd</code> is <code>null</code>, returns
     *         <code>null</code>.
     * @throws IllegalArgumentException
     *             if argument <code>cd</code> does not correspond to a
     *             <code>TotalPhysicalMemoryNotificationInfo</code> with the
     *             following attribute:
     *             <ul>
     *             <li><code>newTotalPhysicalMemory</code>( <code>java.lang.Long</code>)
     *             </ul>
     */
    public static TotalPhysicalMemoryNotificationInfo from(CompositeData cd) {
        TotalPhysicalMemoryNotificationInfo result = null;

        if (cd != null) {
            // Does cd meet the necessary criteria to create a new
            // TotalPhysicalMemoryNotificationInfo ? If not then one of the
            // following method invocations will exit on an
            // IllegalArgumentException...
            ManagementUtils.verifyFieldNumber(cd, 1);
            String[] attributeNames = { "newTotalPhysicalMemory" }; //$NON-NLS-1$ 
            ManagementUtils.verifyFieldNames(cd, attributeNames);
            String[] attributeTypes = { "java.lang.Long" };
            ManagementUtils
                    .verifyFieldTypes(cd, attributeNames, attributeTypes); //$NON-NLS-1$

            // Extract the values of the attributes and use them to construct
            // a new TotalPhysicalMemoryNotificationInfo.
            Object[] attributeVals = cd.getAll(attributeNames);
            long memoryVal = ((Long) attributeVals[0]).longValue();
            result = new TotalPhysicalMemoryNotificationInfo(memoryVal);
        }// end if cd is not null
        return result;
    }
}

/*
 * $Log$
 * Revision 1.2  2005/08/19 22:41:19  pchurch
 * Tidy up comments and add some javadoc.
 *
 * Revision 1.1  2005/05/12 10:50:45  gharley
 * Initial version (added in order to expedite the wiring up of notifications for the extended OperatingSystemMXBean).
 *
 */
