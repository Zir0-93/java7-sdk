/*
 * Licensed Materials - Property of IBM,
 * (c) Copyright IBM Corp. 2005, 2012  All Rights Reserved.
 */

package java.lang.management;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

import com.ibm.lang.management.ManagementUtils;

/**
 * Represents the memory usage of either a memory pool or the heap and non-heap
 * memory areas of the virtual machine.
 * 
 * @author gharley
 * @since 1.5
 */
public class MemoryUsage {

    // TODO : Candidate for removal ?
    private static CompositeType REF_MEM_USG_CTYPE = createMemoryUsageCompositeTypeObject();

    /**
     * The initial amount of memory requested from the underlying operating
     * system when the virtual machine started up. The value is in bytes.
     */
    private long init;

    /**
     * The amount of memory currently in use. The value is in bytes.
     */
    private long used;

    /**
     * The amount of memory that the virtual machine can currently assume is
     * available for it to use. It is possible that this value may change over
     * the lifetime of the virtual machine. The value is in bytes.
     */
    private long committed;

    /**
     * The maximum amount of memory that is available to the virtual machine.
     * May change over the lifecycle of the virtual machine. The value is in
     * bytes.
     */
    private long max;

    private String TOSTRING_VALUE;

    /**
     * Constructs a new <code>MemoryUsage</code> instance.
     * 
     * @param init
     *            if defined, the initial amount of memory that can be allocated
     *            by the virtual machine in bytes. If not defined, then
     *            <code>-1</code>.
     * @param used
     *            the number of bytes currently used for memory.
     * @param committed
     *            the number of bytes of committed memory.
     * @param max
     *            if defined, the maximum number of bytes that can be used for
     *            memory management purposes. If not defined, <code>-1</code>.
     * @throws IllegalArgumentException
     *             if any of the following conditions applies:
     *             <ul>
     *             <li><code>init</code> &lt; <code>-1</code>
     *             <li><code>max</code> &lt; <code>-1</code>
     *             <li><code>used</code> &lt; <code>0</code>
     *             <li><code>committed</code> &lt; <code>0</code>
     *             <li><code>used</code> &gt; <code>committed</code>
     *             <li><code>committed</code> &gt; <code>max</code> if
     *             <code>max</code> is not <code>-1</code>.
     *             </ul>
     */
    public MemoryUsage(long init, long used, long committed, long max) {
        // Validate inputs
        if (init < -1) {
            throw new IllegalArgumentException(
                    "init argument cannot be less than -1");
        }

        if (max < -1) {
            throw new IllegalArgumentException(
                    "max argument cannot be less than -1");
        }

        if (used < 0) {
            throw new IllegalArgumentException(
                    "used argument cannot be less than 0");
        }

        if (committed < 0) {
            throw new IllegalArgumentException(
                    "committed argument cannot be less than 0");
        }

        if (used > committed) {
            throw new IllegalArgumentException(
                    "used value cannot be larger than the committed value");
        }

        if ((max != -1) && (committed > max)) {
            throw new IllegalArgumentException(
                    "committed value cannot be larger than the max value");
        }

        this.init = init;
        this.used = used;
        this.committed = committed;
        this.max = max;
    }

    /**
     * Returns the amount of memory that has been pledged by the operating
     * system for the virtual machine to use. This value is in bytes.
     * 
     * @return the number of bytes committed to memory.
     */
    public long getCommitted() {
        return this.committed;
    }

    /**
     * Returns the initial amount of memory requested from the underlying
     * operating system when the virtual machine started up. The value is given
     * in bytes.
     * <p>
     * if the initial memory size was not defined, this method will return a
     * value of <code>-1</code>.
     * </p>
     * 
     * @return the initial amount of memory requested at virtual machine start
     *         up. <code>-1</code> if not defined.
     */
    public long getInit() {
        return this.init;
    }

    /**
     * Returns the maximum amount of memory that is available to the virtual
     * machine which may change over the lifecycle of the virtual machine. The
     * value is in bytes.
     * 
     * @return if defined, the maximum memory size in bytes. If not defined,
     *         <code>-1</code>.
     */
    public long getMax() {
        return this.max;
    }

    /**
     * Returns the number of bytes currently used for memory management
     * purposes.
     * 
     * @return the current number of bytes used for memory.
     */
    public long getUsed() {
        return this.used;
    }

    /**
     * Returns a text description of this memory usage.
     * 
     * @return a text description of this memory usage.
     */
    public String toString() {
        // Since MemoryUsages are immutable the string value need only be
        // calculated the one time
        if (TOSTRING_VALUE == null) {
            StringBuilder buff = new StringBuilder();
            buff.append("init = ");
            buff.append(this.init);
            appendSizeInKBytes(buff, this.init);

            buff.append("used = ");
            buff.append(this.used);
            appendSizeInKBytes(buff, this.used);

            buff.append("committed = ");
            buff.append(this.committed);
            appendSizeInKBytes(buff, this.committed);

            buff.append("max = ");
            buff.append(this.max);
            appendSizeInKBytes(buff, this.max);
            TOSTRING_VALUE = buff.toString().trim();
        }
        return TOSTRING_VALUE;
    }

    /**
     * Convenience method which takes a {@link StringBuilder} and appends to it
     * the <code>long</code> value expressed as an integral number of
     * kilobytes.
     * <p>
     * If <code>value</code> is <code>-1</code> then the appended text will
     * comprise of the word &quot;undefined&quot;.
     * </p>
     * 
     * @param buff
     *            an existing <code>StringBuilder</code> that will be updated
     *            with <code>value</code> expressed in kilobytes.
     * @param value
     *            a <code>long</code> value.
     */
    private static void appendSizeInKBytes(StringBuilder buff, long value) {
        // it would be better to write "undefined" for values of -1 but in 
        // order to do as the RI does...
        if (value == -1) {
            buff.append("(-1K) ");
        } else {
            buff.append("(" + (value / 1024) + "K) ");
        }
    }

