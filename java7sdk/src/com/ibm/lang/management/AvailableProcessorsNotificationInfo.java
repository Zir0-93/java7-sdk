/*
 * Licensed Materials - Property of IBM,
 * (c) Copyright IBM Corp. 2005, 2012  All Rights Reserved.
 */

package com.ibm.lang.management;

import javax.management.openmbean.CompositeData;

/**
 * Encapsulates the details of a DLPAR notification emitted by a
 * {@link com.ibm.lang.management.OperatingSystemMXBean} when the number
 * of available processors changes.
 * Specifically, this notifiation indicates that the value returned by
 * {@link java.lang.management.OperatingSystemMXBean#getAvailableProcessors()}
 * has changed.
 * 
 * @author gharley
 * @since 1.5
 */
public class AvailableProcessorsNotificationInfo {

    public static final String AVAILABLE_PROCESSORS_CHANGE = "com.ibm.management.available.processors.change"; //$NON-NLS-1$

    private int newAvailableProcessors;

    /**
     * Constructs a new instance of this object.
     * 
     * @param newAvailableProcessors
     *            the new number of processors available
     */
    public AvailableProcessorsNotificationInfo(int newAvailableProcessors) {
        this.newAvailableProcessors = newAvailableProcessors;
    }

    /**
     * Returns the new number of available processors after the change that
     * initiated this notification.
     * 
     * @return the number of available processors
     */
    public int getNewAvailableProcessors() {
        return this.newAvailableProcessors;
    }

    /**
     * Receives a {@link CompositeData}representing a
     * <code>AvailableProcessorsNotificationInfo</code> object and attempts to
     * return the root <code>AvailableProcessorsNotificationInfo</code>
     * instance.
     * 
     * @param cd
     *            a <code>CompositeDate</code> that represents a
     *            <code>AvailableProcessorsNotificationInfo</code>.
     * @return if <code>cd</code> is non- <code>null</code>, returns a new
     *         instance of <code>AvailableProcessorsNotificationInfo</code>.
     *         If <code>cd</code> is <code>null</code>, returns
     *         <code>null</code>.
     * @throws IllegalArgumentException
     *             if argument <code>cd</code> does not correspond to a
     *             <code>AvailableProcessorsNotificationInfo</code> with the
     *             following attribute:
     *             <ul>
     *             <li><code>newAvailableProcessors</code>(
     *             <code>java.lang.Integer</code>)
     *             </ul>
     */
    public static AvailableProcessorsNotificationInfo from(CompositeData cd) {
        AvailableProcessorsNotificationInfo result = null;

        if (cd != null) {
            // Does cd meet the necessary criteria to create a new
            // AvailableProcessorsNotificationInfo ? If not then one of the
            // following method invocations will exit on an
            // IllegalArgumentException...
            ManagementUtils.verifyFieldNumber(cd, 1);
            String[] attributeNames = { "newAvailableProcessors" }; //$NON-NLS-1$ 
            ManagementUtils.verifyFieldNames(cd, attributeNames);
            String[] attributeTypes = { "java.lang.Integer" };
            ManagementUtils
                    .verifyFieldTypes(cd, attributeNames, attributeTypes); //$NON-NLS-1$

            // Extract the values of the attributes and use them to construct
            // a new AvailableProcessorsNotificationInfo.
            Object[] attributeVals = cd.getAll(attributeNames);
            int availableProcessorsVal = ((Integer) attributeVals[0])
                    .intValue();
            result = new AvailableProcessorsNotificationInfo(
                    availableProcessorsVal);
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
