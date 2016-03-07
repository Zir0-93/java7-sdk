/*
 * Licensed Materials - Property of IBM,
 * (c) Copyright IBM Corp. 2005, 2012  All Rights Reserved.
 */

package com.ibm.lang.management;

import javax.management.openmbean.CompositeData;

/**
 * Encapsulates the details of a DLPAR notification emitted by a
 * {@link com.ibm.lang.management.OperatingSystemMXBean} when the available
 * processing capacity changes.
 * Specifically, this notifiation indicates that the value returned by
 * {@link com.ibm.lang.management.OperatingSystemMXBean#getProcessingCapacity()}
 * has changed.
 * 
 * @author gharley
 * @since 1.5
 */
public class ProcessingCapacityNotificationInfo {

    public static final String PROCESSING_CAPACITY_CHANGE = "com.ibm.management.processing.capacity.change"; //$NON-NLS-1$

    private int newProcessingCapacity;

    /**
     * Constructs a new instance of this object.
     * 
     * @param newProcessingCapacity
     *            the new processing capacity in units of 1% of a physical
     *            processor's capacity
     */
    public ProcessingCapacityNotificationInfo(int newProcessingCapacity) {
        this.newProcessingCapacity = newProcessingCapacity;
    }

    /**
     * Returns the new processing capacity after the change that this
     * notification corresponds to.
     * 
     * @return the new processing capacity in units of 1% of a physical
     *         processor's capacity.
     */
    public int getNewProcessingCapacity() {
        return this.newProcessingCapacity;
    }

    /**
     * Receives a {@link CompositeData}representing a
     * <code>ProcessingCapacityNotificationInfo</code> object and attempts to
     * return the root <code>ProcessingCapacityNotificationInfo</code>
     * instance.
     * 
     * @param cd
     *            a <code>CompositeDate</code> that represents a
     *            <code>ProcessingCapacityNotificationInfo</code>.
     * @return if <code>cd</code> is non- <code>null</code>, returns a new
     *         instance of <code>ProcessingCapacityNotificationInfo</code>.
     *         If <code>cd</code> is <code>null</code>, returns
     *         <code>null</code>.
     * @throws IllegalArgumentException
     *             if argument <code>cd</code> does not correspond to a
     *             <code>ProcessingCapacityNotificationInfo</code> with the
     *             following attribute:
     *             <ul>
     *             <li><code>newProcessingCapacity</code>(
     *             <code>java.lang.Integer</code>)
     *             </ul>
     */
    public static ProcessingCapacityNotificationInfo from(CompositeData cd) {
        ProcessingCapacityNotificationInfo result = null;

        if (cd != null) {
            // Does cd meet the necessary criteria to create a new
            // ProcessingCapacityNotificationInfo ? If not then one of the
            // following method invocations will exit on an
            // IllegalArgumentException...
            ManagementUtils.verifyFieldNumber(cd, 1);
            String[] attributeNames = { "newProcessingCapacity" }; //$NON-NLS-1$ 
            ManagementUtils.verifyFieldNames(cd, attributeNames);
            String[] attributeTypes = { "java.lang.Integer" };
            ManagementUtils
                    .verifyFieldTypes(cd, attributeNames, attributeTypes); //$NON-NLS-1$

            // Extract the values of the attributes and use them to construct
            // a new ProcessingCapacityNotificationInfo.
            Object[] attributeVals = cd.getAll(attributeNames);
            int capacityVal = ((Integer) attributeVals[0]).intValue();
            result = new ProcessingCapacityNotificationInfo(capacityVal);
        }// end if cd is not null
        return result;
    }
}

/*
 * $Log$
 * Revision 1.3  2005/08/19 22:41:19  pchurch
 * Tidy up comments and add some javadoc.
 *
 * Revision 1.2  2005/05/20 21:41:43  pschurch
 * make getNewProcessingCapacity public
 *
 * Revision 1.1  2005/05/12 10:50:45  gharley
 * Initial version (added in order to expedite the wiring up of notifications for the extended OperatingSystemMXBean).
 *
 */