    /**
     * Receives a {@link CompositeData}representing a <code>MemoryUsage</code>
     * object and attempts to return the root <code>MemoryUsage</code>
     * instance.
     * 
     * @param cd
     *            a <code>CompositeDate</code> that represents a
     *            <code>MemoryUsage</code>.
     * @return if <code>cd</code> is non- <code>null</code>, returns a new
     *         instance of <code>MemoryUsage</code>. If <code>cd</code> is
     *         <code>null</code>, returns <code>null</code>.
     * @throws IllegalArgumentException
     *             if argument <code>cd</code> does not correspond to a
     *             <code>MemoryUsage</code> with the following attributes all
     *             of type <code>java.long.Long</code>:
     *             <ul>
     *             <li><code>committed</code>
     *             <li><code>init</code>
     *             <li><code>max</code>
     *             <li><code>used</code>
     *             </ul>
     */
    public static MemoryUsage from(CompositeData cd) {
        MemoryUsage result = null;

        if (cd != null) {
            // Does cd meet the necessary criteria to create a new
            // MemoryUsage ? If not then exit on an IllegalArgumentException
            // TODO : Use this method ? verifyType(cd);

            ManagementUtils.verifyFieldNumber(cd, 4);
            String[] attributeNames = { "init", "used", "committed", "max" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            ManagementUtils.verifyFieldNames(cd, attributeNames);
            String[] attributeTypes = { "java.lang.Long", "java.lang.Long",
                    "java.lang.Long", "java.lang.Long" };
            ManagementUtils
                    .verifyFieldTypes(cd, attributeNames, attributeTypes); //$NON-NLS-1$

            // Extract the values of the attributes and use them to construct
            // a new MemoryUsage.
            Object[] attributeVals = cd.getAll(attributeNames);
            long initVal = ((Long) attributeVals[0]).longValue();
            long usedVal = ((Long) attributeVals[1]).longValue();
            long committedVal = ((Long) attributeVals[2]).longValue();
            long maxVal = ((Long) attributeVals[3]).longValue();
            result = new MemoryUsage(initVal, usedVal, committedVal, maxVal);
        }// end if cd is not null
        return result;
    }

    /**
     * @param cd
     *            a <code>CompositeDataType</code>
     * @throws IllegalArgumentException
     *             if <code>cd</code> is incompatibe with the expected
     *             <code>CompositeDataType</code>.
     */
    private static void verifyType(CompositeData cd) {
        if (!REF_MEM_USG_CTYPE.isValue(cd)) {
            throw new IllegalArgumentException(
                    "CompositeData object is not of the expected type.");
        }
    }

    // TODO : Candidate for removal ?
    /**
     * Returns a new instance of {@link CompositeType} which is describes a
     * <code>MemoryUsage</code> object.
     * 
     * @return a new <code>CompositeType</code>
     */
    private static CompositeType createMemoryUsageCompositeTypeObject() {
        CompositeType result = null;
        String[] typeNames = { "init", "used", "committed", "max" };
        String[] typeDescs = { "init-desc", "used-desc", "committed-dec",
                "max-desc" };
        OpenType[] typeTypes = { SimpleType.LONG, SimpleType.LONG,
                SimpleType.LONG, SimpleType.LONG };
        try {
            result = new CompositeType("MemoryUsageType", "desc", typeNames,
                    typeDescs, typeTypes);
        } catch (OpenDataException e) {
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }// end if
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!(obj instanceof MemoryUsage)) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        // Safe to cast if the instanceof test above was passed.
        MemoryUsage mu = (MemoryUsage) obj;
        if (!(mu.getInit() == this.getInit())) {
            return false;
        }
        if (!(mu.getCommitted() == this.getCommitted())) {
            return false;
        }
        if (!(mu.getUsed() == this.getUsed())) {
            return false;
        }
        if (!(mu.getMax() == this.getMax())) {
            return false;
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return new String(Long.toString(this.getCommitted())
                + Long.toString(this.getInit()) + Long.toString(this.getMax())
                + Long.toString(this.getUsed())).hashCode();
    }
}

/*
 * $Log$
 * Revision 1.6  2006/06/16 15:42:22  gharley
 * Cache toString() return value and, while we are in there, make the return look like the equivalent on the RI
 * Revision 1.5 2005/06/13 10:02:03 gharley Only
 * inform user of non-fatal exceptions if the system property
 * "com.ibm.lang.management" is set.
 * 
 * Revision 1.4 2005/05/25 20:23:36 gharley Updated unit test bucket now that
 * 5.0 VM available that enables direct testing of the MXBeans. Shook out one or
 * two bugs in the code. Note that OperatingSystemMXBeans are still broken &
 * that there is a problem with MemoryMXBean.getNonHeapmemoryUsage(). Awaiting
 * new VM before tackling these...
 * 
 * Revision 1.3 2005/05/23 11:50:24 gharley Corrected toString() method to give
 * size in KB for each field.
 * 
 * Revision 1.2 2005/02/09 22:19:06 gharley Implemented equals() and hashCode()
 * methods. Revision 1.1 2005/01/11 10:56:10 gharley Initial upload
 * 
 * Revision 1.1 2005/01/07 10:05:53 gharley Initial creation
 * 
 */
